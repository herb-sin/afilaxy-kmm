package com.afilaxy.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
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
import com.afilaxy.domain.model.UserProfile
import com.afilaxy.presentation.profile.ProfileViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenNew(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val profile = state.profile
    var showEditSheet by remember { mutableStateOf(false) }

    // Campos de edição — inicializados com os dados do perfil
    var editName        by remember { mutableStateOf("") }
    var editPhone       by remember { mutableStateOf("") }
    var editBloodType   by remember { mutableStateOf("") }
    var editAllergies   by remember { mutableStateOf("") }
    var editMedications by remember { mutableStateOf("") }
    var editConditions  by remember { mutableStateOf("") }
    var editNotes       by remember { mutableStateOf("") }
    var editEmergName   by remember { mutableStateOf("") }
    var editEmergPhone  by remember { mutableStateOf("") }
    var editEmergRel    by remember { mutableStateOf("") }

    // Preenche campos quando o perfil chega pela primeira vez
    LaunchedEffect(profile) {
        if (profile != null) {
            editName        = profile.name
            editPhone       = profile.phone
            editBloodType   = profile.healthData?.bloodType ?: ""
            editAllergies   = profile.healthData?.allergies?.joinToString(", ") ?: ""
            editMedications = profile.healthData?.medications?.joinToString(", ") ?: ""
            editConditions  = profile.healthData?.conditions?.joinToString(", ") ?: ""
            editNotes       = profile.healthData?.notes ?: ""
            editEmergName   = profile.emergencyContact?.name ?: ""
            editEmergPhone  = profile.emergencyContact?.phone ?: ""
            editEmergRel    = profile.emergencyContact?.relationship ?: ""
        }
    }

    // Limpa mensagens após exibir
    LaunchedEffect(state.successMessage, state.error) {
        if (state.successMessage != null || state.error != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Hero Header ─────────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .padding(top = 32.dp, bottom = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Nome + Badge + Email
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = profile?.name ?: "Carregando...",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = if (profile?.isHealthProfessional == true) "Profissional de Saúde" else "Paciente Verificado",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                        if (!profile?.email.isNullOrBlank()) {
                            Text(
                                text = profile?.email ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Editar Perfil
                    Button(
                        onClick = { showEditSheet = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Editar Perfil", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // ── Loading bar ──────────────────────────────────────────────────────────
        if (state.isLoading || state.isSaving) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        // ── Mensagem de Sucesso ──────────────────────────────────────────────────
        state.successMessage?.let { msg ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                        Text(msg, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }

        // ── Mensagem de Erro ─────────────────────────────────────────────────────
        state.error?.let { err ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(err, color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp))
                }
            }
        }

        // ── Bento Grid de Saúde ──────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileInfoCard(
                        title = "Tipo de Asma",
                        value = profile?.healthData?.conditions?.firstOrNull() ?: "Não informado",
                        icon = Icons.Default.Favorite,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileInfoCard(
                        title = "Tipo Sanguíneo",
                        value = profile?.healthData?.bloodType?.takeIf { it.isNotBlank() } ?: "Não informado",
                        icon = Icons.Default.Bloodtype,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfileInfoCard(
                        title = "Contato Emergência",
                        value = profile?.emergencyContact?.name?.takeIf { it.isNotBlank() } ?: "Não informado",
                        icon = Icons.Default.ContactPhone,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileInfoCard(
                        title = "Protocolo de Crise",
                        value = "Ver passos de emergência",
                        icon = Icons.AutoMirrored.Filled.List,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToHelp
                    )
                }
            }
        }

        // ── Seção Medicação ──────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Medicação Atual", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Default.MedicalServices, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary)
                    }

                    val meds = profile?.healthData?.medications ?: emptyList()
                    val controle = meds.count { it.contains("controle", ignoreCase = true) || it.contains("manutenc", ignoreCase = true) }
                    val resgate  = meds.count { it.contains("resgate", ignoreCase = true) || it.contains("bronco", ignoreCase = true) }
                    val outros   = maxOf(0, meds.size - controle - resgate)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MedCountChip("Controle", controle, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                        MedCountChip("Resgate",  resgate,  MaterialTheme.colorScheme.error,   Modifier.weight(1f))
                        MedCountChip("Outros",   outros,   MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    }
                }
            }
        }

        // ── Ações Rápidas ────────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Ações Rápidas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp))
                    QuickActionItem(Icons.Default.Favorite,     "Histórico Médico",  "Ver registros de saúde")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    QuickActionItem(Icons.Default.Notifications, "Notificações",      "Gerenciar alertas")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    QuickActionItem(Icons.Default.Shield,        "Privacidade",       "Configurações de dados",
                        onClick = onNavigateToPrivacy)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    QuickActionItem(Icons.Default.Settings,      "Configurações",     "Preferências do app",
                        onClick = onNavigateToSettings)
                }
            }
        }
    }

    // ── Edit Bottom Sheet ────────────────────────────────────────────────────────
    if (showEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            EditProfileSheetContent(
                name = editName,           onNameChange = { editName = it },
                phone = editPhone,          onPhoneChange = { editPhone = it },
                bloodType = editBloodType,  onBloodTypeChange = { editBloodType = it },
                allergies = editAllergies,  onAllergiesChange = { editAllergies = it },
                medications = editMedications, onMedicationsChange = { editMedications = it },
                conditions = editConditions,  onConditionsChange = { editConditions = it },
                notes = editNotes,          onNotesChange = { editNotes = it },
                emergName = editEmergName,  onEmergNameChange = { editEmergName = it },
                emergPhone = editEmergPhone, onEmergPhoneChange = { editEmergPhone = it },
                emergRel = editEmergRel,    onEmergRelChange = { editEmergRel = it },
                isSaving = state.isSaving,
                onSave = {
                    val current = profile ?: return@EditProfileSheetContent
                    val split: (String) -> List<String> = { s ->
                        s.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    }
                    viewModel.updateProfile(
                        current.copy(
                            name = editName,
                            phone = editPhone,
                            healthData = UserHealthData(
                                bloodType  = editBloodType,
                                allergies  = split(editAllergies),
                                medications = split(editMedications),
                                conditions  = split(editConditions),
                                notes       = editNotes
                            ),
                            emergencyContact = EmergencyContact(
                                name         = editEmergName,
                                phone        = editEmergPhone,
                                relationship = editEmergRel
                            )
                        )
                    )
                    showEditSheet = false
                },
                onCancel = { showEditSheet = false }
            )
        }
    }
}

