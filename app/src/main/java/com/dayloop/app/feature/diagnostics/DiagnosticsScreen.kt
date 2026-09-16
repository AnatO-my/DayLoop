package com.dayloop.app.feature.diagnostics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dayloop.app.core.alarms.AlarmDiagnosticEntry
import com.dayloop.app.core.notifications.ReminderCapabilities
import com.dayloop.app.di.LocalAppGraph
import com.dayloop.app.ui.components.DayLoopBottomActionBar
import com.dayloop.app.ui.components.DayLoopHeroCard
import com.dayloop.app.ui.components.DayLoopScreen
import com.dayloop.app.ui.theme.dayLoopTokens
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DiagnosticTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

@Composable
fun DiagnosticsRoute(
    onBack: () -> Unit,
) {
    val appGraph = LocalAppGraph.current
    val entries by appGraph.alarmDiagnosticsRepository.observeEntries().collectAsState(emptyList())
    val capabilities = remember { appGraph.reminderNotifications.capabilities() }
    val scope = rememberCoroutineScope()
    var isRepairing by remember { mutableStateOf(false) }

    DiagnosticsScreen(
        uiState = DiagnosticsUiState(
            capabilities = capabilities,
            entries = entries.sortedByDescending { it.createdAt },
            isRepairing = isRepairing
        ),
        onBack = onBack,
        onRepair = {
            isRepairing = true
            scope.launch {
                try {
                    appGraph.scheduleReconciler.reconcile()
                } finally {
                    isRepairing = false
                }
            }
        }
    )
}

@Composable
fun DiagnosticsScreen(
    uiState: DiagnosticsUiState,
    onBack: () -> Unit,
    onRepair: () -> Unit,
) {
    val tokens = MaterialTheme.dayLoopTokens

    DayLoopScreen {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text("Diagnostics", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onBack) { Text("Close") }
            }

            Spacer(Modifier.height(tokens.spacing.contentGap))

            LazyColumn(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.compactGap),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    CapabilitiesSection(uiState.capabilities)
                }
                item {
                    Text("Recent Events", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                items(uiState.entries) { entry ->
                    DiagnosticEntryRow(entry)
                }
                if (!uiState.hasEntries) {
                    item {
                        Text("No events recorded yet.", color = tokens.colors.supportingText)
                    }
                }
            }

            Spacer(Modifier.height(tokens.spacing.contentGap))

            DayLoopBottomActionBar(
                text = if (uiState.isRepairing) "Repairing..." else "Repair schedules",
                onClick = onRepair,
                enabled = !uiState.isRepairing
            )
        }
    }
}

@Composable
private fun CapabilitiesSection(capabilities: ReminderCapabilities?) {
    val tokens = MaterialTheme.dayLoopTokens
    DayLoopHeroCard {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.microGap)) {
            Text("Capabilities", style = MaterialTheme.typography.titleSmall, color = tokens.colors.onActionSurface.copy(alpha = 0.7f))
            if (capabilities == null) {
                Text("Loading capabilities...")
            } else {
                CapabilityRow("Exact Alarms", capabilities.exactAlarmsAllowed)
                CapabilityRow("Notifications", capabilities.notificationsAllowed)
                CapabilityRow("Full Screen", capabilities.fullScreenAllowed)
                CapabilityRow("Alarm Channel", capabilities.alarmChannelEnabled)
            }
        }
    }
}

@Composable
private fun CapabilityRow(label: String, allowed: Boolean) {
    val tokens = MaterialTheme.dayLoopTokens
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Text(
            if (allowed) "Available" else "Denied",
            color = if (allowed) MaterialTheme.colorScheme.primary else tokens.colors.warning,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DiagnosticEntryRow(entry: AlarmDiagnosticEntry) {
    val tokens = MaterialTheme.dayLoopTokens
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(tokens.shapeScale.comfortableCorner),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(tokens.spacing.compactGap)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(entry.event.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(DiagnosticTimeFormatter.format(entry.createdAt), style = MaterialTheme.typography.labelSmall, color = tokens.colors.supportingText)
            }
            entry.message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = tokens.colors.supportingText)
            }
            entry.taskId?.let {
                Text("Task: ${it.value}", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
