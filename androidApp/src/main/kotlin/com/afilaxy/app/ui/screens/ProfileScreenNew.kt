package com.afilaxy.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.afilaxy.domain.model.EmergencyContact
import com.afilaxy.domain.model.UserHealthData
import com.afilaxy.presentation.profile.ProfileViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenNew(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val profile = state.profile
    var showEditDialog by remember { mutableStateOf(false) }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            HeroHeader(
                name = profile?.name ?: "Usuário",
                isHealthProfessional = profile?.isHealthProfessional ?: false
            )
        }
        
        // Bento Grid - Informações de Saúde
        item {
            HealthInfoBentoGrid(
                healthData = profile?.healthData,
                emergencyContact = profile?.emergencyContact
            )
        }
        
        // Seção Medicação
        item {
            MedicationSection(
                medications = profile?.healthData?.medications ?: emptyList()
            )
        }
        
        // Botão Editar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Editar Perfil")
                }
                
                OutlinedButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configurações")
                }
            }
        }
        
        // Estado de loading/erro
        if (state.isLoading) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
        
        state.error?.let { error ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
    
    // Dialog de edição (placeholder - pode ser BottomSheet ou tela separada)
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar Perfil") },
            text = { Text("Funcionalidade de edição será implementada em breve.") },
            confirmButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun HeroHeader(
    name: String,
    isHealthProfessional: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (isHealthProfessional) "Profissional Verificado" else "Paciente Verificado",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthInfoBentoGrid(
    healthData: UserHealthData?,
    emergencyContact: EmergencyContact?
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.height(280.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            InfoCard(
                title = "Tipo de Asma",
                value = healthData?.conditions?.firstOrNull() ?: "Não informado",
                icon = Icons.Default.Favorite
            )
        }
        
        item {
            InfoCard(
                title = "Último Exame",
                value = healthData?.notes?.take(20) ?: "Não informado",
                icon = Icons.Default.Assignment
            )
        }
        
        item {
            InfoCard(
                title = "Tipo Sanguíneo",
                value = healthData?.bloodType ?: "Não informado",
                icon = Icons.Default.Bloodtype
            )
        }
        
        item {
            InfoCard(
                title = "Contato Emergência",
                value = emergencyContact?.name ?: "Não informado",
                icon = Icons.Default.ContactPhone
            )
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    value: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun MedicationSection(medications: List<String>) {
    Column {
        Text(
            text = "Medicação Atual",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MedicationCard(
                title = "Controle",
                description = "Corticoide inalatório",
                modifier = Modifier.weight(1f)
            )
            MedicationCard(
                title = "Manutenção", 
                description = "Uso contínuo",
                modifier = Modifier.weight(1f)
            )
            MedicationCard(
                title = "Resgate",
                description = "Broncodilatador",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MedicationCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}