package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

data class FaqItem(val pergunta: String, val resposta: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutocuidadoScreen(navController: NavController) {
    val faqItems = remember {
        listOf(
            FaqItem(
                "O que é asma?",
                "Asma é uma doença crônica que afeta as vias respiratórias, causando inflamação e estreitamento dos brônquios."
            ),
            FaqItem(
                "Quais são os sintomas?",
                "Falta de ar, chiado no peito, tosse e aperto no peito são os principais sintomas."
            ),
            FaqItem(
                "Como usar a bombinha?",
                "Agite a bombinha, expire completamente, coloque o bocal na boca, pressione e inspire profundamente."
            ),
            FaqItem(
                "O que fazer em uma crise?",
                "Use a bombinha de alívio imediato, sente-se em posição confortável e respire calmamente. Se não melhorar, procure ajuda médica."
            ),
            FaqItem(
                "Como prevenir crises?",
                "Evite gatilhos (poeira, fumaça, pólen), use medicação preventiva conforme prescrito e mantenha acompanhamento médico."
            )
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Autocuidado") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Perguntas Frequentes sobre Asma",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            items(faqItems) { item ->
                FaqCard(item)
            }
        }
    }
}

@Composable
fun FaqCard(item: FaqItem) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    item.pergunta,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Recolher" else "Expandir"
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    item.resposta,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
