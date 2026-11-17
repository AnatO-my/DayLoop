package com.dayloop.app.feature.focus

import com.dayloop.app.R
import java.time.Instant
import java.util.Locale

internal data class FocusNotificationPresentation(
    val mode: FocusMode,
    val paused: Boolean,
    val remaining: String,
) {
    val label: String get() = mode.label + if (paused) " paused" else ""
    val iconRes: Int get() = when (mode) {
        FocusMode.WORK -> R.drawable.ic_focus_work
        FocusMode.SHORT_BREAK -> R.drawable.ic_focus_short_break
        FocusMode.LONG_BREAK -> R.drawable.ic_focus_long_break
    }

    companion object {
        fun from(state: FocusState, now: Instant): FocusNotificationPresentation {
            val seconds = state.secondsAt(now).coerceAtLeast(0)
            return FocusNotificationPresentation(state.activeMode ?: state.mode,
                state.activeMode != null && state.endsAt == null,
                String.format(Locale.US, "%02d:%02d", seconds / 60, seconds % 60))
        }
    }
}

internal enum class FocusNotificationCommand { PAUSE, RESUME, END }

// Viewing another tab does not invalidate controls for the active session.
internal fun FocusState.notificationToken(): String =
    "$notificationRevision:${activeMode?.name}:${endsAt?.toEpochMilli()}:$remaining:$sessionDuration"
