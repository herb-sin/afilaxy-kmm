package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Protocolo & Ajuda") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        val context = LocalContext.current
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Protocolo de Crise de Asma ────────────────────────────────────
            item {
                Text(
                    "Em caso de crise de asma",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            item {
                CrisisStepCard(
                    step = 1,
                    title = "Sente-se em posição vertical",
                    description = "Mantenha a calma. Sente-se ereto ou incline-se levemente para frente, apoiando as mãos nos joelhos. Evite deitar.",
                    color = androidx.compose.ui.graphics.Color(0xFF1565C0)
                )
            }

            item {
                CrisisStepCard(
                    step = 2,
                    title = "Use o inalador de resgate",
                    description = "Salbutamol (Aerolin): 2 a 4 jatos. Aguarde 30 segundos entre cada jato. Repita a cada 20 min se necessário, até 3 vezes.",
                    color = androidx.compose.ui.graphics.Color(0xFF2E7D32)
                )
            }

            item {
                CrisisStepCard(
                    step = 3,
                    title = "Peça ajuda se não melhorar",
                    description = "Se não houver melhora após 10 minutos: acione o SAMU pelo Afilaxy ou ligue diretamente.",
                    color = androidx.compose.ui.graphics.Color(0xFFC62828)
                )
            }

            item {
                // Botão de ação rápida para o SAMU
                Button(
                    onClick = {
                        val uri = android.net.Uri.parse("tel:192")
                        context.startActivity(android.content.Intent(android.content.Intent.ACTION_DIAL, uri))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFC62828)
                    )
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Ligar para o SAMU — 192")
                }
            }

            item { HorizontalDivider() }

            // ── Como usar o Afilaxy ───────────────────────────────────────────
            item {
                Text(
                    "Como usar o Afilaxy",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            item {
                HelpCard(
                    icon = Icons.Default.Warning,
                    title = "Criar Emergência",
                    description = "Toque no botão vermelho de emergência na tela inicial. O app notificará helpers próximos com inaladores disponíveis."
                )
            }

            item {
                HelpCard(
                    icon = Icons.Default.Person,
                    title = "Ser Helper",
                    description = "Ative o Modo Ajudante no card da tela inicial. Quando alguém precisar de ajuda próximo a você, você receberá uma notificação."
                )
            }

            item {
                HelpCard(
                    icon = Icons.Default.LocationOn,
                    title = "Permissões de Localização",
                    description = "Para receber alertas de emergências próximas, permita acesso à localização 'o tempo todo' nas configurações do app."
                )
            }

            item {
                HelpCard(
                    icon = Icons.Default.Notifications,
                    title = "Notificações",
                    description = "Mantenha as notificações ativadas para ser alertado imediatamente sobre emergências."
                )
            }

            item { HorizontalDivider() }

            item {
                Text(
                    "Perguntas Frequentes",
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            item {
                FAQItem(
                    question = "O que fazer em uma emergência?",
                    answer = "Toque no botão de emergência, aguarde um helper aceitar, e siga o protocolo de crise enquanto aguarda."
                )
            }

            item {
                FAQItem(
                    question = "Como cancelar uma emergência?",
                    answer = "Na tela de aguardo, toque em 'Cancelar Emergência'."
                )
            }

            item {
                FAQItem(
                    question = "Posso ser helper e solicitar emergência?",
                    answer = "Sim! Ao tocar em 'Emergência', o Modo Ajudante é desativado automaticamente."
                )
            }

            item {
                FAQItem(
                    question = "O app substitui o SAMU?",
                    answer = "Não. O Afilaxy conecta pessoas para compartilhar medicação de resgate enquanto o socorro profissional não chega. Em crises graves, ligue 192."
                )
            }
        }
    }
}

@Composable
private fun CrisisStepCard(step: Int, title: String, description: String, color: androidx.compose.ui.graphics.Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = color,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(
                        step.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            }
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun HelpCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FAQItem(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    question,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
