package com.afilaxy.app.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Um alvo do tour de boas-vindas: o texto exibido e a posição do item na LazyColumn
 * (usado para rolar até ele e para localizar seus bounds reais na tela).
 */
data class CoachMarkTarget(
    val lazyListIndex: Int,
    val title: String,
    val description: String,
)

/**
 * Estado do tour de boas-vindas da Home. A lista de [targets] é definida pela própria tela
 * (que já sabe quais cards existem nesta sessão, ex.: card de check-in pode não existir fora
 * da janela horária).
 */
class HomeCoachMarkState(val targets: List<CoachMarkTarget>) {
    var currentStep by mutableStateOf(0)
        private set

    var isActive by mutableStateOf(false)
        private set

    fun start() {
        if (targets.isEmpty()) return
        currentStep = 0
        isActive = true
    }

    fun dismiss() {
        isActive = false
    }

    fun advance() {
        if (currentStep >= targets.lastIndex) {
            dismiss()
        } else {
            currentStep += 1
        }
    }
}

@Composable
fun rememberHomeCoachMarkState(targets: List<CoachMarkTarget>) = remember(targets) { HomeCoachMarkState(targets) }

/**
 * Overlay de "spotlight": escurece a tela inteira e recorta um retângulo arredondado
 * transparente sobre o alvo atual, com um balão de texto e navegação (Pular / Próximo/Entendi).
 *
 * A posição do alvo é lida diretamente de [listState].layoutInfo a cada frame, em vez de
 * depender de um callback (onGloballyPositioned) disparado durante a animação de scroll.
 * Isso elimina uma classe de bug onde o recorte usava uma posição intermediária/desatualizada
 * do scroll e aparecia deslocado (acima ou abaixo) do card real: layoutInfo é sempre a fonte
 * da verdade corrente do LazyColumn, então o recorte acompanha o scroll em tempo real e já
 * nasce correto assim que a rolagem assenta — sem depender de qual frame um callback disparou por último.
 *
 * [listBoundsInRoot] é a posição do próprio LazyColumn na tela (capturada uma única vez, não
 * muda com o scroll). [contentPaddingHorizontal] deve bater com o contentPadding horizontal
 * passado ao LazyColumn, para o recorte não incluir essa margem.
 */
@Composable
fun HomeCoachMarkOverlay(
    state: HomeCoachMarkState,
    listState: LazyListState,
    listBoundsInRoot: Rect?,
    contentPaddingHorizontal: Dp = 16.dp
) {
    AnimatedVisibility(
        visible = state.isActive,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        val density = LocalDensity.current
        val screenHeightPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
        val paddingPx = with(density) { 10.dp.toPx() }
        val cornerRadiusPx = with(density) { 16.dp.toPx() }
        val horizontalPaddingPx = with(density) { contentPaddingHorizontal.toPx() }
        val step = state.targets.getOrNull(state.currentStep)

        // Posição real do item-alvo, lida ao vivo do layout do LazyColumn — nunca uma
        // captura antiga de um frame de animação. Null enquanto o item não está
        // (ainda) composto/visível, ex.: durante o trecho inicial do scroll até ele.
        val target: Rect? = if (listBoundsInRoot != null && step != null) {
            listState.layoutInfo.visibleItemsInfo
                .find { it.index == step.lazyListIndex }
                ?.let { info ->
                    Rect(
                        left = listBoundsInRoot.left + horizontalPaddingPx,
                        top = listBoundsInRoot.top + info.offset,
                        right = listBoundsInRoot.right - horizontalPaddingPx,
                        bottom = listBoundsInRoot.top + info.offset + info.size
                    )
                }
        } else null

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { state.advance() }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            ) {
                drawRect(color = Color.Black.copy(alpha = 0.72f))
                if (target != null) {
                    drawRoundRect(
                        color = Color.Transparent,
                        topLeft = Offset(target.left - paddingPx, target.top - paddingPx),
                        size = Size(target.width + paddingPx * 2, target.height + paddingPx * 2),
                        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                        blendMode = BlendMode.Clear
                    )
                }
            }

            if (target != null && step != null) {
                val bubbleBelowTarget = target.bottom < screenHeightPx * 0.62f
                val topPaddingDp = if (bubbleBelowTarget) {
                    with(density) { (target.bottom + paddingPx + 12.dp.toPx()).toDp() }
                } else 0.dp
                val bottomPaddingDp = if (!bubbleBelowTarget) {
                    with(density) { (screenHeightPx - target.top + paddingPx + 12.dp.toPx()).toDp() }
                } else 0.dp

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(if (bubbleBelowTarget) Alignment.TopStart else Alignment.BottomStart)
                        .padding(horizontal = 20.dp)
                        .padding(top = topPaddingDp, bottom = bottomPaddingDp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { /* consome o toque — não avança ao tocar no próprio balão */ }
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "${state.currentStep + 1}/${state.targets.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = step.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = step.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { state.dismiss() }) {
                                    Text("Pular")
                                }
                                Button(onClick = { state.advance() }) {
                                    Text(
                                        if (state.currentStep == state.targets.size - 1) "Entendi"
                                        else "Próximo"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
