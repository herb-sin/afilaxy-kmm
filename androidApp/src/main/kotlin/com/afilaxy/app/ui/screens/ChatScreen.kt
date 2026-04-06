package com.afilaxy.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import android.view.WindowManager
import com.afilaxy.app.R
import com.afilaxy.domain.model.ChatMessage
import com.afilaxy.domain.repository.EmergencyRepository
import com.afilaxy.presentation.chat.ChatViewModel
import com.afilaxy.presentation.emergency.EmergencyViewModel
import com.afilaxy.util.FileLogger
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
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
    onNavigateBackResolved: () -> Unit = onNavigateBack,
    onNavigateToRating: (String, String) -> Unit = { _, _ -> },
    viewModel: ChatViewModel = koinViewModel { parametersOf(emergencyId) },
    emergencyViewModel: EmergencyViewModel? = null
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }
    var showResolveDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var samuCalled by remember { mutableStateOf(false) }

    val emergencyRepo: EmergencyRepository = koinInject()
    val scope = rememberCoroutineScope()

    // Participantes da emergência (para saber quem avaliar)
    var reviewedId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(emergencyId) {
        emergencyRepo.getEmergencyParticipants(emergencyId).onSuccess { (requesterId, helperId) ->
            val currentUserId = state.currentUserId
            reviewedId = if (currentUserId == requesterId) helperId else requesterId
        }
    }

    // Observa o status da emergência DIRETAMENTE via EmergencyRepository (Firestore snapshot).
    // Mesmo padrão do statusListener do iOS ChatView — funciona para os dois lados
    // (requester e helper) sem depender do ViewModel, evitando a race condition que
    // existia antes entre emergencyStatus e currentEmergency chegando juntos.
    var resolvedByOther by remember { mutableStateOf(false) }
    LaunchedEffect(emergencyId) {
        emergencyRepo.observeEmergencyStatus(emergencyId).collect { status ->
            if (status == "resolved" || status == "finished") {
                resolvedByOther = true
            }
        }
    }

    val isResolved = resolvedByOther
    val resolvedBannerText = when {
        resolvedByOther -> "✅ Emergência encerrada pela outra parte"
        else -> "✅ Emergência encerrada — chat bloqueado"
    }

    // ADJUST_RESIZE + imePadding: quando o teclado abre, o conteúdo encolhe
    // de baixo para cima — o LazyColumn de mensagens fica visível acima do input,
    // e o input fica fixo logo acima do teclado. Melhor UX que ADJUST_PAN.
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        val original = window?.attributes?.softInputMode
        @Suppress("DEPRECATION")
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        onDispose {
            original?.let { window.setSoftInputMode(it) }
        }
    }

    // Bloqueia back press — se emergência já encerrada pela outra parte, limpa estado e vai à home
    BackHandler {
        if (isResolved) onNavigateBackResolved()
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
                    IconButton(onClick = {
                        // Se a outra parte já encerrou, limpa estado e vai direto à home
                        if (resolvedByOther) onNavigateBackResolved()
                        else showResolveDialog = true
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    // Botão SAMU — visível enquanto emergência ativa e ainda não acionado
                    if (!isResolved && !samuCalled) {
                        TextButton(onClick = {
                            scope.launch {
                                emergencyRepo.updateSamuCalled(emergencyId)
                                samuCalled = true
                            }
                        }) {
                            Text("🚑 SAMU", color = MaterialTheme.colorScheme.error)
                        }
                    } else if (samuCalled) {
                        Text(
                            text = "🚑 SAMU acionado",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    if (!resolvedByOther) {
                        TextButton(onClick = { showResolveDialog = true }) {
                            Text("Resolver", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()  // encolhe o conteúdo quando o teclado abre
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
                            text = resolvedBannerText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        // Quando a outra parte encerrou: botão direto sem "Continuar no Chat"
                        if (resolvedByOther) {
                            TextButton(onClick = { onNavigateBackResolved() }) {
                                Text("Encerrar")
                            }
                        }
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
            text = { Text("A outra parte será avisada que a emergência foi encerrada.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResolveDialog = false
                        emergencyViewModel?.onResolveEmergency()
                        // Mostra avaliação se soubermos quem avaliar
                        if (reviewedId != null) showRatingDialog = true
                        else onNavigateBack()
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

    if (showRatingDialog) {
        val rid = reviewedId
        RatingDialog(
            onSubmit = { rating, comment ->
                showRatingDialog = false
                if (rid != null) {
                    scope.launch {
                        emergencyRepo.submitReview(emergencyId, rid, rating, comment)
                    }
                }
                onNavigateBack()
            },
            onSkip = {
                showRatingDialog = false
                onNavigateBack()
            }
        )
    }
}

// Mensagens de sistema são exibidas centralizadas — senderId == "system"
@Composable
private fun MessageBubble(
    message: ChatMessage,
    currentUserId: String?
) {
    val isSystem = message.senderId == "system"
    if (isSystem) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message.message,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
        return
    }
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

// ── RatingDialog ──────────────────────────────────────────────────────────────

@Composable
private fun RatingDialog(
    onSubmit: (rating: Int, comment: String?) -> Unit,
    onSkip: () -> Unit
) {
    var selectedRating by remember { mutableStateOf(0) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text("Como foi o atendimento?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Avalie a pessoa que te ajudou nesta emergência.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Estrelas
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 1..5) {
                        Text(
                            text = if (i <= selectedRating) "⭐" else "☆",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.clickable { selectedRating = i }
                        )
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = { Text("Comentário opcional") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSubmit(
                        selectedRating.coerceAtLeast(1),
                        comment.trim().ifEmpty { null }
                    )
                }
            ) { Text("Enviar e Encerrar") }
        },
        dismissButton = {
            TextButton(onClick = onSkip) { Text("Pular") }
        }
    )
}
