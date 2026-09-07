package com.dayloop.app.di

import androidx.compose.runtime.staticCompositionLocalOf

val LocalAppGraph = staticCompositionLocalOf<AppGraph> {
    error("AppGraph is not available in this Compose tree.")
}