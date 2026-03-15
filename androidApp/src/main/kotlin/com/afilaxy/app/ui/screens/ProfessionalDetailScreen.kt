package com.afilaxy.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.afilaxy.domain.model.HealthProfessional
import com.afilaxy.domain.model.Specialty
import com.afilaxy.domain.model.SubscriptionPlan
import com.afilaxy.presentation.professional.ProfessionalDetailViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalDetailScreen(
    professionalId: String,
    onNavigateBack: () -> Unit,
    viewModel: ProfessionalDetailViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(professionalId) {
        viewModel.loadProfessional(professionalId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalhes do Profissional") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.error ?: "Erro desconhecido")
                }
            }
            state.professional != null -> {
                ProfessionalDetailContent(
                    professional = state.professional!!,
                    modifier = Modifier.padding(padding),
                    onWhatsAppClick = {
                        val raw = state.professional!!.whatsapp ?: return@ProfessionalDetailContent
                        val digits = raw.replace(Regex("[^0-9]"), "").take(15)
                        if (digits.isBlank()) return@ProfessionalDetailContent
                        val uri = Uri.Builder()
                            .scheme("https")
                            .authority("wa.me")
                            .appendPath("55$digits")
                            .build()
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    },
                    onPhoneClick = {
                        val raw = state.professional!!.phone ?: return@ProfessionalDetailContent
                        val digits = raw.replace(Regex("[^0-9+]"), "").take(15)
                        if (digits.isBlank()) return@ProfessionalDetailContent
                        val uri = Uri.Builder()
                            .scheme("tel")
                            .opaquePart(digits)
                            .build()
                        context.startActivity(Intent(Intent.ACTION_DIAL, uri))
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfessionalDetailContent(
    professional: HealthProfessional,
    modifier: Modifier = Modifier,
    onWhatsAppClick: () -> Unit,
    onPhoneClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header com foto e nome
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = professional.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (professional.specialty) {
                        Specialty.PNEUMOLOGIST -> "Pneumologista"
                        Specialty.ALLERGIST -> "Alergista"
                        Specialty.PHYSIOTHERAPIST -> "Fisioterapeuta"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "CRM: ${professional.crm}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Badge do plano
        if (professional.subscriptionPlan != SubscriptionPlan.NONE) {
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        when (professional.subscriptionPlan) {
                            SubscriptionPlan.PREMIUM -> "⭐ PREMIUM"
                            SubscriptionPlan.PRO -> "✨ PRO"
                            SubscriptionPlan.BASIC -> "📋 BÁSICO"
                            else -> ""
                        }
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = when (professional.subscriptionPlan) {
                        SubscriptionPlan.PREMIUM -> Color(0xFFFFD700)
                        SubscriptionPlan.PRO -> Color(0xFFC0C0C0)
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    }
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Avaliação
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${professional.rating}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = " (${professional.totalReviews} avaliações)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bio
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Sobre",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = professional.bio,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Endereço
        professional.clinicAddress?.let { address ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = address,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Botões de ação
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (professional.whatsapp != null) {
                Button(
                    onClick = onWhatsAppClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366)
                    )
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("WhatsApp")
                }
            }
            
            if (professional.phone != null) {
                OutlinedButton(
                    onClick = onPhoneClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ligar")
                }
            }
        }
    }
}
