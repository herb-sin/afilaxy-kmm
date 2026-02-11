package com.afilaxy.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.afilaxy.app.navigation.NavGraph
import com.afilaxy.app.ui.theme.AflixyTheme
import com.google.firebase.messaging.FirebaseMessaging
import org.koin.androidx.compose.KoinAndroidContext

/**
 * MainActivity do Afilaxy KMM
 * Ponto de entrada da aplicação Android
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        // Obter FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM", "Token obtained: $token")
            } else {
                Log.e("FCM", "Failed to get token", task.exception)
            }
        }
        
        // Verificar se foi aberto via notificação
        val emergencyId = intent.getStringExtra("emergencyId")
        val openEmergencyResponse = intent.getBooleanExtra("openEmergencyResponse", false)
        
        setContent {
            KoinAndroidContext {
                AflixyTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavGraph(
                            startDestination = if (openEmergencyResponse && emergencyId != null) {
                                "emergency_response/$emergencyId"
                            } else null
                        )
                    }
                }
            }
        }
    }
}
