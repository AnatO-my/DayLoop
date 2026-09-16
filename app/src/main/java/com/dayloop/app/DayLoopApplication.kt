package com.dayloop.app

import android.app.Application
import com.dayloop.app.di.AppGraph
import com.dayloop.app.di.DefaultAppGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DayLoopApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        appGraph.reminderNotifications.createChannels()
        
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            appGraph.widgetStateReconciler.startObserving()
        }
    }

    val appGraph: AppGraph by lazy {
        DefaultAppGraph(applicationContext = this)
    }
}
