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
import com.afilaxy.app.ui.screens.EmergencyOverlayActivity
import com.afilaxy.domain.repository.AuthRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class AflixyFirebaseMessagingService : FirebaseMessagingService() {
    
    private val authRepository: AuthRepository by inject()
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d("FCM", "🔥 FCM RECEBIDO! Data: ${remoteMessage.data}")
        
        remoteMessage.data.let { data ->
            when (data["type"]) {
                "emergency_request" -> {
                    Log.d("FCM", "🆘 EMERGÊNCIA RECEBIDA!")
                    val emergencyId = data["emergencyId"] ?: ""
                    val requesterName = data["requesterName"] ?: "Alguém"
                    val title = data["title"] ?: "🆘 Emergência de Asma"
                    val body = data["body"] ?: "$requesterName precisa de ajuda"
                    showEmergencyNotification(title, body, emergencyId)
                }
                "emergency_cancelled" -> {
                    Log.d("FCM", "❌ EMERGÊNCIA CANCELADA!")
                    val emergencyId = data["emergencyId"] ?: ""
                    cancelNotification(emergencyId)
                }
                "helper_response" -> {
                    Log.d("FCM", "✅ RESPOSTA DE HELPER RECEBIDA!")
                    val title = data["title"] ?: "✅ Helper Encontrado!"
                    val body = data["body"] ?: "Alguém está vindo te ajudar"
                    showNotification(title, body, null)
                }
                else -> {
                    val title = data["title"] ?: "Afilaxy"
                    val body = data["body"] ?: ""
                    showNotification(title, body, null)
                }
            }
        }
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                authRepository.updateFcmToken(token)
            } catch (e: Exception) {
                Log.e("FCM", "Error updating token", e)
            }
        }
    }
    
    private fun showEmergencyNotification(title: String, body: String, emergencyId: String) {
        val channelId = "afilaxy_emergency"
        
        // Intent para abrir EmergencyOverlayActivity
        val overlayIntent = Intent(this, EmergencyOverlayActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("emergency_id", emergencyId)
            putExtra("requesterName", body.replace("precisa de ajuda próximo a você", "").trim())
            putExtra("distance", "desconhecida")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, emergencyId.hashCode(), overlayIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
        
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Emergências",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de emergências de asma"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(
                    android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        notificationManager.notify(emergencyId.hashCode(), notificationBuilder.build())
        
        // Tentar abrir overlay diretamente se app estiver em foreground
        try {
            startActivity(overlayIntent)
        } catch (e: Exception) {
            Log.e("FCM", "Não foi possível abrir overlay diretamente", e)
        }
    }
    
    private fun cancelNotification(emergencyId: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(emergencyId.hashCode())
    }
    
    private fun showNotification(title: String, body: String, emergencyId: String?) {
        val channelId = "afilaxy_emergency"
        val notificationId = System.currentTimeMillis().toInt()
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            emergencyId?.let { putExtra("emergencyId", it) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
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
}
