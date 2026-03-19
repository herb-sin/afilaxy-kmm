package com.afilaxy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.afilaxy.app.navigation.NavGraph
import com.afilaxy.app.ui.theme.AflixyTheme
import com.afilaxy.presentation.emergency.EmergencyViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    // ViewModel com escopo de Activity — sobrevive à navegação entre telas
    private val emergencyViewModel: EmergencyViewModel by viewModel()

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

        // Inicia observer de emergências próximas com escopo de Activity
        if (FirebaseAuth.getInstance().currentUser != null) {
            startEmergencyObserver()
        }

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

    private fun startEmergencyObserver() {
        lifecycleScope.launch {
            try {
                val lm = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
                val location = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                    ?: return@launch
                emergencyViewModel.startObservingIncomingEmergencies(location.latitude, location.longitude)
            } catch (e: SecurityException) {
                android.util.Log.e("MainActivity", "startEmergencyObserver: ${e.message}")
            }
        }
    }
}
