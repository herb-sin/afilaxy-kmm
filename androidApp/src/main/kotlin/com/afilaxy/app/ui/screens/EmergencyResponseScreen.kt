package com.afilaxy.app.ui.screens

import android.app.NotificationManager
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.afilaxy.app.R
import com.afilaxy.presentation.emergency.EmergencyViewModel
import com.afilaxy.util.FileLogger
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyResponseScreen(
    navController: NavController,
    emergencyId: String,
    viewModel: EmergencyViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var secondsLeft by remember { mutableStateOf(180) }
    val context = LocalContext.current

    // Cancela a notificação do sistema usando o mesmo ID derivado do emergencyId
    fun cancelEmergencyNotification() {
        val nm = context.getSystemService(NotificationManager::class.java)
        val safeId = emergencyId.filter { it.isLetterOrDigit() }.take(8).ifEmpty { "0" }
            .fold(0) { acc, c -> acc * 31 + c.code }
        nm.cancel(safeId)
    }

    // Sincroniza countdown e pré-carrega estado da emergência no ViewModel
    LaunchedEffect(Unit) {
        FileLogger.log("INFO", "EmergencyResponseScreen", "opened emergencyId=$emergencyId")
        viewModel.fetchEmergencyExpiresAt(emergencyId)
        // Garante que o ViewModel conhece o emergencyId mesmo em cold start via notificação
        viewModel.preloadEmergencyId(emergencyId)
    }

    LaunchedEffect(state.emergencyExpiresAt) {
        val expiresAt = state.emergencyExpiresAt ?: return@LaunchedEffect
        // Emergência já expirada ao abrir — cancela notificação e volta imediatamente
        if (System.currentTimeMillis() >= expiresAt) {
            FileLogger.log("INFO", "EmergencyResponseScreen", "already expired on open — dismissing emergencyId=$emergencyId")
            cancelEmergencyNotification()
            navController.popBackStack()
            return@LaunchedEffect
        }
        while (true) {
            val remaining = ((expiresAt - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            secondsLeft = remaining
            if (remaining == 0) {
                cancelEmergencyNotification()
                navController.popBackStack()
                break
            }
            kotlinx.coroutines.delay(1_000)
        }
    }

    // Navegar para chat após aceitar
    LaunchedEffect(state.hasActiveEmergency, state.emergencyId) {
        FileLogger.log("DEBUG", "EmergencyResponseScreen", "hasActiveEmergency=${state.hasActiveEmergency} emergencyId=${state.emergencyId}")
        if (state.hasActiveEmergency && state.emergencyId == emergencyId && !state.isRequester) {
            FileLogger.log("INFO", "EmergencyResponseScreen", "navigating to chat emergencyId=$emergencyId")
            navController.navigate("chat/$emergencyId") {
                popUpTo("emergency_response/$emergencyId") { inclusive = true }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🆘 Emergência Disponível") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "👥 Alguém precisa de ajuda!",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Uma pessoa próxima a você está em uma situação de emergência e precisa de ajuda.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Você pode ajudar?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val minutes = secondsLeft / 60
                    val seconds = secondsLeft % 60
                    Text(
                        text = "Expira em %d:%02d".format(minutes, seconds),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (secondsLeft <= 30)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            viewModel.onAcceptEmergency(emergencyId)
                        },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Aceitar e Ajudar")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Recusar")
                    }
                }
            }
            
            state.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
