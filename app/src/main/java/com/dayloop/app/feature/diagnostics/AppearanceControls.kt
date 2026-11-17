package com.dayloop.app.feature.diagnostics

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dayloop.app.settings.AppFont
import com.dayloop.app.settings.AppPalette
import com.dayloop.app.settings.AppSettings
import com.dayloop.app.settings.HomePresentation
import kotlin.math.roundToInt

@Suppress("UNUSED_PARAMETER")
@Composable
internal fun AppearanceControls(settings: AppSettings, saving: Boolean,
    onFontSelected: (AppFont) -> Unit = {},
    onHomePresentationSelected: (HomePresentation) -> Unit = {},
    onNameSaved: (String) -> Unit = {},
    onSave: (AppPalette, Float, Boolean, Float, String?) -> Unit,
    onSavePalette: (String, String) -> Unit = { _, _ -> },
    onDeletePalette: (String) -> Unit = {},
    onAiAssistantToggle: (Boolean) -> Unit = {}) {
    var weight by remember(settings.fontWeightScale) { mutableFloatStateOf(settings.fontWeightScale) }
    var typographyExpanded by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable(settings.displayName) { mutableStateOf(settings.displayName) }
    val validName = name.trim().length <= 40 && name.none { it.isISOControl() }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(name, { name = it }, label = { Text("Display name") },
            singleLine = true, enabled = !saving, isError = !validName,
            supportingText = { if (!validName) Text("Use up to 40 characters without line breaks.") },
            modifier = Modifier.fillMaxWidth())
        TextButton(onClick = { onNameSaved(name.trim()) }, enabled = !saving && validName && name.trim() != settings.displayName) {
            Text("Save name")
        }
        HorizontalDivider()
        TextButton(onClick = { typographyExpanded = !typographyExpanded }, modifier = Modifier.fillMaxWidth()) {
            Text("Typography", Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
            Text(if (typographyExpanded) "−" else "+", color = MaterialTheme.colorScheme.onSurface)
        }
        if (typographyExpanded) {
            com.dayloop.app.ui.components.DayLoopOptionGrid(
                listOf(AppFont.FUNNEL_DISPLAY, AppFont.JETBRAINS_MONO, AppFont.GEIST_MONO, AppFont.STACK_SANS_TEXT).map {
                    com.dayloop.app.ui.components.DayLoopPillOption(it.name, it.label)
                }, settings.font.name, { onFontSelected(AppFont.valueOf(it)) }, enabled = !saving)
            Text("Font weight ${(weight * 100).roundToInt()}%")
            Slider(value = weight, onValueChange = { weight = it }, valueRange = 0.5f..1.5f, steps = 10,
                enabled = !saving, onValueChangeFinished = {
                    onSave(settings.palette, settings.textScale, settings.reducedMotion, weight, settings.customColorHex)
                })
        }
        HorizontalDivider()
        Text("Home screen", style = MaterialTheme.typography.titleMedium)
        Text("Choose how Today is arranged. Tasks and reminders work the same in every layout.", style = MaterialTheme.typography.bodySmall)
        com.dayloop.app.ui.components.DayLoopOptionGrid(
            HomePresentation.entries.map { presentation ->
                com.dayloop.app.ui.components.DayLoopPillOption(presentation.name, presentation.label)
            },
            settings.homePresentation.name,
            { onHomePresentationSelected(HomePresentation.valueOf(it)) },
            enabled = !saving,
        )
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("DayLoop Assistant", Modifier.weight(1f))
            Switch(settings.aiAssistantEnabled, enabled = !saving, onCheckedChange = onAiAssistantToggle)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Reduce motion", Modifier.weight(1f))
            Switch(settings.reducedMotion, enabled = !saving, onCheckedChange = {
                onSave(settings.palette, settings.textScale, it, settings.fontWeightScale, settings.customColorHex)
            })
        }
    }
}
