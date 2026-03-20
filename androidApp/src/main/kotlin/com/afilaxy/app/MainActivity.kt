package com.afilaxy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.afilaxy.app.navigation.NavGraph
import com.afilaxy.app.ui.theme.AflixyTheme
import com.afilaxy.presentation.emergency.EmergencyViewModel
import com.afilaxy.util.FileLogger
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
            // Layer 2 — relançamento: se já era helper, reinicia observer com last known location
            if (emergencyViewModel.state.value.isHelperMode) {
                startEmergencyObserver()
            }
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
                            } else null,
                            emergencyViewModel = emergencyViewModel
                        )
                    }
                }
            }
        }
    }

    private fun startEmergencyObserver() {
        lifecycleScope.launch {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)
                // Tenta last known primeiro (rápido)
                var location = fusedClient.lastLocation.await()
                if (location == null) {
                    // Solicita localização atual com timeout implícito do sistema
                    FileLogger.log("INFO", "MainActivity", "lastLocation null, requesting current location")
                    val cts = CancellationTokenSource()
                    location = fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token).await()
                }
                if (location == null) {
                    FileLogger.log("WARN", "MainActivity", "startEmergencyObserver: no location available, skipping")
                    return@launch
                }
                FileLogger.log("INFO", "MainActivity", "startEmergencyObserver: lat=${location.latitude} lon=${location.longitude}")
                emergencyViewModel.startObservingIncomingEmergencies(location.latitude, location.longitude)
            } catch (e: SecurityException) {
                FileLogger.log("ERROR", "MainActivity", "startEmergencyObserver: SecurityException: ${e.message}")
            } catch (e: Exception) {
                FileLogger.log("ERROR", "MainActivity", "startEmergencyObserver: ${e.message}")
            }
        }
    }
}
