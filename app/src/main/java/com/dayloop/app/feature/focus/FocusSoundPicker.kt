package com.dayloop.app.feature.focus

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dayloop.app.R
import com.dayloop.app.ui.components.DayLoopIcon
import com.dayloop.app.ui.theme.dayLoopTokens

@Composable
internal fun FocusSoundPicker(
    selected: FocusSound,
    onSelect: (FocusSound) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val tokens = MaterialTheme.dayLoopTokens
    var open by rememberSaveable { mutableStateOf(false) }
    val ink = LocalContentColor.current
    Surface(
        onClick = { open = true }, enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(tokens.shapeScale.inputCorner)),
        shape = RoundedCornerShape(tokens.shapeScale.inputCorner),
        color = Color.Transparent, contentColor = ink,
    ) {
        Row(Modifier.heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("BACKGROUND SOUND", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Text(selected.pickerLabel(), style = MaterialTheme.typography.titleMedium)
            }
            DayLoopIcon(R.drawable.ic_action_forward, tint = ink)
        }
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Background sound") },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()).selectableGroup()) {
                    FocusSound.entries.forEach { sound ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(tokens.shapeScale.inputCorner))
                            .selectable(selected = sound == selected, enabled = enabled,
                                role = Role.RadioButton, onClick = {
                                    onSelect(sound)
                                    open = false
                                })
                            .heightIn(min = 56.dp).padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            RadioButton(selected = sound == selected, onClick = null, enabled = enabled)
                            Text(sound.pickerLabel(), modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Close") } },
        )
    }
}

private fun FocusSound.pickerLabel() = if (this == FocusSound.NONE) "Off" else label
