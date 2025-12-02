package com.payten.whitelabel

import org.junit.Test

/**
 * Unit tests for card number masking.
 *
 * Tests PCI-DSS compliant card number masking for display.
 */
class CardNumberMaskingTest {

    // ==================== Basic Masking Tests ====================

    @Test
    fun `mask standard 16-digit card number`() {
        // Given
        val cardNumber = "1234567890123456"

        // When
        val masked = maskCardNumber(cardNumber)

        // Then
        assert(masked == "************3456") { "Expected ************3456 but got $masked" }
    }

    @Test
    fun `mask 15-digit AMEX card number`() {
        // Given
        val amexNumber = "123456789012345"

        // When
        val masked = maskCardNumber(amexNumber, visibleDigits = 4)

        // Then
        assert(masked == "***********2345") { "Expected ***********2345 but got $masked" }
    }

    @Test
    fun `mask with different visible digits`() {
        // Given
        val cardNumber = "1234567890123456"

        // When
        val masked4 = maskCardNumber(cardNumber, visibleDigits = 4)
        val masked6 = maskCardNumber(cardNumber, visibleDigits = 6)

        // Then
        assert(masked4 == "************3456") { "4 digits visible: $masked4" }
        assert(masked6 == "**********123456") { "6 digits visible: $masked6" }
    }

    // ==================== PCI-DSS Compliance Tests ====================

    @Test
    fun `masked card shows only last 4 digits`() {
        // Given - PCI-DSS requires showing at most first 6 and last 4
        val cardNumber = "5555444433332222"

        // When
        val masked = maskCardNumber(cardNumber, visibleDigits = 4)

        // Then
        assert(masked.takeLast(4) == "2222") { "Last 4 digits should be visible" }
        assert(masked.count { it == '*' } == 12) { "Should have 12 masked digits" }
    }

    @Test
    fun `masked card does not expose middle digits`() {
        // Given
        val cardNumber = "4111111111111111" // Test Visa

        // When
        val masked = maskCardNumber(cardNumber)

        // Then
        assert(!masked.contains("11111111")) { "Should not contain consecutive middle digits" }
        assert(masked.contains("****")) { "Should contain masking characters" }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `mask card number shorter than visible digits`() {
        // Given
        val shortNumber = "123"

        // When
        val masked = maskCardNumber(shortNumber, visibleDigits = 4)

        // Then - should show all digits if shorter than visible count
        assert(masked == "123" || masked == "***") { "Short number handling: $masked" }
    }

    @Test
    fun `mask empty card number`() {
        // Given
        val emptyNumber = ""

        // When
        val masked = maskCardNumber(emptyNumber)

        // Then
        assert(masked == "") { "Empty number should return empty string" }
    }

    @Test
    fun `mask card number with spaces`() {
        // Given
        val cardWithSpaces = "1234 5678 9012 3456"

        // When - remove spaces first
        val cleaned = cardWithSpaces.replace(" ", "")
        val masked = maskCardNumber(cleaned)

        // Then
        assert(masked == "************3456") { "Expected ************3456 but got $masked" }
    }

    @Test
    fun `mask card number with dashes`() {
        // Given
        val cardWithDashes = "1234-5678-9012-3456"

        // When - remove dashes first
        val cleaned = cardWithDashes.replace("-", "")
        val masked = maskCardNumber(cleaned)

        // Then
        assert(masked == "************3456") { "Expected ************3456 but got $masked" }
    }

    // ==================== Display Format Tests ====================

    @Test
    fun `format masked card with spaces for display`() {
        // Given
        val cardNumber = "1234567890123456"

        // When
        val masked = maskCardNumber(cardNumber)
        val formatted = formatMaskedCard(masked)

        // Then
        assert(formatted.contains(" ")) { "Should have spaces for readability" }
        assert(formatted == "**** **** **** 3456") { "Expected **** **** **** 3456 but got $formatted" }
    }

    @Test
    fun `masked card preserves card type identification`() {
        // Given
        val visaNumber = "4111111111111111"
        val mastercardNumber = "5555444433332222"

        // When - show first 1 digit + last 4
        val visaMasked = maskCardNumber(visaNumber, showFirst = 1, visibleDigits = 4)
        val mcMasked = maskCardNumber(mastercardNumber, showFirst = 1, visibleDigits = 4)

        // Then
        assert(visaMasked.startsWith("4")) { "Visa starts with 4: $visaMasked" }
        assert(mcMasked.startsWith("5")) { "Mastercard starts with 5: $mcMasked" }
    }

    // ==================== Helper Functions ====================

    private fun maskCardNumber(
        cardNumber: String,
        visibleDigits: Int = 4,
        showFirst: Int = 0,
        maskChar: Char = '*'
    ): String {
        if (cardNumber.length <= visibleDigits) return cardNumber

        val firstPart = if (showFirst > 0) cardNumber.take(showFirst) else ""
        val maskedPart = maskChar.toString().repeat(
            (cardNumber.length - visibleDigits - showFirst).coerceAtLeast(0)
        )
        val lastPart = cardNumber.takeLast(visibleDigits)

        return firstPart + maskedPart + lastPart
    }

    private fun formatMaskedCard(masked: String): String {
        // Format as groups of 4
        return masked.chunked(4).joinToString(" ")
    }

    // ==================== Security Tests ====================

    @Test
    fun `masked card length matches original`() {
        // Given
        val cardNumber = "1234567890123456"

        // When
        val masked = maskCardNumber(cardNumber)

        // Then
        assert(masked.length == cardNumber.length) {
            "Masked length should match original: ${masked.length} vs ${cardNumber.length}"
        }
    }

    @Test
    fun `multiple maskings of same card produce same result`() {
        // Given
        val cardNumber = "1234567890123456"

        // When
        val masked1 = maskCardNumber(cardNumber)
        val masked2 = maskCardNumber(cardNumber)

        // Then
        assert(masked1 == masked2) { "Masking should be deterministic" }
    }

    @Test
    fun `different cards produce different masked outputs`() {
        // Given
        val card1 = "1234567890123456"
        val card2 = "1234567890129999"

        // When
        val masked1 = maskCardNumber(card1)
        val masked2 = maskCardNumber(card2)

        // Then
        assert(masked1 != masked2) { "Different cards should mask differently" }
        assert(masked1.endsWith("3456"))
        assert(masked2.endsWith("9999"))
    }
}
