package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.afilaxy.domain.model.ContentCategory
import com.afilaxy.domain.model.EducationalContent
import com.afilaxy.presentation.education.EducationListViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EducationListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    viewModel: EducationListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedCategory by remember { mutableStateOf<ContentCategory?>(null) }
    var showFilterMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aprenda sobre Asma") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                actions = {
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
                                selectedCategory = null
                                viewModel.filterByCategory(null)
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🆘 Medicação de Resgate") },
                            onClick = {
                                selectedCategory = ContentCategory.RESCUE
                                viewModel.filterByCategory(ContentCategory.RESCUE)
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🛡️ Medicação de Manutenção") },
                            onClick = {
                                selectedCategory = ContentCategory.MAINTENANCE
                                viewModel.filterByCategory(ContentCategory.MAINTENANCE)
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("🏥 Como conseguir no SUS") },
                            onClick = {
                                selectedCategory = ContentCategory.SUS
                                viewModel.filterByCategory(ContentCategory.SUS)
                                showFilterMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("❓ Perguntas Frequentes") },
                            onClick = {
                                selectedCategory = ContentCategory.FAQ
                                viewModel.filterByCategory(ContentCategory.FAQ)
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
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            state.error ?: "Erro desconhecido",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.content) { content ->
                            EducationCard(
                                content = content,
                                onClick = { onNavigateToDetail(content.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EducationCard(
    content: EducationalContent,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        getCategoryEmoji(content.category),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        content.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        content.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${content.readTimeMinutes} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Text(
                    content.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getCategoryEmoji(category: ContentCategory): String {
    return when (category) {
        ContentCategory.RESCUE -> "🆘"
        ContentCategory.MAINTENANCE -> "🛡️"
        ContentCategory.SUS -> "🏥"
        ContentCategory.FAQ -> "❓"
    }
}
