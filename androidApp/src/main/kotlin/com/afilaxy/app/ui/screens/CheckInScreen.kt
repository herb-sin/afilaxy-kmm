package com.afilaxy.app.ui.screens

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import com.afilaxy.domain.model.CheckInType
import com.afilaxy.domain.model.HealthSnapshot
import com.afilaxy.presentation.checkin.CheckInViewModel
import org.koin.androidx.compose.koinViewModel

private val HEALTH_PERMISSIONS = setOf(
    HealthPermission.getReadPermission(HeartRateRecord::class),
    HealthPermission.getReadPermission(SleepSessionRecord::class),
    HealthPermission.getReadPermission(OxygenSaturationRecord::class),
    HealthPermission.getReadPermission(RespiratoryRateRecord::class)
)

@Composable
fun CheckInScreen(
    type: CheckInType,
    quickAnswer: Boolean? = null,
    riskScore: Int? = null,
    aqi: Int? = null,
    temperature: Float? = null,
    humidity: Float? = null,
    onDone: () -> Unit,
    viewModel: CheckInViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(type) {
        viewModel.initialize(type, riskScore, aqi, temperature, humidity)
    }

    // quickAnswer=true → tudo ok, auto-submit com defaults positivos
    LaunchedEffect(quickAnswer, state.isLoading) {
        if (quickAnswer == true && !state.isLoading && !state.alreadyDoneToday && !state.isSubmitted) {
            when (type) {
                CheckInType.MORNING -> viewModel.submitMorningCheckIn(
                    hasInhaler = true, nocturnalSymptoms = false, onControllerMedication = true
                )
                CheckInType.EVENING -> viewModel.submitEveningCheckIn(
                    hadCrisis = false, usedRescueInhaler = false, onControllerMedication = true
                )
            }
        }
    }

    when {
        state.isLoading -> CheckInLoading()
        state.alreadyDoneToday || state.isSubmitted -> CheckInDone(type, onDone)
        type == CheckInType.MORNING -> MorningCheckInContent(
            inhalerName = state.rescueInhalerName,
            riskScore = state.riskScore,
            healthSnapshot = state.healthSnapshot,
            healthAvailable = state.healthAvailable,
            healthPermissionsGranted = state.healthPermissionsGranted,
            onHealthPermissionGranted = { viewModel.reloadHealthSnapshot() },
            onSubmit = { hasInhaler, nocturnalSymptoms, onMedication ->
                viewModel.submitMorningCheckIn(hasInhaler, nocturnalSymptoms, onMedication)
            }
        )
        type == CheckInType.EVENING -> EveningCheckInContent(
            healthSnapshot = state.healthSnapshot,
            healthAvailable = state.healthAvailable,
            healthPermissionsGranted = state.healthPermissionsGranted,
            onHealthPermissionGranted = { viewModel.reloadHealthSnapshot() },
            onSubmit = { hadCrisis, severity, usedInhaler, onMedication ->
                viewModel.submitEveningCheckIn(
                    hadCrisis = hadCrisis,
                    severity = severity,
                    usedRescueInhaler = usedInhaler,
                    onControllerMedication = onMedication
                )
            }
        )
    }
}

// ── Check-in Matinal ──────────────────────────────────────────────────────────

