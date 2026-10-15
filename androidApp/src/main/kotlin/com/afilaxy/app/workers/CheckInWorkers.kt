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
import com.afilaxy.app.util.canPostNotifications
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ── Canais de notificação ──────────────────────────────────────────────────────

const val CHANNEL_CHECKIN = "checkin_channel"

fun createCheckInNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_CHECKIN,
            "Check-in de Bem-estar",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Lembretes diários de bem-estar e alertas da comunidade"
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
        val inhalerName = inputData.getString(KEY_INHALER_NAME)?.takeIf { it.isNotBlank() }

        // Gate de score REAL: o inputData(KEY_RISK_SCORE) era sempre 0 (nenhum
        // chamador passa score), então a condição abaixo nunca era satisfeita e a
        // notificação nunca disparava. Consulta o snapshot do Firestore antes.
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val riskScore = uid?.let { fetchLatestRiskScore(it) }

        val shouldNotify = when {
            // Deslogado: não notifica (mas re-agenda sempre, ao final).
            uid == null -> false
            // Score desconhecido (sem snapshot OU falha de rede): NOTIFICA mesmo
            // assim — fail-open. O lembrete é benigno e perder o check-in diário
            // piora o dado do produto; nenhum conteúdo sensível é exposto.
            riskScore == null -> true
            // Score conhecido: só em risco moderado ou superior (>= 45).
            else -> riskScore >= MIN_RISK_SCORE_FOR_NOTIFICATION
        }
        if (shouldNotify) {
            showMorningNotification(inhalerName)
        }

        // Re-agenda para o próximo dia (incondicional)
        scheduleNext(context)
        return Result.success()
    }

    /**
     * Lê o score de risco real em `risk_scores/{uid}/snapshots/{yyyy-MM-dd}`.
     * Se o snapshot de hoje não existir, busca o último disponível (orderBy "date").
     * Retorna null quando não há snapshot ou quando a leitura falha (rede),
     * sinalizando "desconhecido" para o fail-open do [doWork].
     * SimpleDateFormat em vez de java.time.LocalDate: minSdk 24 (LocalDate é API 26).
     */
    private suspend fun fetchLatestRiskScore(uid: String): Int? {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val snapshots = FirebaseFirestore.getInstance()
            .collection("risk_scores").document(uid).collection("snapshots")
        return try {
            val todayDoc = snapshots.document(today).get().await()
            (todayDoc.get("score") as? Number)?.toInt()
                ?: snapshots
                    .orderBy("date", Query.Direction.DESCENDING)
                    .limit(1)
                    .get().await().documents.firstOrNull()
                    ?.let { (it.get("score") as? Number)?.toInt() }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Cancelamento do worker NÃO é falha de rede: propagar para que o
            // coroutine encerre e o fail-open (notificação) não dispare após o
            // cancelamento.
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private fun showMorningNotification(inhalerName: String?) {
        // Sem permissão POST_NOTIFICATIONS concedida o notify() seria descartado
        // em silêncio pelo sistema — evita o trabalho inútil (C1).
        if (!context.canPostNotifications()) return

        createCheckInNotificationChannel(context)

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
            .setContentTitle("☀️ Check-in Matinal")
            .setContentText("Como está seu bem-estar esta manhã?")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("A qualidade do ar hoje está baixa. Como você está se sentindo?")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_send, "✅ Bem", yesPending)
            .addAction(android.R.drawable.ic_delete, "❌ Não muito bem", noPending)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            ?.notify(NOTIFICATION_ID_MORNING, notification)
    }
}

// ── Worker Noturno (21:00) ─────────────────────────────────────────────────────

/**
 * Dispara notificação noturna para check-in de bem-estar do dia.
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
        // Sem permissão POST_NOTIFICATIONS concedida o notify() seria descartado
        // em silêncio pelo sistema — evita o trabalho inútil (C1).
        if (!context.canPostNotifications()) return

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
            .setContentTitle("🌙 Check-in Noturno")
            .setContentText("Como foi seu dia hoje?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_send, "✅ Bem", yesPending)
            .addAction(android.R.drawable.ic_delete, "⚠️ Dia difícil", noPending)
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
