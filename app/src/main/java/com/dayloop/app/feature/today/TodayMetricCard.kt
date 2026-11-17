package com.dayloop.app.feature.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.dayloop.app.R
import com.dayloop.app.ui.components.DayLoopIcon

// The two outlines share geometry: the hit/ripple silhouette also includes the arrow disc.
internal class TodayMetricShape(private val includeArrow: Boolean = false) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val corner = with(density) { 24.dp.toPx() }.coerceAtMost(size.width / 4)
        val notch = with(density) { 56.dp.toPx() }.coerceAtMost(size.width / 2)
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(corner, 0f)
            lineTo(w - notch - corner, 0f)
            quadraticTo(w - notch, 0f, w - notch, corner)
            lineTo(w - notch, notch - corner)
            quadraticTo(w - notch, notch, w - notch + corner, notch)
            lineTo(w - corner, notch)
            quadraticTo(w, notch, w, notch + corner)
            lineTo(w, h - corner)
            quadraticTo(w, h, w - corner, h)
            lineTo(corner, h)
            quadraticTo(0f, h, 0f, h - corner)
            lineTo(0f, corner)
            quadraticTo(0f, 0f, corner, 0f)
            close()
            if (includeArrow) {
                val diameter = with(density) { 48.dp.toPx() }
                addOval(Rect(w - diameter, 0f, w, diameter))
            }
        }
        return Outline.Generic(path)
    }
}

@Composable
internal fun TodayMetricCard(
    value: String,
    label: String,
    period: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = Color(0xFF171B16)
    Box(
        modifier.heightIn(min = 180.dp)
            .clip(TodayMetricShape(includeArrow = true))
            .clickable(enabled = enabled, role = Role.Button,
                onClickLabel = "Show ${label.replace('\n', ' ').lowercase()}", onClick = onClick)
    ) {
        Box(Modifier.matchParentSize().background(color, TodayMetricShape()))
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 36.dp, bottom = 16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineLarge, color = ink)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = ink)
            Text(
                text = period,
                style = MaterialTheme.typography.bodySmall,
                color = ink.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp, start = 0.dp)
            )
        }
        Box(Modifier.align(Alignment.TopEnd).size(48.dp).background(Color(0xFF30332E), CircleShape),
            contentAlignment = Alignment.Center) {
            DayLoopIcon(R.drawable.ic_action_forward, modifier = Modifier.rotate(-45f), tint = Color.White)
        }
    }
}
