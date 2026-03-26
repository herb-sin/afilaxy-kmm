package com.afilaxy.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.afilaxy.app.navigation.AppRoutes
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
import org.koin.android.ext.android.get
import org.koin.androidx.compose.KoinAndroidContext
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val emergencyViewModel: EmergencyViewModel by viewModel()

    // Destino pendente de navegação (preenchido por onCreate ou onNewIntent)
    private val pendingDestination = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Configurar status bar para ser visível
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true // Ícones escuros em fundo claro
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                android.util.Log.d("FCM", "Token obtido com sucesso")
                // Atualiza token no Firestore sempre que o app inicia com usuário autenticado
                if (FirebaseAuth.getInstance().currentUser != null) {
                    val token = task.result
                    lifecycleScope.launch {
                        try {
                            get<com.afilaxy.domain.repository.AuthRepository>().updateFcmToken(token)
                        } catch (e: Exception) {
                            android.util.Log.e("FCM", "Erro ao atualizar token no login", e)
                        }
                    }
                }
            } else android.util.Log.e("FCM", "Falha ao obter token")
        }

        resolveIntent(intent)

        if (FirebaseAuth.getInstance().currentUser != null) {
            startEmergencyObserver()
        }

        setContent {
            KoinAndroidContext {
                AflixyTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val authResolved = remember { mutableStateOf(false) }
                        val startDestination = remember { mutableStateOf<String?>(null) }

                        DisposableEffect(Unit) {
                            val auth = FirebaseAuth.getInstance()
                            val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                                if (!authResolved.value) {
                                    val user = firebaseAuth.currentUser
                                    startDestination.value = when {
                                        user != null && pendingDestination.value != null -> pendingDestination.value
                                        user != null -> AppRoutes.HOME
                                        else -> AppRoutes.LOGIN
                                    }
                                    authResolved.value = true
                                }
                            }
                            auth.addAuthStateListener(listener)
                            onDispose { auth.removeAuthStateListener(listener) }
                        }

                        if (authResolved.value) {
                            NavGraph(
                                startDestination = startDestination.value,
                                emergencyViewModel = emergencyViewModel,
                                pendingDestination = pendingDestination
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        resolveIntent(intent)
    }

    private fun resolveIntent(intent: Intent) {
        val emergencyId = intent.getStringExtra("emergencyId")
            ?.takeIf { it.isNotBlank() && it.all { c -> c.isLetterOrDigit() || c == '-' || c == '_' } }
        // openEmergencyResponse pode vir como Boolean (extras Android) ou String "true" (FCM data payload)
        val openEmergencyResponse = intent.getBooleanExtra("openEmergencyResponse", false)
            || intent.getStringExtra("openEmergencyResponse") == "true"
        val openEmergencyRequest  = intent.getBooleanExtra("openEmergencyRequest", false)
            || intent.getStringExtra("openEmergencyRequest") == "true"
        val openChat              = intent.getBooleanExtra("openChat", false)
            || intent.getStringExtra("openChat") == "true"

        pendingDestination.value = when {
            emergencyId != null && openChat              -> "chat/$emergencyId"
            emergencyId != null && openEmergencyResponse -> "emergency_response/$emergencyId"
            emergencyId != null && openEmergencyRequest  -> "emergency_request/$emergencyId"
            else -> null
        }
    }

    private fun startEmergencyObserver() {
        lifecycleScope.launch {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(this@MainActivity)
                var location = fusedClient.lastLocation.await()
                if (location == null) {
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
