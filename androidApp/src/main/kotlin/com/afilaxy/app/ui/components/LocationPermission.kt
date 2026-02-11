package com.afilaxy.app.ui.components

import android.Manifest
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestLocationPermission(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit = {}
) {
    val foregroundPermissions = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    val foregroundState = rememberMultiplePermissionsState(permissions = foregroundPermissions)
    
    val backgroundState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberMultiplePermissionsState(permissions = listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
    } else null
    
    var showRationale by remember { mutableStateOf(false) }
    var requestingBackground by remember { mutableStateOf(false) }
    
    LaunchedEffect(foregroundState.allPermissionsGranted, backgroundState?.allPermissionsGranted) {
        if (foregroundState.allPermissionsGranted) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (backgroundState?.allPermissionsGranted == true) {
                    onPermissionGranted()
                } else if (!requestingBackground) {
                    requestingBackground = true
                }
            } else {
                onPermissionGranted()
            }
        }
    }
    
    LaunchedEffect(Unit) {
        if (!foregroundState.allPermissionsGranted) {
            if (foregroundState.permissions.any { it.status.shouldShowRationale }) {
                showRationale = true
            } else {
                foregroundState.launchMultiplePermissionRequest()
            }
        }
    }
    
    LaunchedEffect(requestingBackground) {
        if (requestingBackground && backgroundState != null) {
            backgroundState.launchMultiplePermissionRequest()
        }
    }
    
    if (showRationale) {
        AlertDialog(
            onDismissRequest = { 
                showRationale = false
                onPermissionDenied()
            },
            title = { Text("Permissão de Localização") },
            text = { 
                Text("O Afilaxy precisa acessar sua localização para encontrar helpers próximos e criar emergências. Para modo helper, é necessário acesso em segundo plano.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showRationale = false
                    foregroundState.launchMultiplePermissionRequest()
                }) {
                    Text("Permitir")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRationale = false
                    onPermissionDenied()
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
