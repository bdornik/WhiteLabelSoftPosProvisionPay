package com.payten.whitelabel

import org.junit.Test
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Unit tests for currency formatting.
 *
 * Tests European currency format (1.234,56) used in the application.
 */
class CurrencyFormattingTest {

    // ==================== European Format Tests ====================

    @Test
    fun `format currency with European locale`() {
        // Given
        val amount = 1234.56

        // When
        val symbols = DecimalFormatSymbols(Locale.GERMANY) // Uses comma for decimal, dot for thousands
        val formatter = DecimalFormat("#,##0.00", symbols)
        val formatted = formatter.format(amount)

        // Then
        assert(formatted == "1.234,56") { "Expected 1.234,56 but got $formatted" }
    }

    @Test
    fun `format small amounts`() {
        // Given
        val testCases = mapOf(
            0.50 to "0,50",
            1.00 to "1,00",
            9.99 to "9,99",
            10.00 to "10,00"
        )

        val symbols = DecimalFormatSymbols(Locale.GERMANY)
        val formatter = DecimalFormat("#,##0.00", symbols)

        // When & Then
        testCases.forEach { (amount, expected) ->
            val formatted = formatter.format(amount)
            assert(formatted == expected) { "Expected $expected for $amount, got $formatted" }
        }
    }

    @Test
    fun `format large amounts with thousand separators`() {
        // Given
        val testCases = mapOf(
            1000.00 to "1.000,00",
            10000.00 to "10.000,00",
            100000.00 to "100.000,00",
            1000000.00 to "1.000.000,00"
        )

        val symbols = DecimalFormatSymbols(Locale.GERMANY)
        val formatter = DecimalFormat("#,##0.00", symbols)

        // When & Then
        testCases.forEach { (amount, expected) ->
            val formatted = formatter.format(amount)
            assert(formatted == expected) { "Expected $expected for $amount, got $formatted" }
        }
    }

    @Test
    fun `format zero amount`() {
        // Given
        val amount = 0.0

        // When
        val symbols = DecimalFormatSymbols(Locale.GERMANY)
        val formatter = DecimalFormat("#,##0.00", symbols)
        val formatted = formatter.format(amount)

        // Then
        assert(formatted == "0,00") { "Zero should format as 0,00" }
    }

    // ==================== Precision Tests ====================

    @Test
    fun `format preserves two decimal places`() {
        // Given
        val amounts = listOf(1.0, 1.5, 1.10, 1.99, 100.0)

        val symbols = DecimalFormatSymbols(Locale.GERMANY)
        val formatter = DecimalFormat("#,##0.00", symbols)

        // When & Then
        amounts.forEach { amount ->
            val formatted = formatter.format(amount)
            val decimalPart = formatted.substringAfter(',')
            assert(decimalPart.length == 2) {
                "Should have 2 decimal places, got $formatted"
            }
        }
    }

    @Test
    fun `format rounds to two decimal places`() {
        // Given
        val testCases = mapOf(
            1.234 to "1,23",
            1.235 to "1,24", // Rounds up
            1.236 to "1,24",
            9.999 to "10,00"
        )

        val symbols = DecimalFormatSymbols(Locale.GERMANY)
        val formatter = DecimalFormat("#,##0.00", symbols)

        // When & Then
        testCases.forEach { (amount, expected) ->
            val formatted = formatter.format(amount)
            assert(formatted == expected) { "Expected $expected for $amount, got $formatted" }
        }
    }

    // ==================== Negative Amount Tests ====================

    @Test
    fun `format negative amounts for refunds`() {
        // Given
        val testCases = mapOf(
            -1.00 to "-1,00",
            -10.50 to "-10,50",
            -100.00 to "-100,00",
            -1234.56 to "-1.234,56"
        )

        val symbols = DecimalFormatSymbols(Locale.GERMANY)
        val formatter = DecimalFormat("#,##0.00", symbols)

        // When & Then
        testCases.forEach { (amount, expected) ->
            val formatted = formatter.format(amount)
            assert(formatted == expected) { "Expected $expected for $amount, got $formatted" }
        }
    }

    // ==================== Parsing Tests ====================

    @Test
    fun `parse European formatted amount`() {
        // Given
        val formattedAmount = "1.234,56"

        // When
        val symbols = DecimalFormatSymbols(Locale.GERMANY)
        val formatter = DecimalFormat("#,##0.00", symbols)
        val parsed = formatter.parse(formattedAmount)?.toDouble()

        // Then
        assert(parsed == 1234.56) { "Expected 1234.56 but got $parsed" }
    }

    @Test
    fun `parse amounts without thousand separators`() {
        // Given
        val formattedAmount = "123,45"

        // When
        val symbols = DecimalFormatSymbols(Locale.GERMANY)
        val formatter = DecimalFormat("#,##0.00", symbols)
        val parsed = formatter.parse(formattedAmount)?.toDouble()

        // Then
        assert(parsed == 123.45) { "Expected 123.45 but got $parsed" }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `format very small amounts`() {
        // Given
        val testCases = mapOf(
            0.01 to "0,01",
            0.09 to "0,09",
            0.10 to "0,10"
        )

        val symbols = DecimalFormatSymbols(Locale.GERMANY)
        val formatter = DecimalFormat("#,##0.00", symbols)

        // When & Then
        testCases.forEach { (amount, expected) ->
            val formatted = formatter.format(amount)
            assert(formatted == expected) { "Expected $expected for $amount, got $formatted" }
        }
    }

    @Test
    fun `format maximum credit card amount`() {
        // Given - typical max is around 999,999.99
        val maxAmount = 999999.99

        // When
        val symbols = DecimalFormatSymbols(Locale.GERMANY)
        val formatter = DecimalFormat("#,##0.00", symbols)
        val formatted = formatter.format(maxAmount)

        // Then
        assert(formatted == "999.999,99") { "Expected 999.999,99 but got $formatted" }
    }

    @Test
    fun `different locales use different separators`() {
        // Given
        val amount = 1234.56

        // When - US format
        val usSymbols = DecimalFormatSymbols(Locale.US)
        val usFormatter = DecimalFormat("#,##0.00", usSymbols)
        val usFormatted = usFormatter.format(amount)

        // When - German/European format
        val deSymbols = DecimalFormatSymbols(Locale.GERMANY)
        val deFormatter = DecimalFormat("#,##0.00", deSymbols)
        val deFormatted = deFormatter.format(amount)

        // Then
        assert(usFormatted == "1,234.56") { "US format uses dot for decimal" }
        assert(deFormatted == "1.234,56") { "European format uses comma for decimal" }
        assert(usFormatted != deFormatted) { "Formats should be different" }
    }
}
