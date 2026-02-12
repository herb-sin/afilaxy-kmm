package com.afilaxy.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.afilaxy.app.navigation.AppRoutes
import com.afilaxy.domain.repository.AuthRepository
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    authRepository: AuthRepository = koinInject()
) {
    val scope = rememberCoroutineScope()
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
        ) {
            SettingsItem(
                icon = Icons.Default.Person,
                title = "Perfil",
                onClick = { navController.navigate(AppRoutes.PROFILE) }
            )
            
            HorizontalDivider()
            
            SettingsItem(
                icon = Icons.Default.Notifications,
                title = "Notificações",
                onClick = { navController.navigate(AppRoutes.NOTIFICATIONS) }
            )
            
            HorizontalDivider()
            
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Ajuda",
                onClick = { navController.navigate(AppRoutes.HELP) }
            )
            
            HorizontalDivider()
            
            SettingsItem(
                icon = Icons.Default.Info,
                title = "Sobre o Projeto",
                onClick = { navController.navigate(AppRoutes.ABOUT) }
            )
            
            HorizontalDivider()
            
            SettingsItem(
                icon = Icons.Default.Lock,
                title = "Termos de Uso",
                onClick = { navController.navigate(AppRoutes.TERMS) }
            )
            
            HorizontalDivider()
            
            SettingsItem(
                icon = Icons.Default.Lock,
                title = "Política de Privacidade",
                onClick = { navController.navigate(AppRoutes.PRIVACY) }
            )
            
            HorizontalDivider()
            
            SettingsItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                title = "Sair",
                onClick = { showLogoutDialog = true }
            )
        }
    }
    
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sair") },
            text = { Text("Deseja realmente sair?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            authRepository.logout()
                            navController.navigate(AppRoutes.LOGIN) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                ) {
                    Text("Sim")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Não")
                }
            }
        )
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge)
    }
}
