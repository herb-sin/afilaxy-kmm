package com.afilaxy.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.afilaxy.app.ui.theme.ThemeState
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
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val profile = state.profile
    var showEditSheet by remember { mutableStateOf(false) }
    val historyViewModel: HistoryViewModel = koinViewModel()
    val historyState by historyViewModel.state.collectAsState()
    val recentHistory = historyState.filteredHistory.take(3)

    // Campos de edição — inicializados com os dados do perfil
    var editName        by remember { mutableStateOf("") }
    var editPhone       by remember { mutableStateOf("") }
    var editBloodType   by remember { mutableStateOf("") }
    var editAllergies   by remember { mutableStateOf("") }
    var editNotes       by remember { mutableStateOf("") }
    var editEmergName   by remember { mutableStateOf("") }
    var editEmergPhone  by remember { mutableStateOf("") }
    var editEmergRel    by remember { mutableStateOf("") }
    var editHasGerd       by remember { mutableStateOf(false) }
    var editHasSleepApnea by remember { mutableStateOf(false) }
    var editHasRhinitis   by remember { mutableStateOf(false) }
    var editHasObesity    by remember { mutableStateOf(false) }
    var editHasFoodAllergy     by remember { mutableStateOf(false) }
    var editHasNsaidAllergy    by remember { mutableStateOf(false) }
    var editHasInhalantAllergy by remember { mutableStateOf(false) }
    var editHasWheelchair       by remember { mutableStateOf(false) }
    var editHasLowVision        by remember { mutableStateOf(false) }
    var editHasSpecialCondition by remember { mutableStateOf(false) }

    // Preenche campos quando o perfil chega pela primeira vez
    LaunchedEffect(profile) {
        if (profile != null) {
            editName        = profile.name
            editPhone       = profile.phone.filter { it.isDigit() }.take(11)
            editBloodType   = profile.healthData?.bloodType ?: ""
            editAllergies   = profile.healthData?.allergies?.joinToString(", ") ?: ""
            editNotes       = profile.healthData?.notes ?: ""
            editEmergName   = profile.emergencyContact?.name ?: ""
            editEmergPhone  = (profile.emergencyContact?.phone ?: "").filter { it.isDigit() }.take(11)
            editEmergRel    = profile.emergencyContact?.relationship ?: ""
            editHasGerd       = profile.healthData?.hasGerd ?: false
            editHasSleepApnea = profile.healthData?.hasSleepApnea ?: false
            editHasRhinitis   = profile.healthData?.hasRhinitis ?: false
            editHasObesity    = profile.healthData?.hasObesity ?: false
            editHasFoodAllergy     = profile.healthData?.hasFoodAllergy ?: false
            editHasNsaidAllergy    = profile.healthData?.hasNsaidAllergy ?: false
            editHasInhalantAllergy = profile.healthData?.hasInhalantAllergy ?: false
            editHasWheelchair       = profile.healthData?.hasWheelchair ?: false
            editHasLowVision        = profile.healthData?.hasLowVision ?: false
            editHasSpecialCondition = profile.healthData?.hasSpecialCondition ?: false
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
                    // Avatar + botão de câmera (visual apenas — upload de foto ainda não implementado)
                    val context = LocalContext.current
                    Box(modifier = Modifier.size(80.dp)) {
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
                        IconButton(
                            onClick = {
                                android.widget.Toast.makeText(context, "Upload de foto em breve", android.widget.Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Alterar foto do perfil",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Nome + Badge + Email
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = profile?.name ?: "Carregando...",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (profile?.isHealthProfessional == true) {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "Profissional de Saúde",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
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

        // ── Aparência ─────────────────────────────────────────────────────────────────────────
        item {
            ThemePreferenceCard()
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
                allergies = editAllergies,  onAllergiesChange = { editAllergies = it },
                notes = editNotes,          onNotesChange = { editNotes = it },
                emergName = editEmergName,  onEmergNameChange = { editEmergName = it },
                emergPhone = editEmergPhone, onEmergPhoneChange = { editEmergPhone = it },
                emergRel = editEmergRel,    onEmergRelChange = { editEmergRel = it },
                hasGerd = editHasGerd,             onHasGerdChange = { editHasGerd = it },
                hasSleepApnea = editHasSleepApnea, onHasSleepApneaChange = { editHasSleepApnea = it },
                hasRhinitis = editHasRhinitis,     onHasRhinitisChange = { editHasRhinitis = it },
                hasObesity = editHasObesity,       onHasObesityChange = { editHasObesity = it },
                hasFoodAllergy = editHasFoodAllergy,         onHasFoodAllergyChange = { editHasFoodAllergy = it },
                hasNsaidAllergy = editHasNsaidAllergy,       onHasNsaidAllergyChange = { editHasNsaidAllergy = it },
                hasInhalantAllergy = editHasInhalantAllergy, onHasInhalantAllergyChange = { editHasInhalantAllergy = it },
                hasWheelchair = editHasWheelchair,             onHasWheelchairChange = { editHasWheelchair = it },
                hasLowVision = editHasLowVision,               onHasLowVisionChange = { editHasLowVision = it },
                hasSpecialCondition = editHasSpecialCondition, onHasSpecialConditionChange = { editHasSpecialCondition = it },
                isSaving = state.isSaving,
                onSave = {
                    val current = profile ?: return@EditProfileSheetContent
                    val split: (String) -> List<String> = { s ->
                        s.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    }
                    val safeName = editName.trim().takeIf { it.isNotBlank() } ?: current.name
                    viewModel.updateProfile(
                        current.copy(
                            name = safeName,
                            phone = editPhone,
                            healthData = UserHealthData(
                                bloodType     = current.healthData?.bloodType ?: "",
                                allergies     = split(editAllergies),
                                medications   = current.healthData?.medications ?: emptyList(),
                                conditions    = current.healthData?.conditions ?: emptyList(),
                                notes         = editNotes,
                                hasGerd       = editHasGerd,
                                hasSleepApnea = editHasSleepApnea,
                                hasRhinitis   = editHasRhinitis,
                                hasObesity    = editHasObesity,
                                hasFoodAllergy     = editHasFoodAllergy,
                                hasNsaidAllergy    = editHasNsaidAllergy,
                                hasInhalantAllergy = editHasInhalantAllergy,
                                hasWheelchair = editHasWheelchair,
                                hasLowVision  = editHasLowVision,
                                hasSpecialCondition = editHasSpecialCondition
                            ),
                            emergencyContact = EmergencyContact(
                                name         = editEmergName,
                                phone        = editEmergPhone,
                                relationship = editEmergRel
                            )
                        )
                    )
                    showEditSheet = false
                }
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
                    Text("Meu Perfil", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                IconButton(onClick = onEditProfile, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, "Editar", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                ProfileInfoCard(
                    "Contato Emergência",
                    profile?.emergencyContact?.let { c ->
                        val name = c.name.takeIf { it.isNotBlank() }
                        val phone = c.phone.takeIf { it.isNotBlank() }
                        when {
                            name != null && phone != null -> "$name\n$phone"
                            name != null -> name
                            phone != null -> phone
                            else -> "Não informado"
                        }
                    } ?: "Não informado",
                    Icons.Default.ContactPhone,
                    Modifier.weight(1f)
                )
                ProfileInfoCard(
                    "Guia de Emergência",
                    "Ver passos de apoio",
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
    notes: String,       onNotesChange: (String) -> Unit,
    emergName: String,   onEmergNameChange: (String) -> Unit,
    emergPhone: String,  onEmergPhoneChange: (String) -> Unit,
    emergRel: String,    onEmergRelChange: (String) -> Unit,
    hasGerd: Boolean,         onHasGerdChange: (Boolean) -> Unit,
    hasSleepApnea: Boolean,   onHasSleepApneaChange: (Boolean) -> Unit,
    hasRhinitis: Boolean,     onHasRhinitisChange: (Boolean) -> Unit,
    hasObesity: Boolean,      onHasObesityChange: (Boolean) -> Unit,
    hasFoodAllergy: Boolean,     onHasFoodAllergyChange: (Boolean) -> Unit,
    hasNsaidAllergy: Boolean,    onHasNsaidAllergyChange: (Boolean) -> Unit,
    hasInhalantAllergy: Boolean, onHasInhalantAllergyChange: (Boolean) -> Unit,
    hasWheelchair: Boolean,       onHasWheelchairChange: (Boolean) -> Unit,
    hasLowVision: Boolean,        onHasLowVisionChange: (Boolean) -> Unit,
    hasSpecialCondition: Boolean, onHasSpecialConditionChange: (Boolean) -> Unit,
    isSaving: Boolean,
    onSave: () -> Unit
) {
    var nameError by remember { mutableStateOf(false) }

    val onPhoneDigits: (String) -> Unit = { raw -> onPhoneChange(raw.filter { it.isDigit() }.take(11)) }
    val onEmergPhoneDigits: (String) -> Unit = { raw -> onEmergPhoneChange(raw.filter { it.isDigit() }.take(11)) }

    val validateAndSave: () -> Unit = {
        nameError = name.trim().isBlank()
        if (!nameError) onSave()
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Cabeçalho
        item {
            Text(
                "Editar Perfil",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
            HorizontalDivider()
        }
        // Seção 1: Informações Pessoais
        item {
            EditSection(icon = Icons.Default.Person, title = "Informações Pessoais") {
                ProfileTextField(
                    label = "Nome completo *",
                    value = name,
                    onValueChange = { onNameChange(it); nameError = false },
                    isError = nameError,
                    errorMessage = if (nameError) "Nome é obrigatório" else null
                )
                ProfileTextField(
                    label = "Telefone",
                    value = phone,
                    onValueChange = onPhoneDigits,
                    keyboardType = KeyboardType.Phone,
                    visualTransformation = BrPhoneVisualTransformation
                )
            }
        }
        // Seção 2b: Comorbidades (autodeclaradas, sem verificação clínica — alimenta o motor de risco)
        item {
            EditSection(icon = Icons.Default.HealthAndSafety, title = "Comorbidades (opcional)") {
                ProfileInfoBox("Marque as que se aplicam. Usado só para calcular seu nível de risco com mais precisão, nunca compartilhado.")
                ProfileSwitchRow("Refluxo / DRGE", hasGerd, onHasGerdChange)
                ProfileSwitchRow("Apneia do sono", hasSleepApnea, onHasSleepApneaChange)
                ProfileSwitchRow("Rinite alérgica", hasRhinitis, onHasRhinitisChange)
                ProfileSwitchRow("Obesidade", hasObesity, onHasObesityChange)
                HorizontalDivider()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Lock, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "Dados sensíveis protegidos conforme a LGPD",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // Seção 2b-2: Alergias (toggles específicos autodeclarados alimentam o motor de
        // risco num bucket próprio, separado do das comorbidades; o campo de texto livre
        // ao final é só registro pessoal, não pontua)
        item {
            EditSection(icon = Icons.Default.Sick, title = "Alergias (opcional)") {
                ProfileInfoBox("Marque as que se aplicam. Usado só para calcular seu nível de risco com mais precisão, nunca compartilhado.")
                ProfileSwitchRow("Alergia alimentar confirmada", hasFoodAllergy, onHasFoodAllergyChange)
                ProfileSwitchRow("Alergia a anti-inflamatórios (AINEs/aspirina)", hasNsaidAllergy, onHasNsaidAllergyChange)
                ProfileSwitchRow("Alergia a ácaros, pólen, mofo ou pelos de animais", hasInhalantAllergy, onHasInhalantAllergyChange)
                HorizontalDivider()
                ProfileTextField("Outras alergias (opcional)", allergies, onAllergiesChange)
                Text(
                    "Visível apenas para você no seu perfil pessoal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp)
                )
                HorizontalDivider()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Lock, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "Dados sensíveis protegidos conforme a LGPD",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // Seção 2c: Informações para quem for te ajudar — só visível ao Helper numa
        // emergência ativa; não alimenta o motor de risco (propósito diferente das comorbidades).
        item {
            EditSection(icon = Icons.Default.Help, title = "Informações para quem for te ajudar") {
                ProfileInfoBox("Mostradas só para quem aceitar seu pedido de ajuda numa emergência.")
                ProfileSwitchRow(
                    label = "Uso cadeira de rodas",
                    checked = hasWheelchair,
                    onCheckedChange = onHasWheelchairChange,
                    icon = Icons.Default.Accessible
                )
                ProfileSwitchRow(
                    label = "Baixa visão ou cegueira",
                    checked = hasLowVision,
                    onCheckedChange = onHasLowVisionChange,
                    icon = Icons.Default.VisibilityOff
                )
                ProfileSwitchRow(
                    label = "Outra condição que exija atenção especial",
                    checked = hasSpecialCondition,
                    onCheckedChange = onHasSpecialConditionChange,
                    subtitle = "Descreva no campo de detalhes abaixo"
                )
                ProfileTextField("Detalhes complementares (opcional)", notes, onNotesChange, singleLine = false)
            }
        }
        // Seção 3: Contato de Emergência
        item {
            EditSection(icon = Icons.Default.ContactPhone, title = "Contato de Emergência") {
                ProfileTextField("Nome", emergName, onEmergNameChange)
                ProfileTextField(
                    label = "Telefone",
                    value = emergPhone,
                    onValueChange = onEmergPhoneDigits,
                    keyboardType = KeyboardType.Phone,
                    visualTransformation = BrPhoneVisualTransformation
                )
                ProfileTextField("Parentesco (ex: Mãe)", emergRel, onEmergRelChange)
            }
        }
        // Botão de salvar no rodapé
        item {
            Button(
                onClick = validateAndSave,
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Salvar Alterações", fontWeight = FontWeight.Bold)
                }
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
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp))
                }
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

// ── ThemePreferenceCard ───────────────────────────────────────────────────────────

@Composable
private fun ThemePreferenceCard() {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Settings, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    "Aparência",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("system" to "Sistema", "light" to "Claro", "dark" to "Escuro")
                    .forEach { (value, label) ->
                        FilterChip(
                            selected = ThemeState.preference == value,
                            onClick = {
                                ThemeState.preference = value
                                context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                                    .edit().putString("theme_preference", value).apply()
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
            }
        }
    }
}

// ── ProfileSwitchRow ─────────────────────────────────────────────────────────────

@Composable
private fun ProfileSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }
            Column {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        PillToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// ── PillToggle — toggle estilo pílula (flat), mais próximo do visual do mockup
// do que o Switch padrão do Material3 (que tem contorno e thumb menor). ──────────

@Composable
private fun PillToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val trackColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1),
        label = "pillTrackColor"
    )
    Box(
        modifier = Modifier
            .width(46.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(50))
            .background(trackColor)
            .clickable { onCheckedChange(!checked) }
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

// ── ProfileInfoBox — caixa de aviso clara (contexto de uso do dado) ──────────────

@Composable
private fun ProfileInfoBox(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp)
        )
    }
}

// ── ProfileTextField ─────────────────────────────────────────────────────────────

@Composable
private fun ProfileTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = singleLine,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            isError = isError,
            visualTransformation = visualTransformation
        )
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

// ── BrPhoneVisualTransformation ───────────────────────────────────────────────────

private object BrPhoneVisualTransformation : VisualTransformation {
    // Posição do dígito i na string formatada (base-0)
    private val offsets10 = intArrayOf(1, 2, 5, 6, 7, 8, 10, 11, 12, 13)   // (XX) XXXX-XXXX
    private val offsets11 = intArrayOf(1, 2, 5, 6, 7, 8, 9, 11, 12, 13, 14) // (XX) XXXXX-XXXX

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val n = digits.length
        val is11 = n >= 11

        val formatted = when {
            n == 0 -> ""
            n <= 2 -> "(${digits}"
            n <= 6 -> "(${digits.substring(0, 2)}) ${digits.substring(2)}"
            n <= 10 -> "(${digits.substring(0, 2)}) ${digits.substring(2, 6)}-${digits.substring(6)}"
            else -> "(${digits.substring(0, 2)}) ${digits.substring(2, 7)}-${digits.substring(7, 11)}"
        }

        val arr = if (is11) offsets11 else offsets10

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = when {
                offset <= 0 -> 0
                offset <= n -> arr[offset - 1] + 1
                else -> formatted.length
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                var i = n - 1
                while (i >= 0 && arr[i] >= offset) i--
                return (i + 1).coerceIn(0, n)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
