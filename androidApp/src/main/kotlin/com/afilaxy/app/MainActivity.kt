package com.afilaxy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.afilaxy.app.navigation.NavGraph
import com.afilaxy.app.ui.theme.AflixyTheme
import com.google.firebase.auth.FirebaseAuth
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

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                android.util.Log.d("FCM", "Token obtido com sucesso")
            } else {
                android.util.Log.e("FCM", "Falha ao obter token")
            }
        }

        // Only honour deep-link extras when the user is already authenticated
        val isAuthenticated = FirebaseAuth.getInstance().currentUser != null
        val emergencyId = intent.getStringExtra("emergencyId")
            ?.takeIf { it.isNotBlank() && it.all { c -> c.isLetterOrDigit() || c == '-' || c == '_' } }
        val openEmergencyResponse = intent.getBooleanExtra("openEmergencyResponse", false)
        
        setContent {
            KoinAndroidContext {
                AflixyTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavGraph(
                            startDestination = if (isAuthenticated && openEmergencyResponse && emergencyId != null) {
                                "emergency_response/$emergencyId"
                            } else null
                        )
                    }
                }
            }
        }
    }
}
