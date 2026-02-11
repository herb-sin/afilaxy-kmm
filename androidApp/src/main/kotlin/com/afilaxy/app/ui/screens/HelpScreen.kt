package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajuda") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                    description = "Toque no botão vermelho de emergência, descreva a situação e aguarde um helper aceitar."
                )
            }
            
            item {
                HelpCard(
                    icon = Icons.Default.Person,
                    title = "Ser Helper",
                    description = "Ative o modo helper no menu principal. Você receberá notificações de emergências próximas."
                )
            }
            
            item {
                HelpCard(
                    icon = Icons.Default.LocationOn,
                    title = "Permissões de Localização",
                    description = "Para receber emergências próximas, permita acesso à localização 'o tempo todo'."
                )
            }
            
            item {
                HelpCard(
                    icon = Icons.Default.Notifications,
                    title = "Notificações",
                    description = "Mantenha as notificações ativadas para ser alertado sobre emergências."
                )
            }
            
            item {
                Divider()
            }
            
            item {
                Text(
                    "Perguntas Frequentes",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            
            item {
                FAQItem(
                    question = "O que fazer em uma emergência?",
                    answer = "Toque no botão de emergência, descreva a situação e aguarde. Um helper próximo será notificado."
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
                    answer = "Sim! Você pode alternar entre os modos a qualquer momento."
                )
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
