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

    val safeLat = destinationLat.takeIf { it.isFinite() && it in -90.0..90.0 } ?: run { onBackClick(); return }
    val safeLng = destinationLng.takeIf { it.isFinite() && it in -180.0..180.0 } ?: run { onBackClick(); return }
    val safeName = destinationName.replace(Regex("[<>&\"'\\x00-\\x1F]"), "").take(100).ifBlank { "Pessoa em emergência" }

    val destination = LatLng(safeLat, safeLng)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(destination, 15f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Navegação para $safeName") },
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
                    title = safeName
                )
            }

            Button(
                onClick = {
                    val latStr = safeLat.toBigDecimal().toPlainString()
                        val lngStr = safeLng.toBigDecimal().toPlainString()
                        val uri = Uri.Builder()
                            .scheme("google.navigation")
                            .appendQueryParameter("q", "$latStr,$lngStr")
                            .build()
                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            setPackage("com.google.android.apps.maps")
                        }
                        if (intent.resolveActivity(context.packageManager) != null) {
                            context.startActivity(intent)
                        } else {
                            val fallbackUri = Uri.Builder()
                                .scheme("geo")
                                .path("$latStr,$lngStr")
                                .appendQueryParameter("q", "$latStr,$lngStr($safeName)")
                                .build()
                            context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
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
