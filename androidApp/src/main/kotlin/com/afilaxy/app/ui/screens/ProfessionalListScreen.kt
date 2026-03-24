package com.afilaxy.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.afilaxy.domain.model.HealthProfessional
import com.afilaxy.domain.model.PlanTier
import com.afilaxy.domain.model.Specialty
import com.afilaxy.presentation.professional.ProfessionalListViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToCrmLookup: () -> Unit = {},
    viewModel: ProfessionalListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var selectedSpecialty by remember { mutableStateOf<Specialty?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profissionais Recomendados") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCrmLookup) {
                        Icon(Icons.Default.Badge, "Consultar CRM")
                    }
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Default.FilterList, "Filtrar")
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Todos") },
                            onClick = {
                                selectedSpecialty = null
                                viewModel.filterBySpecialty(null)
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Pneumologistas") },
                            onClick = {
                                selectedSpecialty = Specialty.PNEUMOLOGIST
                                viewModel.filterBySpecialty(Specialty.PNEUMOLOGIST)
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Alergistas") },
                            onClick = {
                                selectedSpecialty = Specialty.ALLERGIST
                                viewModel.filterBySpecialty(Specialty.ALLERGIST)
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Fisioterapeutas") },
                            onClick = {
                                selectedSpecialty = Specialty.PHYSIOTHERAPIST
                                viewModel.filterBySpecialty(Specialty.PHYSIOTHERAPIST)
                                showFilterMenu = false
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null,
                            modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Text(state.error ?: "Erro desconhecido",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadProfessionals() }) { Text("Tentar Novamente") }
                    }
                }
                state.professionals.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PersonSearch, contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Text("Nenhum profissional encontrado", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.professionals) { professional ->
                            ProfessionalCard(
                                professional = professional,
                                onClick = { onNavigateToDetail(professional.id) },
                                onContactClick = {
                                    professional.whatsapp?.let { whatsapp ->
                                        val digits = whatsapp.replace(Regex("[^0-9]"), "").take(15)
                                        if (digits.isBlank()) return@let
                                        val uri = Uri.Builder()
                                            .scheme("https").authority("wa.me")
                                            .appendPath("55$digits").build()
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfessionalCard(
    professional: HealthProfessional,
    onClick: () -> Unit = {},
    onContactClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = professional.profilePhoto ?: "https://via.placeholder.com/64",
                        contentDescription = "Foto de ${professional.name}",
                        modifier = Modifier.size(64.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(professional.name, style = MaterialTheme.typography.titleMedium)
                            if (professional.subscriptionPlan.tier() != PlanTier.NONE) {
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.Verified, contentDescription = "Perfil Verificado",
                                    tint = Color(0xFF1DA1F2), modifier = Modifier.size(20.dp))
                            }
                        }
                        Text(getSpecialtyName(professional.specialty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("CRM: ${professional.crm}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                // Badge de plano removido da UI do paciente — plano é dado interno (conformidade CFM)
            }

            if (professional.bio.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(professional.bio, style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3, overflow = TextOverflow.Ellipsis)
            }

            if (professional.rating > 0) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null,
                        tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${professional.rating} (${professional.totalReviews} avaliações)",
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onContactClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = professional.whatsapp != null
            ) {
                Icon(Icons.Default.Phone, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Entrar em Contato")
            }
        }
    }
}

private fun getSpecialtyName(specialty: Specialty): String = when (specialty) {
    Specialty.PNEUMOLOGIST -> "Pneumologista"
    Specialty.ALLERGIST -> "Alergista"
    Specialty.PHYSIOTHERAPIST -> "Fisioterapeuta"
}

