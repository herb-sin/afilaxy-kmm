package com.afilaxy.app.ui.screens

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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afilaxy.domain.model.CheckInType
import com.afilaxy.domain.model.HealthSnapshot
import com.afilaxy.presentation.checkin.CheckInViewModel
import org.koin.androidx.compose.koinViewModel


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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(type) {
        viewModel.initialize(type, riskScore, aqi, temperature, humidity)
    }

    // quickAnswer=true → tudo ok, auto-submit com defaults positivos
    LaunchedEffect(quickAnswer, state.isLoading) {
        if (quickAnswer == true && !state.isLoading && !state.alreadyDoneToday && !state.isSubmitted) {
            when (type) {
                CheckInType.MORNING -> viewModel.submitMorningCheckIn(
                    wellbeingA = true, wellbeingB = true, wellbeingC = true
                )
                CheckInType.EVENING -> viewModel.submitEveningCheckIn(
                    wellbeingA = true, wellbeingB = true, wellbeingC = true
                )
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Box(Modifier.fillMaxSize()) {
        when {
            state.isLoading -> CheckInLoading()
            state.showCriticalWellbeingCard -> CriticalWellbeingCard(
                onDismiss = { viewModel.dismissCriticalCard(); onDone() },
                onOpenGuide = { viewModel.dismissCriticalCard(); onDone() }
            )
            state.alreadyDoneToday || state.isSubmitted -> CheckInDone(type, onDone)
            type == CheckInType.MORNING -> MorningCheckInContent(
                riskScore = state.riskScore,
                healthSnapshot = state.healthSnapshot,
                healthAvailable = state.healthAvailable,
                healthPermissionsGranted = state.healthPermissionsGranted,
                onHealthPermissionGranted = { viewModel.reloadHealthSnapshot() },
                onSubmit = { a, b, c -> viewModel.submitMorningCheckIn(a, b, c) }
            )
            type == CheckInType.EVENING -> EveningCheckInContent(
                healthSnapshot = state.healthSnapshot,
                healthAvailable = state.healthAvailable,
                healthPermissionsGranted = state.healthPermissionsGranted,
                onHealthPermissionGranted = { viewModel.reloadHealthSnapshot() },
                onSubmit = { a, b, c -> viewModel.submitEveningCheckIn(a, b, c) }
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// ── Check-in Matinal ──────────────────────────────────────────────────────────

@Composable
private fun MorningCheckInContent(
    riskScore: Int?,
    healthSnapshot: HealthSnapshot?,
    healthAvailable: Boolean,
    healthPermissionsGranted: Boolean,
    onHealthPermissionGranted: () -> Unit,
    onSubmit: (wellbeingA: Boolean, wellbeingB: Boolean, wellbeingC: Boolean) -> Unit
) {
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
            Text("🌅", fontSize = 64.sp)
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
                        "⚠️ Qualidade do ar baixa hoje: $riskScore/100",
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
                        label = "Dormi bem esta noite",
                        checked = nocturnalSymptoms,
                        onToggle = { nocturnalSymptoms = it },
                        activeColor = Color(0xFFFFCC02)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    CheckInItem(
                        label = "Me sinto bem esta manhã",
                        checked = hasInhaler,
                        onToggle = { hasInhaler = it },
                        activeColor = Color.White
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    CheckInItem(
                        label = "Estou com boa energia",
                        checked = onMedication,
                        onToggle = { onMedication = it },
                        activeColor = Color.White
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onSubmit(nocturnalSymptoms, hasInhaler, onMedication) },
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
    onSubmit: (wellbeingA: Boolean, wellbeingB: Boolean, wellbeingC: Boolean) -> Unit
) {
    var hadCrisis by remember { mutableStateOf(true) }
    var usedInhaler by remember { mutableStateOf(false) }
    var onMedication by remember { mutableStateOf(true) }

    val canSubmit = true

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
                        label = "Tive um bom dia",
                        checked = hadCrisis, onToggle = { hadCrisis = it },
                        activeColor = Color(0xFF81C784)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    CheckInItem(
                        label = "Pratiquei atividade física",
                        checked = usedInhaler, onToggle = { usedInhaler = it },
                        activeColor = Color(0xFFFFCC02)
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    CheckInItem(
                        label = "Me cuidei bem hoje",
                        checked = onMedication, onToggle = { onMedication = it },
                        activeColor = Color.White
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onSubmit(hadCrisis, usedInhaler, onMedication) },
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

// Health Connect em standby — stub vazio até CNPJ/conta organização aprovada.
@Composable
private fun HealthConnectPermissionButton(onGranted: () -> Unit) {
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

// ── Card de bem-estar crítico ─────────────────────────────────────────────────

@Composable
private fun CriticalWellbeingCard(onDismiss: () -> Unit, onOpenGuide: () -> Unit) {
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF212121)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C))
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("⚠️", fontSize = 48.sp)
                Text(
                    "Percebemos um dia difícil",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Você registrou bem-estar muito baixo. Sabe como agir se precisar de apoio?",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = {
                        uriHandler.openUri("https://afilaxy.com/guia")
                        onOpenGuide()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Ver Guia de Suporte", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        "Estou bem",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        }
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
                if (type == CheckInType.MORNING) "Obrigado!\nContinue cuidando de você durante o dia."
                else "Obrigado pelo registro!\nSeus dados ajudam a entender seu bem-estar.",
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
