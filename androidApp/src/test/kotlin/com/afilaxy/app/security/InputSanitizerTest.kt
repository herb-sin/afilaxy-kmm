package com.afilaxy.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests para InputSanitizer (camada de segurança Android).
 * Executa como JVM local unit test (não requer dispositivo/emulador).
 */
class InputSanitizerTest {

    // ---- sanitizeEmail ------------------------------------------------------

    @Test
    fun `sanitizeEmail should return lowercase trimmed valid email`() {
        assertEquals("user@example.com", InputSanitizer.sanitizeEmail("  USER@EXAMPLE.COM  "))
    }

    @Test
    fun `sanitizeEmail should return empty string for null`() {
        assertEquals("", InputSanitizer.sanitizeEmail(null))
    }

    @Test
    fun `sanitizeEmail should return empty string for blank`() {
        assertEquals("", InputSanitizer.sanitizeEmail("   "))
    }

    @Test
    fun `sanitizeEmail should return empty string for invalid email`() {
        assertEquals("", InputSanitizer.sanitizeEmail("notanemail"))
        assertEquals("", InputSanitizer.sanitizeEmail("@domain.com"))
        assertEquals("", InputSanitizer.sanitizeEmail("user@"))
    }

    @Test
    fun `sanitizeEmail should accept valid email formats`() {
        assertEquals("user.name@sub.domain.co", InputSanitizer.sanitizeEmail("user.name@sub.domain.co"))
        assertEquals("valid123@test.org", InputSanitizer.sanitizeEmail("valid123@test.org"))
    }

    // ---- sanitizeName -------------------------------------------------------

    @Test
    fun `sanitizeName should return trimmed valid name`() {
        assertEquals("João Silva", InputSanitizer.sanitizeName("  João Silva  "))
    }

    @Test
    fun `sanitizeName should return empty for null`() {
        assertEquals("", InputSanitizer.sanitizeName(null))
    }

    @Test
    fun `sanitizeName should return empty for names with numbers`() {
        assertEquals("", InputSanitizer.sanitizeName("John123"))
    }

    @Test
    fun `sanitizeName should return empty for names exceeding 50 chars`() {
        assertEquals("", InputSanitizer.sanitizeName("A".repeat(51)))
    }

    // ---- sanitizeText -------------------------------------------------------

    @Test
    fun `sanitizeText should strip NOSQL operators`() {
        val dirty = "test \$where something"
        val clean = InputSanitizer.sanitizeText(dirty)
        assertFalse(clean.contains("\$where"))
    }

    @Test
    fun `sanitizeText should strip dangerous characters`() {
        val dirty = "hello <world> & \"test\""
        val clean = InputSanitizer.sanitizeText(dirty)
        assertFalse(clean.contains("<"))
        assertFalse(clean.contains(">"))
        assertFalse(clean.contains("\""))
        assertFalse(clean.contains("&"))
    }

    @Test
    fun `sanitizeText should truncate at 200 chars`() {
        val long = "a".repeat(300)
        val clean = InputSanitizer.sanitizeText(long)
        assertTrue(clean.length <= 200)
    }

    @Test
    fun `sanitizeText should return empty for null`() {
        assertEquals("", InputSanitizer.sanitizeText(null))
    }

    @Test
    fun `sanitizeText should return empty for blank`() {
        assertEquals("", InputSanitizer.sanitizeText("   "))
    }

    // ---- isValidEmail -------------------------------------------------------

    @Test
    fun `isValidEmail should return true for valid email`() {
        assertTrue(InputSanitizer.isValidEmail("valid@example.com"))
    }

    @Test
    fun `isValidEmail should return false for null`() {
        assertFalse(InputSanitizer.isValidEmail(null))
    }

    @Test
    fun `isValidEmail should return false for invalid email`() {
        assertFalse(InputSanitizer.isValidEmail("invalid"))
        assertFalse(InputSanitizer.isValidEmail(""))
    }

    // ---- isValidName --------------------------------------------------------

    @Test
    fun `isValidName should return true for valid name`() {
        assertTrue(InputSanitizer.isValidName("Ana"))
        assertTrue(InputSanitizer.isValidName("João Silva"))
    }

    @Test
    fun `isValidName should return false for null`() {
        assertFalse(InputSanitizer.isValidName(null))
    }

    @Test
    fun `isValidName should return false for name with numbers`() {
        assertFalse(InputSanitizer.isValidName("John123"))
    }

    @Test
    fun `isValidName should return false for name exceeding 50 chars`() {
        assertFalse(InputSanitizer.isValidName("A".repeat(51)))
    }
}
