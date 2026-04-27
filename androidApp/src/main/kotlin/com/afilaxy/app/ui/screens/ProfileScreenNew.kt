package com.afilaxy.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import com.afilaxy.domain.model.EmergencyHistory
import com.afilaxy.domain.model.UserHealthData
import com.afilaxy.domain.model.UserProfile
import com.afilaxy.presentation.history.HistoryViewModel
import com.afilaxy.presentation.profile.ProfileViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenNew(
    onNavigateToHistory: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val profile = state.profile
    var showEditSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val historyViewModel: HistoryViewModel = koinViewModel()
    val historyState by historyViewModel.state.collectAsState()
    val recentHistory = historyState.filteredHistory.take(3)

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

        // ── Agenda de Saúde ───────────────────────────────────────────────────────
        item {
            AgendaDeSaudeCard(
                profile = profile,
                recentHistory = recentHistory,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToHelp = onNavigateToHelp,
                onEditProfile = { showEditSheet = true }
            )
        }

        // ── Sair da Conta ──────────────────────────────────────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                onClick = { showLogoutDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Sair da Conta",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sair da Conta", fontWeight = FontWeight.Bold) },
            text = { Text("Tem certeza que deseja sair? Você precisará fazer login novamente.") },
            confirmButton = {
                Button(
                    onClick = { showLogoutDialog = false; onLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Sair") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") }
            }
        )
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
                                bloodType  = profile?.healthData?.bloodType ?: "",
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

// ── AgendaDeSaudeCard ─────────────────────────────────────────────────────────────

@Composable
private fun AgendaDeSaudeCard(
    profile: UserProfile?,
    recentHistory: List<EmergencyHistory>,
    onNavigateToHistory: () -> Unit,
    onNavigateToHelp: () -> Unit,
    onEditProfile: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Cabeçalho
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                    Text("Agenda de Saúde", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = onEditProfile, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
            HorizontalDivider()
            // Dados clínicos
            ProfileInfoCard(
                "Tipo de Asma",
                profile?.healthData?.conditions?.firstOrNull() ?: "Não informado",
                Icons.Default.Favorite,
                Modifier.fillMaxWidth()
            )
            // Medicação

            val meds = profile?.healthData?.medications ?: emptyList()
            val controle = meds.count { it.contains("controle", ignoreCase = true) || it.contains("manutenc", ignoreCase = true) }
            val resgate  = meds.count { it.contains("resgate",  ignoreCase = true) || it.contains("bronco",   ignoreCase = true) }
            val outros   = maxOf(0, meds.size - controle - resgate)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Medicação Atual", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                    MedCountChip("Controle", controle, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    MedCountChip("Resgate",  resgate,  MaterialTheme.colorScheme.error,   Modifier.weight(1f))
                    MedCountChip("Outros",   outros,   MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                }
            }
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                ProfileInfoCard(
                    "Contato Emergência",
                    profile?.emergencyContact?.name?.takeIf { it.isNotBlank() } ?: "Não informado",
                    Icons.Default.ContactPhone,
                    Modifier.weight(1f)
                )
                ProfileInfoCard(
                    "Protocolo de Crise",
                    "Ver passos de emergência",
                    Icons.AutoMirrored.Filled.List,
                    Modifier.weight(1f),
                    onClick = onNavigateToHelp
                )
            }
            HorizontalDivider()
            // Ocorrências Recentes
            Text(
                "Ocorrências Recentes",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (recentHistory.isEmpty()) {
                Text(
                    "Nenhuma ocorrência registrada",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                recentHistory.forEach { event -> AgendaHistoryItem(event) }
            }
            TextButton(onClick = onNavigateToHistory, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Filled.List, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Ver histórico completo", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun AgendaHistoryItem(event: EmergencyHistory) {
    val statusColor = when (event.status) {
        "resolved"  -> MaterialTheme.colorScheme.secondary
        "cancelled" -> MaterialTheme.colorScheme.error
        "matched"   -> MaterialTheme.colorScheme.tertiary
        else        -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusLabel = when (event.status) {
        "resolved"  -> "Resolvida"
        "cancelled" -> "Cancelada"
        "matched"   -> "Em Atendimento"
        else        -> event.status
    }
    val dateStr = remember(event.timestamp) {
        SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")).format(Date(event.timestamp))
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), Arrangement.spacedBy(10.dp), Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(statusColor, CircleShape))
        Column(Modifier.weight(1f)) {
            Text(statusLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = statusColor)
            Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Edit Bottom Sheet Content ─────────────────────────────────────────────────────

// ── EditProfileSheetContent ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileSheetContent(
    name: String,        onNameChange: (String) -> Unit,
    phone: String,       onPhoneChange: (String) -> Unit,
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
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Cabeçalho
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) { Text("Cancelar") }
                Text("Editar Perfil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    TextButton(onClick = onSave) {
                        Text("Salvar", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            HorizontalDivider()
        }
        // Seção 1: Informações Pessoais
        item {
            EditSection(icon = Icons.Default.Person, title = "Informações Pessoais") {
                ProfileTextField("Nome completo", name, onNameChange)
                ProfileTextField("Telefone", phone, onPhoneChange)
            }
        }
        // Seção 2: Dados de Saúde
        item {
            EditSection(icon = Icons.Default.Favorite, title = "Dados de Saúde") {
                ProfileTextField("Tipo de Asma / Condições médicas", conditions, onConditionsChange)
                ProfileTextField("Alergias (separadas por vírgula)", allergies, onAllergiesChange)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    ProfileTextField("Medicação Atual (separada por vírgula)", medications, onMedicationsChange)
                    Text(
                        "Ex: fluticasona controle, salbutamol resgate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                ProfileTextField("Observações adicionais", notes, onNotesChange, singleLine = false)
            }
        }
        // Seção 3: Contato de Emergência
        item {
            EditSection(icon = Icons.Default.ContactPhone, title = "Contato de Emergência") {
                ProfileTextField("Nome", emergName, onEmergNameChange)
                ProfileTextField("Telefone", emergPhone, onEmergPhoneChange)
                ProfileTextField("Parentesco (ex: Mãe)", emergRel, onEmergRelChange)
            }
        }
    }
}

// ── EditSection ─────────────────────────────────────────────────────────────────

@Composable
private fun EditSection(
    icon: ImageVector,
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider()
            content()
        }
    }
}

// ── ProfileTextField ─────────────────────────────────────────────────────────────

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
