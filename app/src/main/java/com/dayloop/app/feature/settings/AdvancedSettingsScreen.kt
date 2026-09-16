package com.dayloop.app.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dayloop.app.feature.diagnostics.DiagnosticsRoute
import com.dayloop.app.ui.components.DayLoopScreen

@Composable
fun AdvancedSettingsScreen(
    onDismiss: () -> Unit,
    refreshKey: Int = 0
) {
    BackHandler(onBack = onDismiss)
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(Modifier.fillMaxSize()) {
            DiagnosticsRoute(
                onBack = onDismiss,
                refreshKey = refreshKey
            )
        }
    }
}
