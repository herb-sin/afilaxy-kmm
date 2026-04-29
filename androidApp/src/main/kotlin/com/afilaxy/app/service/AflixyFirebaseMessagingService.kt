package com.afilaxy.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.afilaxy.app.MainActivity
import com.afilaxy.app.R
import com.afilaxy.domain.repository.AuthRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class AflixyFirebaseMessagingService : FirebaseMessagingService() {

    private val authRepository: AuthRepository by inject()

    private companion object {
        const val SANITIZE_MAX_LENGTH = 200       // limite de CWE-117 para strings externas em log
        const val VIBRATION_PAUSE_MS = 0L         // pausa inicial do padrão de vibração
        const val VIBRATION_BUZZ_MS = 1_000L      // duração do buzz de emergência
        const val VIBRATION_GAP_MS = 500L         // gap entre buzzes
        const val NOTIFICATION_ID_HASH_LENGTH = 8     // chars alfanuméricos para hash estável
        const val NOTIFICATION_ID_HASH_MULTIPLIER = 31 // multiplicador primo do hash
        const val NOTIFICATION_TEXT_TITLE_MAX = 64    // máx chars de título de notificação
        const val NOTIFICATION_TEXT_BODY_MAX = 128    // máx chars de corpo de notificação
    }

    // Remove \n, \r e demais caracteres de controle de dados externos antes de logar (CWE-117)
    private fun String.sanitizeForLog(): String =
        replace(Regex("[\\r\\n\\t\\x00-\\x1F\\x7F]"), " ").take(SANITIZE_MAX_LENGTH)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("FCM", "FCM recebido. Keys: ${remoteMessage.data.keys.joinToString().sanitizeForLog()}")

        remoteMessage.data.let { data ->
            when (data["type"]) {
                // "new_emergency" é o tipo gerado pelo NotificationRepositoryImpl (KMM).
                // "emergency_request" e "emergency" são aliases de versões anteriores.
                "new_emergency", "emergency_request", "emergency" -> {
                    Log.d("FCM", "Emerg\u00eancia recebida")
                    val emergencyId = data["emergencyId"] ?: ""
                    val requesterName = data["requesterName"] ?: "Algu\u00e9m"
                    val title = data["title"] ?: "Emerg\u00eancia de Asma"
                    val body = data["body"] ?: "$requesterName precisa de ajuda"
                    showEmergencyNotification(
                        title.sanitizeForLog(), body.sanitizeForLog(), emergencyId.sanitizeForLog()
                    )
                }
                "emergency_cancelled" -> {
                    Log.d("FCM", "Emerg\u00eancia cancelada")
                    val emergencyId = data["emergencyId"] ?: ""
                    cancelNotification(emergencyId)
                }
                "helper_response" -> {
                    Log.d("FCM", "Resposta de helper recebida")
                    val title = data["title"] ?: "Helper Encontrado!"
                    val body = data["body"] ?: "Alguém está vindo te ajudar"
                    showNotification(title.sanitizeForLog(), body.sanitizeForLog(), null)
                }
                "helper_matched" -> {
                    Log.d("FCM", "Helper aceitou emergência")
                    val emergencyId = data["emergencyId"] ?: ""
                    val helperName = data["helperName"] ?: "Alguém"
                    val title = "Helper Encontrado!"
                    val body = "$helperName está vindo te ajudar"
                    showHelperMatchedNotification(title.sanitizeForLog(), body.sanitizeForLog(), emergencyId.sanitizeForLog())
                }
                "chat" -> {
                    Log.d("FCM", "Mensagem de chat recebida")
                    val emergencyId = data["emergencyId"] ?: ""
                    val senderName = data["senderName"] ?: "Mensagem"
                    val body = data["message"] ?: ""
                    showChatNotification(senderName.sanitizeForLog(), body.sanitizeForLog(), emergencyId.sanitizeForLog())
                }
                else -> {
                    val title = data["title"] ?: "Afilaxy"
                    val body = data["body"] ?: ""
                    showNotification(title.sanitizeForLog(), body.sanitizeForLog(), null)
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Novo token registrado")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                authRepository.updateFcmToken(token)
            } catch (e: Exception) {
                Log.e("FCM", "Erro ao atualizar token", e)
            }
        }
    }
    
    private fun showChatNotification(senderName: String, body: String, emergencyId: String) {
        val channelId = "afilaxy_chat"
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("emergencyId", emergencyId)
            putExtra("openChat", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, safeNotificationId("chat_$emergencyId"), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(channelId, "Mensagens", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        notificationManager.notify(
            safeNotificationId("chat_$emergencyId"),
            NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(senderName)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
        )
    }

    private fun showHelperMatchedNotification(title: String, body: String, emergencyId: String) {
        val channelId = "afilaxy_emergency"
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("emergencyId", emergencyId)
            putExtra("openEmergencyRequest", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, emergencyId.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(channelId, "Emergências", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        notificationManager.notify(
            safeNotificationId(emergencyId),
            NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
        )
    }

    private fun showEmergencyNotification(title: String, body: String, emergencyId: String) {
        val channelId = "afilaxy_emergency"
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("emergencyId", emergencyId)
            putExtra("openEmergencyResponse", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, safeNotificationId(emergencyId), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(channelId, "Emergências", NotificationManager.IMPORTANCE_HIGH).apply {
                    enableVibration(true)
                    vibrationPattern = longArrayOf(VIBRATION_PAUSE_MS, VIBRATION_BUZZ_MS, VIBRATION_GAP_MS, VIBRATION_BUZZ_MS)
                }
            )
        }
        notificationManager.notify(
            safeNotificationId(emergencyId),
            NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(VIBRATION_PAUSE_MS, VIBRATION_BUZZ_MS, VIBRATION_GAP_MS, VIBRATION_BUZZ_MS))
                .build()
        )
    }

    /**
     * Deriva um notificationId estável a partir de um ID externo sem usar String.hashCode(),
     * que processa o conteúdo bruto sem sanitização (CWE-611).
     * Usa apenas os primeiros 8 chars alfanuméricos do ID para gerar um Int determinístico.
     */
    private fun safeNotificationId(externalId: String): Int {
        val safe = externalId.filter { it.isLetterOrDigit() }
            .take(NOTIFICATION_ID_HASH_LENGTH).ifEmpty { "0" }
        return safe.fold(0) { acc, c -> acc * NOTIFICATION_ID_HASH_MULTIPLIER + c.code }
    }
    
    private fun cancelNotification(emergencyId: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(safeNotificationId(emergencyId))
    }
    
    private fun showNotification(title: String, body: String, emergencyId: String?) {
        val channelId = "afilaxy_emergency"
        val notificationId = emergencyId
            ?.let { safeNotificationId(it) }
            ?: (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

        val safeTitle = sanitizeNotificationText(title, NOTIFICATION_TEXT_TITLE_MAX)
        val safeBody  = sanitizeNotificationText(body, NOTIFICATION_TEXT_BODY_MAX)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            emergencyId?.let { putExtra("emergencyId", sanitizeNotificationText(it, NOTIFICATION_TEXT_TITLE_MAX)) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(safeTitle)
            .setContentText(safeBody)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Emergências",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de emergências"
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    /**
     * Remove metacaracteres XML/HTML e caracteres de controle de strings externas
     * antes de passá-las ao NotificationCompat.Builder (CWE-611).
     */
    private fun sanitizeNotificationText(input: String, maxLength: Int): String =
        input
            .replace(Regex("[\\x00-\\x1F\\x7F<>&\"']"), "")
            .trim()
            .take(maxLength)
            .ifEmpty { "Afilaxy" }
}
