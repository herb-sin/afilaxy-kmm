package com.afilaxy.app.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.afilaxy.app.MainActivity
import java.util.concurrent.TimeUnit
import java.util.Calendar

// ── Canais de notificação ──────────────────────────────────────────────────────

const val CHANNEL_CHECKIN = "checkin_channel"

fun createCheckInNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_CHECKIN,
            "Check-in de Saúde",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alertas diários de risco e check-in da bombinha de resgate"
        }
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }
}

// ── Worker Matinal (7:30 AM) ───────────────────────────────────────────────────

/**
 * Dispara notificação matinal em dias de risco moderado ou alto
 * perguntando se o usuário está com sua bombinha de resgate.
 *
 * Usa OneTimeWorkRequest reagendável para garantir hora exata (07:30).
 * Re-enfileira automaticamente ao completar.
 */
class MorningCheckInWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "morning_checkin_worker"
        const val KEY_INHALER_NAME = "inhaler_name"
        const val KEY_RISK_SCORE = "risk_score"
        const val MIN_RISK_SCORE_FOR_NOTIFICATION = 45  // score mínimo para notificar (risco moderado+)
        private const val PENDING_INTENT_MORNING_MAIN = 1001
        private const val PENDING_INTENT_MORNING_YES  = 1002
        private const val PENDING_INTENT_MORNING_NO   = 1003

        fun scheduleNext(context: Context, inhalerName: String? = null, riskScore: Int = 0) {
            val delay = minutesUntil(hour = 7, minute = 30)
            val data = workDataOf(
                KEY_INHALER_NAME to (inhalerName ?: ""),
                KEY_RISK_SCORE to riskScore
            )
            val request = OneTimeWorkRequestBuilder<MorningCheckInWorker>()
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .setInputData(data)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                // KEEP: não substitui se já estiver agendado (chamado de scheduleNext dentro do doWork).
                // REPLACE é usado apenas pelo AflixyApplication na inicialização first-run.
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        /** Agenda com KEEP — usado na inicialização do app para não resetar worker já agendado. */
        fun scheduleNextKeep(context: Context, inhalerName: String? = null, riskScore: Int = 0) {
            val delay = minutesUntil(hour = 7, minute = 30)
            val data = workDataOf(
                KEY_INHALER_NAME to (inhalerName ?: ""),
                KEY_RISK_SCORE to riskScore
            )
            val request = OneTimeWorkRequestBuilder<MorningCheckInWorker>()
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .setInputData(data)
                .addTag(WORK_NAME)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        val riskScore = inputData.getInt(KEY_RISK_SCORE, 0)
        val inhalerName = inputData.getString(KEY_INHALER_NAME)?.takeIf { it.isNotBlank() }

        // Só notifica em risco moderado ou superior (score >= 45)
        if (riskScore >= MIN_RISK_SCORE_FOR_NOTIFICATION) {
            showMorningNotification(inhalerName)
        }

        // Re-agenda para o próximo dia
        scheduleNext(context)
        return Result.success()
    }

    private fun showMorningNotification(inhalerName: String?) {
        createCheckInNotificationChannel(context)

        val inhalerDisplay = inhalerName ?: "broncodilatador de resgate"

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_screen", "morning_checkin")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, PENDING_INTENT_MORNING_MAIN, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action "Sim"
        val yesIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_screen", "morning_checkin_yes")
        }
        val yesPending = PendingIntent.getActivity(
            context, PENDING_INTENT_MORNING_YES, yesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action "Não"
        val noIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_screen", "morning_checkin_no")
        }
        val noPending = PendingIntent.getActivity(
            context, PENDING_INTENT_MORNING_NO, noIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CHECKIN)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Risco de crise alto hoje")
            .setContentText("Você está com seu $inhalerDisplay?")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("A qualidade do ar e o clima de hoje indicam risco elevado. " +
                        "Você está com seu $inhalerDisplay?")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_send, "✅ Sim", yesPending)
            .addAction(android.R.drawable.ic_delete, "❌ Não", noPending)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            ?.notify(NOTIFICATION_ID_MORNING, notification)
    }
}

// ── Worker Noturno (21:00) ─────────────────────────────────────────────────────

/**
 * Pergunta à noite se o usuário teve uma crise de asma durante o dia.
 * Coleta o dado de label para o futuro modelo de ML.
 */
class EveningCheckInWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "evening_checkin_worker"
        private const val PENDING_INTENT_EVENING_MAIN = 2001
        private const val PENDING_INTENT_EVENING_YES  = 2002
        private const val PENDING_INTENT_EVENING_NO   = 2003

        fun scheduleNext(context: Context) {
            val delay = minutesUntil(hour = 21, minute = 0)
            val request = OneTimeWorkRequestBuilder<EveningCheckInWorker>()
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        /** Agenda com KEEP — usado na inicialização do app para não resetar worker já agendado. */
        fun scheduleNextKeep(context: Context) {
            val delay = minutesUntil(hour = 21, minute = 0)
            val request = OneTimeWorkRequestBuilder<EveningCheckInWorker>()
                .setInitialDelay(delay, TimeUnit.MINUTES)
                .addTag(WORK_NAME)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        showEveningNotification()
        scheduleNext(context)
        return Result.success()
    }

    private fun showEveningNotification() {
        createCheckInNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_screen", "evening_checkin")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, PENDING_INTENT_EVENING_MAIN, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val yesIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_screen", "evening_checkin_yes")
        }
        val yesPending = PendingIntent.getActivity(
            context, PENDING_INTENT_EVENING_YES, yesIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val noIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_screen", "evening_checkin_no")
        }
        val noPending = PendingIntent.getActivity(
            context, PENDING_INTENT_EVENING_NO, noIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CHECKIN)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📋 Check-in noturno")
            .setContentText("Você teve alguma crise de asma hoje?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_send, "✅ Não tive", noPending)
            .addAction(android.R.drawable.ic_delete, "⚠️ Tive uma crise", yesPending)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            ?.notify(NOTIFICATION_ID_EVENING, notification)
    }
}

// ── Helpers ────────────────────────────────────────────────────────────────────

private const val MILLIS_PER_MINUTE = 60_000L

const val NOTIFICATION_ID_MORNING = 5001
const val NOTIFICATION_ID_EVENING = 5002

/** Calcula minutos até o próximo horário-alvo (mesmo dia ou dia seguinte).
 *
 * Usa `!after(now)` em vez de `before(now)` para cobrir o caso de borda onde
 * target == now (worker acabou de disparar). `maxOf(60L, ...)` garante mínimo
 * de 1 hora de delay, evitando que o ciclo dispare em loop rápido.
 */
fun minutesUntil(hour: Int, minute: Int): Long {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        // !after(now) cobre: target < now OU target == now
        if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
    }
    return maxOf(60L, (target.timeInMillis - now.timeInMillis) / MILLIS_PER_MINUTE)
}
