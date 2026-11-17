package com.dayloop.app.feature.wardrobe

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private const val CAPTURE_DIRECTORY = "wardrobe_capture"
private const val STALE_CAPTURE_MILLIS = 24L * 60 * 60 * 1000

private fun captureFile(context: Context, name: String) = File(File(context.cacheDir, CAPTURE_DIRECTORY), name)

/**
 * Gallery and camera entry points for wardrobe photos.
 *
 * Neither needs a runtime permission: the system Photo Picker grants access to the chosen
 * image only, and the camera is the device's own camera app writing to a private cache
 * file through a FileProvider. Captures are temporary; call [ClothingImagePicker.discardCapture]
 * once the photo has been used, and anything left behind is removed after a day.
 */
@Composable
fun rememberClothingImagePicker(
    onError: (String) -> Unit = {},
    onImageSelected: (Uri) -> Unit,
): ClothingImagePicker {
    val context = LocalContext.current.applicationContext
    val currentOnSelected by rememberUpdatedState(onImageSelected)
    val currentOnError by rememberUpdatedState(onError)
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) currentOnSelected(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { saved ->
        val name = pendingCapture
        if (saved && name != null) {
            currentOnSelected(FileProvider.getUriForFile(context, "${context.packageName}.wardrobe.capture", captureFile(context, name)))
        } else if (name != null) {
            captureFile(context, name).delete()
            pendingCapture = null
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val cutoff = System.currentTimeMillis() - STALE_CAPTURE_MILLIS
            File(context.cacheDir, CAPTURE_DIRECTORY).listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
        }
    }

    return remember {
        ClothingImagePicker(
            pickFromGallery = {
                pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            takePhoto = {
                try {
                    val name = "capture-${UUID.randomUUID()}.jpg"
                    val file = captureFile(context, name)
                    file.parentFile?.mkdirs()
                    pendingCapture = name
                    cameraLauncher.launch(FileProvider.getUriForFile(context, "${context.packageName}.wardrobe.capture", file))
                } catch (_: ActivityNotFoundException) {
                    pendingCapture = null
                    currentOnError("No camera app is available. Choose a photo instead.")
                } catch (_: Exception) {
                    pendingCapture = null
                    currentOnError("The camera could not be opened. Choose a photo instead.")
                }
            },
            discard = {
                pendingCapture?.let { captureFile(context, it).delete() }
                pendingCapture = null
            },
        )
    }
}

class ClothingImagePicker(
    private val pickFromGallery: () -> Unit,
    private val takePhoto: () -> Unit = {},
    private val discard: () -> Unit = {},
) {
    fun launchGallery() = pickFromGallery()

    /** Opens the device camera app. No CAMERA permission is requested or needed. */
    fun launchCamera() = takePhoto()

    /** Deletes the temporary camera file, if any. Safe to call when there is none. */
    fun discardCapture() = discard()
}