@Composable
private fun MorningCheckInContent(
    inhalerName: String?,
    riskScore: Int?,
    healthSnapshot: HealthSnapshot?,
    healthAvailable: Boolean,
    healthPermissionsGranted: Boolean,
    onHealthPermissionGranted: () -> Unit,
    onSubmit: (hasInhaler: Boolean, nocturnalSymptoms: Boolean, onControllerMedication: Boolean) -> Unit
) {
    val inhalerDisplay = inhalerName ?: "broncodilatador de resgate"
    var nocturnalSymptoms by remember { mutableStateOf(false) }
    var hasInhaler by remember { mutableStateOf(true) }
    var onMedication by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFE65100), Color(0xFFFF8F00))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("💊", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Check-in Matinal",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp, fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Como foi sua noite e manhã?",
                color = Color.White, fontSize = 22.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )

            if (riskScore != null && riskScore >= 45) {
                Spacer(Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "⚠️ Risco de crise hoje: $riskScore/100",
                        color = Color.White,
                        modifier = Modifier.padding(12.dp, 8.dp),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            HealthCard(
                snapshot = healthSnapshot,
                available = healthAvailable,
                permissionsGranted = healthPermissionsGranted,
                onPermissionGranted = onHealthPermissionGranted,
                cardBackground = Color.White.copy(alpha = 0.18f)
            )

            Spacer(Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CheckInItem(
                        label = "Acordei de noite com crise",
                        checked = nocturnalSymptoms,
                        onToggle = { nocturnalSymptoms = it },
                        activeColor = Color(0xFFFFCC02)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    CheckInItem(
                        label = "Estou com meu $inhalerDisplay",
                        checked = hasInhaler,
                        onToggle = { hasInhaler = it },
                        activeColor = Color.White
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    CheckInItem(
                        label = "Estou fazendo o tratamento",
                        checked = onMedication,
                        onToggle = { onMedication = it },
                        activeColor = Color.White
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onSubmit(hasInhaler, nocturnalSymptoms, onMedication) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White, contentColor = Color(0xFFE65100)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Confirmar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Sua resposta ajuda a entender seu padrão de risco.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp, textAlign = TextAlign.Center
            )
        }
    }
}

// ── Check-in Noturno ──────────────────────────────────────────────────────────

@Composable
private fun EveningCheckInContent(
    healthSnapshot: HealthSnapshot?,
    healthAvailable: Boolean,
    healthPermissionsGranted: Boolean,
    onHealthPermissionGranted: () -> Unit,
    onSubmit: (hadCrisis: Boolean, severity: String?, usedInhaler: Boolean, onControllerMedication: Boolean) -> Unit
) {
    var hadCrisis by remember { mutableStateOf(false) }
    var selectedSeverity by remember { mutableStateOf<String?>(null) }
    var usedInhaler by remember { mutableStateOf(false) }
    var onMedication by remember { mutableStateOf(true) }

    LaunchedEffect(hadCrisis) { if (!hadCrisis) selectedSeverity = null }

    val canSubmit = !hadCrisis || selectedSeverity != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A237E), Color(0xFF283593))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🌙", fontSize = 64.sp)
            Spacer(Modifier.height(12.dp))
            Text("Check-in Noturno", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "Como foi seu dia?",
                color = Color.White, fontSize = 22.sp,
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            HealthCard(
                snapshot = healthSnapshot,
                available = healthAvailable,
                permissionsGranted = healthPermissionsGranted,
                onPermissionGranted = onHealthPermissionGranted,
                cardBackground = Color.White.copy(alpha = 0.12f)
            )

            Spacer(Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CheckInItem(
                        label = "Tive uma crise de asma hoje",
                        checked = hadCrisis, onToggle = { hadCrisis = it },
                        activeColor = Color(0xFFEF5350)
                    )
                    AnimatedVisibility(visible = hadCrisis) {
                        Column(modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)) {
                            Text(
                                "Qual foi a intensidade?",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp, fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("leve" to "🟡 Leve", "moderada" to "🟠 Moderada", "grave" to "🔴 Grave")
                                    .forEach { (value, label) ->
                                        val isSelected = selectedSeverity == value
                                        OutlinedButton(
                                            onClick = { selectedSeverity = value },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                containerColor = if (isSelected) Color.White.copy(0.2f) else Color.Transparent,
                                                contentColor = Color.White
                                            ),
                                            border = androidx.compose.foundation.BorderStroke(
                                                if (isSelected) 2.dp else 1.dp, Color.White
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                        ) {
                                            Text(
                                                label, fontSize = 11.sp, textAlign = TextAlign.Center,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                            }
                        }
                    }
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    CheckInItem(
                        label = "Usei minha bombinha Salbutamol hoje",
                        checked = usedInhaler, onToggle = { usedInhaler = it },
                        activeColor = Color(0xFFFFCC02)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    CheckInItem(
                        label = "Estou fazendo o tratamento",
                        checked = onMedication, onToggle = { onMedication = it },
                        activeColor = Color.White
                    )
                }
            }

            if (hadCrisis && selectedSeverity == null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Selecione a intensidade da crise para continuar",
                    color = Color(0xFFFFCC02), fontSize = 12.sp, textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onSubmit(hadCrisis, selectedSeverity, usedInhaler, onMedication) },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    disabledContainerColor = Color.White.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Confirmar",
                    color = if (canSubmit) Color(0xFF1A237E) else Color.White.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold, fontSize = 16.sp
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Seus dados ajudam a melhorar seu cuidado.",
                color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp, textAlign = TextAlign.Center
            )
        }
    }
}

