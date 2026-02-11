package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Termos de Uso") },
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
                    "Termos de Uso do Afilaxy",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            
            item {
                Text(
                    "Última atualização: Janeiro de 2024",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            item {
                TermSection(
                    title = "1. Aceitação dos Termos",
                    content = "Ao utilizar o Afilaxy, você concorda com estes termos de uso. Se não concordar, não utilize o aplicativo."
                )
            }
            
            item {
                TermSection(
                    title = "2. Descrição do Serviço",
                    content = "O Afilaxy é uma plataforma que conecta pessoas em situações de emergência com helpers voluntários próximos. O serviço não substitui atendimento médico profissional."
                )
            }
            
            item {
                TermSection(
                    title = "3. Responsabilidades do Usuário",
                    content = "• Fornecer informações verdadeiras\n• Usar o serviço apenas para emergências reais\n• Respeitar outros usuários\n• Não utilizar o app para fins ilegais"
                )
            }
            
            item {
                TermSection(
                    title = "4. Responsabilidades do Helper",
                    content = "• Responder apenas emergências que possa atender\n• Agir com responsabilidade e ética\n• Não se apresentar como profissional de saúde se não for\n• Seguir protocolos de segurança"
                )
            }
            
            item {
                TermSection(
                    title = "5. Limitação de Responsabilidade",
                    content = "O Afilaxy não se responsabiliza por:\n• Ações de helpers ou solicitantes\n• Qualidade do atendimento prestado\n• Danos decorrentes do uso do app\n• Disponibilidade de helpers"
                )
            }
            
            item {
                TermSection(
                    title = "6. Privacidade",
                    content = "Seus dados são tratados conforme nossa Política de Privacidade. Coletamos localização apenas durante uso ativo do app."
                )
            }
            
            item {
                TermSection(
                    title = "7. Modificações",
                    content = "Podemos modificar estes termos a qualquer momento. Alterações significativas serão notificadas no app."
                )
            }
            
            item {
                TermSection(
                    title = "8. Contato",
                    content = "Para dúvidas sobre estes termos, entre em contato através do app ou email: suporte@afilaxy.com"
                )
            }
            
            item {
                Divider()
            }
            
            item {
                Text(
                    "Ao continuar usando o Afilaxy, você confirma que leu e concordou com estes termos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun TermSection(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                content,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
