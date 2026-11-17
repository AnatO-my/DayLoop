package com.dayloop.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Full labels and independent touch targets for choices that do not fit a segmented row. */
@Composable
fun DayLoopOptionGrid(
    options: List<DayLoopPillOption>,
    selectedOptionId: String,
    onOptionSelected: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth().selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { option ->
                    val selected = option.id == selectedOptionId
                    Surface(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .selectable(selected, enabled = enabled, role = Role.RadioButton,
                                onClick = { onOptionSelected(option.id) }),
                        shape = MaterialTheme.shapes.small,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    ) {
                        Text(option.label, style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.heightIn(min = 52.dp).padding(14.dp))
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
