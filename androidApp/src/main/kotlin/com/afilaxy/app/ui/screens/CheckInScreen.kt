package com.afilaxy.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afilaxy.domain.model.CheckInType
import com.afilaxy.presentation.checkin.CheckInViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun CheckInScreen(
    type: CheckInType,
    quickAnswer: Boolean? = null,   // true=sim, false=não (via action da notificação)
    riskScore: Int? = null,
    aqi: Int? = null,
    onDone: () -> Unit,
    viewModel: CheckInViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(type) {
        viewModel.initialize(type, riskScore, aqi)
    }

    // Resposta rápida via action da notificação
    LaunchedEffect(quickAnswer, state.isLoading) {
        if (quickAnswer != null && !state.isLoading && !state.alreadyDoneToday && !state.isSubmitted) {
            when (type) {
                CheckInType.MORNING -> viewModel.submitMorningCheckIn(quickAnswer)
                CheckInType.EVENING -> viewModel.submitEveningCheckIn(hadCrisis = quickAnswer)
            }
        }
    }

    when {
        state.isLoading -> CheckInLoading()
        state.alreadyDoneToday || state.isSubmitted -> CheckInDone(type, onDone)
        type == CheckInType.MORNING -> MorningCheckInContent(
            inhalerName = state.rescueInhalerName,
            riskScore = state.riskScore,
            onYes = { viewModel.submitMorningCheckIn(true) },
            onNo = { viewModel.submitMorningCheckIn(false) }
        )
        type == CheckInType.EVENING -> EveningCheckInContent(
            onNoCrisis = { viewModel.submitEveningCheckIn(hadCrisis = false) },
            onHadCrisis = { severity, usedInhaler ->
                viewModel.submitEveningCheckIn(
                    hadCrisis = true,
                    severity = severity,
                    usedRescueInhaler = usedInhaler
                )
            }
        )
    }
}

@Composable
private fun MorningCheckInContent(
    inhalerName: String?,
    riskScore: Int?,
    onYes: () -> Unit,
    onNo: () -> Unit
) {
    val inhalerDisplay = inhalerName ?: "broncodilatador de resgate"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFE65100), Color(0xFFFF8F00))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("💊", fontSize = 72.sp)
            Spacer(Modifier.height(20.dp))

            Text(
                "Check-in Matinal",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))

            Text(
                "Você está com seu\n$inhalerDisplay?",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )

            if (riskScore != null && riskScore >= 45) {
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.2f)
                    ),
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

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = onYes,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFFE65100)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("✅  Sim, estou com ela", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNo,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("❌  Não tenho comigo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "Sua resposta ajuda a entender seu padrão de risco.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EveningCheckInContent(
    onNoCrisis: () -> Unit,
    onHadCrisis: (severity: String?, usedInhaler: Boolean?) -> Unit
) {
    var showCrisisDetail by remember { mutableStateOf(false) }
    var selectedSeverity by remember { mutableStateOf<String?>(null) }
    var usedInhaler by remember { mutableStateOf<Boolean?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A237E), Color(0xFF283593))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🌙", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text("Check-in Noturno", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                "Você teve alguma\ncrise de asma hoje?",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )

            Spacer(Modifier.height(40.dp))

            AnimatedContent(targetState = showCrisisDetail, label = "crisis_detail") { detail ->
                if (!detail) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = { showCrisisDetail = true },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF5350)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("⚠️  Sim, tive uma crise", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onNoCrisis,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF43A047)
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("✅  Não, dia tranquilo", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                } else {
                    // Detalhes da crise
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Qual foi a intensidade?",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        listOf("leve" to "🟡 Leve", "moderada" to "🟠 Moderada", "grave" to "🔴 Grave")
                            .forEach { (value, label) ->
                                val isSelected = selectedSeverity == value
                                OutlinedButton(
                                    onClick = { selectedSeverity = value },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) Color.White.copy(0.2f) else Color.Transparent,
                                        contentColor = Color.White
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isSelected) 2.dp else 1.dp, Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                            }
                        Spacer(Modifier.height(12.dp))
                        Text("Usou o broncodilatador de resgate?", color = Color.White.copy(0.8f), fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            listOf(true to "Sim", false to "Não").forEach { (value, label) ->
                                val isSelected = usedInhaler == value
                                OutlinedButton(
                                    onClick = { usedInhaler = value },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) Color.White.copy(0.2f) else Color.Transparent,
                                        contentColor = Color.White
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isSelected) 2.dp else 1.dp, Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text(label) }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Button(
                            onClick = { onHadCrisis(selectedSeverity, usedInhaler) },
                            enabled = selectedSeverity != null,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Salvar", color = Color(0xFF1A237E), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckInDone(type: CheckInType, onDone: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1B5E20)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✅", fontSize = 72.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                if (type == CheckInType.MORNING) "Obrigado!\nFique com sua bombinha sempre que sair."
                else "Obrigado pelo registro!\nSeus dados ajudam a melhorar seu cuidado.",
                color = Color.White,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
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
