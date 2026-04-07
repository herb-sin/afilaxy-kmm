package com.afilaxy.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
fun ProfessionalsScreenNew(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {},
    onNavigateToCrmLookup: () -> Unit = {},
    viewModel: ProfessionalListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var selectedSpecialty by remember { mutableStateOf<Specialty?>(null) }

    // O ViewModel.init() dispara loadProfessionals() durante o warmup do Koin,
    // antes do token Firebase estar pronto — resulta em lista vazia sem erro visível.
    // Este LaunchedEffect força um reload quando a tela aparece (auth sempre pronta aqui).
    LaunchedEffect(Unit) { viewModel.loadProfessionals() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Profissionais",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCrmLookup) {
                        Icon(Icons.Default.Badge, "Consultar CRM")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    LoadingState()
                }
                
                state.error != null -> {
                    ErrorState(
                        error = state.error ?: "",
                        onRetry = { viewModel.loadProfessionals() }
                    )
                }
                
                state.professionals.isEmpty() -> {
                    EmptyState()
                }
                
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            SpecialtyFilters(
                                selectedSpecialty = selectedSpecialty,
                                onSpecialtySelected = { specialty ->
                                    selectedSpecialty = specialty
                                    viewModel.filterBySpecialty(specialty)
                                }
                            )
                        }
                        
                        item {
                            StatsHeader(professionals = state.professionals)
                        }

                        item {
                            ProfessionalCTABanner(context = context)
                        }
                        
                        items(state.professionals) { professional ->
                            ProfessionalCardNew(
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
private fun ProfessionalCTABanner(context: android.content.Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    "Sou Profissional de Saúde",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "Cadastre-se e apareça como referência no Afilaxy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Button(
                onClick = {
                    val uri = Uri.parse("https://afilaxy.com/profissionais")
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    "Quero me cadastrar",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun SpecialtyFilters(
    selectedSpecialty: Specialty?,
    onSpecialtySelected: (Specialty?) -> Unit
) {
    Column {
        Text(
            "Especialidades",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    onClick = { onSpecialtySelected(null) },
                    label = { Text("Todos") },
                    selected = selectedSpecialty == null
                )
            }
            
            item {
                FilterChip(
                    onClick = { onSpecialtySelected(Specialty.PNEUMOLOGIST) },
                    label = { Text("Pneumologistas") },
                    selected = selectedSpecialty == Specialty.PNEUMOLOGIST,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Air,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            
            item {
                FilterChip(
                    onClick = { onSpecialtySelected(Specialty.ALLERGIST) },
                    label = { Text("Alergistas") },
                    selected = selectedSpecialty == Specialty.ALLERGIST,
                    leadingIcon = {
                        Icon(
                            Icons.Default.Healing,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            
            item {
                FilterChip(
                    onClick = { onSpecialtySelected(Specialty.PHYSIOTHERAPIST) },
                    label = { Text("Fisioterapeutas") },
                    selected = selectedSpecialty == Specialty.PHYSIOTHERAPIST,
                    leadingIcon = {
                        Icon(
                            Icons.Default.FitnessCenter,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun StatsHeader(professionals: List<HealthProfessional>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = professionals.size.toString(),
                label = "Profissionais",
                icon = Icons.Default.Person
            )
            
            StatItem(
                value = professionals.count { it.subscriptionPlan.tier() != PlanTier.NONE }.toString(),
                label = "Verificados",
                icon = Icons.Default.Verified
            )
            
            StatItem(
                value = String.format("%.1f", professionals.filter { it.rating > 0 }.map { it.rating }.average().takeIf { !it.isNaN() } ?: 0.0),
                label = "Avaliação",
                icon = Icons.Default.Star
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProfessionalCardNew(
    professional: HealthProfessional,
    onClick: () -> Unit = {},
    onContactClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = professional.profilePhoto ?: "https://via.placeholder.com/80",
                    contentDescription = "Foto de ${professional.name}",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            professional.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        if (professional.subscriptionPlan.tier() != PlanTier.NONE) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Perfil Verificado",
                                tint = Color(0xFF1DA1F2),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        getSpecialtyName(professional.specialty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Text(
                        "CRM: ${professional.crm}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (professional.bio.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    professional.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (professional.rating > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) { index ->
                        Icon(
                            if (index < professional.rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${professional.rating} (${professional.totalReviews} avaliações)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver Perfil")
                }
                
                Button(
                    onClick = onContactClick,
                    modifier = Modifier.weight(1f),
                    enabled = professional.whatsapp != null
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Contato")
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Carregando profissionais...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Ops! Algo deu errado",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Tentar Novamente")
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.PersonSearch,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Nenhum profissional encontrado",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Tente alterar os filtros ou verifique sua conexão",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun getSpecialtyName(specialty: Specialty): String = when (specialty) {
    Specialty.PNEUMOLOGIST -> "Pneumologista"
    Specialty.ALLERGIST -> "Alergista"
    Specialty.PHYSIOTHERAPIST -> "Fisioterapeuta"
}