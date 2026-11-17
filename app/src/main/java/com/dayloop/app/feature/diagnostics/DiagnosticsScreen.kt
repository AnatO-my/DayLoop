package com.dayloop.app.feature.diagnostics

import androidx.compose.material3.MaterialTheme
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.dayloop.app.R
import com.dayloop.app.ui.theme.DayLoopTheme
import com.dayloop.app.settings.ThemeMode
import com.dayloop.app.core.alarms.AlarmDiagnosticEntry
import com.dayloop.app.core.notifications.ReminderCapabilities
import com.dayloop.app.di.LocalAppGraph
import com.dayloop.app.ui.components.DayLoopHeroCard
import com.dayloop.app.ui.components.DayLoopScreen
import com.dayloop.app.ui.theme.dayLoopTokens
import kotlinx.coroutines.launch
import com.dayloop.app.settings.AppSettings
import com.dayloop.app.settings.GuideKey
import com.dayloop.app.feature.onboarding.DayLoopMascot
import com.dayloop.app.feature.onboarding.MascotMood
import com.dayloop.app.ui.components.DayLoopIcon
import com.dayloop.app.ui.components.DayLoopIconButton
import com.dayloop.app.ui.guides.GuideDefinitions
import com.dayloop.app.widget.WidgetPinningManager
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DiagnosticTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

