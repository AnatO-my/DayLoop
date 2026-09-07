package com.dayloop.app

import android.app.Application
import com.dayloop.app.di.AppGraph
import com.dayloop.app.di.DefaultAppGraph

class DayLoopApplication : Application() {
    val appGraph: AppGraph by lazy {
        DefaultAppGraph(applicationContext = this)
    }
}