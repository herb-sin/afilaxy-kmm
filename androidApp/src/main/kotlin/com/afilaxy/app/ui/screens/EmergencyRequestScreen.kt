package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.afilaxy.app.R
import com.afilaxy.presentation.emergency.EmergencyViewModel
import com.afilaxy.util.FileLogger
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyRequestScreen(
    navController: NavController,
    emergencyId: String,
    viewModel: EmergencyViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    var secondsLeft by remember { mutableStateOf(180) }
    LaunchedEffect(state.emergencyExpiresAt) {
        val expiresAt = state.emergencyExpiresAt ?: return@LaunchedEffect
        while (true) {
            val remaining = ((expiresAt - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            secondsLeft = remaining
            if (remaining == 0) {
                FileLogger.log("INFO", "EmergencyRequestScreen", "countdown expired — auto cancel")
                if (state.hasActiveEmergency) viewModel.onCancelEmergency()
                break
            }
            delay(1_000)
        }
    }

    LaunchedEffect(Unit) {
        FileLogger.log("INFO", "EmergencyRequestScreen", "opened emergencyId=$emergencyId hasActive=${state.hasActiveEmergency}")
        // Garantir que o observer está ativo para este emergencyId
        viewModel.observeEmergencyStatus(emergencyId)
    }

    // Navegar para chat quando helper aceitar — apenas se for o requester e emergencyId correto
    // rememberSaveable: sobrevive à recomposição causada por uma segunda notificação FCM
    // empurrando uma nova instância da tela com navigatedToChat=false (mesmo emergencyId).
    var navigatedToChat by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.emergencyStatus, state.emergencyId) {
        FileLogger.log("DEBUG", "EmergencyRequestScreen", "emergencyStatus=${state.emergencyStatus}")
        if (state.emergencyStatus == "matched" && !navigatedToChat && state.isRequester
            && (state.emergencyId == null || state.emergencyId == emergencyId)) {
            // Guard de rota: não navega se já estiver no chat ou se a instância anterior
            // já abriu o chat para este emergencyId (segunda instância criada por FCM tardio).
            val currentRoute = navController.currentDestination?.route ?: ""
            if (currentRoute.startsWith("chat/")) {
                FileLogger.log("DEBUG", "EmergencyRequestScreen",
                    "chat já aberto (currentRoute=$currentRoute) — ignorando emergencyId=$emergencyId")
                navigatedToChat = true
                return@LaunchedEffect
            }
            navigatedToChat = true
            val chatId = state.emergencyId ?: emergencyId
            FileLogger.log("INFO", "EmergencyRequestScreen",
                "navigating to chat emergencyId=$chatId (from request screen)")
            navController.navigate("chat/$chatId") {
                popUpTo("emergency_request/$emergencyId") { inclusive = true }
            }
        }
    }

    // Navegar de volta apenas quando cancelado (não quando foi para o chat)
    LaunchedEffect(state.hasActiveEmergency) {
        if (!state.hasActiveEmergency && !navigatedToChat) navController.popBackStack()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.emergency_active)) },
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
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = stringResource(R.string.emergency_waiting),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onErrorContainer,
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
                            MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Helpers próximos foram notificados. Aguarde alguém aceitar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            FileLogger.log("INFO", "EmergencyRequestScreen", "cancelEmergency tapped hasActive=${state.hasActiveEmergency}")
                            viewModel.onCancelEmergency()
                        },
                        enabled = state.hasActiveEmergency,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onError,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(R.string.emergency_cancel))
                        }
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
