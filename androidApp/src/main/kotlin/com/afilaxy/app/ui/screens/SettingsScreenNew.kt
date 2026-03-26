package com.afilaxy.app.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import com.afilaxy.app.navigation.AppRoutes
import com.afilaxy.domain.repository.AuthRepository
import com.afilaxy.presentation.auth.AuthViewModel
import com.afilaxy.util.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenNew(
    navController: NavController,
    authRepository: AuthRepository = koinInject(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val scope = rememberCoroutineScope()
    val authState by authViewModel.state.collectAsState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Configurações",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                UserProfileCard(
                    userName = authState.user?.displayName ?: "Usuário",
                    userEmail = authState.user?.email ?: "",
                    onEditProfile = { navController.navigate(AppRoutes.PROFILE) }
                )
            }
            
            item {
                SettingsSection(
                    title = "Conta",
                    items = listOf(
                        SettingsItemData(
                            icon = Icons.Default.Person,
                            title = "Editar Perfil",
                            subtitle = "Nome, foto e informações pessoais",
                            onClick = { navController.navigate(AppRoutes.PROFILE) }
                        ),
                        SettingsItemData(
                            icon = Icons.Default.Notifications,
                            title = "Notificações",
                            subtitle = "Configurar alertas e lembretes",
                            onClick = { navController.navigate(AppRoutes.NOTIFICATIONS) }
                        ),
                        SettingsItemData(
                            icon = Icons.Default.Security,
                            title = "Privacidade",
                            subtitle = "Controle de dados e segurança",
                            onClick = { navController.navigate(AppRoutes.PRIVACY) }
                        )
                    )
                )
            }
            
            item {
                SettingsSection(
                    title = "Suporte",
                    items = listOf(
                        SettingsItemData(
                            icon = Icons.Default.Help,
                            title = "Central de Ajuda",
                            subtitle = "FAQ e tutoriais",
                            onClick = { navController.navigate(AppRoutes.HELP) }
                        ),
                        SettingsItemData(
                            icon = Icons.Default.Info,
                            title = "Sobre o Afilaxy",
                            subtitle = "Versão e informações do app",
                            onClick = { navController.navigate(AppRoutes.ABOUT) }
                        ),
                        SettingsItemData(
                            icon = Icons.Default.Feedback,
                            title = "Enviar Feedback",
                            subtitle = "Ajude-nos a melhorar",
                            onClick = { /* TODO: Implementar feedback */ }
                        )
                    )
                )
            }
            
            item {
                SettingsSection(
                    title = "Legal",
                    items = listOf(
                        SettingsItemData(
                            icon = Icons.Default.Description,
                            title = "Termos de Uso",
                            subtitle = "Condições de utilização",
                            onClick = { navController.navigate(AppRoutes.TERMS) }
                        ),
                        SettingsItemData(
                            icon = Icons.Default.Policy,
                            title = "Política de Privacidade",
                            subtitle = "Como tratamos seus dados",
                            onClick = { navController.navigate(AppRoutes.PRIVACY) }
                        )
                    )
                )
            }
            
            item {
                DeveloperSectionNew()
            }
            
            item {
                LogoutCard(
                    onLogout = { showLogoutDialog = true }
                )
            }
        }
    }
    
    if (showLogoutDialog) {
        LogoutDialog(
            onConfirm = {
                scope.launch {
                    authRepository.logout()
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                showLogoutDialog = false
            },
            onDismiss = { showLogoutDialog = false }
        )
    }
}

@Composable
private fun UserProfileCard(
    userName: String,
    userEmail: String,
    onEditProfile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEditProfile() }
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.firstOrNull()?.uppercase() ?: "U",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    userName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    userEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    items: List<SettingsItemData>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(20.dp, 16.dp, 20.dp, 8.dp)
            )
            
            items.forEachIndexed { index, item ->
                SettingsItemNew(item = item)
                if (index < items.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsItemNew(item: SettingsItemData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() }
            .padding(20.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            item.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (item.subtitle.isNotEmpty()) {
                Text(
                    item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun DeveloperSectionNew() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var logSize by remember { mutableStateOf("Calculando...") }
    var showClearDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val size = FileLogger.getTotalSize()
            logSize = "${size / 1024} KB"
        }
    }
    
    SettingsSection(
        title = "Desenvolvedor",
        items = listOf(
            SettingsItemData(
                icon = Icons.Default.Share,
                title = "Exportar Logs",
                subtitle = "Tamanho: $logSize",
                onClick = {
                    scope.launch(Dispatchers.IO) {
                        val logs = FileLogger.getAllLogs()
                        if (logs.isNotEmpty()) {
                            val uris = logs.map { file ->
                                FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                            }
                            
                            withContext(Dispatchers.Main) {
                                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                    type = "text/plain"
                                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Exportar Logs"))
                            }
                        }
                    }
                }
            ),
            SettingsItemData(
                icon = Icons.Default.Delete,
                title = "Limpar Logs",
                subtitle = "Remove todos os arquivos de log",
                onClick = { showClearDialog = true }
            )
        )
    )
    
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Limpar Logs") },
            text = { Text("Deseja realmente limpar todos os logs? Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            FileLogger.clearLogs()
                            logSize = "0 KB"
                        }
                        showClearDialog = false
                    }
                ) {
                    Text("Limpar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun LogoutCard(onLogout: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLogout() }
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                "Sair da Conta",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun LogoutDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Sair da Conta",
                fontWeight = FontWeight.Bold
            ) 
        },
        text = { 
            Text("Tem certeza que deseja sair? Você precisará fazer login novamente para acessar o app.") 
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Sair")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

private data class SettingsItemData(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String = "",
    val onClick: () -> Unit
)