package com.dayloop.app.feature.idle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalConfiguration
import com.dayloop.app.ui.theme.DayLoopTheme
import com.dayloop.app.core.model.*
import com.dayloop.app.feature.onboarding.DayLoopMascot
import com.dayloop.app.feature.onboarding.MascotMood
import com.dayloop.app.settings.ThemeMode
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle


@Composable
fun IdleScreen(date: LocalDate, tasks: List<Task>, onExit: () -> Unit) {
    Dialog(onDismissRequest = onExit, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        IdleScreenContent(date, tasks, onExit)
    }
}

@Composable
internal fun IdleScreenContent(date: LocalDate, tasks: List<Task>, onExit: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val threshold = with(LocalDensity.current) { 64.dp.toPx() }
    val exit by rememberUpdatedState(onExit)

    Box(Modifier.fillMaxSize()) {
        // Backdrop click to dismiss
        Box(Modifier.matchParentSize().clickable(onClickLabel = "Dismiss idle calendar", onClick = onExit))

        // Surface spans edge-to-edge completely without padding constraints
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .semantics { paneTitle = "Idle calendar"; dismiss { onExit(); true } }
                .pointerInput(threshold) {
                    var distance = 0f
                    detectVerticalDragGestures(
                        onDragStart = { distance = 0f },
                        onDragCancel = { distance = 0f },
                        onDragEnd = { if (distance <= -threshold) exit() }
                    ) { change, amount ->
                        change.consume()
                        distance += amount
                    }
                },
            color = colors.background,
            shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
        ) {
            // CalendarFace receives the inner padding so its text and layout remain safely
            // clear of the status bar/notch while the background covers the entire top edge.
            CalendarFace(
                date = date,
                tasks = tasks,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            )
        }
    }
}

@Composable
internal fun CalendarFace(date: LocalDate, tasks: List<Task>, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.testTag("calendar-face")) {
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                CalendarHeading(date, Modifier.weight(1f).fillMaxHeight())
                CalendarDots(date, tasks, Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                CalendarHeading(date, Modifier.weight(0.52f).fillMaxWidth())
                CalendarDots(date, tasks, Modifier.weight(0.48f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CalendarHeading(date: LocalDate, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]

    Column(modifier, verticalArrangement = Arrangement.Bottom) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomStart) {
            Column(horizontalAlignment = Alignment.Start) {
                DayLoopMascot(
                    modifier = Modifier.size(120.dp),
                    mood = MascotMood.STARGAZING
                )
                Spacer(Modifier.height(16.dp))
                BasicText(date.dayOfMonth.toString(), modifier = Modifier.testTag("calendar-day"),
                    style = MaterialTheme.typography.displayLarge.copy(color = colors.onBackground,
                        fontWeight = FontWeight.Bold, lineHeight = TextUnit.Unspecified),
                    maxLines = 1, autoSize = TextAutoSize.StepBased(32.sp, 164.sp))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            BasicText(date.month.getDisplayName(TextStyle.FULL, locale).uppercase(locale),
                modifier = Modifier.weight(1f).alignByBaseline().testTag("calendar-month"),
                style = MaterialTheme.typography.headlineSmall.copy(color = colors.onBackground, fontWeight = FontWeight.Bold),
                maxLines = 1, autoSize = TextAutoSize.StepBased(12.sp, 28.sp))
            Text(date.format(DateTimeFormatter.ofPattern("EEE", locale)), Modifier.alignByBaseline().testTag("calendar-weekday"),
                style = MaterialTheme.typography.titleLarge, color = colors.onBackground.copy(alpha = 0.7f))
        }
        Text(date.year.toString(), Modifier.testTag("calendar-year"), style = MaterialTheme.typography.titleLarge,
            color = colors.onBackground.copy(alpha = 0.65f))
    }
}

@Composable
private fun CalendarDots(date: LocalDate, tasks: List<Task>, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val first = date.withDayOfMonth(1)
    val offset = first.dayOfWeek.value - 1
    val weeks = (offset + date.lengthOfMonth() + 6) / 7
    val past = if (colors.background.luminance() < 0.5f) Color(0xFFE8E8E8) else Color(0xFF002A32)
    val completedDays = remember(tasks) { tasks.filter { it.status == TaskStatus.COMPLETED }.mapNotNull {
        when (val schedule = it.schedule) {
            is TaskSchedule.AllDay -> schedule.date
            is TaskSchedule.Deadline -> schedule.dueAt.toLocalDate()
            is TaskSchedule.TimeBlock -> schedule.startsAt.toLocalDate()
            else -> null
        }
    }.toSet() }
    val density = LocalDensity.current
    val labelPixels = with(density) { (24.dp * fontScale).roundToPx() }
    val gapPixels = with(density) { 3.dp.roundToPx() }
    val labels = with(density) { labelPixels.toDp() }
    val gap = with(density) { gapPixels.toDp() }
    BoxWithConstraints(modifier, contentAlignment = Alignment.TopCenter) {
        // Quantise once: independent dp rounding otherwise squeezes the final cell in a row.
        val pixels = with(density) { kotlin.math.floor(calendarDiameter(maxWidth.roundToPx().toFloat(),
            maxHeight.roundToPx().toFloat(), weeks, gapPixels.toFloat(), labelPixels.toFloat())).toInt() }
        val diameter = with(density) { pixels.toDp() }
        val gridWidth = with(density) { (pixels * 7 + gapPixels * 6).toDp() }
        Column(Modifier.width(gridWidth)) {
            Row(Modifier.fillMaxWidth().height(labels), horizontalArrangement = Arrangement.spacedBy(gap)) {
                repeat(7) { i ->
                    Box(Modifier.width(diameter), contentAlignment = Alignment.Center) {
                        Text(DayOfWeek.of(i + 1).getDisplayName(TextStyle.NARROW, locale),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (i == date.dayOfWeek.value - 1) colors.primary else colors.onBackground.copy(alpha = 0.55f))
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                repeat(weeks) { week ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        repeat(7) { column ->
                            val day = week * 7 + column - offset + 1
                            val actual = day in 1..date.lengthOfMonth()
                            val cellDate = if (actual) first.withDayOfMonth(day) else null
                            val color = when {
                                cellDate == date -> colors.primary
                                cellDate != null && cellDate.isBefore(date) -> past
                                actual -> colors.onBackground.copy(alpha = 0.14f)
                                else -> Color.Transparent
                            }
                            Box(Modifier.size(diameter).background(color, CircleShape)
                                .then(if (!actual) Modifier.border(1.dp, colors.onBackground.copy(alpha = 0.14f), CircleShape) else Modifier)
                                .testTag("calendar-cell-${week * 7 + column}")
                                .semantics {
                                    if (actual) contentDescription = "$cellDate" +
                                            when { cellDate == date -> ", today"; cellDate!!.isBefore(date) -> ", past day"; else -> ", upcoming" } +
                                            if (cellDate in completedDays) ", scheduled work completed" else ""
                                })
                        }
                    }
                }
            }
        }
    }
}
@Preview(name = "Idle Screen Light", showBackground = true)
@Preview(name = "Idle Screen Dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun IdleScreenContentPreview() {
    // Determine dark mode based on whether the preview is rendering in night mode
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    DayLoopTheme(themeMode = if (isDark) ThemeMode.DARK else ThemeMode.LIGHT) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            IdleScreenContent(
                date = LocalDate.now(),
                tasks = emptyList(),
                onExit = {}
            )
        }
    }
}