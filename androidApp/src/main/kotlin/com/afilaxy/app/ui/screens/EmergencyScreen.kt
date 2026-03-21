package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afilaxy.app.R
import com.afilaxy.app.ui.components.RequestLocationPermission
import com.afilaxy.domain.model.Helper
import com.afilaxy.presentation.emergency.EmergencyViewModel
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

/**
 * Tela de Emergência
 * Exibe lista de helpers próximos e permite criar emergência
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    onNavigateToResponse: (String) -> Unit,
    onNavigateToRequest: (String) -> Unit,
    viewModel: EmergencyViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showLocationPermission by remember { mutableStateOf(false) }
    var navigatedToRequestId by remember { mutableStateOf<String?>(null) }
    
    // Navegar para EmergencyRequestScreen após criar emergência
    LaunchedEffect(state.emergencyId, state.hasActiveEmergency) {
        val id = state.emergencyId
        if (id != null && state.hasActiveEmergency && !state.isCreatingEmergency && navigatedToRequestId != id) {
            navigatedToRequestId = id
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
                        Text(
                            text = stringResource(R.string.emergency_create),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            if (state.hasActiveEmergency) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.emergency_active),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        
                        if (state.currentEmergency?.assignedHelperId == null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.emergency_waiting),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { 
                                viewModel.onCancelEmergency()
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(stringResource(R.string.emergency_cancel))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Lista de helpers próximos
            Text(
                text = stringResource(R.string.emergency_searching),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            if (state.nearbyHelpers.isEmpty() && !state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.emergency_no_helpers),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.nearbyHelpers) { helper ->
                        HelperCard(
                            helper = helper,
                            onAccept = {
                                viewModel.onAcceptEmergency(state.emergencyId ?: "")
                                onNavigateToResponse(state.emergencyId ?: "")
                            },
                            canAccept = state.isHelperMode && !state.hasActiveEmergency
                        )
                    }
                }
            }
            
            // Mensagem de erro
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

@Composable
private fun HelperCard(
    helper: Helper,
    onAccept: () -> Unit,
    canAccept: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = helper.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(
                        R.string.emergency_distance,
                        (helper.distance / 1000.0).roundToInt().toString()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (canAccept) {
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(stringResource(R.string.emergency_accept))
                }
            }
        }
    }
}
