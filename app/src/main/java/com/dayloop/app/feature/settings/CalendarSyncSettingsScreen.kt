package com.dayloop.app.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dayloop.app.R
import com.dayloop.app.core.calendar.ExternalCalendar
import com.dayloop.app.ui.components.DayLoopButton
import com.dayloop.app.ui.components.DayLoopIconButton
import com.dayloop.app.ui.components.DayLoopScreen
import com.dayloop.app.ui.theme.dayLoopTokens

@Composable
fun CalendarSyncSettingsScreen(
    hasPermission: Boolean,
    calendars: List<ExternalCalendar>,
    selectedCalendarIds: Set<Long>,
    onRequestPermission: () -> Unit,
    onToggleCalendar: (Long, Boolean) -> Unit,
    onBack: () -> Unit
) {
    val tokens = MaterialTheme.dayLoopTokens

    DayLoopScreen {
        Column(Modifier.fillMaxSize()) {
            // Header
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                DayLoopIconButton(R.drawable.ic_action_back, "Back", onClick = onBack)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Google Calendar Sync",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(tokens.spacing.sectionGap))

            if (!hasPermission) {
                // Permission Warning Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(tokens.shapeScale.inputCorner)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "Calendar Permission Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "DayLoop needs calendar access to display your Google Calendar events alongside your tasks and sync scheduled work.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(12.dp))
                        DayLoopButton(onClick = onRequestPermission) {
                            Text("Grant Permission")
                        }
                    }
                }
            } else {
                Text(
                    text = "CONNECTED CALENDARS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.itemGap)
                ) {
                    if (calendars.isEmpty()) {
                        item {
                            Text(
                                text = "No external calendars found on this device.",
                                color = tokens.colors.mutedText,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }

                    items(calendars, key = { it.id }) { cal ->
                        val isSelected = selectedCalendarIds.contains(cal.id)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(tokens.shapeScale.inputCorner)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Calendar Color Indicator
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            color = Color(cal.color.takeIf { it != 0 } ?: 0xFF1A73E8.toInt()),
                                            shape = CircleShape
                                        )
                                )

                                Spacer(Modifier.width(12.dp))

                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = cal.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = cal.accountName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tokens.colors.mutedText
                                    )
                                }

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        onToggleCalendar(cal.id, checked)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