@Composable
fun DiagnosticsRoute(
    onBack: () -> Unit,
    refreshKey: Int = 0,
    onReplayGuide: ((GuideKey) -> Unit)? = null,
    onReplayIntroduction: (() -> Unit)? = null,
    replayEnabled: Boolean = true,
) {
    val appGraph = LocalAppGraph.current
    val entries by appGraph.alarmDiagnosticsRepository.observeEntries().collectAsState(emptyList())
    val capabilities = remember(refreshKey) { appGraph.reminderNotifications.capabilities() }
    val scope = rememberCoroutineScope()
    var isRepairing by remember { mutableStateOf(false) }
    val preferences by appGraph.settingsRepository.settings.collectAsState(AppSettings())
    var savingTheme by remember { mutableStateOf(false) }
    var themeError by remember { mutableStateOf<String?>(null) }
    var repairError by remember { mutableStateOf<String?>(null) }
    var showEfficiencyReport by remember { mutableStateOf(false) }
    val wardrobeTransfer = rememberWardrobeTransfer(appGraph)

    if (showEfficiencyReport) {
        EfficiencyReport(preferences.usageStats, onBack = { showEfficiencyReport = false })
        return
    }

    DiagnosticsScreen(
        uiState = DiagnosticsUiState(
            capabilities = capabilities,
            entries = entries.sortedByDescending { it.createdAt },
            isRepairing = isRepairing
        ),
        onBack = onBack,
        onReplayGuide = onReplayGuide,
        onReplayIntroduction = onReplayIntroduction,
        replayEnabled = replayEnabled,
        appearanceExtras = {
            AppearanceControls(preferences, savingTheme, onFontSelected = { font ->
                if (!savingTheme) {
                    savingTheme = true
                    scope.launch {
                        try { appGraph.settingsRepository.setFont(font); themeError = null }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { themeError = "Could not save font. Try again." }
                        finally { savingTheme = false }
                    }
                }
            }, onHomePresentationSelected = { presentation ->
                if (!savingTheme) {
                    savingTheme = true
                    scope.launch {
                        try { appGraph.settingsRepository.setHomePresentation(presentation); themeError = null }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { themeError = "Could not save home screen. Try again." }
                        finally { savingTheme = false }
                    }
                }
            }, onNameSaved = { name ->
                if (!savingTheme) {
                    savingTheme = true
                    scope.launch {
                        try { appGraph.settingsRepository.setDisplayName(name); themeError = null }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { themeError = "Could not save name. Try again." }
                        finally { savingTheme = false }
                    }
                }
            }, onSave = { palette, scale, motion, fontWeight, customHex ->
                if (!savingTheme) {
                    savingTheme = true
                    themeError = null
                    scope.launch {
                        try { appGraph.settingsRepository.setAppearance(palette, scale, motion, fontWeight, customHex) }
                        catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { themeError = "Appearance could not be saved. Try again." }
                        finally { savingTheme = false }
                    }
                }
            }, onSavePalette = { name, hex ->
                scope.launch {
                    try { appGraph.settingsRepository.savePalette(name, hex) }
                    catch (_: Exception) { themeError = "Could not save palette." }
                }
            }, onDeletePalette = { name ->
                scope.launch {
                    try { appGraph.settingsRepository.deletePalette(name) }
                    catch (_: Exception) { themeError = "Could not delete palette." }
                }
            }, onAiAssistantToggle = { enabled ->
                scope.launch {
                    try { appGraph.settingsRepository.setAiAssistantEnabled(enabled) }
                    catch (_: Exception) { themeError = "Could not update Assistant setting." }
                }
            })
        },
        repairError = repairError,
        themeMode = preferences.themeMode,
        savingTheme = savingTheme,
        themeError = themeError,
        onThemeModeSelected = { mode ->
            if (!savingTheme) {
                savingTheme = true
                themeError = null
                scope.launch {
                    try {
                        appGraph.settingsRepository.setThemeMode(mode)
                    } catch (_: IOException) {
                        themeError = "Appearance could not be saved. Try again."
                    } finally {
                        savingTheme = false
                    }
                }
            }
        },
        onRepair = {
            isRepairing = true
            repairError = null
            scope.launch {
                try {
                    appGraph.scheduleReconciler.reconcile()
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    repairError = "Repair could not finish. Please try again."
                } finally {
                    isRepairing = false
                }
            }
        },
        onFixCapability = { capability ->
            val intent = when (capability) {
                "notifications" -> appGraph.capabilityRepairFlow.notificationSettingsIntent()
                "exact_alarms" -> appGraph.capabilityRepairFlow.exactAlarmSettingsIntent()
                "full_screen" -> appGraph.capabilityRepairFlow.fullScreenIntentSettingsIntent()
                else -> appGraph.capabilityRepairFlow.applicationDetailsIntent()
            }
            intent?.let { appGraph.applicationContext.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        },
        onClearHistory = {
            scope.launch { appGraph.alarmDiagnosticsRepository.clearAllEntries() }
        },
        onViewEfficiencyReport = { showEfficiencyReport = true },
        backupStatus = wardrobeTransfer.status,
        onCreateBackup = wardrobeTransfer.onExport,
        onImportBackup = wardrobeTransfer.onImport,
        backupBusy = wardrobeTransfer.busy,
    )
}

@Composable
fun DiagnosticsScreen(
    uiState: DiagnosticsUiState,
    onBack: () -> Unit,
    onRepair: () -> Unit,
    onFixCapability: (String) -> Unit,
    onClearHistory: () -> Unit,
    onViewEfficiencyReport: () -> Unit = {},
    onCreateBackup: () -> Unit = {},
    onImportBackup: () -> Unit = {},
    backupBusy: Boolean = false,
    backupStatus: String? = null,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    savingTheme: Boolean = false,
    themeError: String? = null,
    onThemeModeSelected: (ThemeMode) -> Unit = {},
    repairError: String? = null,
    appearanceExtras: @Composable () -> Unit = {},
    onReplayGuide: ((GuideKey) -> Unit)? = null,
    onReplayIntroduction: (() -> Unit)? = null,
    replayEnabled: Boolean = true,
) {
    val tokens = MaterialTheme.dayLoopTokens
    var eventsExpanded by rememberSaveable { mutableStateOf(false) }
    var guidesExpanded by rememberSaveable { mutableStateOf(false) }
    var remindersExpanded by rememberSaveable { mutableStateOf(false) }
    var confirmClearHistory by rememberSaveable { mutableStateOf(false) }

    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            title = { Text("Clear recent events?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmClearHistory = false; onClearHistory() }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearHistory = false }) { Text("Cancel") }
            }
        )
    }

    DayLoopScreen(modifier = Modifier.fillMaxHeight(), contentPadding = PaddingValues(tokens.spacing.pageMargin)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(tokens.spacing.relatedGap)) {
                    Text("Advanced", modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
                }
                Row {
                    onReplayIntroduction?.let { replay ->
                        DayLoopIconButton(R.drawable.ic_action_reset, "Replay introduction",
                            onClick = replay, enabled = replayEnabled)
                    }
                    DayLoopIconButton(R.drawable.ic_action_close, "Close More", onClick = onBack)
                }
            }

            Spacer(Modifier.height(tokens.spacing.sectionGap))

            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.sectionGap),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    val creditText = buildAnnotatedString {
                        append("Icons: ")
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append("coolicons")
                        }
                        append(" by Kryston Schwarze")
                    }
                    Text(
                        text = creditText,
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.colors.mutedText,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                item {
                    if (onReplayGuide != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { guidesExpanded = !guidesExpanded }, modifier = Modifier.fillMaxWidth()) {
                                Text("Guides", Modifier.weight(1f), color = tokens.colors.text)
                                Text(if (guidesExpanded) "−" else "+", color = tokens.colors.text)
                            }
                            if (guidesExpanded) {
                            listOf(
                                "First Launch" to GuideDefinitions.FirstLaunch,
                                "Today Screen" to GuideDefinitions.Today,
                                "Task Editor" to GuideDefinitions.TaskEditor,
                                "Scheduling" to GuideDefinitions.Scheduling,
                                "Plans (P&Gs)" to GuideDefinitions.Plans,
                                "Focus Mode" to GuideDefinitions.Focus,
                                "Wardrobe" to GuideDefinitions.Wardrobe,
                                "Calendar" to GuideDefinitions.Calendar
                            ).forEach { (label, key) ->
                                TextButton(
                                    onClick = { onReplayGuide(key) },
                                    enabled = replayEnabled,
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        DayLoopIcon(R.drawable.ic_action_reset, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(12.dp))
                                        Text(label, style = MaterialTheme.typography.bodyLarge)
                                    }
                                }
                            }
                            }
                        }
                    }
                }
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(tokens.shapeScale.cardCorner)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            ThemeMode.entries.forEach { mode ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(MaterialTheme.dayLoopTokens.shapeScale.cardCorner))
                                        .selectable(
                                            selected = themeMode == mode,
                                            enabled = !savingTheme,
                                            role = Role.RadioButton,
                                            onClick = { onThemeModeSelected(mode) },
                                        )
                                        .heightIn(min = 48.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(selected = themeMode == mode, onClick = null, enabled = !savingTheme)
                                    Spacer(Modifier.width(12.dp))
                                    Text(when (mode) {
                                        ThemeMode.SYSTEM -> "Use system setting"
                                        ThemeMode.LIGHT -> "Light"
                                        ThemeMode.DARK -> "Dark"
                                    })
                                }
                            }
                            themeError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            Spacer(Modifier.height(8.dp))
                            appearanceExtras()
                        }
                    }
                }
                item {
                    Column {
                        HorizontalDivider()
                        Spacer(Modifier.height(16.dp))
                        Text("Data & maintenance", style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onViewEfficiencyReport) {
                            Text("View Efficiency Report")
                        }
                        TextButton(onClick = onCreateBackup, enabled = !backupBusy) {
                            Text("Export wardrobe details")
                        }
                        TextButton(onClick = onImportBackup, enabled = !backupBusy) {
                            Text("Import wardrobe details")
                        }
                        backupStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary) }
                    }
                }
                item {
                    Column {
                        TextButton(onClick = { remindersExpanded = !remindersExpanded }, modifier = Modifier.fillMaxWidth()) {
                            Text("Reminders & permissions", Modifier.weight(1f), color = tokens.colors.text)
                            Text(if (remindersExpanded) "−" else "+", color = tokens.colors.text)
                        }
                        if (remindersExpanded) {
                        CapabilitiesSection(uiState.capabilities, onFixCapability)
                        repairError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        TextButton(onClick = onRepair, enabled = !uiState.isRepairing) {
                            Text(if (uiState.isRepairing) "Repairing..." else "Repair reminders")
                        }
                        }
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Recent events", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                            Row {
                                if (uiState.hasEntries) {
                                    DayLoopIconButton(R.drawable.ic_action_delete, "Clear recent events",
                                        onClick = { confirmClearHistory = true })
                                }
                                DayLoopIconButton(if (eventsExpanded) R.drawable.ic_action_close else R.drawable.ic_action_add,
                                    if (eventsExpanded) "Collapse recent events" else "Expand recent events",
                                    onClick = { eventsExpanded = !eventsExpanded })
                            }
                        }
                        if (eventsExpanded) {
                            if (uiState.hasEntries) {
                                uiState.entries.forEach { entry ->
                                    DiagnosticEntryRow(entry)
                                    Spacer(Modifier.height(tokens.spacing.microGap))
                                }
                            } else {
                                Text("No events recorded yet.", color = tokens.colors.mutedText)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(tokens.spacing.sectionGap))

        }
    }
}

