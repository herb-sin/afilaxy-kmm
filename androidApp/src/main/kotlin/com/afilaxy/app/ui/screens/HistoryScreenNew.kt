package com.afilaxy.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
fun HistoryScreenNew(
    onNavigateBack: () -> Unit,
    viewModel: HistoryViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val authState by authViewModel.state.collectAsState()
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var showFilterSheet by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Histórico",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    FilterChip(
                        onClick = { showFilterSheet = true },
                        label = { 
                            Text(
                                when (selectedFilter) {
                                    HistoryFilter.ALL -> "Todas"
                                    HistoryFilter.RESOLVED -> "Resolvidas"
                                    HistoryFilter.CANCELLED -> "Canceladas"
                                    HistoryFilter.AS_REQUESTER -> "Solicitante"
                                    HistoryFilter.AS_HELPER -> "Helper"
                                }
                            ) 
                        },
                        selected = selectedFilter != HistoryFilter.ALL,
                        leadingIcon = {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
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
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Carregando histórico...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                state.error != null -> {
                    ErrorState(
                        error = state.error ?: "",
                        onRetry = { viewModel.loadHistory() }
                    )
                }
                
                state.filteredHistory.isEmpty() -> {
                    EmptyState(filter = selectedFilter)
                }
                
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            StatsCard(emergencies = state.filteredHistory)
                        }
                        
                        items(state.filteredHistory) { emergency ->
                            EmergencyCard(
                                emergency = emergency,
                                currentUserId = authState.user?.uid
                            )
                        }
                    }
                }
            }
        }
        
        if (showFilterSheet) {
            FilterBottomSheet(
                currentFilter = selectedFilter,
                onFilterSelected = { filter ->
                    selectedFilter = filter
                    viewModel.applyFilter(filter)
                    showFilterSheet = false
                },
                onDismiss = { showFilterSheet = false }
            )
        }
    }
}

@Composable
private fun StatsCard(emergencies: List<EmergencyHistory>) {
    val resolved = emergencies.count { it.status == "resolved" }
    val cancelled = emergencies.count { it.status == "cancelled" }
    val total = emergencies.size
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Resumo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    value = total.toString(),
                    label = "Total",
                    color = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    value = resolved.toString(),
                    label = "Resolvidas",
                    color = Color(0xFF4CAF50)
                )
                StatItem(
                    value = cancelled.toString(),
                    label = "Canceladas",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmergencyCard(
    emergency: EmergencyHistory,
    currentUserId: String?
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val isRequester = emergency.requesterId == currentUserId
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(status = emergency.status)
                
                RoleBadge(
                    isRequester = isRequester
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Solicitante",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        emergency.requesterName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (emergency.helperName != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Helper",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            emergency.helperName ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Iniciada: ${dateFormat.format(Date(emergency.timestamp))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (emergency.resolvedAt != null) {
                Text(
                    "Finalizada: ${dateFormat.format(Date(emergency.resolvedAt ?: 0L))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (text, color, icon) = when (status) {
        "resolved" -> Triple("Resolvida", Color(0xFF4CAF50), Icons.Default.CheckCircle)
        "cancelled" -> Triple("Cancelada", MaterialTheme.colorScheme.error, Icons.Default.Cancel)
        "matched" -> Triple("Em Atendimento", Color(0xFFFF9800), Icons.Default.Handshake)
        else -> Triple(status, MaterialTheme.colorScheme.onSurfaceVariant, Icons.Default.Info)
    }
    
    Row(
        modifier = Modifier
            .background(
                color.copy(alpha = 0.1f),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = color
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RoleBadge(isRequester: Boolean) {
    val text = if (isRequester) "Você solicitou" else "Você ajudou"
    val color = if (isRequester) MaterialTheme.colorScheme.primary else Color(0xFF4CAF50)
    
    Box(
        modifier = Modifier
            .background(
                color.copy(alpha = 0.1f),
                CircleShape
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    currentFilter: HistoryFilter,
    onFilterSelected: (HistoryFilter) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Filtrar por",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val filters = listOf(
                HistoryFilter.ALL to "Todas as emergências",
                HistoryFilter.RESOLVED to "Emergências resolvidas",
                HistoryFilter.CANCELLED to "Emergências canceladas",
                HistoryFilter.AS_REQUESTER to "Como solicitante",
                HistoryFilter.AS_HELPER to "Como helper"
            )
            
            filters.forEach { (filter, label) ->
                FilterOption(
                    label = label,
                    selected = currentFilter == filter,
                    onClick = { onFilterSelected(filter) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FilterOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Ops! Algo deu errado",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tentar Novamente")
        }
    }
}

@Composable
private fun EmptyState(filter: HistoryFilter) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            when (filter) {
                HistoryFilter.ALL -> "Nenhuma emergência ainda"
                HistoryFilter.RESOLVED -> "Nenhuma emergência resolvida"
                HistoryFilter.CANCELLED -> "Nenhuma emergência cancelada"
                HistoryFilter.AS_REQUESTER -> "Você ainda não solicitou ajuda"
                HistoryFilter.AS_HELPER -> "Você ainda não ajudou ninguém"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            when (filter) {
                HistoryFilter.ALL -> "Quando você solicitar ou oferecer ajuda, o histórico aparecerá aqui"
                HistoryFilter.AS_REQUESTER -> "Quando precisar de ajuda, toque no botão de emergência"
                HistoryFilter.AS_HELPER -> "Ative o modo helper para começar a ajudar outras pessoas"
                else -> "Tente alterar o filtro para ver outras emergências"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
