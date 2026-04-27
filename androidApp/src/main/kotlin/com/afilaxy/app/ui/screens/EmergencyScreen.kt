package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afilaxy.app.R
import com.afilaxy.app.ui.components.RequestLocationPermission
import com.afilaxy.presentation.emergency.EmergencyViewModel
import com.afilaxy.util.FileLogger
import org.koin.androidx.compose.koinViewModel

/**
 * Tela de Emergência
 * Exibe lista de helpers próximos e permite criar emergência
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRequest: (String) -> Unit,
    viewModel: EmergencyViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showLocationPermission by remember { mutableStateOf(false) }

    // Navegar para EmergencyRequestScreen após criar emergência
    // navigatedToRequestId é persistido no ViewModel para sobreviver a recomposições
    LaunchedEffect(Unit) {
        // Desbloqueio defensivo: reseta isLoading se estava travado de um accept anterior
        viewModel.resetStuckLoading()
    }

    LaunchedEffect(state.emergencyId, state.hasActiveEmergency, state.isRequester) {
        val id = state.emergencyId
        FileLogger.log("DEBUG", "EmergencyScreen", "hasActive=${state.hasActiveEmergency} isRequester=${state.isRequester} isLoading=${state.isLoading} emergencyId=$id")
        if (id != null && state.hasActiveEmergency && state.isRequester
            && !state.isCreatingEmergency && !viewModel.wasNavigatedToRequest(id)) {
            viewModel.markNavigatedToRequest(id)
            onNavigateToRequest(id)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.emergency_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
                .padding(16.dp)
        ) {
            if (!state.hasActiveEmergency) {
                Button(
                    onClick = {
                        if (!state.isCreatingEmergency && !state.hasActiveEmergency) {
                            showLocationPermission = true
                        }
                    },
                    enabled = !state.isLoading && !state.isCreatingEmergency,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onError
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onError,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.emergency_create),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (state.error != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = state.error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            if (showLocationPermission) {
                RequestLocationPermission(
                    requiresBackground = false,
                    onPermissionGranted = {
                        showLocationPermission = false
                        viewModel.onCreateEmergency()
                    },
                    onPermissionDenied = {
                        showLocationPermission = false
                    }
                )
            }
        }
    }
}

