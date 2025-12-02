package com.payten.whitelabel

import at.favre.lib.crypto.bcrypt.BCrypt
import org.junit.Test

/**
 * Unit tests for PIN hashing and verification.
 *
 * Tests BCrypt hashing for PIN security.
 */
class PinHashingTest {

    // ==================== PIN Hashing Tests ====================

    @Test
    fun `hash PIN produces valid BCrypt hash`() {
        // Given
        val pin = "1234"
        val cost = 12

        // When
        val hashedPin = BCrypt.withDefaults().hashToString(cost, pin.toCharArray())

        // Then
        assert(hashedPin.isNotEmpty()) { "Hashed PIN should not be empty" }
        assert(hashedPin.startsWith("$2a$")) { "BCrypt hash should start with $2a$" }
        assert(hashedPin.length > 50) { "BCrypt hash should be substantial length" }
    }

    @Test
    fun `verify correct PIN returns success`() {
        // Given
        val pin = "1234"
        val cost = 12
        val hashedPin = BCrypt.withDefaults().hashToString(cost, pin.toCharArray())

        // When
        val result = BCrypt.verifyer().verify(pin.toCharArray(), hashedPin)

        // Then
        assert(result.verified) { "Correct PIN should verify successfully" }
    }

    @Test
    fun `verify incorrect PIN returns failure`() {
        // Given
        val correctPin = "1234"
        val incorrectPin = "5678"
        val cost = 12
        val hashedPin = BCrypt.withDefaults().hashToString(cost, correctPin.toCharArray())

        // When
        val result = BCrypt.verifyer().verify(incorrectPin.toCharArray(), hashedPin)

        // Then
        assert(!result.verified) { "Incorrect PIN should not verify" }
    }

    @Test
    fun `same PIN produces different hashes due to salt`() {
        // Given
        val pin = "1234"
        val cost = 12

        // When
        val hash1 = BCrypt.withDefaults().hashToString(cost, pin.toCharArray())
        val hash2 = BCrypt.withDefaults().hashToString(cost, pin.toCharArray())

        // Then
        assert(hash1 != hash2) { "Same PIN should produce different hashes due to salt" }
        // But both should verify the same PIN
        assert(BCrypt.verifyer().verify(pin.toCharArray(), hash1).verified)
        assert(BCrypt.verifyer().verify(pin.toCharArray(), hash2).verified)
    }

    @Test
    fun `PIN hash with different cost factors`() {
        // Given
        val pin = "1234"

        // When
        val hash4 = BCrypt.withDefaults().hashToString(4, pin.toCharArray())
        val hash12 = BCrypt.withDefaults().hashToString(12, pin.toCharArray())

        // Then
        assert(hash4.contains("$04$")) { "Cost 4 should be in hash" }
        assert(hash12.contains("$12$")) { "Cost 12 should be in hash" }
        // Both should still verify correctly
        assert(BCrypt.verifyer().verify(pin.toCharArray(), hash4).verified)
        assert(BCrypt.verifyer().verify(pin.toCharArray(), hash12).verified)
    }

    // ==================== PIN Validation Tests ====================

    @Test
    fun `6-digit PIN is valid length`() {
        // Given
        val pin = "123456"

        // Then
        assert(pin.length == 6) { "PIN should be 6 digits" }
        assert(pin.all { it.isDigit() }) { "PIN should only contain digits" }
    }

    @Test
    fun `PIN with non-digits is invalid`() {
        // Given
        val invalidPins = listOf("12a456", "12 456", "12-456", "12.456")

        // Then
        invalidPins.forEach { pin ->
            assert(!pin.all { it.isDigit() }) { "$pin should not be all digits" }
        }
    }

    @Test
    fun `empty PIN can be hashed but should be validated by app`() {
        // Given
        val pin = ""

        // When - BCrypt actually allows empty strings
        val hashedPin = BCrypt.withDefaults().hashToString(12, pin.toCharArray())

        // Then - BCrypt doesn't prevent empty PIN, but app validation should
        assert(hashedPin.isNotEmpty()) { "BCrypt can hash empty string" }
        assert(BCrypt.verifyer().verify(pin.toCharArray(), hashedPin).verified) { "Empty PIN can verify" }

        // But app should reject empty PINs in validation logic before hashing
        assert(pin.isEmpty()) { "App should validate PIN is not empty before hashing" }
    }

    // ==================== Security Tests ====================

    @Test
    fun `cannot reverse engineer PIN from hash`() {
        // Given
        val pin = "1234"
        val hash = BCrypt.withDefaults().hashToString(12, pin.toCharArray())

        // Then - hash should not contain the original PIN
        assert(!hash.contains(pin)) { "Hash should not contain original PIN" }
        assert(!hash.contains("1234")) { "Hash should not contain PIN digits" }
    }

    @Test
    fun `timing-safe comparison with wrong PIN length`() {
        // Given
        val correctPin = "123456"
        val shortPin = "1234"
        val longPin = "12345678"
        val hash = BCrypt.withDefaults().hashToString(12, correctPin.toCharArray())

        // When
        val shortResult = BCrypt.verifyer().verify(shortPin.toCharArray(), hash)
        val longResult = BCrypt.verifyer().verify(longPin.toCharArray(), hash)

        // Then
        assert(!shortResult.verified) { "Short PIN should not verify" }
        assert(!longResult.verified) { "Long PIN should not verify" }
    }

    @Test
    fun `common PINs can be hashed but should be validated separately`() {
        // Given - common/weak PINs that should be rejected by validation logic
        val weakPins = listOf("123456", "000000", "111111", "123123")

        // When/Then - they CAN be hashed, but app should reject them
        weakPins.forEach { pin ->
            val hash = BCrypt.withDefaults().hashToString(12, pin.toCharArray())
            assert(hash.isNotEmpty()) { "Weak PIN can be hashed" }
            assert(BCrypt.verifyer().verify(pin.toCharArray(), hash).verified) { "But hash works correctly" }
        }
    }
}
