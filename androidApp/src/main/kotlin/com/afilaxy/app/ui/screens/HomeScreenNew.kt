package com.afilaxy.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.afilaxy.app.ui.components.RequestLocationPermission
import com.afilaxy.domain.repository.PreferencesRepository
import com.afilaxy.presentation.auth.AuthViewModel
import com.afilaxy.presentation.emergency.EmergencyViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenNew(
    onNavigateToEmergency: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToCommunity: () -> Unit = {},
    onNavigateToAutocuidado: () -> Unit = {},
    onNavigateToProfessionals: () -> Unit,
    onNavigateToEducation: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: EmergencyViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val emergencyState by viewModel.state.collectAsState()
    val authState by authViewModel.state.collectAsState()
    val prefsRepo: PreferencesRepository = koinInject()
    var isHelperPending by remember { mutableStateOf(false) }
    var helperIntended by remember { mutableStateOf(false) }
    var showHelperPermission by remember { mutableStateOf(false) }
    var showHelperConsentDialog by remember { mutableStateOf(false) }
    // -1 = carregando, 0 = nenhum pedido, 1+ = atenção/urgente
    var weeklyCount by remember { mutableStateOf(-1) }

    // Listener em tempo real: atualiza automaticamente quando a Cloud Function
    // onEmergencyFinalized escrever em user_stats após a emergência ser resolvida.
    DisposableEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) { weeklyCount = 0; return@DisposableEffect onDispose {} }
        val cal = Calendar.getInstance()
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        val year = cal.get(Calendar.YEAR)
        val weekKey = "%d-W%02d".format(year, week)
        val listener = FirebaseFirestore.getInstance()
            .collection("user_stats")
            .document(uid)
            .addSnapshotListener { doc: DocumentSnapshot?, _ ->
                @Suppress("UNCHECKED_CAST")
                val weekly = doc?.get("weeklyCount") as? Map<String, Any>
                weeklyCount = (weekly?.get(weekKey) as? Long)?.toInt() ?: 0
            }
        onDispose { listener.remove() }
    }

    // Diálogo de explicação antes de solicitar localização em background
    var showBackgroundLocationRationale by remember { mutableStateOf(false) }
    // Launcher para ACCESS_BACKGROUND_LOCATION (solicitado separadamente no Android 10+)
    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        // Independente do resultado (aceito ou negado), ativa o helper.
        // Sem "Sempre": helper funciona só em foreground.
        // Com "Sempre": helper funciona em background também.
        isHelperPending = true
        helperIntended = true
        viewModel.activateHelperWithLocation()
    }

    // Sincroniza o estado local com o ViewModel quando a operação conclui.
    LaunchedEffect(emergencyState.isHelperMode) {
        isHelperPending = false
        helperIntended = emergencyState.isHelperMode
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header com saudação + Helper Mode
        item {
            HomeWelcomeCard(
                userName = authState.user?.displayName ?: authState.user?.name ?: "Usuário",
                weeklyCount = weeklyCount,
                // Enquanto pendente, mostra o valor pretendido pelo usuário.
                // Após o ViewModel confirmar, LaunchedEffect zera isHelperPending.
                isHelperMode = if (isHelperPending) helperIntended else emergencyState.isHelperMode,
                onHelperModeToggle = { enable ->
                    if (isHelperPending) return@HomeWelcomeCard  // ignora tap duplo
                    if (enable) {
                        val consentGiven = prefsRepo.getBoolean("helper_map_consent_v1", false)
                        if (consentGiven) {
                            // Consentimento já dado anteriormente — vai direto para permissão
                            showHelperPermission = true
                        } else {
                            // Primeira vez — exibe dialog LGPD antes de qualquer outra ação
                            showHelperConsentDialog = true
                        }
                    } else {
                        isHelperPending = true
                        helperIntended = false
                        viewModel.deactivateHelper()
                    }
                }
            )
        }

        // Botão de Emergência Principal
        item {
            HomeEmergencyButton(
                onClick = onNavigateToEmergency,
                isActive = emergencyState.currentEmergency != null
            )
        }

        // Atalhos rápidos — Row + Row em vez de LazyVerticalGrid aninhado
        item {
            HomeQuickActions(
                onNavigateToProfessionals = onNavigateToProfessionals,
                onNavigateToEducation = onNavigateToEducation,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToProfile = onNavigateToProfile
            )
        }

        // Estatísticas da comunidade
        item {
            HomeStatsCard()
        }
    }

    // Dialog de consentimento LGPD — exibido apenas na primeira ativação.
    // Explica que a localização aproximada fica visível para outros usuários.
    if (showHelperConsentDialog) {
        AlertDialog(
            onDismissRequest = {
                showHelperConsentDialog = false
                helperIntended = emergencyState.isHelperMode
            },
            icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Sua localização será visível") },
            text = {
                Text(
                    "Ao ativar o Modo Ajudante, sua posição aproximada (precisão \u00b1100 m) " +
                    "ficará visível no mapa para outros usuários do Afilaxy\n\n" +
                    "Seus dados são anonimizados: nome de exibição e localização aproximada apenas. " +
                    "Nenhum endereço ou dado pessoal sensível é compartilhado.\n\n" +
                    "Você pode desativar o Modo Ajudante a qualquer momento."
                )
            },
            confirmButton = {
                Button(onClick = {
                    prefsRepo.putBoolean("helper_map_consent_v1", true)
                    showHelperConsentDialog = false
                    showHelperPermission = true  // próximo passo: pedir permissão de localização
                }) {
                    Text("Concordar e Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showHelperConsentDialog = false
                    helperIntended = emergencyState.isHelperMode
                }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Após a permissão foreground ser concedida, verifica se background também pode ser pedido.
    if (showHelperPermission) {
        RequestLocationPermission(
            onPermissionGranted = {
                showHelperPermission = false
                // Verifica se ACCESS_BACKGROUND_LOCATION já foi concedida ou se o Android
                // é < 10 (sem conceito de background location separado).
                val alreadyHasBackground = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) == PermissionChecker.PERMISSION_GRANTED
                if (alreadyHasBackground) {
                    // Já tem tudo — ativa direto
                    isHelperPending = true
                    helperIntended = true
                    viewModel.activateHelperWithLocation()
                } else {
                    // Exibe diálogo de rationale antes do pedido de background
                    showBackgroundLocationRationale = true
                }
            },
            onPermissionDenied = {
                showHelperPermission = false
                isHelperPending = false
                helperIntended = emergencyState.isHelperMode
            }
        )
    }

    // Diálogo de explicação do "Permitir sempre" antes de solicitar a permissão de background.
    if (showBackgroundLocationRationale) {
        AlertDialog(
            onDismissRequest = {
                // Usuário fechou sem decidir — ativa helper sem background
                showBackgroundLocationRationale = false
                isHelperPending = true
                helperIntended = true
                viewModel.activateHelperWithLocation()
            },
            icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Localização em segundo plano") },
            text = {
                Text(
                    "Para receber alertas de emergência mesmo quando o Afilaxy estiver fechado, " +
                    "precisamos que você selecione \"Permitir sempre\" na próxima tela.\n\n" +
                    "Sem essa permissão, o Modo Ajudante funciona apenas enquanto o app estiver aberto."
                )
            },
            confirmButton = {
                Button(onClick = {
                    showBackgroundLocationRationale = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                }) {
                    Text("Entendi, Permitir")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackgroundLocationRationale = false
                    // Ativa sem background — funciona em foreground
                    isHelperPending = true
                    helperIntended = true
                    viewModel.activateHelperWithLocation()
                }) {
                    Text("Agora não")
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------
// Componentes privados
// ---------------------------------------------------------------------------

@Composable
private fun HomeWelcomeCard(
    userName: String,
    weeklyCount: Int,
    isHelperMode: Boolean,
    onHelperModeToggle: (Boolean) -> Unit
) {
    // Cor do gradiente e mensagens dependem da contagem semanal
    val (gradientColors, headline, body) = when (weeklyCount) {
        -1 -> Triple(
            listOf(Color(0xFF1A73E8), Color(0xFF0D47A1)),
            "Carregando seu status...",
            ""
        )
        0 -> Triple(
            listOf(Color(0xFF1976D2), Color(0xFF1565C0)),
            "Essa semana nenhum pedido de socorro.",
            "Sua asma parece controlada! Continue assim. 💙"
        )
        1 -> Triple(
            listOf(Color(0xFFF4A825), Color(0xFFE65100)),
            "Você já fez 1 pedido de emergência essa semana.",
            "Se precisar da bombinha novamente, considere marcar uma consulta. 🩺"
        )
        else -> Triple(
            listOf(Color(0xFFC62828), Color(0xFF7B1A1A)),
            "Você já fez $weeklyCount pedidos de emergência essa semana.",
            "Sua asma pode estar fora de controle. Agende uma consulta com urgência! ❗"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(colors = gradientColors),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "Olá, $userName!",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            // Status semanal
            if (weeklyCount == -1) {
                // Skeleton placeholder enquanto carrega
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp)
                        .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                )
            } else {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp)
                )
                if (body.isNotEmpty()) {
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                // Pill de contagem
                if (weeklyCount >= 0) {
                    val pillText = when (weeklyCount) {
                        0    -> "0 pedidos esta semana"
                        1    -> "1 pedido esta semana"
                        else -> "$weeklyCount pedidos esta semana"
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        Text(
                            text = pillText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Helper toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Modo Ajudante",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isHelperMode) "Ativo — você pode receber pedidos" else "Desativado",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
                Switch(
                    checked = isHelperMode,
                    onCheckedChange = onHelperModeToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = Color.White,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
}

@Composable
private fun HomeEmergencyButton(
    onClick: () -> Unit,
    isActive: Boolean
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Emergency,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isActive) "Emergência Ativa" else "Solicitar Ajuda",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HomeQuickActions(
    onNavigateToProfessionals: () -> Unit,
    onNavigateToEducation: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Acesso Rápido",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeActionCard(
                title = "Profissionais",
                icon = Icons.Default.MedicalServices,
                onClick = onNavigateToProfessionals,
                modifier = Modifier.weight(1f)
            )
            HomeActionCard(
                title = "Educação",
                icon = Icons.Default.School,
                onClick = onNavigateToEducation,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeActionCard(
                title = "Histórico",
                icon = Icons.Default.History,
                onClick = onNavigateToHistory,
                modifier = Modifier.weight(1f)
            )
            HomeActionCard(
                title = "Perfil",
                icon = Icons.Default.Person,
                onClick = onNavigateToProfile,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun HomeStatsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Comunidade Afilaxy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HomeStatItem("0", "Emergências")
                HomeStatItem("0", "Ajudas")
                HomeStatItem("0", "Membros")
            }
        }
    }
}

@Composable
private fun HomeStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}