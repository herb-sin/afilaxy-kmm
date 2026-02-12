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
fun PrivacyScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Política de Privacidade") },
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
                    "Política de Privacidade",
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
                PrivacySection(
                    title = "1. Informações Coletadas",
                    content = "Coletamos:\n• Nome e email (cadastro)\n• Localização GPS (durante uso ativo)\n• Dados de emergências criadas\n• Mensagens de chat\n• Token FCM para notificações"
                )
            }
            
            item {
                PrivacySection(
                    title = "2. Uso das Informações",
                    content = "Usamos seus dados para:\n• Conectar você com helpers próximos\n• Enviar notificações de emergências\n• Melhorar o serviço\n• Garantir segurança da plataforma"
                )
            }
            
            item {
                PrivacySection(
                    title = "3. Localização",
                    content = "Sua localização é coletada apenas quando:\n• Você cria uma emergência\n• Está no modo helper ativo\n• Não rastreamos localização em segundo plano sem necessidade"
                )
            }
            
            item {
                PrivacySection(
                    title = "4. Compartilhamento de Dados",
                    content = "Compartilhamos dados apenas:\n• Com helpers durante emergências ativas\n• Com autoridades se legalmente exigido\n• Nunca vendemos seus dados"
                )
            }
            
            item {
                PrivacySection(
                    title = "5. Armazenamento",
                    content = "Dados são armazenados no Firebase (Google Cloud) com:\n• Criptografia em trânsito e repouso\n• Acesso restrito\n• Backups regulares\n• Servidores em conformidade com LGPD"
                )
            }
            
            item {
                PrivacySection(
                    title = "6. Seus Direitos",
                    content = "Você pode:\n• Acessar seus dados\n• Solicitar correção\n• Solicitar exclusão\n• Revogar consentimento\n• Exportar dados"
                )
            }
            
            item {
                PrivacySection(
                    title = "7. Retenção de Dados",
                    content = "Mantemos dados:\n• Conta ativa: enquanto usar o app\n• Emergências: 90 dias após resolução\n• Chats: 30 dias após emergência\n• Logs: 30 dias"
                )
            }
            
            item {
                PrivacySection(
                    title = "8. Cookies e Tecnologias",
                    content = "Usamos:\n• Firebase Analytics (anônimo)\n• Crashlytics (relatórios de erro)\n• Você pode desativar nas configurações"
                )
            }
            
            item {
                PrivacySection(
                    title = "9. Segurança",
                    content = "Implementamos:\n• Autenticação Firebase\n• HTTPS em todas comunicações\n• Regras de segurança Firestore\n• Monitoramento de atividades suspeitas"
                )
            }
            
            item {
                PrivacySection(
                    title = "10. Contato",
                    content = "Para exercer seus direitos ou dúvidas:\n• Email: privacidade@afilaxy.com\n• Resposta em até 15 dias úteis"
                )
            }
            
            item {
                HorizontalDivider()
            }
            
            item {
                Text(
                    "Esta política está em conformidade com a LGPD (Lei Geral de Proteção de Dados).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun PrivacySection(title: String, content: String) {
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
