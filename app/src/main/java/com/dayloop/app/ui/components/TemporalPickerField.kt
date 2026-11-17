package com.dayloop.app.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dayloop.app.R
import java.time.*
import java.time.format.DateTimeFormatter

enum class TemporalKind { DATE, TIME }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemporalPickerField(label: String, value: String, kind: TemporalKind, onChange: (String) -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, optional: Boolean = false,
    error: Boolean = false) {
    var open by rememberSaveable { mutableStateOf(false) }
    val parsedDate = runCatching { LocalDate.parse(value) }.getOrNull()
    val parsedTime = runCatching { LocalTime.parse(value) }.getOrNull()
    val display = if (kind == TemporalKind.DATE) parsedDate?.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        else parsedTime?.format(DateTimeFormatter.ofPattern("HH:mm"))
    Surface(onClick = { open = true }, enabled = enabled, modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(Modifier.heightIn(min = 64.dp).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(display ?: value.ifBlank { "Not set" }, style = MaterialTheme.typography.bodyMedium)
            }
            if (optional && value.isNotBlank()) DayLoopIconButton(resource = R.drawable.ic_action_close, label = "Clear $label", enabled = enabled, onClick = { onChange("") })
            else DayLoopIcon(if (kind == TemporalKind.DATE) R.drawable.ic_nav_plan else R.drawable.ic_nav_focus)
        }
    }
    if (open && kind == TemporalKind.DATE) {
        val state = rememberDatePickerState(initialSelectedDateMillis = parsedDate
            ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli())
        DatePickerDialog(onDismissRequest = { open = false },
            confirmButton = { TextButton(enabled = state.selectedDateMillis != null, onClick = {
                state.selectedDateMillis?.let { onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString()) }
                open = false
            }) { Text("Done") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Cancel") } }) {
            DatePicker(state = state, title = { Text(label, Modifier.padding(24.dp), style = MaterialTheme.typography.titleMedium) })
        }
    }
    if (open && kind == TemporalKind.TIME) {
        val state = rememberTimePickerState(initialHour = parsedTime?.hour ?: 9, initialMinute = parsedTime?.minute ?: 0,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current))
        val largeText = LocalDensity.current.fontScale > 1.2f
        var keyboard by rememberSaveable { mutableStateOf(largeText) }
        Dialog(onDismissRequest = { open = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(16.dp), shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(label, style = MaterialTheme.typography.titleMedium)
                    if (keyboard) TimeInput(state) else TimePicker(state)
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        DayLoopIconButton(
                            resource = if (keyboard) R.drawable.ic_nav_focus else R.drawable.ic_action_edit,
                            label = if (keyboard) "Clock picker" else "Type time",
                            onClick = { keyboard = !keyboard }
                        )
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { open = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            onChange(LocalTime.of(state.hour, state.minute).format(DateTimeFormatter.ofPattern("HH:mm")))
                            open = false
                        }) { Text("Done") }
                    }
                }
            }
        }
    }
}
