package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afilaxy.domain.model.EmergencyHistory
import com.afilaxy.presentation.auth.AuthViewModel
import com.afilaxy.presentation.history.HistoryFilter
import com.afilaxy.presentation.history.HistoryViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val authState by authViewModel.state.collectAsState()
    var showFilterMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Filtrar")
                    }
                    
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todas") },
                            onClick = {
                                viewModel.applyFilter(HistoryFilter.ALL)
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Resolvidas") },
                            onClick = {
                                viewModel.applyFilter(HistoryFilter.RESOLVED)
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Canceladas") },
                            onClick = {
                                viewModel.applyFilter(HistoryFilter.CANCELLED)
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Como Solicitante") },
                            onClick = {
                                viewModel.applyFilter(HistoryFilter.AS_REQUESTER)
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Como Helper") },
                            onClick = {
                                viewModel.applyFilter(HistoryFilter.AS_HELPER)
                                showFilterMenu = false
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadHistory() }) {
                            Text("Tentar Novamente")
                        }
                    }
                }
                
                state.filteredHistory.isEmpty() -> {
                    Text(
                        text = "Nenhuma emergência encontrada",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.filteredHistory) { emergency ->
                            EmergencyHistoryCard(
                                emergency = emergency,
                                currentUserId = authState.user?.uid
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyHistoryCard(
    emergency: EmergencyHistory,
    currentUserId: String?
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (emergency.status) {
                "resolved" -> MaterialTheme.colorScheme.primaryContainer
                "cancelled" -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (emergency.status) {
                        "resolved" -> "✅ Resolvida"
                        "cancelled" -> "❌ Cancelada"
                        "matched" -> "🤝 Em Atendimento"
                        else -> "⏳ ${emergency.status}"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                
                Icon(
                    imageVector = when (emergency.status) {
                        "resolved" -> Icons.Default.CheckCircle
                        "cancelled" -> Icons.Default.Close
                        else -> Icons.Default.Info
                    },
                    contentDescription = null,
                    tint = when (emergency.status) {
                        "resolved" -> MaterialTheme.colorScheme.primary
                        "cancelled" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            HorizontalDivider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Solicitante",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = emergency.requesterName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                if (emergency.helperName != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Helper",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = emergency.helperName ?: "",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            Text(
                text = dateFormat.format(Date(emergency.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (emergency.resolvedAt != null) {
                Text(
                    text = "Resolvida em: ${dateFormat.format(Date(emergency.resolvedAt ?: 0L))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
