package com.afilaxy.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.afilaxy.app.R
import com.afilaxy.app.analytics.AnalyticsManager
import org.koin.compose.koinInject

private const val PROFESSIONALS_URL = "https://afilaxy.com/profissionais"
private const val CELLULA_MATER_URL = "https://cellulamater.com.br/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalScreen(
    onNavigateToCrmLookup: () -> Unit
) {
    val context = LocalContext.current
    val analytics: AnalyticsManager = koinInject()

    // Métrica exigida pela cláusula 2.3 do contrato de parceria com a Cellula Mater
    // (relatório mensal de acessos/navegação na área de parceria). Só é registrada se o
    // usuário deu consentimento de analytics (ver AnalyticsManager) — respeitando a LGPD
    // citada na cláusula 5.3 do próprio contrato.
    LaunchedEffect(Unit) {
        analytics.logScreenView("Apoio")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parceira Institucional", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_cellula_mater),
                contentDescription = "Cellula Mater — parceira institucional",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(96.dp)
                    .clickable {
                        analytics.logEvent("partner_link_clicked", mapOf("partner" to "cellula_mater"))
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(CELLULA_MATER_URL)))
                    }
            )

            // Espaço reservado para futuras parceiras (logos adicionais entrarão aqui) —
            // separa visualmente a área institucional das ações abaixo.
            Spacer(modifier = Modifier.height(96.dp))

            PortalActionCard(
                icon = Icons.Default.PersonAdd,
                title = "Quero me tornar Parceiro",
                description = "Parcerias CRM, CREFITO, CRP e Clínicas",
                buttonLabel = "Conhecer o Portal",
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PROFESSIONALS_URL)))
                }
            )

            PortalActionCard(
                icon = Icons.Default.Search,
                title = "Consultar CRM",
                description = "Confirme o registro de um médico no Conselho Federal de Medicina.",
                buttonLabel = "Consultar",
                onClick = onNavigateToCrmLookup
            )
        }
    }
}

@Composable
private fun PortalActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(buttonLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
