package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afilaxy.domain.model.Evento

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventoDetailScreen(
    evento: Evento,
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes do Evento") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = evento.titulo,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (evento.organizador != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Organizado por: ${evento.organizador}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Informações básicas
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📅 Informações do Evento",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Data: ${evento.data}", style = MaterialTheme.typography.bodyLarge)
                    }
                    if (evento.horario != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Horário: ${evento.horario}", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Local: ${evento.local}", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // Descrição
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📋 Sobre o Evento",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(evento.descricao, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Como participar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🎫 Como Participar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val instrucoes = when {
                        evento.local.contains("Online", ignoreCase = true) ||
                        evento.local.contains("YouTube", ignoreCase = true) -> """
                            📱 Evento Online:
                            • Acesse o link no horário do evento
                            • Não é necessário inscrição prévia
                            • Participe pelo chat durante a transmissão
                        """.trimIndent()
                        evento.local.contains("Zoom", ignoreCase = true) ||
                        evento.local.contains("Teams", ignoreCase = true) -> """
                            💻 Evento Virtual:
                            • Inscreva-se previamente para receber o link
                            • Teste sua conexão antes do evento
                            • Mantenha microfone mutado durante as apresentações
                        """.trimIndent()
                        else -> """
                            🏢 Evento Presencial:
                            • Confirme sua presença com antecedência
                            • Chegue 15 minutos antes do horário
                            • Traga documento de identificação
                        """.trimIndent()
                    }
                    Text(instrucoes, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { /* TODO: Implementar inscrição */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Quero Participar")
                    }
                }
            }
        }
    }
}
