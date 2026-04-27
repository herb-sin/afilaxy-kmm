package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.afilaxy.presentation.professional.CrmLookupState
import com.afilaxy.presentation.professional.CrmLookupViewModel
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.koin.androidx.compose.koinViewModel

private val UF_LIST = listOf(
    "AC","AL","AM","AP","BA","CE","DF","ES","GO","MA","MG","MS","MT",
    "PA","PB","PE","PI","PR","RJ","RN","RO","RR","RS","SC","SE","SP","TO"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrmLookupScreen(
    onNavigateBack: () -> Unit,
    viewModel: CrmLookupViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var crm by remember { mutableStateOf("") }
    var uf by remember { mutableStateOf("") }
    var ufExpanded by remember { mutableStateOf(false) }

    val canSearch = crm.isNotBlank() && uf.length == 2 && state !is CrmLookupState.Loading

    fun search() {
        if (!canSearch) return
        focusManager.clearFocus()
        viewModel.setState(CrmLookupState.Loading)
        scope.launch {
            try {
                val result = FirebaseFunctions.getInstance("us-central1")
                    .getHttpsCallable("validateCrm")
                    .call(mapOf("crm" to crm.trim(), "uf" to uf))
                    .await()

                @Suppress("UNCHECKED_CAST")
                val data = result.getData() as? Map<String, Any> ?: emptyMap()

                if (data["found"] == true) {
                    viewModel.setState(
                        CrmLookupState.Success(
                            com.afilaxy.presentation.professional.CrmResult(
                                name = data["name"] as? String ?: "",
                                specialty = data["specialty"] as? String ?: "",
                                situation = data["situation"] as? String ?: "",
                                uf = data["uf"] as? String ?: uf,
                                crm = data["crm"] as? String ?: crm,
                            )
                        )
                    )
                } else {
                    viewModel.setState(CrmLookupState.NotFound)
                }
            } catch (e: FirebaseFunctionsException) {
                viewModel.setState(CrmLookupState.Error(
                    if (e.code == FirebaseFunctionsException.Code.UNAVAILABLE)
                        "Serviço do CFM indisponível. Tente novamente mais tarde."
                    else
                        "Erro ao consultar CRM. Verifique os dados e tente novamente."
                ))
            } catch (e: Exception) {
                viewModel.setState(CrmLookupState.Error("Erro inesperado. Tente novamente."))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consultar CRM") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.reset()
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Verifique se um médico está regularmente inscrito no Conselho Federal de Medicina.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = crm,
                onValueChange = {
                    crm = it.filter { c -> c.isDigit() }.take(10)
                    viewModel.reset()
                },
                label = { Text("Número do CRM") },
                placeholder = { Text("Ex: 123456") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = ufExpanded,
                onExpandedChange = { ufExpanded = it }
            ) {
                OutlinedTextField(
                    value = uf,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("UF") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ufExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = ufExpanded,
                    onDismissRequest = { ufExpanded = false }
                ) {
                    UF_LIST.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = {
                                uf = item
                                ufExpanded = false
                                viewModel.reset()
                            }
                        )
                    }
                }
            }

            Button(
                onClick = ::search,
                enabled = canSearch,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state is CrmLookupState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Consultar")
                }
            }

            when (val s = state) {
                is CrmLookupState.Success -> CrmResultCard(s.result)
                is CrmLookupState.NotFound -> NotFoundCard()
                is CrmLookupState.Error -> ErrorCard(s.message)
                else -> Unit
            }
        }
    }
}

@Composable
private fun CrmResultCard(result: com.afilaxy.presentation.professional.CrmResult) {
    val situationColor = if (result.situation.contains("ativo", ignoreCase = true) ||
        result.situation.contains("regular", ignoreCase = true))
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VerifiedUser, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Médico encontrado", style = MaterialTheme.typography.titleMedium)
            }
            HorizontalDivider()
            LabeledField("Nome", result.name)
            LabeledField("CRM", "${result.crm}/${result.uf}")
            LabeledField("Especialidade", result.specialty.ifBlank { "Não informada" })
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Situação: ", style = MaterialTheme.typography.bodyMedium)
                Text(
                    result.situation.ifBlank { "Não informada" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = situationColor
                )
            }
            Text(
                "Fonte: Conselho Federal de Medicina (CFM)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NotFoundCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PersonOff, contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("CRM não encontrado", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer)
                Text("Verifique o número e a UF informados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer)
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String) {
    Row {
        Text("$label: ", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
