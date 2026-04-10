package com.afilaxy.app

import android.app.Application
import com.afilaxy.app.analytics.AnalyticsManager
import com.afilaxy.app.performance.AnrOptimizer
import com.afilaxy.app.performance.LogOptimizer
import com.afilaxy.app.workers.EveningCheckInWorker
import com.afilaxy.app.workers.MorningCheckInWorker
import com.afilaxy.app.workers.createCheckInNotificationChannel
import com.afilaxy.di.platformModule
import com.afilaxy.di.sharedModule
import com.afilaxy.util.FileLogger
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module

class AflixyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        FileLogger.initialize(this)

        startKoin {
            // DEBUG em builds de desenvolvimento, ERROR em produção
            val koinLogLevel = if ((applicationInfo.flags and
                android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0)
                Level.DEBUG else Level.ERROR
            androidLogger(koinLogLevel)
            androidContext(this@AflixyApplication)
            modules(
                sharedModule(),
                platformModule(),
                module {
                    single { AnalyticsManager(androidContext()) }
                }
            )
        }
        
        AnrOptimizer.executeAsync {
            initializeServices()
        }
    }
    
    private suspend fun initializeServices() {
        withContext(Dispatchers.IO) {
            try {
                val app = FirebaseApp.initializeApp(this@AflixyApplication)
                if (app != null) {
                    LogOptimizer.d("AflixyApp", "Firebase initialized")
                } else {
                    LogOptimizer.d("AflixyApp", "Firebase already initialized")
                }
            } catch (e: Exception) {
                LogOptimizer.e("AflixyApp", "Error initializing services", e)
            }
        }
        withContext(Dispatchers.Main) {
            // Cria canais de notificação (necessário Android 8+)
            createCheckInNotificationChannel(this@AflixyApplication)
            // Agenda workers de check-in (idempotente — ExistingWorkPolicy.REPLACE)
            MorningCheckInWorker.scheduleNext(this@AflixyApplication)
            EveningCheckInWorker.scheduleNext(this@AflixyApplication)
            LogOptimizer.d("AflixyApp", "Check-in workers agendados")
        }
    }
}
