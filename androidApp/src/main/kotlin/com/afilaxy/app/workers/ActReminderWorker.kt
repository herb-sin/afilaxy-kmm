package com.afilaxy.app.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.afilaxy.app.MainActivity
import java.util.concurrent.TimeUnit

// ── Worker mensal do ACT (Asthma Control Test) ─────────────────────────────────

/**
 * Lembra o usuário de refazer o ACT a cada 28 dias — o instrumento tem janela
 * retrospectiva de 4 semanas, então essa é a cadência correta (não diária).
 * Mesmo padrão de auto-reagendamento dos workers de check-in matinal/noturno.
 */
class ActReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "act_reminder_worker"
        const val INTERVAL_DAYS = 28L
        private const val PENDING_INTENT_ACT = 3001
        const val NOTIFICATION_ID_ACT = 5003

        fun scheduleNext(context: Context) {
            val request = OneTimeWorkRequestBuilder<ActReminderWorker>()
                .setInitialDelay(INTERVAL_DAYS, TimeUnit.DAYS)
                .addTag(WORK_NAME)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME, ExistingWorkPolicy.REPLACE, request
            )
        }

        /** Agenda com KEEP — usado na inicialização do app para não resetar worker já agendado. */
        fun scheduleNextKeep(context: Context) {
            val request = OneTimeWorkRequestBuilder<ActReminderWorker>()
                .setInitialDelay(INTERVAL_DAYS, TimeUnit.DAYS)
                .addTag(WORK_NAME)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME, ExistingWorkPolicy.KEEP, request
            )
        }
    }

    override suspend fun doWork(): Result {
        showReminderNotification()
        scheduleNext(context)
        return Result.success()
    }

    private fun showReminderNotification() {
        createCheckInNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_screen", "act_test")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, PENDING_INTENT_ACT, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CHECKIN)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📋 Teste de Controle da Asma (ACT)")
            .setContentText("Faz um mês — hora de reavaliar como sua asma esteve nas últimas 4 semanas.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            ?.notify(NOTIFICATION_ID_ACT, notification)
    }
}
