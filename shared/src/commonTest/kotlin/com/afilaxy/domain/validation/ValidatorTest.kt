package com.afilaxy.domain.validation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class ValidatorTest {

    // ---- isValidEmail --------------------------------------------------------

    @Test
    fun `isValidEmail should accept valid emails`() {
        assertTrue(Validator.isValidEmail("user@example.com"))
        assertTrue(Validator.isValidEmail("user.name+tag@domain.org"))
        assertTrue(Validator.isValidEmail("user_123@sub.domain.co"))
    }

    @Test
    fun `isValidEmail should reject invalid emails`() {
        assertFalse(Validator.isValidEmail(""))
        assertFalse(Validator.isValidEmail("   "))
        assertFalse(Validator.isValidEmail("notanemail"))
        assertFalse(Validator.isValidEmail("@domain.com"))
        assertFalse(Validator.isValidEmail("user@"))
        assertFalse(Validator.isValidEmail("user@domain"))
    }

    // ---- isValidPassword -----------------------------------------------------

    @Test
    fun `isValidPassword should accept strong passwords`() {
        assertTrue(Validator.isValidPassword("Password1"))
        assertTrue(Validator.isValidPassword("Str0ngP@ss"))
        assertTrue(Validator.isValidPassword("UPPER1lower"))
    }

    @Test
    fun `isValidPassword should reject weak passwords`() {
        assertFalse(Validator.isValidPassword(""))
        assertFalse(Validator.isValidPassword("short1A"))      // < 8 chars
        assertFalse(Validator.isValidPassword("alllowercase")) // no uppercase and no digit
        assertFalse(Validator.isValidPassword("NoDigitHere"))  // no digit
        assertFalse(Validator.isValidPassword("12345678"))     // no uppercase
    }

    @Test
    fun `isValidPassword boundary - exactly 8 chars with upper and digit`() {
        assertTrue(Validator.isValidPassword("Passw0rd"))
        assertFalse(Validator.isValidPassword("Passw0r")) // 7 chars
    }

    // ---- isValidName --------------------------------------------------------

    @Test
    fun `isValidName should accept valid names`() {
        assertTrue(Validator.isValidName("João"))
        assertTrue(Validator.isValidName("Ana Silva"))
        assertTrue(Validator.isValidName("AB"))
    }

    @Test
    fun `isValidName should reject invalid names`() {
        assertFalse(Validator.isValidName(""))
        assertFalse(Validator.isValidName("  "))
        assertFalse(Validator.isValidName("A"))             // too short
        assertFalse(Validator.isValidName("A".repeat(101))) // too long
    }

    // ---- isValidPhone -------------------------------------------------------

    @Test
    fun `isValidPhone should accept 10 or 11 digit phones`() {
        assertTrue(Validator.isValidPhone("1234567890"))      // 10 digits
        assertTrue(Validator.isValidPhone("12345678901"))     // 11 digits
        assertTrue(Validator.isValidPhone("(11) 9 1234-5678")) // formatted
    }

    @Test
    fun `isValidPhone should reject invalid phones`() {
        assertFalse(Validator.isValidPhone(""))
        assertFalse(Validator.isValidPhone("123456789"))       // 9 digits
        assertFalse(Validator.isValidPhone("123456789012"))    // 12 digits
    }

    // ---- isValidMessage ----------------------------------------------------

    @Test
    fun `isValidMessage should accept valid messages`() {
        assertTrue(Validator.isValidMessage("Hello!"))
        assertTrue(Validator.isValidMessage("A"))
        assertTrue(Validator.isValidMessage("A".repeat(1000)))
    }

    @Test
    fun `isValidMessage should reject invalid messages`() {
        assertFalse(Validator.isValidMessage(""))
        assertFalse(Validator.isValidMessage("   "))
        assertFalse(Validator.isValidMessage("A".repeat(1001)))
    }

    // ---- sanitizeInput -----------------------------------------------------

    @Test
    fun `sanitizeInput should trim whitespace`() {
        assertEquals("hello", Validator.sanitizeInput("  hello  "))
    }

    @Test
    fun `sanitizeInput should strip HTML tags`() {
        // Tags são removidas, mas o texto interno permanece
        assertEquals("hello world", Validator.sanitizeInput("<b>hello</b> world"))
        assertEquals("alert('xss')", Validator.sanitizeInput("<script>alert('xss')</script>"))
    }

    @Test
    fun `sanitizeInput should truncate at 1000 chars`() {
        val long = "A".repeat(2000)
        assertEquals(1000, Validator.sanitizeInput(long).length)
    }

    // ---- extension functions -----------------------------------------------

    @Test
    fun `String extension isValidEmail should work`() {
        assertTrue("test@example.com".isValidEmail())
        assertFalse("invalid".isValidEmail())
    }

    @Test
    fun `String extension isValidPassword should work`() {
        assertTrue("Secur3Pass".isValidPassword())
        assertFalse("weak".isValidPassword())
    }

    @Test
    fun `String extension sanitize should work`() {
        assertEquals("hello", "  <b>hello</b>  ".sanitize())
    }
}
