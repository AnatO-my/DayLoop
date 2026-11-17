package com.dayloop.app.feature.wardrobe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dayloop.app.core.outfit.recognition.SummaryRow
import com.dayloop.app.core.outfit.recognition.Sureness
import com.dayloop.app.ui.components.DayLoopButton
import com.dayloop.app.ui.components.DayLoopOutlinedButton
import com.dayloop.app.ui.theme.dayLoopTokens

/**
 * The short confirmation shown when the recogniser was sure enough to save as is:
 * "Navy jeans?" with one line per detail and how sure each one is. Saving from here is
 * still the user's explicit choice; "Edit details" opens the full form.
 */
@Composable
internal fun ColumnScope.GarmentQuickConfirm(
    image: Any,
    headline: String,
    rows: List<SummaryRow>,
    busy: Boolean,
    error: String?,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onStartBlank: () -> Unit,
) {
    val colors = MaterialTheme.dayLoopTokens.colors
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
        WardrobeImageFrame(
            model = image,
            contentDescription = "Photo of the item being added",
            modifier = Modifier.fillMaxSize().padding(16.dp),
        )
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).background(colors.raised).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Recognised on this device. Your photo is not uploaded.", style = MaterialTheme.typography.bodySmall, color = colors.secondaryText)
        Text(headline, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        rows.forEach { SurenessRow(it) }
        error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DayLoopOutlinedButton(onClick = onEdit, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Edit details") }
            DayLoopButton(onClick = onSave, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Looks right, save") }
        }
        TextButton(onClick = onStartBlank, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Not this item? Start from blank", color = colors.accentText)
        }
    }
}

/** One detail with its sureness in words; the three-step meter repeats the word visually. */
@Composable
internal fun SurenessRow(row: SummaryRow) {
    val colors = MaterialTheme.dayLoopTokens.colors
    val tone = when (row.sureness) {
        Sureness.SURE -> colors.success
        Sureness.CHECK -> colors.warning
        Sureness.CANT_TELL -> colors.secondaryText
    }
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(row.label, Modifier.width(84.dp), style = MaterialTheme.typography.bodyMedium, color = colors.secondaryText)
        row.colorHex?.let {
            Box(Modifier.size(16.dp).clip(CircleShape).background(wardrobeColor(it)).border(1.dp, colors.secondaryText, CircleShape))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            row.value ?: "Not set", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            color = if (row.value == null) colors.secondaryText else colors.text,
        )
        Text(row.sureness.label, style = MaterialTheme.typography.bodySmall, color = tone)
        Spacer(Modifier.width(8.dp))
        Row(Modifier.clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(3) { step ->
                Box(Modifier.size(width = 14.dp, height = 6.dp).background(if (step < row.sureness.filledSteps) tone else colors.decorativeBorder))
            }
        }
    }
}
