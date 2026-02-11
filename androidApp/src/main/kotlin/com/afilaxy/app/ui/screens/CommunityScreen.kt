package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.afilaxy.domain.model.Evento
import com.afilaxy.domain.model.Produto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Produtos", "Eventos")
    
    val produtos = remember {
        listOf(
            Produto("1", "Bombinha de Asma", "Bombinha para crises de asma", 45.0, "Medicamentos"),
            Produto("2", "Espaçador", "Espaçador para bombinha", 25.0, "Acessórios")
        )
    }
    
    val eventos = remember {
        listOf(
            Evento("1", "Palestra sobre Asma", "Aprenda a controlar a asma", "15/12/2024", "Centro Comunitário"),
            Evento("2", "Grupo de Apoio", "Encontro mensal de apoio", "20/12/2024", "Online")
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comunidade") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            when (selectedTab) {
                0 -> ProdutosList(produtos)
                1 -> EventosList(eventos)
            }
        }
    }
}

@Composable
fun ProdutosList(produtos: List<Produto>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(produtos) { produto ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(produto.nome, style = MaterialTheme.typography.titleMedium)
                    Text(produto.descricao, style = MaterialTheme.typography.bodyMedium)
                    Text("R$ ${produto.preco}", style = MaterialTheme.typography.titleSmall)
                }
            }
        }
    }
}

@Composable
fun EventosList(eventos: List<Evento>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(eventos) { evento ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(evento.titulo, style = MaterialTheme.typography.titleMedium)
                    Text(evento.descricao, style = MaterialTheme.typography.bodyMedium)
                    Text("📅 ${evento.data}", style = MaterialTheme.typography.bodySmall)
                    Text("📍 ${evento.local}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
