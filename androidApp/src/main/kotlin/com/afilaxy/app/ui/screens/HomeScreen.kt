package com.afilaxy.app.ui.screens

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import com.afilaxy.app.R
import com.afilaxy.app.ui.components.RequestLocationPermission
import com.afilaxy.util.FileLogger
import com.afilaxy.presentation.auth.AuthViewModel
import com.afilaxy.presentation.emergency.EmergencyViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    onNavigateToEmergency: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToCommunity: () -> Unit = {},
    onNavigateToAutocuidado: () -> Unit = {},
    onNavigateToProfessionals: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: EmergencyViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val authState by authViewModel.state.collectAsState()
    var showLocationPermission by remember { mutableStateOf(false) }
    var pendingHelperMode by remember { mutableStateOf(false) }
    var showPermissionsRationale by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Notificação local quando emergência próxima chega via Firestore
    val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
    val notifiedEmergencyIds = remember { mutableSetOf<String>() }
    LaunchedEffect(state.incomingEmergencies) {
        FileLogger.log("INFO", "HomeScreen", "incomingEmergencies updated: count=${state.incomingEmergencies.size}")
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        state.incomingEmergencies
            .filter { it.userId != userId && !notifiedEmergencyIds.contains(it.id) }
            .forEach { emergency ->
                notifiedEmergencyIds.add(emergency.id)
                val channelId = "afilaxy_emergency"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    notificationManager.createNotificationChannel(
                        NotificationChannel(channelId, "Emergências", NotificationManager.IMPORTANCE_HIGH)
                    )
                }
                val intent = android.content.Intent(context, com.afilaxy.app.MainActivity::class.java).apply {
                    flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("emergencyId", emergency.id)
                    putExtra("openEmergencyResponse", true)
                }
                val pendingIntent = android.app.PendingIntent.getActivity(
                    context,
                    emergency.id.hashCode(),
                    intent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(com.afilaxy.app.R.drawable.ic_launcher_foreground)
                    .setContentTitle("🆘 Nova Emergência Próxima!")
                    .setContentText("${emergency.userName} precisa de ajuda")
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()
                notificationManager.notify(emergency.id.hashCode(), notification)
            }
    }

    // Permissões de localização + notificações (Android 13+)
    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    val permissionsState = rememberMultiplePermissionsState(permissions = requiredPermissions)
    val missingPermissions = permissionsState.permissions.filter { !it.status.isGranted }
    val hasAllPermissions = missingPermissions.isEmpty()

    // Ao entrar na tela, verificar se alguma permissão está pendente
    LaunchedEffect(Unit) {
        if (!hasAllPermissions) {
            showPermissionsRationale = true
        }
    }

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

                // Banner de aviso de permissões (persistente enquanto faltarem permissões)
                AnimatedVisibility(visible = !hasAllPermissions) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = buildPermissionWarningText(missingPermissions.map { it.permission }),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            TextButton(
                                onClick = {
                                    permissionsState.launchMultiplePermissionRequest()
                                }
                            ) {
                                Text(
                                    "Corrigir",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

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
                                    viewModel.deactivateHelper()
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

                OutlinedButton(
                    onClick = onNavigateToProfessionals,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.MedicalServices, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("👨‍⚕️ Profissionais")
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
                                viewModel.activateHelperWithLocation()
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

    // Diálogo inicial de permissões ao entrar no app
    if (showPermissionsRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionsRationale = false },
            icon = { Icon(Icons.Default.Lock, contentDescription = null) },
            title = { Text("Permissões Necessárias") },
            text = {
                Text(
                    "Para usar o Afilaxy com segurança, precisamos de:\n\n" +
                    "📍 Localização — para encontrar emergências próximas\n" +
                    "🔔 Notificações — para avisar quando alguém precisar de ajuda",
                    textAlign = TextAlign.Start
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionsRationale = false
                    permissionsState.launchMultiplePermissionRequest()
                }) {
                    Text("Permitir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionsRationale = false }) {
                    Text("Agora não")
                }
            }
        )
    }
}

/** Gera o texto do banner com base nas permissões ausentes. */
private fun buildPermissionWarningText(missingPermissions: List<String>): String {
    val hasLocation = missingPermissions.any { it.contains("LOCATION") }
    val hasNotification = missingPermissions.any { it.contains("POST_NOTIFICATIONS") }
    return when {
        hasLocation && hasNotification -> "Localização e notificações desativadas. O app pode não funcionar corretamente."
        hasLocation -> "Localização desativada. Você não conseguirá criar emergências."
        hasNotification -> "Notificações desativadas. Você não receberá alertas de emergência."
        else -> "Algumas permissões estão faltando."
    }
}
