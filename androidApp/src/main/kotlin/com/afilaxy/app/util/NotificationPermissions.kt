package com.afilaxy.app.util

import android.content.Context
import android.os.Build
import androidx.core.app.NotificationManagerCompat

/**
 * Retorna true se o app pode exibir notificações neste momento.
 *
 * Abaixo do Android 13 (TIRAMISU) a permissão POST_NOTIFICATIONS não existe e as
 * notificações vêm habilitadas por padrão; a partir do 13 ela precisa ser concedida
 * em runtime, senão NotificationManager.notify() é silenciosamente descartado.
 */
fun Context.canPostNotifications(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        NotificationManagerCompat.from(this).areNotificationsEnabled()
