package com.afilaxy.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afilaxy.app.R
import com.afilaxy.app.ui.components.RequestLocationPermission
import com.afilaxy.presentation.auth.AuthViewModel
import com.afilaxy.presentation.emergency.EmergencyViewModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToEmergency: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToCommunity: () -> Unit = {},
    onNavigateToAutocuidado: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: EmergencyViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val authState by authViewModel.state.collectAsState()
    var showLocationPermission by remember { mutableStateOf(false) }
    var pendingHelperMode by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Menu", style = MaterialTheme.typography.headlineSmall)
                        IconButton(onClick = { scope.launch { drawerState.close() } }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Fechar")
                        }
                    }
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Person, null) },
                        label = { Text("Perfil") },
                        selected = false,
                        onClick = { 
                            onNavigateToProfile()
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("Configurações") },
                        selected = false,
                        onClick = { 
                            onNavigateToSettings()
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Favorite, null) },
                        label = { Text("Comunidade") },
                        selected = false,
                        onClick = { 
                            onNavigateToCommunity()
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.FavoriteBorder, null) },
                        label = { Text("Autocuidado") },
                        selected = false,
                        onClick = { 
                            onNavigateToAutocuidado()
                            scope.launch { drawerState.close() }
                        }
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null) },
                        label = { Text("Sair") },
                        selected = false,
                        onClick = {
                            authViewModel.onLogout()
                            onLogout()
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🆘")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Afilaxy")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(Icons.Default.Person, contentDescription = "Perfil")
                        }
                        IconButton(onClick = {
                            authViewModel.onLogout()
                            onLogout()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sair")
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Sistema de Emergência",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Modo Ajudante",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                if (state.isHelperMode) 
                                    "Você está disponível para ajudar" 
                                else 
                                    "Ative para receber pedidos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.isHelperMode,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    pendingHelperMode = true
                                    showLocationPermission = true
                                } else {
                                    viewModel.onToggleHelperMode(false)
                                }
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onNavigateToEmergency,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        "🆘 EMERGÊNCIA",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onNavigateToHistory,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📋 Histórico")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (state.error != null) {
                    Text(
                        text = state.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
                
                if (showLocationPermission) {
                    RequestLocationPermission(
                        onPermissionGranted = {
                            showLocationPermission = false
                            if (pendingHelperMode) {
                                viewModel.onToggleHelperMode(true)
                                pendingHelperMode = false
                            }
                        },
                        onPermissionDenied = {
                            showLocationPermission = false
                            pendingHelperMode = false
                        }
                    )
                }
            }
        }
    }
}
