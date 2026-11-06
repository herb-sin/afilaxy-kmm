package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Palavra que o usuário digita para confirmar — barra exclusão por toque acidental. */
private const val CONFIRMATION_WORD = "EXCLUIR"

/**
 * Exclusão de conta (LGPD art. 18, VI; exigência do Google Play).
 * A exclusão em si é feita pela Cloud Function deleteAccount via [onDeleteAccount].
 *
 * @param hasActiveEmergency bloqueia a exclusão enquanto houver emergência em andamento.
 * @param onBeforeDelete encerra observers/modo ajudante enquanto a sessão ainda é válida.
 * @param onDeleted chamado após sucesso — a sessão local já foi encerrada.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteAccountScreen(
    hasActiveEmergency: Boolean,
    onNavigateBack: () -> Unit,
    onBeforeDelete: () -> Unit,
    onDeleteAccount: suspend () -> Result<Unit>,
    onDeleted: () -> Unit,
) {
    var confirmation by remember { mutableStateOf("") }
    var isDeleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val confirmed = confirmation.trim().equals(CONFIRMATION_WORD, ignoreCase = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Excluir minha conta") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, enabled = !isDeleting) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Esta ação é permanente e não pode ser desfeita.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("O que será apagado", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "• Seu login e seu perfil\n" +
                            "• Perfil médico, medicações, exames e contatos de emergência\n" +
                            "• Check-ins, Teste de Controle da Asma e histórico de risco\n" +
                            "• Seus pedidos de ajuda e as conversas deles\n" +
                            "• Suas mensagens e seu nome nos atendimentos que você fez\n" +
                            "• Avaliações, foto de perfil e assinatura de profissional (cancelada)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Se quiser guardar uma cópia, use antes \"Exportar meus dados\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (hasActiveEmergency) {
                Text(
                    "Você tem uma emergência em andamento. Encerre-a antes de excluir a conta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it; error = null },
                    label = { Text("Digite $CONFIRMATION_WORD para confirmar") },
                    singleLine = true,
                    enabled = !isDeleting,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }

            Button(
                onClick = {
                    isDeleting = true
                    error = null
                    onBeforeDelete()
                    scope.launch {
                        onDeleteAccount()
                            .onSuccess { onDeleted() }
                            .onFailure { e ->
                                isDeleting = false
                                error = friendlyDeleteError(e)
                            }
                    }
                },
                enabled = confirmed && !isDeleting && !hasActiveEmergency,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Excluindo…")
                } else {
                    Text("Excluir minha conta definitivamente")
                }
            }

            OutlinedButton(
                onClick = onNavigateBack,
                enabled = !isDeleting,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
            ) {
                Text("Cancelar")
            }
        }
    }
}

private fun friendlyDeleteError(e: Throwable): String {
    val msg = e.message.orEmpty()
    return when {
        // Mensagem do servidor quando o Stripe não cancelou a assinatura — já é amigável.
        msg.contains("assinatura", ignoreCase = true) -> msg
        msg.contains("network", ignoreCase = true) || msg.contains("UNAVAILABLE") ->
            "Sem conexão com o servidor. Verifique sua internet e tente novamente."
        else -> "Não foi possível excluir a conta agora. Nada foi perdido. Tente novamente."
    }
}
