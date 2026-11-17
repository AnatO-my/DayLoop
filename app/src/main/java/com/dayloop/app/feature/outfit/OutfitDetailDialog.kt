package com.dayloop.app.feature.outfit

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.dayloop.app.R
import com.dayloop.app.core.model.WardrobeMediaId
import com.dayloop.app.core.model.label
import com.dayloop.app.di.LocalAppGraph
import com.dayloop.app.feature.wardrobe.rememberClothingImagePicker
import com.dayloop.app.ui.components.DayLoopIcon
import com.dayloop.app.ui.components.DayLoopIconButton
import com.dayloop.app.ui.components.DayLoopButton
import com.dayloop.app.ui.components.DayLoopOutlinedButton
import com.dayloop.app.ui.theme.dayLoopTokens
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Records which items and photos were worn on [date]. Behaviour is unchanged from the
 * earlier version; only the presentation follows the current theme (token colours,
 * theme shapes, category names in words, 48dp rows with checkbox semantics).
 */
@Composable
fun OutfitDetailDialog(date: LocalDate, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val appGraph = LocalAppGraph.current
        val coroutineScope = rememberCoroutineScope()
        val tokens = MaterialTheme.dayLoopTokens
        val colors = tokens.colors

        val allWardrobeItems by appGraph.outfitRepository.observeActiveWardrobe().collectAsState(initial = emptyList())
        val selectedItemIds = remember { mutableStateListOf<String>() }
        val selectedMediaIds = remember { mutableStateListOf<String>() }

        val imagePicker = rememberClothingImagePicker { uri ->
            coroutineScope.launch {
                val mediaId = appGraph.wardrobeMediaRepository.importImage(uri)
                if (mediaId != null) {
                    selectedMediaIds.add(mediaId.value)
                }
            }
        }

        LaunchedEffect(date) {
            val outfit = appGraph.outfitRepository.getOutfitForDate(date)
            if (outfit != null) {
                selectedItemIds.clear()
                selectedItemIds.addAll(outfit.items.map { it.id.value })
                selectedMediaIds.clear()
                selectedMediaIds.addAll(outfit.photos.map { it.id.value })
            }
        }

        Surface(
            color = colors.background,
            contentColor = colors.text,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(horizontal = tokens.spacing.pageMargin),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DayLoopIconButton(
                        resource = R.drawable.ic_action_back,
                        label = stringResource(R.string.outfit_record_close),
                        onClick = onDismiss,
                        tint = colors.text,
                    )
                    Text(
                        text = stringResource(R.string.outfit_record_title, date),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f).semantics { heading() },
                    )
                }
                Spacer(Modifier.height(tokens.spacing.itemGap))

                Surface(
                    shape = RoundedCornerShape(tokens.shapeScale.cardCorner),
                    color = colors.raised,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(tokens.spacing.cardPadding)) {
                        Text(
                            text = if (allWardrobeItems.isEmpty()) stringResource(R.string.outfit_record_empty) else stringResource(R.string.outfit_record_select_items),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.secondaryText,
                        )
                        Spacer(Modifier.height(tokens.spacing.itemGap))

                        LazyColumn(
                            modifier = Modifier.heightIn(max = 240.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(tokens.spacing.microGap),
                        ) {
                    items(allWardrobeItems, key = { it.id.value }) { item ->
                        val isChecked = selectedItemIds.contains(item.id.value)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .toggleable(value = isChecked, role = Role.Checkbox) { checked ->
                                    if (checked) selectedItemIds.add(item.id.value)
                                    else selectedItemIds.remove(item.id.value)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = isChecked, onCheckedChange = null)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(item.name, style = MaterialTheme.typography.bodyLarge)
                                Text(item.category.label, style = MaterialTheme.typography.bodySmall, color = colors.secondaryText)
                            }
                        }
                    }
                        }
                    }
                }

                Spacer(Modifier.height(tokens.spacing.sectionGap))

                Text(
                    text = stringResource(R.string.outfit_record_photos),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(tokens.spacing.relatedGap))

                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.relatedGap),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    selectedMediaIds.forEach { mediaId ->
                        Box(Modifier.size(64.dp)) {
                            AsyncImage(
                                model = appGraph.wardrobeMediaRepository.getFile(WardrobeMediaId(mediaId)),
                                contentDescription = stringResource(R.string.outfit_record_photo),
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(tokens.shapeScale.cardCorner)),
                                contentScale = ContentScale.Crop
                            )
                            DayLoopIconButton(
                                resource = R.drawable.ic_action_close,
                                label = stringResource(R.string.outfit_record_remove_photo),
                                onClick = { selectedMediaIds.remove(mediaId) },
                                modifier = Modifier.size(24.dp).align(Alignment.TopEnd),
                                tint = colors.error
                            )
                        }
                    }
                    Surface(
                        onClick = { imagePicker.launchGallery() },
                        modifier = Modifier.size(64.dp),
                        shape = RoundedCornerShape(tokens.shapeScale.cardCorner),
                        color = colors.raised
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            DayLoopIcon(R.drawable.ic_photo, description = stringResource(R.string.outfit_record_add_photo), tint = colors.secondaryText)
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.relatedGap),
                ) {
                    DayLoopOutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.wardrobe_cancel)) }
                    DayLoopButton(
                        onClick = {
                        coroutineScope.launch {
                            appGraph.outfitRepository.recordOutfit(date, selectedItemIds.toList(), selectedMediaIds.toList())
                            onDismiss()
                        }
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) { Text(stringResource(R.string.outfit_record_save)) }
                }
            }
        }
    }
}
