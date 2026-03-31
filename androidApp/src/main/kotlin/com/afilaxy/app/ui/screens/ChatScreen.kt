package com.afilaxy.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import android.view.WindowManager
import com.afilaxy.app.R
import com.afilaxy.domain.model.ChatMessage
import com.afilaxy.presentation.chat.ChatViewModel
import com.afilaxy.presentation.emergency.EmergencyViewModel
import com.afilaxy.util.FileLogger
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Tela de Chat
 * Comunicação em tempo real entre solicitante e helper
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    emergencyId: String,
    onNavigateBack: () -> Unit,
    onNavigateToRating: (String, String) -> Unit = { _, _ -> },
    viewModel: ChatViewModel = koinViewModel { parametersOf(emergencyId) },
    emergencyViewModel: EmergencyViewModel? = null
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }
    var showResolveDialog by remember { mutableStateOf(false) }

    // Detecta emergência encerrada via EmergencyViewModel.
    // wasEmergencyActive: marca se já vimos esse emergencyId ativo em algum momento.
    // isResolved = verdadeiro SOMENTE quando currentEmergency == null (emergência encerrada).
    // NÃO marca como resolved se o ViewModel passou para uma ID diferente — isso acontece
    // quando o device muda de papel (ex: helper vira requester numa nova emergência); o
    // chat da emergência original continua válido até o resolve explícito.
    // Usar .value diretamente (sem by) evita o erro de tipo State<T>? vs State<T?>.
    // Ler .value dentro de @Composable é reativo — registra o observer de recomposição.
    val emergencyVmState = emergencyViewModel?.state?.collectAsState()?.value
    var wasEmergencyActive by remember { mutableStateOf(false) }
    LaunchedEffect(emergencyVmState?.currentEmergency) {
        if (emergencyVmState?.currentEmergency?.id == emergencyId) wasEmergencyActive = true
    }
    val isResolved = wasEmergencyActive
        && emergencyVmState != null
        && emergencyVmState?.currentEmergency == null

    // Para Compose + edge-to-edge (Android 11+ / API 30+), ADJUST_RESIZE não funciona
    // porque o window não redimensiona mais. A abordagem correta é:
    //   ADJUST_NOTHING — impede o sistema de mover qualquer coisa
    //   imePadding() na Column externa — Compose lê WindowInsets.ime e reduz
    //   a Column pelo tamanho do teclado; Box(weight=1f) absorve o delta e
    //   o Surface (input) sobe para o bottom da Column já reduzida.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        val original = window?.attributes?.softInputMode
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        onDispose {
            original?.let { window.setSoftInputMode(it) }
        }
    }

    // Bloqueia back press — se emergência já encerrada, retorna direto
    BackHandler {
        if (isResolved) onNavigateBack()
        else showResolveDialog = true
    }

    LaunchedEffect(Unit) {
        FileLogger.log("INFO", "ChatScreen", "opened emergencyId=$emergencyId")
    }
    LaunchedEffect(state.messages.size) {
        FileLogger.log("DEBUG", "ChatScreen", "messages count=${state.messages.size} currentUserId=${state.currentUserId}")
        // Scroll para o Último item (mais recente) — lista está em ordem ascendente de timestamp
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size - 1)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.chat_title)) },
                navigationIcon = {
                    IconButton(onClick = { showResolveDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(onClick = { showResolveDialog = true }) {
                        Text("Resolver", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                // paddingValues já inclui nav bar + IME do Scaffold quando o teclado abre
        ) {
            // Área de mensagens — ocupa todo espaço restante acima do input
            Box(modifier = Modifier.weight(1f)) {
                if (state.messages.isEmpty()) {
                    // Estado vazio
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.chat_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Lista de mensagens em ordem cronológica (mais antiga no topo, mais recente em baixo)
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = state.messages,
                            key = { it.timestamp }
                        ) { message ->
                            MessageBubble(
                                message = message,
                                currentUserId = state.currentUserId
                            )
                        }
                    }
                }
                
                // Indicador de carregamento
                if (state.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                    )
                }
            }

            // Banner de emergência encerrada
            if (isResolved) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✅ Emergência encerrada — chat bloqueado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Surface(shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { if (!isResolved) messageText = it },
                        placeholder = {
                            Text(if (isResolved) "Emergência encerrada"
                                 else stringResource(R.string.chat_type_message))
                        },
                        enabled = !isResolved,
                        modifier = Modifier.weight(1f),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank() && !isResolved) {
                                viewModel.sendMessage(messageText)
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank() && !isResolved
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = stringResource(R.string.chat_send),
                            tint = if (messageText.isNotBlank() && !isResolved)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    
    if (showResolveDialog) {
        AlertDialog(
            onDismissRequest = { showResolveDialog = false },
            title = { Text("Encerrar Emergência") },
            text = { Text("Você precisa resolver ou cancelar a emergência antes de sair.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResolveDialog = false
                        emergencyViewModel?.onResolveEmergency()
                        onNavigateBack()
                    }
                ) {
                    Text("Resolver")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResolveDialog = false }) {
                    Text("Continuar no Chat")
                }
            }
        )
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    currentUserId: String?
) {
    val isCurrentUser = message.senderId == currentUserId
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            color = if (isCurrentUser) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isCurrentUser)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrentUser)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
