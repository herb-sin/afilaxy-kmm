package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afilaxy.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import org.koin.compose.koinInject

/**
 * Tela de verificação de email — portada do legado, migrada para KMM AuthRepository.
 * Bloqueia o acesso ao app até que o usuário confirme o email.
 * Verifica automaticamente a cada 3s (até 20 tentativas = ~1 min).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmailVerificationScreen(
    userEmail: String = "",
    onVerified: () -> Unit,
    onLogout: () -> Unit,
    authRepository: AuthRepository = koinInject()
) {
    var isChecking by remember { mutableStateOf(false) }
    var showResendSuccess by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    var cooldownTime by remember { mutableStateOf(0) }
    var emailDisplay by remember { mutableStateOf(userEmail) }

    // Carrega email do repositório se não foi fornecido via argumento
    LaunchedEffect(Unit) {
        if (emailDisplay.isEmpty()) {
            emailDisplay = authRepository.getCurrentUser()?.email ?: ""
        }
    }

    // Countdown do cooldown de reenvio
    LaunchedEffect(cooldownTime) {
        if (cooldownTime > 0) {
            delay(1000L)
            cooldownTime--
        }
    }

    // Auto-polling: verifica a cada 3s, máximo 20 tentativas (~1 min)
    LaunchedEffect(Unit) {
        var verified = false
        var attempts = 0
        while (!verified && attempts < 20) {
            delay(3000L)
            authRepository.reloadUser()
            verified = authRepository.isEmailVerified()
            if (verified) onVerified()
            attempts++
        }
    }

    // Verificação manual
    LaunchedEffect(isChecking) {
        if (isChecking) {
            authRepository.reloadUser()
            if (authRepository.isEmailVerified()) {
                onVerified()
            }
            isChecking = false
        }
    }

    // Reenvio de email
    LaunchedEffect(isResending) {
        if (isResending) {
            authRepository.sendEmailVerification()
            isResending = false
            showResendSuccess = true
            cooldownTime = 60
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verificação de Email") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Sair")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Email,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Verifique seu email",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Enviamos um link de verificação para:",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = emailDisplay,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Clique no link do email para verificar sua conta.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "💡 Não encontrou? Verifique sua caixa de spam.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Botão: verificar manualmente
            Button(
                onClick = { isChecking = true },
                enabled = !isChecking,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verificar Agora")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botão: reenviar email com cooldown 60s
            OutlinedButton(
                onClick = { isResending = true },
                enabled = !isResending && cooldownTime == 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                when {
                    isResending -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enviando...")
                    }
                    cooldownTime > 0 -> Text("Aguarde ${cooldownTime}s")
                    else -> Text("Reenviar Email")
                }
            }

            // Card de sucesso de reenvio
            if (showResendSuccess) {
                LaunchedEffect(Unit) {
                    delay(5000L)
                    showResendSuccess = false
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Email reenviado!",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Atenção: links anteriores foram invalidados. Use apenas o link do email mais recente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
