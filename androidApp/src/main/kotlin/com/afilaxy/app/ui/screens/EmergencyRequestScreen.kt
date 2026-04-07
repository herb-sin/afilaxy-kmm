package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.afilaxy.app.R
import com.afilaxy.domain.repository.EmergencyRepository
import com.afilaxy.domain.repository.PreferencesRepository
import com.afilaxy.presentation.emergency.EmergencyViewModel
import com.afilaxy.util.FileLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyRequestScreen(
    navController: NavController,
    emergencyId: String,
    viewModel: EmergencyViewModel = koinViewModel(),
    // Guard externo: injeta o set de chatNavigatedIds do NavGraph para bloquear
    // uma segunda instância da tela (criada por FCM tardio ou recomposição do
    // EmergencyScreen) que tente navegar para um chat já aberto.
    alreadyInChat: (String) -> Boolean = { false }
) {
    val state by viewModel.state.collectAsState()
    val emergencyRepo: EmergencyRepository = koinInject()
    val prefs: PreferencesRepository = koinInject()
    val scope = rememberCoroutineScope()

    // Grava timestamp da primeira emergência para NPS timing
    LaunchedEffect(Unit) {
        if (prefs.getString("first_emergency_at", null) == null) {
            prefs.putString("first_emergency_at", System.currentTimeMillis().toString())
        }
    }

    // Gravidade selecionada pelo usuário enquanto aguarda o helper
    var selectedSeverity by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(selectedSeverity) {
        val sev = selectedSeverity ?: return@LaunchedEffect
        scope.launch { emergencyRepo.updateSeverity(emergencyId, sev) }
    }

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
    var navigatedToChat by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.emergencyStatus, state.emergencyId) {
        FileLogger.log("DEBUG", "EmergencyRequestScreen", "emergencyStatus=${state.emergencyStatus}")
        if (state.emergencyStatus == "matched" && !navigatedToChat && state.isRequester
            && (state.emergencyId == null || state.emergencyId == emergencyId)) {

            // Guard 1 (externo): NavGraph já registrou que o chat foi aberto nesta sessão
            if (alreadyInChat(emergencyId)) {
                FileLogger.log("INFO", "EmergencyRequestScreen",
                    "chat já aberto — auto-retornando ao chat emergencyId=$emergencyId")
                navigatedToChat = true
                // Esta tela foi empurrada incorretamente enquanto o chat estava ativo.
                // popBackStack() retira emergency_request da pilha e devolve o usuário ao chat.
                navController.popBackStack()
                return@LaunchedEffect
            }

            // Guard 2 (local): pilha contém chat/ → ignorar e auto-pop
            val backStack = navController.currentBackStack.value
            val inChat = backStack.any { (it.destination.route ?: "").startsWith("chat/") }
            if (inChat) {
                FileLogger.log("INFO", "EmergencyRequestScreen",
                    "chat já na pilha (Guard 2) — auto-retornando emergencyId=$emergencyId")
                navigatedToChat = true
                navController.popBackStack()
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
                    
                    Spacer(modifier = Modifier.height(20.dp))

                    // ── Seletor de gravidade ──────────────────────────┅
                    Text(
                        text = "Como você está se sentindo?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SeverityChip("🟡", "Leve", "leve", selectedSeverity) { selectedSeverity = it }
                        SeverityChip("🟠", "Moderada", "moderada", selectedSeverity) { selectedSeverity = it }
                        SeverityChip("🔴", "Grave", "grave", selectedSeverity) { selectedSeverity = it }
                    }

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

@Composable
private fun SeverityChip(
    emoji: String,
    label: String,
    value: String,
    selected: String?,
    onSelect: (String) -> Unit
) {
    val isSelected = selected == value
    Surface(
        onClick = { onSelect(value) },
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.2f)
        else
            Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected)
                MaterialTheme.colorScheme.onErrorContainer
            else
                MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = androidx.compose.ui.Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = androidx.compose.ui.Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
