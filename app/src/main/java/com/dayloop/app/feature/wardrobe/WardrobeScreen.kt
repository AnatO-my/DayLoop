package com.dayloop.app.feature.wardrobe

import androidx.compose.material3.MaterialTheme
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dayloop.app.core.model.ClothingCategory
import com.dayloop.app.core.model.ClothingItem
import com.dayloop.app.core.model.ClothingItemId
import com.dayloop.app.di.LocalAppGraph
import com.dayloop.app.R
import com.dayloop.app.ui.components.*
import com.dayloop.app.ui.guides.GuideTargets
import com.dayloop.app.ui.guides.guideTarget
import com.dayloop.app.ui.theme.dayLoopTokens
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
private fun ClothingCategory.label() = when (this) {
    ClothingCategory.TOP -> stringResource(R.string.wardrobe_category_tops)
    ClothingCategory.BOTTOM -> stringResource(R.string.wardrobe_category_bottoms)
    ClothingCategory.FOOTWEAR -> stringResource(R.string.wardrobe_category_shoes)
    ClothingCategory.HEADWEAR -> stringResource(R.string.wardrobe_category_headwear)
    ClothingCategory.OUTERWEAR -> stringResource(R.string.wardrobe_category_outerwear)
    ClothingCategory.ACCESSORY -> stringResource(R.string.wardrobe_category_accessories)
}

