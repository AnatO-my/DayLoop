package com.dayloop.app.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.dayloop.app.MainActivity
import com.dayloop.app.settings.ThemeMode
import com.dayloop.app.settings.AppFont

internal data class WidgetColors(val background: ColorProvider, val text: ColorProvider,
    val muted: ColorProvider, val task: ColorProvider, val onTask: ColorProvider,
    val accent: ColorProvider, val onAccent: ColorProvider, val family: FontFamily)

internal fun widgetColors(context: Context, mode: ThemeMode, font: AppFont): WidgetColors {
    val dark = mode == ThemeMode.DARK || (mode == ThemeMode.SYSTEM &&
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES)
    val family = when (font) {
        AppFont.JETBRAINS_MONO, AppFont.GEIST_MONO, AppFont.SYSTEM_MONO -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }
    fun c(hex: Long) = ColorProvider(Color(hex))
    // Same values as the app's DayLoop tokens (surface, text, muted, raised, accent, onAccent).
    return if (dark) WidgetColors(c(0xFF141719), c(0xFFF2F3F4), c(0xFFABB0B3), c(0xFF202427), c(0xFFF2F3F4),
        c(0xFFFF6A24), c(0xFF000000), family)
    else WidgetColors(c(0xFFFFFFFF), c(0xFF000000), c(0xFF555555), c(0xFFE9ECEF), c(0xFF000000),
        c(0xFFFF6A24), c(0xFF000000), family)
}

internal fun widgetOpen(context: Context, route: String, taskId: String? = null): Action {
    val intent = Intent(context, MainActivity::class.java).apply {
        data = Uri.Builder().scheme("dayloop").authority("widget").appendPath(route)
            .apply { taskId?.let { appendPath(it) } }.build()
        putExtra("widget_route", route)
        taskId?.let { putExtra("edit_task_id", it) }
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    return actionStartActivity(intent)
}

@Composable
internal fun WidgetAction(label: String, action: Action?, colors: WidgetColors,
    modifier: GlanceModifier = GlanceModifier) {
    Box(modifier.height(48.dp).background(colors.accent).cornerRadius(4.dp)
        .let { if (action == null) it else it.clickable(action) }
        .padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
        Text(label, style = TextStyle(fontFamily = colors.family, color = colors.onAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold), maxLines = 1)
    }
}
