package com.dayloop.app.feature.diagnostics

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dayloop.app.settings.AppPalette
import com.dayloop.app.ui.designsystem.DayLoopChoice
import com.dayloop.app.settings.AppSettings
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppearanceControls(settings: AppSettings, saving: Boolean,
                                onFontSelected: (com.dayloop.app.settings.AppFont) -> Unit = {},
                                onSave: (AppPalette, Float, Boolean, Float, String?) -> Unit,
                                onSavePalette: (String, String) -> Unit = { _, _ -> },
                                onDeletePalette: (String) -> Unit = {},
                                onAiAssistantToggle: (Boolean) -> Unit = {}) {
    var weight by remember(settings.fontWeightScale) { mutableFloatStateOf(settings.fontWeightScale) }
    var customHex by remember(settings.customColorHex) { mutableStateOf(settings.customColorHex ?: "") }
    var paletteName by remember { mutableStateOf("") }
    var showSavePalette by remember { mutableStateOf(false) }
    val normalizedHex = com.dayloop.app.settings.normalizeHexColor(customHex)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Colour blend", style = MaterialTheme.typography.titleSmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppPalette.entries.forEach { palette ->
                val selected = settings.palette == palette && settings.customColorHex == null
                val colour = when (palette) {
                    AppPalette.MATCHA -> Color(0xFFA8CF3D)
                    AppPalette.EMERALD -> Color(0xFF54DDA2)
                    AppPalette.ROSE -> Color(0xFFF4A7BF)
                }
                Column(Modifier.weight(1f).selectable(selected, enabled = !saving, role = Role.RadioButton,
                    onClick = { onSave(palette, settings.textScale, settings.reducedMotion, settings.fontWeightScale, null) }).padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(40.dp).border(if (selected) 3.dp else 1.dp,
                        if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                        CircleShape).padding(5.dp).background(colour, CircleShape))
                    Text(palette.label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Text("Custom HEX Colour", style = MaterialTheme.typography.labelMedium)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = customHex,
                onValueChange = { if (it.length <= 7) customHex = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("#A8CF3D") },
                singleLine = true,
                enabled = !saving,
                isError = customHex.isNotBlank() && normalizedHex == null
            )
            Button(
                onClick = {
                    normalizedHex?.let { onSave(settings.palette, settings.textScale, settings.reducedMotion, settings.fontWeightScale, it) }
                },
                enabled = !saving && normalizedHex != null
            ) {
                Text("Apply")
            }
        }

        if (settings.savedPalettes.isNotEmpty()) {
            Text("Saved palettes", style = MaterialTheme.typography.labelMedium)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(settings.savedPalettes.size) { i ->
                    val palette = settings.savedPalettes[i]
                    val selected = settings.customColorHex == palette.colorHex
                    val color = try { Color(android.graphics.Color.parseColor(palette.colorHex)) } catch (_: Exception) { Color.Gray }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.combinedClickable(
                            onClick = { onSave(settings.palette, settings.textScale, settings.reducedMotion, settings.fontWeightScale, palette.colorHex) },
                            onLongClick = { onDeletePalette(palette.name) }
                        )
                    ) {
                        Box(Modifier.size(44.dp).border(if (selected) 2.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape).padding(4.dp).background(color, CircleShape))
                        Text(palette.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }

        if (settings.customColorHex != null && settings.savedPalettes.none { it.colorHex == settings.customColorHex }) {
            TextButton(onClick = { showSavePalette = true }, enabled = !saving) {
                Text("Save current color as palette")
            }
        }

        Text("Advanced", style = MaterialTheme.typography.titleSmall)
        DayLoopChoice("Font",
            com.dayloop.app.settings.AppFont.entries.map { it.name to it.label }, settings.font.name,
            { onFontSelected(com.dayloop.app.settings.AppFont.valueOf(it)) }, !saving)

        Text("Font weight ${(weight * 100).roundToInt()}%")
        Slider(value = weight, onValueChange = { weight = it }, valueRange = 0.5f..1.5f, steps = 10,
            enabled = !saving,
            onValueChangeFinished = { onSave(settings.palette, settings.textScale, settings.reducedMotion, weight, settings.customColorHex) })

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("DayLoop Assistant", modifier = Modifier.weight(1f))
            Switch(checked = settings.aiAssistantEnabled, enabled = !saving,
                onCheckedChange = { onAiAssistantToggle(it) })
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Reduce motion", modifier = Modifier.weight(1f))
            Switch(checked = settings.reducedMotion, enabled = !saving,
                onCheckedChange = { onSave(settings.palette, settings.textScale, it, settings.fontWeightScale, settings.customColorHex) })
        }
        TextButton(enabled = !saving, onClick = {
            weight = 1f
            customHex = ""
            onSave(AppPalette.MATCHA, 1f, false, 1f, null)
        }) {
            Text("Reset appearance")
        }
    }

    if (showSavePalette) {
        AlertDialog(
            onDismissRequest = { showSavePalette = false },
            title = { Text("Save palette") },
            text = {
                OutlinedTextField(
                    value = paletteName,
                    onValueChange = { paletteName = it },
                    label = { Text("Palette name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(
                    enabled = paletteName.isNotBlank() && !saving,
                    onClick = {
                        settings.customColorHex?.let { onSavePalette(paletteName.trim(), it) }
                        showSavePalette = false
                        paletteName = ""
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSavePalette = false }) { Text("Cancel") }
            }
        )
    }
}