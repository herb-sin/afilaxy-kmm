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
import com.google.firebase.appcheck.FirebaseAppCheck
import com.afilaxy.app.appcheck.getAppCheckProviderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
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
                    single { AnalyticsManager(androidContext(), get()) }
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
                val appCheck = FirebaseAppCheck.getInstance()
                appCheck.installAppCheckProviderFactory(getAppCheckProviderFactory())
                LogOptimizer.d("AflixyApp", "App Check instalado")
                // Pre-warm: aguarda o token Play Integrity antes das primeiras writes ao
                // Firestore. Elimina a janela de PERMISSION_DENIED por timing — sem isso,
                // 39% das requests saem com token inválido (cf. Firebase App Check metrics).
                try {
                    appCheck.getAppCheckToken(false).await()
                    LogOptimizer.d("AflixyApp", "App Check token pronto")
                } catch (e: Exception) {
                    // Token será obtido lazily na 1ª request — falha aqui não é crítica
                    LogOptimizer.w("AflixyApp", "Pre-warm App Check falhou: ${e.message}")
                }
            } catch (e: Exception) {
                LogOptimizer.e("AflixyApp", "Error initializing services", e)
            }
        }
        withContext(Dispatchers.Main) {
            // Cria canais de notificação (necessário Android 8+)
            createCheckInNotificationChannel(this@AflixyApplication)
            // Agenda workers de check-in (idempotente — ExistingWorkPolicy.REPLACE)
            // KEEP: se o worker já estiver agendado do último ciclo, não substituir.
            // REPLACE causaria reset do delay a cada abertura do app, podendo disparar
            // uma notificação imediatamente se minutesUntil() retornar 0 (borda de horário).
            MorningCheckInWorker.scheduleNextKeep(this@AflixyApplication)
            EveningCheckInWorker.scheduleNextKeep(this@AflixyApplication)
            LogOptimizer.d("AflixyApp", "Check-in workers agendados")
        }
    }
}
