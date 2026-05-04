package com.afilaxy.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afilaxy.domain.model.AsthmaRiskLevel
import com.afilaxy.domain.model.RiskScore
import com.afilaxy.presentation.risk.RiskViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Widget de risco de crise asmática exibido na HomeScreen.
 * Mostra score 0-100, nível de risco, fatores e recomendações.
 */
@Composable
fun RiskWidget(
    latitude: Double,
    longitude: Double,
    crises7d: Int = -1,
    crises30d: Int = -1,
    modifier: Modifier = Modifier,
    viewModel: RiskViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(latitude, longitude, crises7d, crises30d) {
        if (latitude != 0.0 && longitude != 0.0) {
            viewModel.loadRiskScore(latitude, longitude, crises7d, crises30d)
        }
    }

    when {
        state.isLoading -> RiskWidgetLoading(modifier)
        state.riskScore != null -> RiskWidgetContent(state.riskScore!!, modifier)
        // Erro ou sem localização: não exibe o widget
    }
}

@Composable
private fun RiskWidgetLoading(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "shimmer_alpha"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
    )
}

@Composable
private fun RiskWidgetContent(score: RiskScore, modifier: Modifier = Modifier) {
    val level = score.riskLevel
    val (gradientStart, gradientEnd) = when (level) {
        AsthmaRiskLevel.LOW       -> Color(0xFF1B5E20) to Color(0xFF2E7D32)
        AsthmaRiskLevel.MODERATE  -> Color(0xFFF57F17) to Color(0xFFF9A825)
        AsthmaRiskLevel.HIGH      -> Color(0xFFE65100) to Color(0xFFFF6D00)
        AsthmaRiskLevel.VERY_HIGH -> Color(0xFFB71C1C) to Color(0xFFD32F2F)
    }

    var showDetails by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        onClick = { showDetails = !showDetails },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(gradientStart, gradientEnd)))
                .padding(16.dp)
        ) {
            Column {
                // Cabeçalho: emoji + título + score
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${level.emoji} Risco ${level.label}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Toque para ${if (showDetails) "ocultar" else "ver"} detalhes",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                    // Score circular
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "${score.score}",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    }
                }

                // Barra de progresso
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { score.score / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )

                // Detalhes expansíveis
                if (showDetails && (score.factors.isNotEmpty() || score.recommendations.isNotEmpty())) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))

                    if (score.factors.isNotEmpty()) {
                        Text(
                            "⚠️ Fatores de risco",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        score.factors.forEach { factor ->
                            Text("• $factor", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (score.recommendations.isNotEmpty()) {
                        Text(
                            "💡 Recomendações",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        score.recommendations.forEach { rec ->
                            Text("• $rec", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