@Composable
fun WardrobeScreen(onDismiss: () -> Unit, onImport: (() -> Unit)? = null) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val graph = LocalAppGraph.current
        val scope = rememberCoroutineScope()
        val context = LocalContext.current
        val wardrobe by graph.outfitRepository.observeAllWardrobe().collectAsState(initial = emptyList())
        val tokens = MaterialTheme.dayLoopTokens
        val colors = tokens.colors
        var filter by rememberSaveable { mutableStateOf<String?>(null) }
        var showArchived by rememberSaveable { mutableStateOf(false) }
        var showAdd by rememberSaveable { mutableStateOf(false) }
        var name by rememberSaveable { mutableStateOf("") }
        var category by rememberSaveable { mutableStateOf(ClothingCategory.TOP.name) }
        var draftPhoto by remember { mutableStateOf<Uri?>(null) }
        var pendingDelete by remember { mutableStateOf<ClothingItem?>(null) }
        var pendingImageItem by remember { mutableStateOf<ClothingItem?>(null) }
        var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
        var busy by remember { mutableStateOf(false) }
        var error by remember { mutableStateOf<String?>(null) }
        val activeWardrobe = wardrobe.filterNot { it.isArchived }
        val visible = wardrobe.filter { (showArchived || !it.isArchived) && (filter == null || it.category.name == filter) }
        var reviewPhoto by remember { mutableStateOf<Uri?>(null) }
        var editingDetails by remember { mutableStateOf<ClothingItem?>(null) }
        var stylingAnchor by remember { mutableStateOf<ClothingItem?>(null) }
        var showStyling by rememberSaveable { mutableStateOf(false) }
        val picker = rememberClothingImagePicker(onError = { error = it }) { uri ->
            if (pendingImageItem != null) pendingImageUri = uri
            // A new item's photo goes through on-device recognition and an editable review.
            else reviewPhoto = uri
        }

        fun changeArchive(item: ClothingItem) {
            if (busy) return
            busy = true
            scope.launch {
                try {
                    if (item.isArchived) graph.outfitRepository.unarchiveClothingItem(item.id.value)
                    else graph.outfitRepository.archiveClothingItem(item.id.value)
                    error = null
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { error = context.getString(R.string.wardrobe_update_failed) }
                finally { busy = false }
            }
        }

        Surface(Modifier.fillMaxSize(), color = colors.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = tokens.spacing.pageMargin)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    DayLoopIconButton(R.drawable.ic_action_back, stringResource(R.string.wardrobe_close), onDismiss,
                        modifier = Modifier.offset(x = -12.dp))
                    Text(stringResource(R.string.wardrobe_title), Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge)
                    DayLoopIconButton(R.drawable.ic_action_sparkle, stringResource(R.string.wardrobe_outfit_ideas), {
                        stylingAnchor = null
                        showStyling = true
                    }, enabled = !busy && wardrobe.any { !it.isArchived })
                    DayLoopIconButton(R.drawable.ic_action_add, stringResource(R.string.wardrobe_add_item), {
                        draftPhoto = null
                        showAdd = true
                    }, modifier = Modifier.guideTarget(GuideTargets.WARDROBE_ADD), enabled = !busy, tint = colors.accentText)
                }
                Spacer(Modifier.height(16.dp))
                WardrobeInventoryRail(activeWardrobe)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    val filters = listOf<String?>(null) + ClothingCategory.entries.map { it.name }
                    filters.forEach { key ->
                        val selected = key == filter
                        Column {
                            TextButton(onClick = { filter = key }) {
                                Text(key?.let { ClothingCategory.valueOf(it).label() } ?: stringResource(R.string.wardrobe_filter_all),
                                    color = if (selected) colors.accentText else colors.secondaryText)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.wardrobe_items_shown, visible.size), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.wardrobe_archived), style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = showArchived, onCheckedChange = { showArchived = it })
                }
                error?.let { Text(it, color = colors.error, style = MaterialTheme.typography.bodySmall) }
                if (visible.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            DayLoopIcon(R.drawable.ic_wardrobe, tint = colors.secondaryText)
                            Spacer(Modifier.height(12.dp))
                            Text(if (wardrobe.isEmpty()) stringResource(R.string.wardrobe_empty) else stringResource(R.string.wardrobe_empty_category),
                                style = MaterialTheme.typography.bodyMedium)
                            if (wardrobe.isEmpty() && onImport != null) {
                                TextButton(onClick = onImport) { Text(stringResource(R.string.wardrobe_import), color = colors.accentText) }
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(140.dp),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        items(visible, key = { it.id.value }) { item ->
                            val media by graph.wardrobeMediaRepository.observeMediaForItem(item.id.value)
                                .collectAsState(initial = emptyList())
                            var menu by remember { mutableStateOf(false) }
                            Column {
                                Box(Modifier.fillMaxWidth().aspectRatio(0.85f).clip(RoundedCornerShape(MaterialTheme.dayLoopTokens.shapeScale.smallCorner))
                                    .background(colors.raised)
                                    .clickable(enabled = !busy, onClickLabel = stringResource(R.string.wardrobe_add_or_replace_photo, item.name)) {
                                        pendingImageItem = item
                                        picker.launchGallery()
                                    }, contentAlignment = Alignment.Center) {
                                    if (media.isNotEmpty()) {
                                        graph.wardrobeMediaRepository.getFile(media.first().id)?.let { file ->
                                            WardrobeImageFrame(
                                                model = file,
                                                contentDescription = item.name,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            DayLoopIcon(R.drawable.ic_photo, tint = colors.secondaryText)
                                            Text(stringResource(R.string.wardrobe_add_photo), style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.name, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                                    Box {
                                        DayLoopIconButton(R.drawable.ic_action_menu, stringResource(R.string.wardrobe_item_actions, item.name), { menu = true }, enabled = !busy)
                                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                            DropdownMenuItem(text = { Text(stringResource(R.string.wardrobe_edit_details)) },
                                                onClick = { menu = false; editingDetails = item })
                                            if (!item.isArchived) {
                                                DropdownMenuItem(text = { Text(stringResource(R.string.wardrobe_style_this)) },
                                                    onClick = { menu = false; stylingAnchor = item; showStyling = true })
                                            }
                                            DropdownMenuItem(text = { Text(if (item.isArchived) stringResource(R.string.wardrobe_restore) else stringResource(R.string.wardrobe_archive)) },
                                                onClick = { menu = false; changeArchive(item) })
                                            DropdownMenuItem(text = { Text(stringResource(R.string.wardrobe_delete)) },
                                                onClick = { menu = false; pendingDelete = item })
                                        }
                                    }
                                }
                                Text(if (item.isArchived) stringResource(R.string.wardrobe_category_archived, item.category.label()) else item.category.label().uppercase(),
                                    style = MaterialTheme.typography.bodySmall, color = colors.secondaryText)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DayLoopOutlinedButton(onClick = {
                        pendingImageItem = null
                        picker.launchCamera()
                    }, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.wardrobe_take_photo), color = colors.text)
                    }
                    DayLoopOutlinedButton(onClick = {
                        pendingImageItem = null
                        picker.launchGallery()
                    }, enabled = !busy, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                        DayLoopIcon(R.drawable.ic_photo, tint = colors.text)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.wardrobe_extract), color = colors.text)
                    }
                }
            }
        }

        if (showAdd) {
            AlertDialog(
                onDismissRequest = { if (!busy) { showAdd = false; draftPhoto = null } },
                title = { Text(stringResource(R.string.wardrobe_new_item)) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        draftPhoto?.let {
                            AsyncImage(it, stringResource(R.string.wardrobe_selected_photo), Modifier.fillMaxWidth().height(120.dp), contentScale = ContentScale.Fit)
                        }
                        OutlinedTextField(name, { name = it }, singleLine = true, label = { Text(stringResource(R.string.wardrobe_name)) }, enabled = !busy)
                        DayLoopOptionGrid(ClothingCategory.entries.map { DayLoopPillOption(it.name, it.label()) },
                            category, { category = it }, enabled = !busy)
                        error?.let { Text(it, color = colors.error) }
                    }
                },
                confirmButton = {
                    DayLoopButton(enabled = name.isNotBlank() && !busy, onClick = {
                        busy = true
                        val item = ClothingItem(ClothingItemId.generate(), name.trim(), ClothingCategory.valueOf(category))
                        scope.launch {
                            try {
                                graph.outfitRepository.saveClothingItem(item)
                                draftPhoto?.let { pendingImageItem = item; pendingImageUri = it }
                                draftPhoto = null
                                name = ""
                                showAdd = false
                                error = null
                            } catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { error = context.getString(R.string.wardrobe_save_item_failed) }
                            finally { busy = false }
                        }
                    }) { Text(stringResource(R.string.wardrobe_save)) }
                },
                dismissButton = { TextButton(enabled = !busy, onClick = { showAdd = false; draftPhoto = null }) { Text(stringResource(R.string.wardrobe_cancel)) } },
            )
        }
        pendingImageUri?.let { uri ->
            SimpleCropScreen(imageUri = uri, onConfirm = { bitmap, labels ->
                val item = pendingImageItem
                if (item != null && !busy) {
                    busy = true
                    scope.launch {
                        try {
                            val id = if (bitmap != null) graph.wardrobeMediaRepository.importBitmap(bitmap, labels)
                                else graph.wardrobeMediaRepository.importImage(uri)
                            checkNotNull(id)
                            graph.wardrobeMediaRepository.linkMediaToItem(item.id, id, isPrimary = true)
                            pendingImageUri = null
                            pendingImageItem = null
                            error = null
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { error = context.getString(R.string.wardrobe_save_photo_failed) }
                        finally { busy = false }
                    }
                }
            }, onCancel = { if (!busy) { pendingImageUri = null; pendingImageItem = null } })
        }
        reviewPhoto?.let { uri ->
            GarmentReviewDialog(
                photo = uri,
                onFinished = { reviewPhoto = null; picker.discardCapture(); error = null },
                onCancel = { reviewPhoto = null; picker.discardCapture() },
            )
        }
        editingDetails?.let { item -> GarmentDetailsDialog(item) { editingDetails = null } }
        if (showStyling) StylingScreen(stylingAnchor) { showStyling = false; stylingAnchor = null }
        pendingDelete?.let { item ->
            AlertDialog(onDismissRequest = { if (!busy) pendingDelete = null },
                title = { Text(stringResource(R.string.wardrobe_delete_item_title)) },
                text = { Text(stringResource(R.string.wardrobe_delete_item_message, item.name)) },
                confirmButton = {
                    TextButton(enabled = !busy, onClick = {
                        busy = true
                        scope.launch {
                            try { graph.outfitRepository.deleteClothingItem(item.id.value); pendingDelete = null; error = null }
                            catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { error = context.getString(R.string.wardrobe_delete_failed); pendingDelete = null }
                            finally { busy = false }
                        }
                    }) { Text(stringResource(R.string.wardrobe_delete), color = colors.error) }
                },
                dismissButton = { TextButton(enabled = !busy, onClick = { pendingDelete = null }) { Text(stringResource(R.string.wardrobe_cancel)) } })
        }
    }
}

