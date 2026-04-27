package com.afilaxy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.afilaxy.presentation.emergency.EmergencyViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import org.koin.compose.koinInject
import org.koin.androidx.compose.koinViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    pharmacyMode: Boolean = false,
    emergencyViewModel: EmergencyViewModel = koinViewModel()
) {
    val locationRepository: LocationRepository = koinInject()
    val emergencyState = emergencyViewModel.state.collectAsState().value

    // São Paulo como fallback — substituído pela localização real via LaunchedEffect
    val saoPaulo = LatLng(-23.5505, -46.6333)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(saoPaulo, 15f)
    }

    // Localização real do dispositivo
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var mapLoadError by remember { mutableStateOf(false) }
    var mapLoaded by remember { mutableStateOf(false) }
    var pharmacies by remember { mutableStateOf<List<PharmacyPlace>>(emptyList()) }
    var isLoadingPharmacies by remember { mutableStateOf(false) }

    // Busca a localização real e inicia observer de helpers próximos ao abrir
    LaunchedEffect(Unit) {
        val loc = locationRepository.getCurrentLocation()
        if (loc != null) {
            val realLatLng = LatLng(loc.latitude, loc.longitude)
            userLocation = realLatLng
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(realLatLng, 15f)
            )
            if (pharmacyMode) {
                isLoadingPharmacies = true
                try {
                    val query = "[out:json];node[\"amenity\"=\"pharmacy\"](around:5000,${loc.latitude},${loc.longitude});out 20;"
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
                        result.add(PharmacyPlace(
                            name = tags?.optString("name") ?: "Farmácia",
                            lat = el.getDouble("lat"),
                            lon = el.getDouble("lon"),
                            phone = tags?.optString("phone") ?: ""
                        ))
                    }
                    pharmacies = result
                } catch (_: Exception) {
                } finally {
                    isLoadingPharmacies = false
                }
            } else {
                emergencyViewModel.startObservingNearbyHelpers(loc.latitude, loc.longitude)
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
                    } else { Text("Mapa") }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    if (!pharmacyMode) {
                        val count = emergencyState.nearbyHelpers.size
                        if (count > 0) {
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text("$count")
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.LocationOn,
                                contentDescription = "$count ajudantes próximos",
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                        }
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
            Box(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapLoaded = { mapLoaded = true }
                ) {
                    // Marker do próprio usuário
                    val markerPos = userLocation ?: saoPaulo
                    Marker(
                        state = MarkerState(position = markerPos),
                        title = if (userLocation != null) "Você está aqui" else "Posição padrão (GPS indisponível)",
                        snippet = if (userLocation != null)
                            "${String.format(Locale.ROOT, "%.5f", markerPos.latitude)}, ${String.format(Locale.ROOT, "%.5f", markerPos.longitude)}"
                        else
                            "São Paulo, SP"
                    )

                    if (pharmacyMode) {
                        // Markers de farmácias
                        pharmacies.forEach { p ->
                            Marker(
                                state = MarkerState(position = LatLng(p.lat, p.lon)),
                                title = p.name,
                                snippet = p.phone.ifBlank { null },
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                            )
                        }
                    } else {
                        // Markers de helpers próximos
                        emergencyState.nearbyHelpers.forEach { helper ->
                            Marker(
                                state = MarkerState(
                                    position = LatLng(helper.latitude, helper.longitude)
                                ),
                                title = if (helper.name.isNotBlank()) helper.name else "Ajudante próximo",
                                snippet = if (helper.distance > 0)
                                    "${"%.1f".format(helper.distance)} km de distância"
                                else "Localização aproximada",
                                icon = BitmapDescriptorFactory.defaultMarker(
                                    BitmapDescriptorFactory.HUE_AZURE
                                )
                            )
                        }
                    }
                }

                // Loading overlay — some após 8s sem onMapLoaded → mapLoadError
                if (!mapLoaded) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Timeout: se o mapa não carregar em 8s, exibe o fallback de erro
            LaunchedEffect(mapLoaded) {
                if (!mapLoaded) {
                    kotlinx.coroutines.delay(8_000)
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
                    text = "Verifique se a API key do Google Maps está configurada corretamente no arquivo local.properties",
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
        
        // Location info card as fallback
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
