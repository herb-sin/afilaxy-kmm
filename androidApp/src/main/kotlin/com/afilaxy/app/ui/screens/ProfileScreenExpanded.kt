package com.afilaxy.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.afilaxy.domain.model.*
import com.afilaxy.presentation.medical.MedicalProfileState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenExpanded(
    state: MedicalProfileState,
    onEditProfile: () -> Unit,
    onAddMedication: () -> Unit,
    onAddContact: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Patient Profile Header
        item {
            PatientProfileCard(
                profile = state.medicalProfile,
                onEditProfile = onEditProfile
            )
        }

        // Asma Type Info
        state.medicalProfile?.let { profile ->
            item {
                AsmaTypeCard(profile = profile)
            }
        }

        // Last Exam
        state.medicalProfile?.lastExam?.let { exam ->
            item {
                LastExamCard(exam = exam)
            }
        }

        // Emergency Protocol
        item {
            EmergencyProtocolCard()
        }

        // Emergency Contacts
        item {
            EmergencyContactsCard(
                contacts = state.emergencyContacts,
                onAddContact = onAddContact
            )
        }

        // Medications
        item {
            MedicationsCard(
                medications = state.medications,
                onAddMedication = onAddMedication
            )
        }
    }
}

@Composable
private fun PatientProfileCard(
    profile: MedicalProfile?,
    onEditProfile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Badge
            Surface(
                color = Color(0xFFE3F2FD),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "PACIENTE VERIFICADO",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1976D2)
                )
            }
            
            // Name
            Text(
                text = "Alex Johnson",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 32.sp
            )
            
            // Description
            Text(
                text = "Gerenciando asma intermitente com foco em prevenção e qualidade de vida ativa.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 20.sp
            )
            
            // Edit Button
            Button(
                onClick = onEditProfile,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Editar Perfil",
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AsmaTypeCard(profile: MedicalProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "TIPO DE ASMA",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1976D2)
                )
            }
            
            Text(
                text = when (profile.asmaType) {
                    AsmaType.INTERMITENTE -> "Intermitente"
                    AsmaType.PERSISTENTE_LEVE -> "Persistente Leve"
                    AsmaType.PERSISTENTE_MODERADA -> "Persistente Moderada"
                    AsmaType.PERSISTENTE_GRAVE -> "Persistente Grave"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Sintomas leves ocorrendo menos de duas vezes por semana.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            // Status Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Status: Estável",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4CAF50)
                )
                
                LinearProgressIndicator(
                    progress = 0.8f,
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF4CAF50),
                    trackColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun LastExamCard(exam: MedicalExam) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "ÚLTIMO EXAME",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1976D2)
                )
            }
            
            Text(
                text = when (exam.type) {
                    ExamType.ESPIROMETRIA -> "Espirometria"
                    ExamType.RAIO_X -> "Raio-X"
                    ExamType.TESTE_ALERGIA -> "Teste de Alergia"
                    ExamType.CONSULTA_ROTINA -> "Consulta de Rotina"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Realizado em ${exam.date}. ${exam.results}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            TextButton(
                onClick = { /* Navigate to history */ }
            ) {
                Text(
                    text = "Ver histórico →",
                    color = Color(0xFF1976D2)
                )
            }
        }
    }
}

@Composable
private fun EmergencyProtocolCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFCE4EC)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFE91E63),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "PROTOCOLO DE EMERGÊNCIA",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE91E63)
                )
            }
            
            val steps = listOf(
                "Sentar em posição vertical e tentar manter a calma.",
                "Usar inalador de resgate (Salbutamol): 2 jatos.",
                "Se não houver melhora em 10 min, ligar para emergência."
            )
            
            steps.forEachIndexed { index, step ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = Color(0xFFE91E63),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Column {
                        Text(
                            text = "PASSO ${index + 1}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE91E63)
                        )
                        Text(
                            text = step,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyContactsCard(
    contacts: List<MedicalEmergencyContact>,
    onAddContact: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Contacts,
                        contentDescription = null,
                        tint = Color(0xFF1976D2),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "CONTATOS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1976D2)
                    )
                }
                
                IconButton(onClick = onAddContact) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Contact",
                        tint = Color(0xFF1976D2)
                    )
                }
            }
            
            contacts.forEach { contact ->
                ContactItem(contact = contact)
            }
        }
    }
}

@Composable
private fun ContactItem(contact: MedicalEmergencyContact) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (contact.isProfessional) Icons.Default.LocalHospital else Icons.Default.Person,
            contentDescription = null,
            tint = Color(0xFF1976D2),
            modifier = Modifier.size(24.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${contact.relationship} • ${contact.phone}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun MedicationsCard(
    medications: List<Medication>,
    onAddMedication: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Medicação Atual",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Cronograma diário e medicamentos de alívio rápido.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                TextButton(onClick = { /* Navigate to history */ }) {
                    Text(
                        text = "Histórico de Uso",
                        color = Color(0xFF1976D2)
                    )
                }
            }
            
            medications.forEach { medication ->
                MedicationItem(medication = medication)
            }
            
            OutlinedButton(
                onClick = onAddMedication,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Adicionar Medicação")
            }
        }
    }
}

@Composable
private fun MedicationItem(medication: Medication) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val (iconColor, badgeColor, badgeText) = when (medication.type) {
            MedicationType.CONTROLE -> Triple(Color(0xFF2196F3), Color(0xFF2196F3), "CONTROLE")
            MedicationType.MANUTENCAO -> Triple(Color(0xFF00BCD4), Color(0xFF00BCD4), "MANUTENÇÃO")
            MedicationType.RESGATE -> Triple(Color(0xFFFF5722), Color(0xFFFF5722), "RESGATE")
        }
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (medication.type) {
                    MedicationType.CONTROLE -> Icons.Default.Link
                    MedicationType.MANUTENCAO -> Icons.Default.Refresh
                    MedicationType.RESGATE -> Icons.Default.Bolt
                },
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = medication.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${medication.dosage} • ${medication.frequency}${medication.timing?.let { " ($it)" } ?: ""}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        Surface(
            color = badgeColor,
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = badgeText,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}