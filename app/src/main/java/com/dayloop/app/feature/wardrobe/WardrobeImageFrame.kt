package com.dayloop.app.feature.wardrobe

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dayloop.app.ui.theme.dayLoopTokens

/**
 * Keeps the whole garment visible without leaving a harsh, empty letterbox around photos
 * with a different aspect ratio from their card. The soft backdrop is decorative; TalkBack
 * only announces the foreground image.
 */
@Composable
internal fun WardrobeImageFrame(
    model: Any,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.dayLoopTokens.colors
    Box(
        modifier = modifier.background(colors.raised),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.16f),
            contentScale = ContentScale.Crop,
        )
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize().padding(10.dp),
            contentScale = ContentScale.Fit,
        )
    }
}
