package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.afilaxy.domain.repository.LocationRepository
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import org.koin.compose.koinInject
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    pharmacyMode: Boolean = false
) {
    val locationRepository: LocationRepository = koinInject()

    val saoPaulo = LatLng(-23.5505, -46.6333)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(saoPaulo, 15f)
    }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var mapLoadError by remember { mutableStateOf(false) }
    var mapLoaded by remember { mutableStateOf(false) }
    var pharmacies by remember { mutableStateOf<List<PharmacyPlace>>(emptyList()) }
    var isLoadingPharmacies by remember { mutableStateOf(false) }
    var upas by remember { mutableStateOf<List<UpaPlace>>(emptyList()) }
    var isLoadingUpas by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val loc = locationRepository.getCurrentLocation()
        if (loc != null) {
            val realLatLng = LatLng(loc.latitude, loc.longitude)
            userLocation = realLatLng
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(realLatLng, 14f))

            if (pharmacyMode) {
                isLoadingPharmacies = true
                try {
                    val lat = loc.latitude
                    val lon = loc.longitude
                    val query = """
                        [out:json];
                        (
                          node["amenity"="pharmacy"](around:5000,$lat,$lon);
                          way["amenity"="pharmacy"](around:5000,$lat,$lon);
                        );
                        out center 20;
                    """.trimIndent()
                    val encoded = android.net.Uri.encode(query)
                    val url = "https://overpass-api.de/api/interpreter?data=$encoded"
                    val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        java.net.URL(url).readText()
                    }
                    val elements = org.json.JSONObject(response).getJSONArray("elements")
                    val result = mutableListOf<PharmacyPlace>()
                    for (i in 0 until elements.length()) {
                        val el = elements.getJSONObject(i)
                        val tags = el.optJSONObject("tags")
                        val elLat = if (el.has("lat")) el.getDouble("lat")
                                    else el.optJSONObject("center")?.getDouble("lat") ?: continue
                        val elLon = if (el.has("lon")) el.getDouble("lon")
                                    else el.optJSONObject("center")?.getDouble("lon") ?: continue
                        result.add(PharmacyPlace(
                            name = tags?.optString("name")?.ifBlank { null } ?: "Farmácia",
                            lat = elLat,
                            lon = elLon,
                            phone = tags?.optString("phone") ?: tags?.optString("contact:phone") ?: ""
                        ))
                    }
                    pharmacies = result
                } catch (_: Exception) {
                } finally {
                    isLoadingPharmacies = false
                }
            } else {
                isLoadingUpas = true
                try {
                    val lat = loc.latitude
                    val lon = loc.longitude
                    val query = """
                        [out:json];
                        (
                          node["amenity"="hospital"]["emergency"="yes"](around:10000,$lat,$lon);
                          way["amenity"="hospital"]["emergency"="yes"](around:10000,$lat,$lon);
                          node["amenity"="clinic"]["emergency"="yes"](around:10000,$lat,$lon);
                          way["amenity"="clinic"]["emergency"="yes"](around:10000,$lat,$lon);
                        );
                        out center 20;
                    """.trimIndent()
                    val encoded = android.net.Uri.encode(query)
                    val url = "https://overpass-api.de/api/interpreter?data=$encoded"
                    val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        java.net.URL(url).readText()
                    }
                    val elements = org.json.JSONObject(response).getJSONArray("elements")
                    val result = mutableListOf<UpaPlace>()
                    for (i in 0 until elements.length()) {
                        val el = elements.getJSONObject(i)
                        val tags = el.optJSONObject("tags")
                        val elLat = if (el.has("lat")) el.getDouble("lat")
                                    else el.optJSONObject("center")?.getDouble("lat") ?: continue
                        val elLon = if (el.has("lon")) el.getDouble("lon")
                                    else el.optJSONObject("center")?.getDouble("lon") ?: continue
                        val name = tags?.optString("name")?.ifBlank { null } ?: "UPA"
                        val phone = tags?.optString("phone") ?: tags?.optString("contact:phone") ?: ""
                        result.add(UpaPlace(name = name, lat = elLat, lon = elLon, phone = phone))
                    }
                    upas = result
                } catch (_: Exception) {
                } finally {
                    isLoadingUpas = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (pharmacyMode) {
                        Column {
                            Text("Farmácias 24h")
                            if (isLoadingPharmacies) {
                                Text("Buscando...", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else if (pharmacies.isNotEmpty()) {
                                Text("${pharmacies.size} encontradas no raio de 5 km",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        Column {
                            Text("UPAs próximas")
                            if (isLoadingUpas) {
                                Text("Buscando...", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else if (upas.isNotEmpty()) {
                                Text("${upas.size} encontradas no raio de 10 km",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else if (!isLoadingUpas) {
                                Text("Nenhuma UPA encontrada no raio de 10 km",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    if (!pharmacyMode && upas.isNotEmpty()) {
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Text("${upas.size}")
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Default.LocalHospital,
                            contentDescription = "${upas.size} UPAs próximas",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }
            )
        }
    ) { padding ->
        if (mapLoadError) {
            MapErrorFallback(
                modifier = Modifier.fillMaxSize().padding(padding),
                latitude = userLocation?.latitude ?: saoPaulo.latitude,
                longitude = userLocation?.longitude ?: saoPaulo.longitude,
                onRetry = {
                    mapLoadError = false
                    mapLoaded = false
                }
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapLoaded = { mapLoaded = true }
                ) {
                    val markerPos = userLocation ?: saoPaulo
                    Marker(
                        state = MarkerState(position = markerPos),
                        title = if (userLocation != null) "Você está aqui" else "Posição padrão (GPS indisponível)",
                        snippet = if (userLocation != null)
                            "${String.format(Locale.ROOT, "%.5f", markerPos.latitude)}, " +
                                "${String.format(Locale.ROOT, "%.5f", markerPos.longitude)}"
                        else "São Paulo, SP"
                    )

                    if (pharmacyMode) {
                        pharmacies.forEach { p ->
                            Marker(
                                state = MarkerState(position = LatLng(p.lat, p.lon)),
                                title = p.name,
                                snippet = p.phone.ifBlank { null },
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                            )
                        }
                    } else {
                        upas.forEach { u ->
                            Marker(
                                state = MarkerState(position = LatLng(u.lat, u.lon)),
                                title = u.name,
                                snippet = u.phone.ifBlank { "Toque para ver detalhes" },
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                            )
                        }
                    }
                }

                if (!mapLoaded) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            LaunchedEffect(mapLoaded) {
                if (!mapLoaded) {
                    kotlinx.coroutines.delay(20_000)
                    if (!mapLoaded) mapLoadError = true
                }
            }
        }
    }
}


@Composable
fun MapErrorFallback(
    modifier: Modifier = Modifier,
    latitude: Double,
    longitude: Double,
    onRetry: () -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Erro no mapa",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Erro ao carregar o mapa",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "O mapa não pôde ser carregado. Verifique sua conexão com a internet e tente novamente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Tentar Novamente")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Localização",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Localização Atual",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Latitude: ${String.format(Locale.ROOT, "%.6f", latitude)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Longitude: ${String.format(Locale.ROOT, "%.6f", longitude)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class PharmacyPlace(
    val name: String,
    val lat: Double,
    val lon: Double,
    val phone: String
)

data class UpaPlace(
    val name: String,
    val lat: Double,
    val lon: Double,
    val phone: String
)
