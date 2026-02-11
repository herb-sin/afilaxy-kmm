package com.afilaxy.app

import android.app.Application
import com.afilaxy.app.analytics.AnalyticsManager
import com.afilaxy.app.performance.AnrOptimizer
import com.afilaxy.app.performance.LogOptimizer
import com.afilaxy.di.platformModule
import com.afilaxy.di.sharedModule
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module

class AflixyApplication : Application() {
    
    lateinit var analyticsManager: AnalyticsManager
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        analyticsManager = AnalyticsManager(this)
        
        startKoin {
            androidLogger(Level.DEBUG)
            androidContext(this@AflixyApplication)
            modules(
                sharedModule(),
                platformModule(),
                module {
                    single { analyticsManager }
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
    }
}