// ── InfoCard ─────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileInfoCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.height(90.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(title, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                maxLines = 2)
        }
    }
}

// ── MedCountChip ─────────────────────────────────────────────────────────────────

@Composable
private fun MedCountChip(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(count.toString(), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── QuickActionItem ───────────────────────────────────────────────────────────────

@Composable
private fun QuickActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

// ── Edit Bottom Sheet Content ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheetContent(
    name: String,        onNameChange: (String) -> Unit,
    phone: String,       onPhoneChange: (String) -> Unit,
    bloodType: String,   onBloodTypeChange: (String) -> Unit,
    allergies: String,   onAllergiesChange: (String) -> Unit,
    medications: String, onMedicationsChange: (String) -> Unit,
    conditions: String,  onConditionsChange: (String) -> Unit,
    notes: String,       onNotesChange: (String) -> Unit,
    emergName: String,   onEmergNameChange: (String) -> Unit,
    emergPhone: String,  onEmergPhoneChange: (String) -> Unit,
    emergRel: String,    onEmergRelChange: (String) -> Unit,
    isSaving: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Título + ações
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) { Text("Cancelar") }
            Text("Editar Perfil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                TextButton(onClick = onSave) {
                    Text("Salvar", fontWeight = FontWeight.Bold)
                }
            }
        }

        HorizontalDivider()

        // Informações Pessoais
        SectionLabel("Informações Pessoais")
        ProfileTextField("Nome completo", name, onNameChange)
        ProfileTextField("Telefone", phone, onPhoneChange)

        // Dados de Saúde
        SectionLabel("Dados de Saúde")
        ProfileTextField("Tipo sanguíneo (ex: O+)", bloodType, onBloodTypeChange)
        ProfileTextField("Alergias (separadas por vírgula)", allergies, onAllergiesChange)
        ProfileTextField("Medicamentos em uso", medications, onMedicationsChange)
        ProfileTextField("Condições médicas", conditions, onConditionsChange)
        ProfileTextField("Observações adicionais", notes, onNotesChange, singleLine = false)

        // Contato de Emergência
        SectionLabel("Contato de Emergência")
        ProfileTextField("Nome", emergName, onEmergNameChange)
        ProfileTextField("Telefone", emergPhone, onEmergPhoneChange)
        ProfileTextField("Parentesco (ex: Mãe)", emergRel, onEmergRelChange)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    )
}