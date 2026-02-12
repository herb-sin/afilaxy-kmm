package com.afilaxy.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.afilaxy.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sobre") },
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
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            item {
                Text(
                    "Afilaxy",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item {
                Text(
                    "Versão 2.1.0-kmm",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            
            item {
                Text(
                    "Sistema de Emergência Médica",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            }
            
            item {
                Text(
                    "Conectamos pessoas em situações críticas com helpers próximos, oferecendo suporte rápido e eficiente.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Recursos",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("🚨 Criação de emergências em tempo real")
                        Text("💬 Chat direto com helpers")
                        Text("📍 Busca de helpers próximos")
                        Text("🔐 Autenticação segura")
                        Text("🌍 Multiplataforma (Android/iOS)")
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Tecnologias",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text("• Kotlin Multiplatform Mobile")
                        Text("• Firebase (Auth + Firestore)")
                        Text("• Clean Architecture + MVVM")
                        Text("• Jetpack Compose")
                    }
                }
            }
            
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            
            item {
                Text(
                    "Desenvolvido com ❤️",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            item {
                Text(
                    "© 2024 Afilaxy. Todos os direitos reservados.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
