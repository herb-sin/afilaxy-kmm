package com.afilaxy.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

// ---------------------------------------------------------------------------
// AutocuidadoScreen — paridade de conteúdo com AutocuidadoView.swift (iOS):
// FAQ sobre asma, mesmo texto nas duas plataformas.
// ---------------------------------------------------------------------------

private data class FaqItem(val pergunta: String, val resposta: String)

private val faqItems = listOf(
    FaqItem(
        pergunta = "O que é asma?",
        resposta = "Asma é uma doença crônica que afeta as vias respiratórias, causando inflamação e estreitamento dos brônquios."
    ),
    FaqItem(
        pergunta = "Quais são os sintomas?",
        resposta = "Falta de ar, chiado no peito, tosse e aperto no peito são os principais sintomas."
    ),
    FaqItem(
        pergunta = "Como usar a bombinha?",
        resposta = "Agite a bombinha, expire completamente, coloque o bocal na boca, pressione e inspire profundamente."
    ),
    FaqItem(
        pergunta = "O que fazer em uma crise?",
        resposta = "Use a bombinha de alívio imediato, sente-se em posição confortável e respire calmamente. Se não melhorar, procure ajuda médica."
    ),
    FaqItem(
        pergunta = "Como prevenir crises?",
        resposta = "Evite gatilhos (poeira, fumaça, pólen), use medicação preventiva conforme prescrito e mantenha acompanhamento médico."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutocuidadoScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Autocuidado", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(
                    "Perguntas Frequentes sobre Asma",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(faqItems) { item -> FaqRow(item) }
        }
    }
}

@Composable
private fun FaqRow(item: FaqItem) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                item.pergunta,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                item.resposta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
    HorizontalDivider()
}
