package com.afilaxy.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.afilaxy.domain.repository.PreferencesRepository
import org.koin.compose.koinInject

@Composable
fun ConsentScreen(onConsentGiven: () -> Unit) {
    val prefs: PreferencesRepository = koinInject()
    var analyticsEnabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(56.dp))

        // ── Ícone de cabeçalho ───────────────────────────────────────────────
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(104.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "Antes de começar",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Sua privacidade é importante para nós.\nVeja o que coletamos e por quê.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(36.dp))

        // ── Bloco Essencial ──────────────────────────────────────────────────
        ConsentBlock(
            icon = Icons.Default.Lock,
            title = "Dados Essenciais",
            description = "Localização durante emergências, " +
                "matching com helpers e mensagens de chat. " +
                "Necessários para o funcionamento do app.",
            toggleEnabled = false,
            checked = true,
            onCheckedChange = {},
            badge = "Sempre ativo"
        )

        Spacer(Modifier.height(12.dp))

        // ── Bloco Analytics (opcional) ────────────────────────────────────────
        ConsentBlock(
            icon = Icons.Default.Star,
            title = "Analytics & Melhoria",
            description = "Gravidade de crises, avaliações pós-atendimento e " +
                "NPS. Nos ajuda a melhorar o serviço e realizar pesquisas " +
                "de saúde pública. Pode ser desativado nas Configurações.",
            toggleEnabled = true,
            checked = analyticsEnabled,
            onCheckedChange = { analyticsEnabled = it },
            badge = null
        )

        Spacer(Modifier.height(36.dp))

        // ── Botão principal ──────────────────────────────────────────────────
        Button(
            onClick = {
                prefs.putBoolean("consent_shown", true)
                prefs.putBoolean("analytics_consent", analyticsEnabled)
                onConsentGiven()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "Continuar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Você pode alterar suas preferências de privacidade " +
                "a qualquer momento em Configurações > Privacidade.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ConsentBlock(
    icon: ImageVector,
    title: String,
    description: String,
    toggleEnabled: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    badge: String?
) {
    val bgColor by animateColorAsState(
        targetValue = if (checked)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        label = "blockBg"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(22.dp)
                    .padding(top = 2.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (toggleEnabled) {
                        Switch(
                            checked = checked,
                            onCheckedChange = onCheckedChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    } else if (badge != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
