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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
fun HomeScreenOld(
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
    var locationPermissionKey by remember { mutableStateOf(0) }
    var showPermissionsRationale by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Layer 3 — safety net: inicia/reinicia observer quando coordenadas ou helperMode mudam
    LaunchedEffect(state.isHelperMode, state.userLatitude, state.userLongitude) {
        if (state.isHelperMode &&
            (state.userLatitude != 0.0 || state.userLongitude != 0.0)
        ) {
            viewModel.startObservingIncomingEmergencies(state.userLatitude, state.userLongitude)
        }
    }

    // Notificação local quando emergência próxima chega via Firestore
    val notificationManager = context.getSystemService(android.app.NotificationManager::class.java)
    LaunchedEffect(state.incomingEmergencies) {
        FileLogger.log("INFO", "HomeScreen", "incomingEmergencies updated: count=${state.incomingEmergencies.size}")
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        state.incomingEmergencies
            .filter { it.userId != userId && !viewModel.isAlreadyNotified(it.id) }
            .forEach { emergency ->
                viewModel.markAsNotified(emergency.id)
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

    // NOVO DESIGN: Feed de Comunidade Editorial
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        item {
            HeroCard(onNavigateToEmergency = onNavigateToEmergency)
        }
        
        // Modo Ajudante Card
        item {
            HelperModeCard(
                isHelperMode = state.isHelperMode,
                onToggleHelper = { checked ->
                    if (checked) {
                        pendingHelperMode = true
                        locationPermissionKey++
                        showLocationPermission = true
                    } else {
                        viewModel.deactivateHelper()
                    }
                }
            )
        }
        
        // Emergency Cards
        if (state.incomingEmergencies.isNotEmpty()) {
            items(state.incomingEmergencies) { emergency ->
                EmergencyCard(emergency = emergency)
            }
        }
        
        // Posts da Comunidade (placeholder)
        item {
            CommunityPostsSection()
        }
        
        // Suporte Rápido
        item {
            QuickSupportSection(
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToProfessionals = onNavigateToProfessionals
            )
        }
        
        // Banner de permissões se necessário
        if (!hasAllPermissions) {
            item {
                PermissionsBanner(
                    missingPermissions = missingPermissions.map { it.permission },
                    onRequestPermissions = {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                )
            }
        }
        
        // Erro se houver
        if (state.error != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = state.error ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }

    // Componentes auxiliares
    if (showLocationPermission) {
        key(locationPermissionKey) {
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

@Composable
private fun HeroCard(onNavigateToEmergency: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF1E3A8A), // Azul escuro
                            Color(0xFF3B82F6)  // Azul médio
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxHeight()
            ) {
                Column {
                    Text(
                        text = "Bem-vindo à Comunidade",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Respire fundo. Você não está sozinho.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
                
                Button(
                    onClick = onNavigateToEmergency,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "🆘 Solicitar Ajuda",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun HelperModeCard(
    isHelperMode: Boolean,
    onToggleHelper: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Modo Ajudante",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (isHelperMode)
                        "Você está disponível para ajudar"
                    else
                        "Ative para receber pedidos de ajuda",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isHelperMode,
                onCheckedChange = onToggleHelper
            )
        }
    }
}

@Composable
private fun EmergencyCard(emergency: Any) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            2.dp, 
            MaterialTheme.colorScheme.error
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Emergência Próxima",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    "Alguém precisa de ajuda na sua região",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(
                onClick = { /* Navegar para resposta */ }
            ) {
                Text("Ajudar")
            }
        }
    }
}

@Composable
private fun CommunityPostsSection() {
    Column {
        Text(
            text = "Comunidade",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Posts placeholder
        val posts = listOf(
            "Dica: Use o espaçador sempre que possível",
            "Compartilhe: Como você controla sua asma?",
            "Lembrete: Não esqueça da medicação de manutenção"
        )
        
        posts.forEach { post ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Text(
                    text = post,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickSupportSection(
    onNavigateToHistory: () -> Unit,
    onNavigateToProfessionals: () -> Unit
) {
    Column {
        Text(
            text = "Suporte Rápido",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateToHistory,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Text("Histórico", style = MaterialTheme.typography.labelSmall)
                }
            }
            
            OutlinedButton(
                onClick = onNavigateToProfessionals,
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.MedicalServices, contentDescription = null)
                    Text("Médicos", style = MaterialTheme.typography.labelSmall)
                }
            }
            
            OutlinedButton(
                onClick = { /* Farmácias */ },
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.LocalPharmacy, contentDescription = null)
                    Text("Farmácias", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun PermissionsBanner(
    missingPermissions: List<String>,
    onRequestPermissions: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = buildPermissionWarningText(missingPermissions),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            TextButton(onClick = onRequestPermissions) {
                Text(
                    "Corrigir",
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
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