@Composable
private fun CapabilitiesSection(capabilities: ReminderCapabilities?, onFix: (String) -> Unit) {
    val tokens = MaterialTheme.dayLoopTokens
    val context = LocalContext.current
    val pinningSupported = remember { WidgetPinningManager.isPinningSupported(context) }

    DayLoopHeroCard {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.microGap)) {
            Text("Capabilities", style = MaterialTheme.typography.titleSmall, color = tokens.colors.text.copy(alpha = 0.7f))
            if (capabilities == null) {
                Text("Loading capabilities...")
            } else {
                CapabilityRow("Exact Alarms", capabilities.exactAlarmsAllowed) { onFix("exact_alarms") }
                CapabilityRow("Notifications", capabilities.notificationsAllowed) { onFix("notifications") }
                CapabilityRow("Full Screen", capabilities.fullScreenAllowed) { onFix("full_screen") }
                CapabilityRow("Alarm Channel", capabilities.alarmChannelEnabled) { onFix("notifications") }

                if (pinningSupported) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            WidgetPinningManager.requestPinTodayWidget(context)
                        }
                    ) {
                        Text("Pin Today Widget to Homescreen")
                    }
                }
            }
        }
    }
}

@Composable
private fun CapabilityRow(label: String, allowed: Boolean, onFix: () -> Unit) {
    val tokens = MaterialTheme.dayLoopTokens
    Row(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "$label: ${if (allowed) "Available" else "Denied"}"
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                if (allowed) "Available" else "Denied",
                color = if (allowed) MaterialTheme.colorScheme.primary else tokens.colors.warning,
                fontWeight = FontWeight.Bold
            )
            if (!allowed) {
                TextButton(onClick = onFix, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("Fix", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun DiagnosticEntryRow(entry: AlarmDiagnosticEntry) {
    val tokens = MaterialTheme.dayLoopTokens
    Surface(
        Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Event: ${entry.event.name} at ${DiagnosticTimeFormatter.format(entry.createdAt)}"
            },
        shape = RoundedCornerShape(tokens.shapeScale.cardCorner),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(tokens.spacing.itemGap)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = entry.event.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = DiagnosticTimeFormatter.format(entry.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.colors.mutedText,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            entry.message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.colors.mutedText,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            entry.taskId?.let {
                Text(
                    text = "Task: ${it.value}",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
@Preview(name = "Diagnostics/More Screen Light", showBackground = true)
@Preview(name = "Diagnostics/More Screen Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun DiagnosticsScreenPreview() {
    val isDark = isSystemInDarkTheme()

    DayLoopTheme(darkTheme = isDark) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            DiagnosticsScreen(
                uiState = DiagnosticsUiState(
                    capabilities = null,
                    entries = emptyList(),
                    isRepairing = false
                ),
                onBack = {},
                onRepair = {},
                onFixCapability = {},
                onClearHistory = {},
                themeMode = ThemeMode.SYSTEM,
                savingTheme = false,
                themeError = null,
                onThemeModeSelected = {}
            )
        }
    }
}
