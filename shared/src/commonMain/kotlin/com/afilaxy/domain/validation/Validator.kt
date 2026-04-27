package com.afilaxy.domain.validation

object Validator {
    private const val MIN_PASSWORD_LENGTH = 8
    private const val MIN_NAME_LENGTH = 2
    private const val MAX_NAME_LENGTH = 100
    private const val MIN_PHONE_DIGITS = 10
    private const val MAX_PHONE_DIGITS = 11
    private const val MAX_INPUT_LENGTH = 1000
    private const val MAX_MESSAGE_LENGTH = 1000

    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return emailRegex.matches(email)
    }

    /**
     * Valida a senha com política segura:
     * - Mínimo 8 caracteres
     * - Pelo menos 1 letra maiúscula
     * - Pelo menos 1 dígito
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= MIN_PASSWORD_LENGTH &&
               password.any { it.isUpperCase() } &&
               password.any { it.isDigit() }
    }

    fun isValidName(name: String): Boolean {
        return name.isNotBlank() && name.length in MIN_NAME_LENGTH..MAX_NAME_LENGTH
    }

    fun isValidPhone(phone: String): Boolean {
        val digitsOnly = phone.filter { it.isDigit() }
        return digitsOnly.length in MIN_PHONE_DIGITS..MAX_PHONE_DIGITS
    }

    fun isValidMessage(message: String): Boolean {
        return message.isNotBlank() && message.length in 1..MAX_MESSAGE_LENGTH
    }

    fun sanitizeInput(input: String): String {
        return input.trim()
            .replace(Regex("<[^>]*>"), "")   // remove tags HTML/XML
            .replace(Regex("[\\r\\n\\t]"), " ") // previne log injection (CWE-117)
            .replace(Regex("[\\x00-\\x1F\\x7F]"), "") // remove demais caracteres de controle
            .take(MAX_INPUT_LENGTH)
    }
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

fun String.isValidEmail() = Validator.isValidEmail(this)
fun String.isValidPassword() = Validator.isValidPassword(this)
fun String.isValidName() = Validator.isValidName(this)
fun String.sanitize() = Validator.sanitizeInput(this)
