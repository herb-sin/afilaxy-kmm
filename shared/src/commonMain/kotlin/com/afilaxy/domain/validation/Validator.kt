package com.afilaxy.domain.validation

object Validator {
    
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
        return password.length >= 8 &&
               password.any { it.isUpperCase() } &&
               password.any { it.isDigit() }
    }
    
    fun isValidName(name: String): Boolean {
        return name.isNotBlank() && name.length in 2..100
    }
    
    fun isValidPhone(phone: String): Boolean {
        val digitsOnly = phone.filter { it.isDigit() }
        return digitsOnly.length in 10..11
    }
    
    fun isValidMessage(message: String): Boolean {
        return message.isNotBlank() && message.length in 1..1000
    }
    
    fun sanitizeInput(input: String): String {
        return input.trim()
            .replace(Regex("<[^>]*>"), "")
            .take(1000)
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
