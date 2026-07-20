package com.afilaxy.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
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
import androidx.core.content.FileProvider
import androidx.core.content.PermissionChecker
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import com.afilaxy.app.ui.components.RequestLocationPermission
import com.afilaxy.app.ui.components.RiskWidget
import com.afilaxy.domain.repository.EmergencyRepository
import com.afilaxy.domain.repository.PreferencesRepository
import com.afilaxy.presentation.auth.AuthViewModel
import com.afilaxy.presentation.emergency.EmergencyViewModel
import com.afilaxy.app.BuildConfig
import com.afilaxy.util.FileLogger
import java.util.Calendar
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenNew(
    // Stats elevadas ao NavGraph — listener único por sessão
    weeklyCount: Int = -1,
    totalEmergencies: Int = -1,
    onNavigateToEmergency: () -> Unit,
    onNavigateToCheckIn: ((type: String) -> Unit)? = null,
    onNavigateToAutocuidado: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToPharmacyMap: () -> Unit = {},
    viewModel: EmergencyViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val emergencyState by viewModel.state.collectAsState()
    val authState by authViewModel.state.collectAsState()
    val prefsRepo: PreferencesRepository = koinInject()
    val emergencyRepo: EmergencyRepository = koinInject()
    val scope = rememberCoroutineScope()
    var isHelperPending by rememberSaveable { mutableStateOf(false) }
    var helperIntended by rememberSaveable { mutableStateOf(false) }
    // Evita que LaunchedEffect(isHelperMode) resete isHelperPending na primeira composição
    var helperSyncedOnce by rememberSaveable { mutableStateOf(false) }
    var showHelperPermission by remember { mutableStateOf(false) }
    var showHelperConsentDialog by remember { mutableStateOf(false) }
    var showNps by remember { mutableStateOf(false) }
    var showPostCrisisDialog by remember { mutableStateOf(false) }

    // NPS: exibe uma vez, 7 dias após a primeira emergência
    LaunchedEffect(Unit) {
        val firstAtStr = prefsRepo.getString("first_emergency_at", null) ?: return@LaunchedEffect
        val firstAt = firstAtStr.toLongOrNull() ?: return@LaunchedEffect
        val npsShown = prefsRepo.getBoolean("nps_shown", false)
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        if (!npsShown && System.currentTimeMillis() - firstAt >= sevenDaysMs) {
            showNps = true
        }
    }

    // Diálogo pós-crise: exibido 60 min após um pedido de emergência realizado
    LaunchedEffect(Unit) {
        val requestedAtStr = prefsRepo.getString("last_emergency_requested_at", null) ?: return@LaunchedEffect
        val requestedAt = requestedAtStr.toLongOrNull() ?: return@LaunchedEffect
        val dialogShown = prefsRepo.getBoolean("post_crisis_dialog_shown", false)
        val elapsed = System.currentTimeMillis() - requestedAt
        val sixtyMinMs = 60L * 60 * 1000
        val fortyEightHMs = 48L * 60 * 60 * 1000
        if (!dialogShown && elapsed >= sixtyMinMs && elapsed <= fortyEightHMs) {
            showPostCrisisDialog = true
        }
    }


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
    // Na primeira composição, só sincroniza se não há operação pendente salva,
    // evitando apagar isHelperPending=true restaurado pelo rememberSaveable.
    LaunchedEffect(emergencyState.isHelperMode) {
        if (!helperSyncedOnce) {
            helperSyncedOnce = true
            if (!isHelperPending) helperIntended = emergencyState.isHelperMode
        } else {
            isHelperPending = false
            helperIntended = emergencyState.isHelperMode
        }
    }

    // Exibe o erro de ativação/desativação de helper; reseta o estado pendente para que
    // o toggle reflita a realidade (helper NÃO ativo em caso de falha).
    LaunchedEffect(emergencyState.error) {
        emergencyState.error?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            if (isHelperPending) {
                isHelperPending = false
                helperIntended = emergencyState.isHelperMode
            }
        }
    }

    // Localização para o RiskWidget via FusedLocationProviderClient (mais confiável)
    var riskLat by remember { mutableStateOf(0.0) }
    var riskLng by remember { mutableStateOf(0.0) }
    LaunchedEffect(Unit) {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            @Suppress("MissingPermission")
            val loc = fusedClient.lastLocation.await()
            if (loc != null) {
                riskLat = loc.latitude
                riskLng = loc.longitude
            } else {
                // No cached location — request a fresh fix (up to 10s, balanced accuracy)
                @Suppress("MissingPermission")
                val fresh = withTimeoutOrNull(10_000) {
                    val req = CurrentLocationRequest.Builder()
                        .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                        .build()
                    fusedClient.getCurrentLocation(req, null).await()
                }
                if (fresh != null) {
                    riskLat = fresh.latitude
                    riskLng = fresh.longitude
                }
            }
        } catch (_: Exception) { /* Sem permissão — RiskWidget não exibido */ }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header — card semanal (helper toggle movido para card próprio abaixo)
        item {
            HomeWelcomeCard(
                userName = authState.user?.displayName ?: authState.user?.name ?: "Usuário",
                weeklyCount = weeklyCount,
                totalEmergencies = totalEmergencies,
                // Botões de log visíveis apenas em builds de debug — ocultos em produção
                onExportLogs = if (BuildConfig.DEBUG) ({
                    val logFiles = FileLogger.getAllLogs()
                    if (logFiles.isEmpty()) return@HomeWelcomeCard
                    val uris = logFiles.mapNotNull { file ->
                        try {
                            FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                        } catch (_: Exception) { null }
                    }
                    if (uris.isEmpty()) return@HomeWelcomeCard
                    val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                        type = "text/plain"
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                        putExtra(Intent.EXTRA_SUBJECT, "Afilaxy Logs")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Exportar logs Afilaxy"))
                }) else null,
                onClearLogs = if (BuildConfig.DEBUG) ({ FileLogger.clearLogs() }) else null
            )
        }

        // Widget de risco de crise — visível apenas se localização disponível.
        // Passa weeklyCount e totalEmergencies do NavGraph (fonte autoritativa via SDK nativo)
        // para evitar problemas de tipo na re-leitura do Firestore via dev.gitlive.
        if (riskLat != 0.0 && riskLng != 0.0) {
            item {
                RiskWidget(
                    latitude = riskLat,
                    longitude = riskLng,
                    crises7d = weeklyCount,
                    crises30d = totalEmergencies
                )
            }
        }

        // Check-in card — exibido apenas se houver navegação e dentro da janela horária
        if (onNavigateToCheckIn != null) {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val morningDone = prefsRepo.getBoolean("checkin_morning_done_${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())}", false)
            val eveningDone = prefsRepo.getBoolean("checkin_evening_done_${java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())}", false)
            if (hour < 14 && !morningDone) {
                item { HomeCheckInCard(type = "MORNING", onNavigate = { onNavigateToCheckIn("MORNING") }) }
            } else if (hour >= 18 && !eveningDone) {
                item { HomeCheckInCard(type = "EVENING", onNavigate = { onNavigateToCheckIn("EVENING") }) }
            }
        }

        // Botão de Emergência Principal
        item {
            HomeEmergencyButton(
                onClick = onNavigateToEmergency,
                isActive = emergencyState.hasActiveEmergency
            )
        }

        // Modo Ajudante — card separado (espelha iOS ToggleCard)
        item {
            HomeHelperModeCard(
                isHelperMode = if (isHelperPending) helperIntended else emergencyState.isHelperMode,
                onHelperModeToggle = { enable ->
                    if (isHelperPending) return@HomeHelperModeCard
                    if (enable) {
                        val consentGiven = prefsRepo.getBoolean("helper_map_consent_v1", false)
                        if (consentGiven) showHelperPermission = true
                        else showHelperConsentDialog = true
                    } else {
                        isHelperPending = true
                        helperIntended = false
                        viewModel.deactivateHelper()
                    }
                }
            )
        }

        item {
            HomeQuickActions(onNavigateToAutocuidado = onNavigateToAutocuidado)
        }


        // Suporte Rápido — Farmácias 24h, Protocolo de Crise, SAMU 192
        item { HomeSupportLinksSection(onNavigateToHelp = onNavigateToHelp, onNavigateToPharmacyMap = onNavigateToPharmacyMap) }
    }

    // Dialog de consentimento LGPD + divulgação proeminente de localização (Google Play policy).
    // Exibido apenas na primeira ativação do Modo Ajudante.
    if (showHelperConsentDialog) {
        AlertDialog(
            onDismissRequest = {
                showHelperConsentDialog = false
                helperIntended = emergencyState.isHelperMode
            },
            icon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Uso da sua localização no Modo Ajudante") },
            text = {
                Text(
                    "O Afilaxy coletará sua localização GPS para:\n\n" +
                    "• Exibir sua posição aproximada (±100 m) no mapa para outros usuários\n" +
                    "• Receber alertas de emergências próximas em segundo plano\n\n" +
                    "Apenas nome de exibição e posição aproximada são compartilhados com quem precisar de ajuda. " +
                    "Nenhum endereço exato ou dado pessoal sensível é divulgado. " +
                    "Sua localização nunca é vendida.\n\n" +
                    "Você pode desativar o Modo Ajudante a qualquer momento.\n\n" +
                    "Consulte nossa Política de Privacidade em afilaxy.com/privacidade."
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

    // Diálogo pós-crise: exibido após 60 min de um pedido de emergência
    if (showPostCrisisDialog) {
        AlertDialog(
            onDismissRequest = {
                showPostCrisisDialog = false
                prefsRepo.putBoolean("post_crisis_dialog_shown", true)
            },
            icon = {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Que bom que você está bem") },
            text = {
                Text("A comunidade Afilaxy estava do seu lado. Continue se cuidando!")
            },
            confirmButton = {
                Button(onClick = {
                    showPostCrisisDialog = false
                    prefsRepo.putBoolean("post_crisis_dialog_shown", true)
                }) {
                    Text("Entendido")
                }
            }
        )
    }

    // NPS: exibido uma única vez, 7 dias após a primeira emergência
    if (showNps) {
        NpsDialog(
            onSubmit = { score ->
                showNps = false
                prefsRepo.putBoolean("nps_shown", true)
                scope.launch { emergencyRepo.submitNps(score) }
            },
            onDismiss = {
                showNps = false
                prefsRepo.putBoolean("nps_shown", true)
            }
        )
    }

    // Após a permissão foreground ser concedida, verifica se background também pode ser pedido.
    if (showHelperPermission) {
        RequestLocationPermission(
            skipProminentDisclosure = true,
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
    // Total acumulado de todas as semanas — nunca zera na virada de semana.
    totalEmergencies: Int = -1,
    onExportLogs: (() -> Unit)? = null,
    onClearLogs: (() -> Unit)? = null
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
            "Essa semana você não precisou de ajuda.",
            "Continue assim — cuidar-se no dia a dia faz a diferença."
        )
        1 -> Triple(
            listOf(Color(0xFFF4A825), Color(0xFFE65100)),
            "Você fez 1 pedido de ajuda esta semana.",
            "Alguém parou o que estava fazendo para te ajudar, de graça. " +
                "Que isso te inspire a cuidar de você e da sua comunidade."
        )
        2 -> Triple(
            listOf(Color(0xFFC62828), Color(0xFF7B1A1A)),
            "2º pedido de ajuda esta semana.",
            "Você não está sozinho. A comunidade Afilaxy está aqui — continue se cuidando."
        )
        3 -> Triple(
            listOf(Color(0xFF4A0000), Color(0xFF1A0000)),
            "Você já pediu ajuda $weeklyCount vezes esta semana.",
            "Você não está sozinho. A comunidade Afilaxy está aqui — continue se cuidando."
        )
        else -> Triple(
            listOf(Color(0xFF6A0DAD), Color(0xFF3D0066)),
            "Você já pediu ajuda $weeklyCount vezes esta semana.",
            "Você não está sozinho. A comunidade Afilaxy está aqui — continue se cuidando."
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
                // Pill de contagem — mostra total acumulado + contexto semanal
                if (weeklyCount >= 0) {
                    val total = if (totalEmergencies >= 0) totalEmergencies else weeklyCount
                    val pillText = when {
                        total == 0 -> "nenhum pedido ainda"
                        total == 1 && weeklyCount == 1 -> "1 pedido · 1 esta semana"
                        total == 1 -> "1 pedido no total"
                        weeklyCount > 0 -> "$total no total · $weeklyCount esta semana"
                        else -> "$total no total"
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

        }
        // Botões de log (dev) — discretos no canto superior direito
        if (onExportLogs != null || onClearLogs != null) {
            var showClearDialog by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                if (onClearLogs != null) {
                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Limpar Logs",
                            tint = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                if (onExportLogs != null) {
                    IconButton(
                        onClick = onExportLogs,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Exportar Logs",
                            tint = Color.White.copy(alpha = 0.55f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    title = { Text("Limpar logs?") },
                    text = { Text("Todos os arquivos de log serão apagados.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showClearDialog = false
                            onClearLogs?.invoke()
                        }) { Text("Limpar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearDialog = false }) { Text("Cancelar") }
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeCheckInCard(type: String, onNavigate: () -> Unit) {
    val isMorning = type == "MORNING"
    val emoji = if (isMorning) "💊" else "🌙"
    val title = if (isMorning) "Check-in Matinal" else "Check-in Noturno"
    val subtitle = if (isMorning) "Você está com sua bombinha hoje?" else "Você teve crise hoje?"
    Surface(
        onClick = onNavigate,
        shape = RoundedCornerShape(16.dp),
        color = if (isMorning)
            MaterialTheme.colorScheme.tertiaryContainer
        else
            MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
            // Vermelho quando sem emergência ativa (chama atenção para solicitar ajuda)
            // Azul primary quando emergência ativa (indica estado, não urgência para clicar)
            containerColor = if (isActive) MaterialTheme.colorScheme.primary
                             else MaterialTheme.colorScheme.error
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
    onNavigateToAutocuidado: () -> Unit
) {
    val context = LocalContext.current
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
                title = "Autocuidado",
                subtitle = "Saúde e bem-estar",
                icon = Icons.Default.Healing,
                onClick = onNavigateToAutocuidado,
                modifier = Modifier.weight(1f)
            )
            HomeActionCard(
                title = "Comunidade",
                subtitle = "Grupo no WhatsApp",
                icon = Icons.Default.Group,
                onClick = { openWhatsAppGroup(context) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HomeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(90.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Modo Ajudante — card separado
// ---------------------------------------------------------------------------

@Composable
private fun HomeHelperModeCard(
    isHelperMode: Boolean,
    onHelperModeToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = if (isHelperMode) MaterialTheme.colorScheme.error
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "Modo Ajudante",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isHelperMode) "Você está disponível para ajudar"
                               else "Ative para receber pedidos de ajuda",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = isHelperMode,
                onCheckedChange = onHelperModeToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}



// ---------------------------------------------------------------------------
// WhatsApp — grupo da comunidade
// ---------------------------------------------------------------------------

private const val WHATSAPP_GROUP_URL = "https://chat.whatsapp.com/BmSp54ER4hHBeow0KYCedL"

private fun openWhatsAppGroup(context: android.content.Context) {
    val uri = Uri.parse(WHATSAPP_GROUP_URL)
    val intent = Intent(Intent.ACTION_VIEW, uri)
    context.startActivity(intent)
}

// ---------------------------------------------------------------------------
// Suporte Rápido — Farmácias 24h, Protocolo de Crise, SAMU 192
// ---------------------------------------------------------------------------

@Composable
private fun HomeSupportLinksSection(onNavigateToHelp: () -> Unit, onNavigateToPharmacyMap: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Suporte Rápido",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            HomeSupportLinkRow(
                title = "SAMU 192",
                subtitle = "Emergência médica",
                icon = Icons.Default.Phone,
                color = Color(0xFFC62828)
            ) {
                val uri = Uri.parse("tel:192")
                context.startActivity(Intent(Intent.ACTION_DIAL, uri))
            }
            Spacer(modifier = Modifier.height(8.dp))
            HomeSupportLinkRow(
                title = "Protocolo de Crise",
                subtitle = "Passos para emergência",
                icon = Icons.AutoMirrored.Filled.List,
                color = Color(0xFFF4A825),
                onClick = onNavigateToHelp
            )
            Spacer(modifier = Modifier.height(8.dp))
            HomeSupportLinkRow(
                title = "Farmácias 24h",
                subtitle = "Encontre medicamentos",
                icon = Icons.Default.Add,
                color = Color(0xFF2E7D32)
            ) {
                onNavigateToPharmacyMap()
            }
        }
    }
}

@Composable
private fun HomeSupportLinkRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color,
            modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

// ── NPS Dialog ────────────────────────────────────────────────────────────────

@Composable
private fun NpsDialog(
    onSubmit: (score: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf(-1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Você recomendaria o Afilaxy?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "De 0 (pouco provável) a 10 (com certeza).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 0..5) NpsScoreButton(i, selected) { selected = i }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (i in 6..10) NpsScoreButton(i, selected) { selected = i }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (selected >= 0) onSubmit(selected) else onDismiss() },
                enabled = selected >= 0
            ) { Text("Enviar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Pular") }
        }
    )
}

@Composable
private fun NpsScoreButton(score: Int, selected: Int, onSelect: () -> Unit) {
    val isSelected = score == selected
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(8.dp),
        color = when {
            isSelected -> MaterialTheme.colorScheme.primary
            score <= 6 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            score <= 8 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        },
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = score.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
