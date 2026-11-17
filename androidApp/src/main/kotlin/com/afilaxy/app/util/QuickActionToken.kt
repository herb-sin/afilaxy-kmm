package com.afilaxy.app.util

import android.content.Context
import java.util.UUID

/**
 * Código aleatório por instalação que as ações "Sim" das notificações de check-in levam no
 * intent. A MainActivity é exportada (é a tela de entrada), então qualquer app instalado
 * consegue abri-la com open_screen=*_yes; sem este código, o "Sim" vindo de fora abre o
 * formulário normal em vez de enviar um check-in positivo sozinho. O código fica nas
 * preferências privadas do app, que outros apps não leem.
 */
object QuickActionToken {
    const val EXTRA = "quick_token"
    private const val PREFS = "afilaxy_notification_actions"
    private const val KEY = "quick_token"

    fun get(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY, null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString(KEY, it).apply()
        }
    }

    fun matches(context: Context, value: String?): Boolean =
        value != null && value == get(context)
}
