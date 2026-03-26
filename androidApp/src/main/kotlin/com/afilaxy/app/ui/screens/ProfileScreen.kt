package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.afilaxy.domain.model.EmergencyContact
import com.afilaxy.domain.model.UserHealthData
import com.afilaxy.presentation.profile.ProfileViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenOld(
    onNavigateBack: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val profile = state.profile
    
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var medications by remember { mutableStateOf("") }
    var conditions by remember { mutableStateOf("") }
    var healthNotes by remember { mutableStateOf("") }
    var emergencyName by remember { mutableStateOf("") }
    var emergencyPhone by remember { mutableStateOf("") }
    var emergencyRelationship by remember { mutableStateOf("") }
    
    LaunchedEffect(profile) {
        profile?.let {
            name = it.name
            phone = it.phone
            bloodType = it.healthData?.bloodType ?: ""
            allergies = it.healthData?.allergies?.joinToString(", ") ?: ""
            medications = it.healthData?.medications?.joinToString(", ") ?: ""
            conditions = it.healthData?.conditions?.joinToString(", ") ?: ""
            healthNotes = it.healthData?.notes ?: ""
            emergencyName = it.emergencyContact?.name ?: ""
            emergencyPhone = it.emergencyContact?.phone ?: ""
            emergencyRelationship = it.emergencyContact?.relationship ?: ""
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meu Perfil") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
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
            if (state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            
            state.successMessage?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Text(it, modifier = Modifier.padding(16.dp))
                }
            }
            
            state.error?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(it, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            
            Text("Informações Pessoais", style = MaterialTheme.typography.titleLarge)
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome completo") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Telefone") },
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                modifier = Modifier.fillMaxWidth()
            )
            
            HorizontalDivider()
            
            Text("Dados de Saúde", style = MaterialTheme.typography.titleLarge)
            
            OutlinedTextField(
                value = bloodType,
                onValueChange = { bloodType = it },
                label = { Text("Tipo Sanguíneo") },
                placeholder = { Text("Ex: O+, A-, AB+") },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = allergies,
                onValueChange = { allergies = it },
                label = { Text("Alergias") },
                placeholder = { Text("Separadas por vírgula") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            
            OutlinedTextField(
                value = medications,
                onValueChange = { medications = it },
                label = { Text("Medicamentos em uso") },
                placeholder = { Text("Separados por vírgula") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            
            OutlinedTextField(
                value = conditions,
                onValueChange = { conditions = it },
                label = { Text("Condições médicas") },
                placeholder = { Text("Separadas por vírgula") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )
            
            OutlinedTextField(
                value = healthNotes,
                onValueChange = { healthNotes = it },
                label = { Text("Observações adicionais") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            
            HorizontalDivider()
            
            Text("Contato de Emergência", style = MaterialTheme.typography.titleLarge)
            
            OutlinedTextField(
                value = emergencyName,
                onValueChange = { emergencyName = it },
                label = { Text("Nome") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = emergencyPhone,
                onValueChange = { emergencyPhone = it },
                label = { Text("Telefone") },
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = emergencyRelationship,
                onValueChange = { emergencyRelationship = it },
                label = { Text("Parentesco") },
                placeholder = { Text("Ex: Mãe, Pai, Cônjuge") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Button(
                onClick = {
                    profile?.let {
                        viewModel.updateProfile(
                            it.copy(
                                name = name,
                                phone = phone,
                                healthData = UserHealthData(
                                    bloodType = bloodType,
                                    allergies = allergies.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                    medications = medications.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                    conditions = conditions.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                    notes = healthNotes
                                ),
                                emergencyContact = EmergencyContact(
                                    name = emergencyName,
                                    phone = emergencyPhone,
                                    relationship = emergencyRelationship
                                )
                            )
                        )
                    }
                },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Salvar Alterações")
                }
            }
        }
    }
}
