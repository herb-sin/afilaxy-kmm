package com.afilaxy.app.ui.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/**
 * Solicita permissões de localização seguindo o fluxo obrigatório do Android 10+:
 *
 * 1. Solicitar ACCESS_FINE_LOCATION + ACCESS_COARSE_LOCATION (foreground).
 * 2. SOMENTE após foreground concedido: exibir diálogo explicando por que
 *    "Permitir sempre" é necessário (Android não permite pedir ambos juntos).
 * 3. Após confirmação do usuário: solicitar ACCESS_BACKGROUND_LOCATION.
 * 4. Chamar [onPermissionGranted] apenas quando tudo estiver concedido.
 *
 * Se a permissão foi permanentemente negada, oferece "Abrir Configurações"
 * em vez de um novo request que o Android ignoraria.
 *
 * @param onPermissionGranted Chamado quando todas as permissões necessárias forem concedidas.
 * @param onPermissionDenied  Chamado quando o usuário recusa qualquer permissão.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestLocationPermission(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit = {}
) {
    val context = LocalContext.current

    // --- Passo 1: Permissões de foreground ---
    val foregroundState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // --- Passo 2: Permissão de background (Android 10+ apenas) ---
    val backgroundState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        rememberPermissionState(permission = Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else null

    // Verificar se a permissão foi permanentemente negada
    // (negada e shouldShowRationale == false significa que o usuário marcou "Não perguntar novamente")
    val isPermanentlyDenied = !foregroundState.allPermissionsGranted &&
        foregroundState.permissions.none { it.status.shouldShowRationale } &&
        foregroundState.permissions.any { !it.status.isGranted }

    // Estados de UI
    var showForegroundRationale by remember { mutableStateOf(false) }
    var showBackgroundRationale by remember { mutableStateOf(false) }
    var hasLaunchedRequest by remember { mutableStateOf(false) }

    // --- Efeito inicial: decide o que fazer com foreground ---
    LaunchedEffect(Unit) {
        when {
            foregroundState.allPermissionsGranted -> {
                // Foreground já concedido — verificar background
                handleBackgroundStep(backgroundState?.status?.isGranted, onPermissionGranted) {
                    showBackgroundRationale = true
                }
            }
            foregroundState.permissions.any { it.status.shouldShowRationale } -> {
                // Usuário negou antes — exibir rationale de foreground
                showForegroundRationale = true
            }
            isPermanentlyDenied -> {
                // Negado permanentemente — mostrar rationale com opção de configurações
                showForegroundRationale = true
            }
            else -> {
                // Primeira solicitação — pedir diretamente
                hasLaunchedRequest = true
                foregroundState.launchMultiplePermissionRequest()
            }
        }
    }

    // --- Após o request inicial, verificar resultado ---
    LaunchedEffect(foregroundState.allPermissionsGranted) {
        if (foregroundState.allPermissionsGranted) {
            handleBackgroundStep(backgroundState?.status?.isGranted, onPermissionGranted) {
                showBackgroundRationale = true
            }
        } else if (hasLaunchedRequest && !foregroundState.allPermissionsGranted) {
            // Request foi feito mas negado — mostrar rationale
            showForegroundRationale = true
        }
    }

    // --- Observar mudança no estado de background (Android 10+) ---
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        LaunchedEffect(backgroundState?.status?.isGranted) {
            if (foregroundState.allPermissionsGranted && backgroundState?.status?.isGranted == true) {
                onPermissionGranted()
            }
        }
    }

    // --- Diálogo de rationale para FOREGROUND location ---
    if (showForegroundRationale) {
        AlertDialog(
            onDismissRequest = {
                showForegroundRationale = false
                onPermissionDenied()
            },
            title = { Text("Permissão de Localização") },
            text = {
                Text(
                    if (isPermanentlyDenied)
                        "O Afilaxy precisa de acesso à localização para funcionar. " +
                        "Como a permissão foi negada anteriormente, abra as Configurações e ative manualmente."
                    else
                        "O Afilaxy precisa da sua localização para encontrar helpers " +
                        "próximos e criar emergências com precisão."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showForegroundRationale = false
                    if (isPermanentlyDenied) {
                        // Abrir configurações do app diretamente
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        )
                        onPermissionDenied()
                    } else {
                        hasLaunchedRequest = true
                        foregroundState.launchMultiplePermissionRequest()
                    }
                }) {
                    Text(if (isPermanentlyDenied) "Abrir Configurações" else "Permitir")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showForegroundRationale = false
                    onPermissionDenied()
                }) {
                    Text("Agora não")
                }
            }
        )
    }

    // --- Diálogo de rationale específico para BACKGROUND location (Android 10+) ---
    // Explica claramente por que "Permitir sempre" é necessário antes de pedir
    if (showBackgroundRationale && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        AlertDialog(
            onDismissRequest = {
                // Usuário dispensou — modo helper funciona, mas sem alertas em segundo plano
                showBackgroundRationale = false
                onPermissionGranted() // Conceder acesso foreground apenas
            },
            title = { Text("Localização em Segundo Plano") },
            text = {
                Text(
                    "Para receber alertas de emergência enquanto o app está fechado, " +
                    "o Afilaxy precisa de acesso à localização \"Permitir sempre\".\n\n" +
                    "Na próxima tela, selecione \"Permitir sempre\" nas configurações de localização."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBackgroundRationale = false
                    if (backgroundState?.status?.shouldShowRationale == false &&
                        backgroundState.status.isGranted == false) {
                        // Permanentemente negado — abrir configurações
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        )
                        onPermissionGranted() // Aceita apenas foreground por ora
                    } else {
                        backgroundState?.launchPermissionRequest()
                    }
                }) {
                    Text("Entendi, prosseguir")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackgroundRationale = false
                    onPermissionGranted() // Aceita apenas foreground
                }) {
                    Text("Usar sem segundo plano")
                }
            }
        )
    }
}

/** Verifica se o background já está concedido ou se precisa de rationale. */
private fun handleBackgroundStep(
    isBackgroundGranted: Boolean?,
    onGranted: () -> Unit,
    onNeedRationale: () -> Unit
) {
    when {
        isBackgroundGranted == null -> onGranted() // Android < 10: não precisa de background
        isBackgroundGranted -> onGranted()          // Já concedido anteriormente
        else -> onNeedRationale()                   // Precisa pedir
    }
}