// ── Card de saúde do smartwatch ───────────────────────────────────────────────

@Composable
private fun HealthCard(
    snapshot: HealthSnapshot?,
    available: Boolean,
    permissionsGranted: Boolean,
    onPermissionGranted: () -> Unit,
    cardBackground: Color
) {
    if (!available) return  // dispositivo sem suporte → não exibe nada

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBackground),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (permissionsGranted && snapshot != null) {
            // ── Dados disponíveis ─────────────────────────────────────────────
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Watch, contentDescription = null,
                        tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Smartwatch", color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    snapshot.avgHeartRateBpm?.let {
                        HealthDataChip("🫀", "$it bpm")
                    }
                    snapshot.sleepDurationHours?.let { h ->
                        val label = buildString {
                            append("${h.toInt()}h")
                            snapshot.sleepInterruptions?.takeIf { it > 0 }?.let { append(" · ${it}↑") }
                        }
                        HealthDataChip("💤", label)
                    }
                    snapshot.minSpo2Percent?.let {
                        val color = when {
                            it < 93f -> Color(0xFFEF5350)
                            it < 95f -> Color(0xFFFFCC02)
                            else -> Color.White
                        }
                        HealthDataChip("🫁", "${it.toInt()}%", textColor = color)
                    }
                }
            }
        } else {
            // ── Sem permissão → botão de conexão ─────────────────────────────
            Row(
                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Conectar smartwatch",
                        color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Dados de sono e FC enriquecem o risco",
                        color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp
                    )
                }
                // Launcher extraído em composable separado para respeitar regra de hooks
                HealthConnectPermissionButton(onGranted = onPermissionGranted)
            }
        }
    }
}

@Composable
private fun HealthDataChip(emoji: String, label: String, textColor: Color = Color.White) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 13.sp)
        Spacer(Modifier.width(3.dp))
        Text(label, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/**
 * Composable separado para isolar o rememberLauncherForActivityResult do Health Connect.
 * Chamado condicionalmente em HealthCard — dentro deste composable o hook é incondicional.
 */
@Composable
private fun HealthConnectPermissionButton(onGranted: () -> Unit) {
    val context = LocalContext.current

    val available = remember {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            try { HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE }
            catch (e: Exception) { false }
    }

    // PermissionController.createRequestPermissionResultContract() é estático — não precisa de client
    val launcher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { _ -> onGranted() }

    if (!available) return

    TextButton(onClick = { launcher.launch(HEALTH_PERMISSIONS) }) {
        Text("Ativar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

// ── Componente de item com checkbox ──────────────────────────────────────────

@Composable
private fun CheckInItem(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    activeColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Checkbox(
            checked = checked,
            onCheckedChange = onToggle,
            colors = CheckboxDefaults.colors(
                checkedColor = activeColor,
                uncheckedColor = Color.White.copy(alpha = 0.5f),
                checkmarkColor = if (activeColor == Color.White) Color(0xFFE65100) else Color.Black
            )
        )
    }
}

// ── Estados auxiliares ────────────────────────────────────────────────────────

@Composable
private fun CheckInDone(type: CheckInType, onDone: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1B5E20)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✅", fontSize = 72.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                if (type == CheckInType.MORNING) "Obrigado!\nFique com sua bombinha sempre que sair."
                else "Obrigado pelo registro!\nSeus dados ajudam a melhorar seu cuidado.",
                color = Color.White, fontSize = 18.sp,
                textAlign = TextAlign.Center, lineHeight = 26.sp
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onDone,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text("Voltar", color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CheckInLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
