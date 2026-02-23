package com.afilaxy.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigationScreen(
    destinationLat: Double,
    destinationLng: Double,
    destinationName: String = "Pessoa em emergência",
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val destination = LatLng(destinationLat, destinationLng)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(destination, 15f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Navegação para $destinationName") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
        ) {
            GoogleMap(
                modifier = Modifier.weight(1f),
                cameraPositionState = cameraPositionState
            ) {
                Marker(
                    state = MarkerState(position = destination),
                    title = destinationName
                )
            }

            Button(
                onClick = {
                    try {
                        // Sanitizar coordenadas (prevenir injeção de URI)
                        val safeLat = destinationLat.toBigDecimal().toPlainString()
                        val safeLng = destinationLng.toBigDecimal().toPlainString()
                        val uri = Uri.parse("google.navigation:q=$safeLat,$safeLng")
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        // Fallback para qualquer app de navegação se Google Maps não instalado
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        } else {
                            val fallbackUri = Uri.parse("geo:$safeLat,$safeLng?q=$safeLat,$safeLng")
                            context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
                        }
                    } catch (e: Exception) {
                        // Ignorar silenciosamente — botão apenas não abre se falhar
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("🗺️ Iniciar Navegação Externa")
            }
        }
    }
}
