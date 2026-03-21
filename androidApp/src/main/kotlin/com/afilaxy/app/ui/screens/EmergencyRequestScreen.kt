package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var cancelTapped by remember { mutableStateOf(false) }

    // Countdown timer
    var secondsLeft by remember { mutableStateOf(180) }
    LaunchedEffect(state.emergencyExpiresAt) {
        val expiresAt = state.emergencyExpiresAt ?: return@LaunchedEffect
        while (true) {
            val remaining = ((expiresAt - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
            secondsLeft = remaining
            if (remaining == 0) {
                FileLogger.log("INFO", "EmergencyRequestScreen", "countdown expired — auto cancel")
                if (!cancelTapped) {
                    cancelTapped = true
                    viewModel.onCancelEmergency()
                }
                break
            }
            delay(1_000)
        }
    }

    LaunchedEffect(Unit) {
        FileLogger.log("INFO", "EmergencyRequestScreen", "opened emergencyId=$emergencyId hasActive=${state.hasActiveEmergency}")
    }
    
    // Navegar para chat quando helper aceitar
    LaunchedEffect(state.emergencyStatus) {
        if (state.emergencyStatus == "matched" && state.emergencyId != null) {
            navController.navigate("chat/${state.emergencyId}") {
                popUpTo("emergency_request/$emergencyId") { inclusive = true }
            }
        }
    }
    
    // Navegar de volta quando emergência for cancelada
    val initializedRef = remember { mutableStateOf(false) }
    LaunchedEffect(state.hasActiveEmergency) {
        if (!initializedRef.value) {
            initializedRef.value = true
            return@LaunchedEffect
        }
        if (!state.hasActiveEmergency && !state.isLoading) {
            navController.popBackStack()
        }
    }

    // Timeout de segurança: se cancelTapped mas tela não saiu em 5s, força popBackStack
    LaunchedEffect(cancelTapped) {
        if (cancelTapped) {
            delay(5_000)
            if (navController.currentDestination?.route?.startsWith("emergency_request") == true) {
                FileLogger.log("WARN", "EmergencyRequestScreen", "cancel timeout — forcing popBackStack")
                navController.popBackStack()
            }
        }
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
                            if (cancelTapped) return@Button
                            cancelTapped = true
                            FileLogger.log("INFO", "EmergencyRequestScreen", "cancelEmergency tapped hasActive=${state.hasActiveEmergency}")
                            viewModel.onCancelEmergency()
                        },
                        enabled = state.hasActiveEmergency && !state.isLoading && !cancelTapped,
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
            
            if (state.nearbyHelpers.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "${state.nearbyHelpers.size} helper(s) próximo(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
