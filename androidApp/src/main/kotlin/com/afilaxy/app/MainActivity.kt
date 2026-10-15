package com.afilaxy.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.afilaxy.app.navigation.AppRoutes
import com.afilaxy.app.navigation.NavGraph
import com.afilaxy.app.ui.theme.AflixyTheme
import com.afilaxy.app.ui.theme.ThemeState
import com.afilaxy.app.util.canPostNotifications
import androidx.compose.foundation.isSystemInDarkTheme
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

    // Registrado na inicialização da classe (antes do onCreate), exigência do
    // ActivityResultRegistry para poder lançar o pedido de permissão depois.
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    /**
     * Pede POST_NOTIFICATIONS uma única vez por instalação (chave "notif_perm_asked"
     * gravada ANTES do diálogo), apenas em SDK >= 33 e com notificações desativadas.
     * Sem isso as notificações de check-in/emergência nunca chegam no Android 13+.
     */
    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (canPostNotifications()) return
        val prefs = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        if (prefs.getBoolean("notif_perm_asked", false)) return
        prefs.edit().putBoolean("notif_perm_asked", true).apply()
        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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

        ThemeState.preference = getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
            .getString("theme_preference", "system") ?: "system"

        resolveIntent(intent)

        if (FirebaseAuth.getInstance().currentUser != null) {
            startEmergencyObserver()
        }

        setContent {
            KoinAndroidContext {
                val darkTheme = when (ThemeState.preference) {
                    "dark" -> true
                    "light" -> false
                    else -> isSystemInDarkTheme()
                }
                AflixyTheme(darkTheme = darkTheme) {
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
                                    val pending = pendingDestination.value
                                    val usedPending = user != null && pending != null
                                    startDestination.value = when {
                                        usedPending -> pending
                                        user != null -> AppRoutes.HOME
                                        else -> AppRoutes.LOGIN
                                    }
                                    // Consome o destino pendente para que o LaunchedEffect do
                                    // NavGraph não navegue de novo para o mesmo startDestination.
                                    if (usedPending) pendingDestination.value = null
                                    authResolved.value = true
                                }
                                // Fora do bloco acima de propósito: cobre sessão restaurada
                                // no cold start (listener 1) e login fresco (listener 2).
                                if (firebaseAuth.currentUser != null) {
                                    maybeRequestNotificationPermission()
                                }
                            }
                            auth.addAuthStateListener(listener)
                            onDispose { auth.removeAuthStateListener(listener) }
                        }

                        // Timeout: se App Check bloquear o AuthStateListener, navega para Login após 5s
                        LaunchedEffect(Unit) {
                            delay(5_000)
                            if (!authResolved.value) {
                                startDestination.value = AppRoutes.LOGIN
                                authResolved.value = true
                            }
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
            // "open_screen" vem das notificações de check-in (morning/evening + _yes/_no)
            else -> openScreenDestination(intent.getStringExtra("open_screen"))
        }
    }

    /**
     * Mapeia o extra "open_screen" das notificações de check-in para a rota.
     * `_yes`/`_no` abrem a mesma tela com quickAnswer (auto-submit positivo em
     * `_yes`; `_no` abre o formulário normal para o usuário relatar o dia).
     * Precedência sempre com os casos de emergência (tratados antes).
     */
    private fun openScreenDestination(openScreen: String?): String? = when (openScreen) {
        "morning_checkin"     -> AppRoutes.checkIn("MORNING")
        "morning_checkin_yes" -> AppRoutes.checkIn("MORNING", quick = true)
        "morning_checkin_no"  -> AppRoutes.checkIn("MORNING", quick = false)
        "evening_checkin"     -> AppRoutes.checkIn("EVENING")
        "evening_checkin_yes" -> AppRoutes.checkIn("EVENING", quick = true)
        "evening_checkin_no"  -> AppRoutes.checkIn("EVENING", quick = false)
        else -> null
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
