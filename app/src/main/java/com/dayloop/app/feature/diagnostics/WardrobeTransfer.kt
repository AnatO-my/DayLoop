package com.dayloop.app.feature.diagnostics

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.dayloop.app.core.backup.BackupCipher
import com.dayloop.app.di.AppGraph
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

internal data class WardrobeTransferActions(
    val onExport: () -> Unit,
    val onImport: () -> Unit,
    val busy: Boolean,
    val status: String?,
)

/** Owned by the route, not a lazy-list item: scrolling cannot discard an active transfer. */
@Composable
internal fun rememberWardrobeTransfer(graph: AppGraph): WardrobeTransferActions {
    val scope = rememberCoroutineScope()
    val resolver = graph.applicationContext.contentResolver
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var source by remember { mutableStateOf<Uri?>(null) }
    var encryptedExport by remember { mutableStateOf<String?>(null) }
    // Deliberately not saveable: passwords must not enter saved-instance state.
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }

    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val encrypted = encryptedExport
        encryptedExport = null
        if (uri == null || encrypted == null) {
            busy = false
            status = if (uri == null) "Export cancelled." else "Export interrupted. Start again."
        } else {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException()
                        stream.use { it.write(encrypted.toByteArray(Charsets.UTF_8)) }
                    }
                    status = "Wardrobe details exported. Photos and other app data are not included."
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    status = "Export failed. Remove any incomplete file and try again."
                } finally { busy = false }
            }
        }
    }
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        busy = false
        source = uri
        if (uri != null) {
            exporting = false
            password = ""
            confirmation = ""
            showPassword = true
        }
    }

    if (showPassword) {
        AlertDialog(
            onDismissRequest = { showPassword = false; password = ""; confirmation = ""; source = null },
            title = { Text(if (exporting) "Export wardrobe details" else "Import wardrobe details") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (exporting)
                        "Clothing details and outfit dates only. No photos, tasks or settings. A lost password cannot be recovered."
                    else "Only missing clothing and outfit dates will be added. Existing records and photos will be kept. This file cannot restore photos, tasks or settings.")
                    OutlinedTextField(password, { if (it.length <= 256) password = it },
                        label = { Text("Password (12-256 characters)") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                    if (exporting) OutlinedTextField(confirmation, { if (it.length <= 256) confirmation = it },
                        label = { Text("Confirm password") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                }
            },
            dismissButton = { TextButton(onClick = {
                showPassword = false; password = ""; confirmation = ""; source = null
            }) { Text("Cancel") } },
            confirmButton = {
                TextButton(enabled = password.length in 12..256 && (!exporting || password == confirmation), onClick = {
                    val secret = password.toCharArray()
                    val isExport = exporting
                    val importUri = source
                    showPassword = false
                    password = ""
                    confirmation = ""
                    source = null
                    busy = true
                    status = null
                    scope.launch {
                        var waitingForDestination = false
                        try {
                            if (isExport) {
                                encryptedExport = graph.backupManager.createBackup(secret)
                                saveFile.launch("DayLoop-wardrobe.dayloop")
                                waitingForDestination = true
                            } else {
                                val file = withContext(Dispatchers.IO) {
                                    val stream = resolver.openInputStream(requireNotNull(importUri)) ?: throw IOException()
                                    stream.use { input ->
                                        val output = ByteArrayOutputStream()
                                        val buffer = ByteArray(8192)
                                        while (true) {
                                            val count = input.read(buffer)
                                            if (count == -1) break
                                            require(output.size() + count <= BackupCipher.MAX_FILE_BYTES)
                                            output.write(buffer, 0, count)
                                        }
                                        output.toString("UTF-8")
                                    }
                                }
                                graph.backupManager.restoreBackup(file, secret)
                                status = "Wardrobe details imported. Existing records kept."
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Exception) {
                            encryptedExport = null
                            status = if (isExport) "Export failed. Try again."
                                else "Import failed. Check the password and file. Legacy backups are unsupported."
                        } finally {
                            secret.fill('\u0000')
                            if (!waitingForDestination) busy = false
                        }
                    }
                }) { Text(if (exporting) "Export" else "Import") }
            },
        )
    }

    return WardrobeTransferActions(
        onExport = {
            if (!busy && !showPassword) {
                exporting = true; password = ""; confirmation = ""; showPassword = true; status = null
            }
        },
        onImport = {
            if (!busy && !showPassword) {
                status = null
                busy = true
                try { openFile.launch(arrayOf("*/*")) }
                catch (_: Exception) { busy = false; status = "File picker unavailable." }
            }
        },
        busy = busy || showPassword,
        status = if (busy) "Processing wardrobe file..." else status,
    )
}
