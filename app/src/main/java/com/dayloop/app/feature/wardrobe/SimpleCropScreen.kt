@file:Suppress("DEPRECATION")

package com.dayloop.app.feature.wardrobe

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.exifinterface.media.ExifInterface
import coil.compose.AsyncImage
import com.dayloop.app.R
import com.dayloop.app.core.outfit.ClothingImageProcessor
import com.dayloop.app.ui.components.DayLoopIcon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.createBitmap

@Composable
fun SimpleCropScreen(
    imageUri: Uri,
    onConfirm: (Bitmap?, Map<String, String>) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val processor = remember { ClothingImageProcessor(context) }
    
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var processedData by remember { mutableStateOf<ClothingImageProcessor.ProcessedClothing?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Transformation state for manual cropping
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offset += offsetChange
    }

    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri) ?: return@withContext
                val bytes = inputStream.readBytes()
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                
                val exif = ExifInterface(bytes.inputStream())
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }
                
                originalBitmap = if (matrix.isIdentity) bitmap else {
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Confirm Photo", style = MaterialTheme.typography.headlineMedium)
                Text("Pinch to zoom, drag to move", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color.Black.copy(alpha = 0.05f))
                ) {
                    if (processedData != null) {
                        AsyncImage(
                            model = processedData!!.bitmap,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else if (originalBitmap != null) {
                        Image(
                            bitmap = originalBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .transformable(state = transformState)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                },
                            contentScale = ContentScale.Fit
                        )
                    }
                    
                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }
                
                Spacer(Modifier.height(16.dp))

                if (processedData == null) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isProcessing = true
                                processedData = processor.processImage(imageUri)
                                isProcessing = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing && originalBitmap != null,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        DayLoopIcon(resource = R.drawable.ic_action_sparkle)
                        Spacer(Modifier.width(8.dp))
                        Text("Magic Clean (AI)")
                    }
                    Spacer(Modifier.height(12.dp))
                } else {
                    OutlinedButton(
                        onClick = { processedData = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restore Original")
                    }
                    Spacer(Modifier.height(12.dp))
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), enabled = !isProcessing) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { 
                            if (processedData != null) {
                                onConfirm(processedData?.bitmap, processedData?.labels ?: emptyMap())
                            } else {
                                coroutineScope.launch {
                                    val cropped = withContext(Dispatchers.IO) {
                                        originalBitmap?.let { bmp ->
                                            applyManualCrop(bmp, scale, offset)
                                        }
                                    }
                                    onConfirm(cropped, emptyMap())
                                }
                            }
                        }, 
                        modifier = Modifier.weight(1f), 
                        enabled = !isProcessing && originalBitmap != null
                    ) {
                        Text(if (processedData != null) "Save Cleaned" else "Save Original")
                    }
                }
            }
        }
    }
}

@SuppressLint("UseKt")
private fun applyManualCrop(bitmap: Bitmap, scale: Float, offset: Offset): Bitmap {
    val size = bitmap.width.coerceAtMost(bitmap.height)
    val out = createBitmap(size, size)
    val canvas = Canvas(out)
    val matrix = Matrix()
    
    // This logic is simplified and might need refinement to match the UI preview exactly,
    // but it demonstrates "Manual Cropping" via transformation.
    matrix.postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
    matrix.postScale(scale, scale)
    matrix.postTranslate(size / 2f + offset.x, size / 2f + offset.y)
    
    canvas.drawBitmap(bitmap, matrix, null)
    return out
}