@Composable
private fun WardrobeInventoryRail(items: List<ClothingItem>) {
    val colors = MaterialTheme.dayLoopTokens.colors
    val otherCount = items.count {
        it.category in setOf(ClothingCategory.FOOTWEAR, ClothingCategory.HEADWEAR, ClothingCategory.ACCESSORY)
    }
    val metrics = listOf(
        stringResource(R.string.wardrobe_rail_items) to items.size,
        stringResource(R.string.wardrobe_rail_tops) to items.count { it.category == ClothingCategory.TOP },
        stringResource(R.string.wardrobe_rail_bottoms) to items.count { it.category == ClothingCategory.BOTTOM },
        stringResource(R.string.wardrobe_rail_outer) to items.count { it.category == ClothingCategory.OUTERWEAR },
        stringResource(R.string.wardrobe_rail_other) to otherCount,
    )

    Column(Modifier.fillMaxWidth()) {
        HorizontalDivider(color = colors.text, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            metrics.forEachIndexed { index, (label, count) ->
                if (index > 0) {
                    VerticalDivider(
                        modifier = Modifier.height(20.dp).padding(horizontal = 12.dp),
                        color = colors.text,
                        thickness = 1.dp,
                    )
                }
                Text(
                    text = stringResource(R.string.wardrobe_rail_metric, label, count),
                    color = colors.text,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        HorizontalDivider(color = colors.text, thickness = 1.dp)
    }
}

