package com.afilaxy.app.ui.screens

import androidx.compose.foundation.clickable
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
import com.afilaxy.app.navigation.AppRoutes
import com.afilaxy.domain.model.Evento
import com.afilaxy.domain.model.Produto

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    navController: NavController,
    produtos: List<Produto> = emptyList(),
    eventos: List<Evento> = emptyList()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Produtos", "Eventos")

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
                0 -> ProdutosList(
                    produtos = produtos,
                    onProdutoClick = { produto ->
                        navController.navigate(AppRoutes.produtoDetail(produto.id))
                    }
                )
                1 -> EventosList(
                    eventos = eventos,
                    onEventoClick = { evento ->
                        navController.navigate(AppRoutes.eventoDetail(evento.id))
                    }
                )
            }
        }
    }
}

@Composable
fun ProdutosList(
    produtos: List<Produto>,
    onProdutoClick: (Produto) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(produtos, key = { it.id }) { produto ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProdutoClick(produto) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(produto.nome, style = MaterialTheme.typography.titleMedium)
                    Text(produto.descricao, style = MaterialTheme.typography.bodyMedium)
                    produto.preco?.let {
                        Text("R$ $it", style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    produto.cupom?.let {
                        Text("🏷️ Cupom: $it", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
        }
    }
}

@Composable
fun EventosList(
    eventos: List<Evento>,
    onEventoClick: (Evento) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(eventos, key = { it.id }) { evento ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEventoClick(evento) }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(evento.titulo, style = MaterialTheme.typography.titleMedium)
                    Text(evento.descricao, style = MaterialTheme.typography.bodyMedium)
                    Text("📅 ${evento.data}", style = MaterialTheme.typography.bodySmall)
                    evento.local?.let {
                        Text("📍 $it", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
