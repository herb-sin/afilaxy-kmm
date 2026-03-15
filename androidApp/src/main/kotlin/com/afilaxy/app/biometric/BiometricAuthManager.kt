package com.afilaxy.app.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuthManager(private val context: Context) {
    
    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Autenticação Biométrica",
        subtitle: String = "Use sua digital ou face para continuar",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Autenticação falhou")
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(sanitizePromptText(title, maxLength = 64))
            .setSubtitle(sanitizePromptText(subtitle, maxLength = 128))
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Remove caracteres de controle XML/HTML e limita o tamanho para prevenir
     * injeção de conteúdo malicioso nos prompts biométricos (CWE-611).
     */
    private fun sanitizePromptText(input: String, maxLength: Int): String =
        input
            .replace(Regex("[\\x00-\\x1F\\x7F<>&\"']"), "")
            .trim()
            .take(maxLength)
            .ifEmpty { "Afilaxy" }
}
