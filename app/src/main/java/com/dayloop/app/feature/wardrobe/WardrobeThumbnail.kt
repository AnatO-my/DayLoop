package com.dayloop.app.feature.wardrobe

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dayloop.app.R
import com.dayloop.app.core.model.ClothingItemId
import com.dayloop.app.di.LocalAppGraph
import com.dayloop.app.ui.components.DayLoopIcon
import com.dayloop.app.ui.theme.dayLoopTokens

/**
 * The saved picture of a wardrobe item, or the wardrobe icon when it has none. Decorative:
 * callers show the item's name as text beside it, so it carries no description of its own.
 */
@Composable
internal fun WardrobeThumbnail(itemId: ClothingItemId, modifier: Modifier = Modifier) {
    val graph = LocalAppGraph.current
    val colors = MaterialTheme.dayLoopTokens.colors
    val media by remember(itemId) { graph.wardrobeMediaRepository.observeMediaForItem(itemId.value) }
        .collectAsState(initial = emptyList())
    Box(modifier.clip(RoundedCornerShape(MaterialTheme.dayLoopTokens.shapeScale.inputCorner)).background(colors.surface), contentAlignment = Alignment.Center) {
        val file = media.firstOrNull()?.let { graph.wardrobeMediaRepository.getFile(it.id) }
        if (file != null) {
            WardrobeImageFrame(model = file, contentDescription = null, modifier = Modifier.fillMaxSize())
        } else {
            DayLoopIcon(R.drawable.ic_wardrobe, tint = colors.secondaryText)
        }
    }
}
