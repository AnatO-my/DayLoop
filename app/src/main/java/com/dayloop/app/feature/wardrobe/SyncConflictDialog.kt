package com.dayloop.app.feature.wardrobe

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dayloop.app.R
import com.dayloop.app.core.outfit.sync.ConflictChoice
import com.dayloop.app.core.outfit.sync.ConflictDraft
import com.dayloop.app.core.outfit.sync.FieldConflict
import com.dayloop.app.core.outfit.sync.MergeSide
import com.dayloop.app.core.outfit.sync.SyncRecord
import com.dayloop.app.core.outfit.sync.ThreeWayMerge
import com.dayloop.app.core.outfit.sync.TrackedRecord
import com.dayloop.app.core.outfit.sync.WardrobeSyncMapper
import com.dayloop.app.ui.components.DayLoopIconButton
import com.dayloop.app.ui.components.DayLoopButton
import com.dayloop.app.ui.components.DayLoopOutlinedButton
import com.dayloop.app.ui.theme.DayLoopTheme
import com.dayloop.app.ui.theme.dayLoopTokens

/**
 * Lets the user settle a sync conflict field by field. Nothing is overwritten before
 * "Keep selected"; "Decide later" leaves this phone's version in place and unsent.
 *
 * Not reachable in the shipped app yet: DayLoop has no sync transport (ADR 0006). A future
 * sync host shows this for each record whose [TrackedRecord.conflicts] is not empty and
 * passes the result to `WardrobeSyncEngine.resolveConflict`.
 */
@Composable
fun SyncConflictDialog(
    record: TrackedRecord,
    onResolve: (SyncRecord) -> Unit,
    onLater: () -> Unit,
    thumbnail: (@Composable () -> Unit)? = null,
) {
    Dialog(onDismissRequest = onLater, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        SyncConflictContent(record, onResolve, onLater, thumbnail)
    }
}

@Composable
internal fun SyncConflictContent(
    record: TrackedRecord,
    onResolve: (SyncRecord) -> Unit,
    onLater: () -> Unit,
    thumbnail: (@Composable () -> Unit)? = null,
) {
    val tokens = MaterialTheme.dayLoopTokens
    val colors = tokens.colors
    var draft by remember(record) { mutableStateOf(ConflictDraft(record)) }
    val itemName = record.current.fields[WardrobeSyncMapper.NAME] ?: "This item"
    val merged = record.autoMerged

    Surface(Modifier.fillMaxSize(), color = colors.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = tokens.spacing.pageMargin)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                DayLoopIconButton(R.drawable.ic_action_back, "Decide later", onLater)
                Text("Two versions", Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "$itemName was changed on this phone and on another device. Nothing has been overwritten. Choose what to keep for each detail.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    thumbnail?.invoke()
                    Column {
                        Text(itemName, style = MaterialTheme.typography.titleSmall)
                        Text(
                            buildString {
                                append(if (record.conflicts.size == 1) "1 detail differs" else "${record.conflicts.size} details differ")
                                if (merged.isNotEmpty()) append(" · ${merged.size} other ${if (merged.size == 1) "change" else "changes"} merged automatically")
                            },
                            style = MaterialTheme.typography.bodySmall, color = colors.secondaryText,
                        )
                    }
                }
                record.conflicts.forEach { conflict ->
                    ConflictField(conflict, draft) { draft = it }
                }
                if (merged.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Merged without asking", style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
                        merged.forEach { (field, side) ->
                            Text(
                                "${WardrobeSyncMapper.fieldLabel(field)}: ${if (side == MergeSide.REMOTE) "changed on the other device" else "changed on this phone"}",
                                style = MaterialTheme.typography.bodySmall, color = colors.secondaryText,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(
                draft.blockingProblem ?: "Until you choose, this phone keeps its own version and does not send it.",
                style = MaterialTheme.typography.bodySmall, color = colors.secondaryText,
            )
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DayLoopOutlinedButton(onClick = onLater, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Decide later") }
                DayLoopButton(
                    onClick = { onResolve(draft.resolved()) },
                    enabled = draft.blockingProblem == null,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text("Keep selected") }
            }
        }
    }
}

@Composable
private fun ConflictField(conflict: FieldConflict, draft: ConflictDraft, onChange: (ConflictDraft) -> Unit) {
    val colors = MaterialTheme.dayLoopTokens.colors
    val field = conflict.field
    val chosen = draft.choices[field] ?: ConflictChoice.LOCAL
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(WardrobeSyncMapper.fieldLabel(field), style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
        if (field != ThreeWayMerge.DELETED_FIELD) {
            Text("Was: ${WardrobeSyncMapper.displayValue(field, conflict.base)}", style = MaterialTheme.typography.bodySmall, color = colors.secondaryText)
        }
        VersionOption(
            source = "This phone · kept for now",
            value = WardrobeSyncMapper.displayValue(field, conflict.local),
            selected = chosen == ConflictChoice.LOCAL,
            onSelect = { onChange(draft.choose(field, ConflictChoice.LOCAL)) },
        )
        VersionOption(
            source = "Other device",
            value = WardrobeSyncMapper.displayValue(field, conflict.remote),
            selected = chosen == ConflictChoice.REMOTE,
            onSelect = { onChange(draft.choose(field, ConflictChoice.REMOTE)) },
        )
        if (WardrobeSyncMapper.allowsTypedValue(field)) {
            OutlinedTextField(
                value = draft.custom[field].orEmpty(),
                onValueChange = { onChange(draft.typed(field, it.take(80))) },
                singleLine = true,
                label = { Text("Type a different ${WardrobeSyncMapper.fieldLabel(field).lowercase()}") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun VersionOption(source: String, value: String, selected: Boolean, onSelect: () -> Unit) {
    val colors = MaterialTheme.dayLoopTokens.colors
    val shape = RoundedCornerShape(MaterialTheme.dayLoopTokens.shapeScale.inputCorner)
    Column(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .border(BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.accentText else colors.controlBorder), shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(source, style = MaterialTheme.typography.bodySmall, color = if (selected) colors.accentText else colors.secondaryText)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Sample data only: the "White shirt" conflict from the design reference. */
@Preview
@Composable
private fun SyncConflictContentPreview() {
    val base = SyncRecord("shirt", mapOf("name" to "Shirt", "category" to "TOP"))
    val local = base.copy(fields = base.fields + mapOf("name" to "White work shirt", "occasions" to "CASUAL,WORK"))
    val remote = base.copy(fields = base.fields + mapOf("name" to "Oxford shirt", "color" to "#F5F5F5"))
    val merged = ThreeWayMerge.merge(base, local, remote)
    val record = TrackedRecord(
        current = merged.record, base = remote, baseRevision = 2,
        conflicts = (merged as? com.dayloop.app.core.outfit.sync.MergeResult.Conflicted)?.conflicts.orEmpty(),
        autoMerged = (merged as? com.dayloop.app.core.outfit.sync.MergeResult.Conflicted)?.autoMerged.orEmpty(),
    )
    DayLoopTheme { SyncConflictContent(record, onResolve = {}, onLater = {}) }
}
