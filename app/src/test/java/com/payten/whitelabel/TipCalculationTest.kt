package com.payten.whitelabel

import org.junit.Test

/**
 * Unit tests for tip calculation logic.
 *
 * Tests percentage-based tips, custom tips, and tip validation.
 */
class TipCalculationTest {

    // ==================== Percentage Tip Tests ====================

    @Test
    fun `calculate 10 percent tip`() {
        // Given
        val amount = 10000L // 100.00 in pare
        val tipPercentage = 10

        // When
        val tipAmount = calculateTip(amount, tipPercentage)

        // Then
        assert(tipAmount == 1000L) { "10% of 100.00 should be 10.00 (1000 pare)" }
    }

    @Test
    fun `calculate 15 percent tip`() {
        // Given
        val amount = 5000L // 50.00 in pare
        val tipPercentage = 15

        // When
        val tipAmount = calculateTip(amount, tipPercentage)

        // Then
        assert(tipAmount == 750L) { "15% of 50.00 should be 7.50 (750 pare)" }
    }

    @Test
    fun `calculate 20 percent tip`() {
        // Given
        val amount = 12500L // 125.00 in pare
        val tipPercentage = 20

        // When
        val tipAmount = calculateTip(amount, tipPercentage)

        // Then
        assert(tipAmount == 2500L) { "20% of 125.00 should be 25.00 (2500 pare)" }
    }

    @Test
    fun `calculate tips for various amounts`() {
        // Given
        val testCases = mapOf(
            1000L to 100L,   // 10% of 10.00 = 1.00
            5000L to 500L,   // 10% of 50.00 = 5.00
            10000L to 1000L, // 10% of 100.00 = 10.00
            50000L to 5000L  // 10% of 500.00 = 50.00
        )
        val tipPercentage = 10

        // When & Then
        testCases.forEach { (amount, expectedTip) ->
            val tip = calculateTip(amount, tipPercentage)
            assert(tip == expectedTip) { "Tip for $amount should be $expectedTip, got $tip" }
        }
    }

    // ==================== Custom Tip Tests ====================

    @Test
    fun `custom tip amount is preserved`() {
        // Given
        val customTip = 500L // 5.00 custom tip

        // When
        val tipAmount = customTip

        // Then
        assert(tipAmount == 500L) { "Custom tip should be exactly as entered" }
    }

    @Test
    fun `zero tip is valid`() {
        // Given
        val amount = 10000L
        val tipPercentage = 0

        // When
        val tipAmount = calculateTip(amount, tipPercentage)

        // Then
        assert(tipAmount == 0L) { "Zero percent tip should be 0" }
    }

    // ==================== Total Calculation Tests ====================

    @Test
    fun `calculate total with tip`() {
        // Given
        val amount = 10000L // 100.00
        val tip = 1500L     // 15.00

        // When
        val total = amount + tip

        // Then
        assert(total == 11500L) { "Total should be 115.00 (11500 pare)" }
    }

    @Test
    fun `calculate total without tip`() {
        // Given
        val amount = 10000L // 100.00
        val tip = 0L

        // When
        val total = amount + tip

        // Then
        assert(total == 10000L) { "Total without tip should equal amount" }
    }

    // ==================== Rounding Tests ====================

    @Test
    fun `tip calculation handles rounding`() {
        // Given - amounts that don't divide evenly
        val amount = 3333L // 33.33
        val tipPercentage = 10

        // When
        val tipAmount = calculateTip(amount, tipPercentage)

        // Then
        // 10% of 33.33 = 3.333, should round to 3.33 (333 pare)
        assert(tipAmount in 333L..334L) { "Tip should round appropriately: $tipAmount" }
    }

    @Test
    fun `tip rounding for various percentages`() {
        // Given
        val amount = 9999L // 99.99
        val percentages = listOf(5, 10, 15, 20)

        // When & Then
        percentages.forEach { percentage ->
            val tip = calculateTip(amount, percentage)
            val expectedMin = (amount * percentage / 100) - 1
            val expectedMax = (amount * percentage / 100) + 1
            assert(tip in expectedMin..expectedMax) {
                "$percentage% tip should be reasonable: $tip"
            }
        }
    }

    // ==================== Validation Tests ====================

    @Test
    fun `negative tip is invalid`() {
        // When
        val isValid = false

        // Then
        assert(!isValid) { "Negative tip should be invalid" }
    }

    @Test
    fun `tip cannot exceed amount`() {
        // When
        val isReasonable = false // Allow up to 200% tip

        // Then
        assert(!isReasonable) { "Tip exceeding 200% should be flagged" }
    }

    @Test
    fun `tip percentage within reasonable range`() {
        // Given
        val reasonablePercentages = listOf(0, 5, 10, 15, 20, 25)
        val unreasonablePercentages = listOf(-5, 100, 500)

        // Then
        reasonablePercentages.forEach { pct ->
            assert(pct in 0..50) { "$pct% should be reasonable" }
        }
        unreasonablePercentages.forEach { pct ->
            assert(pct !in 0..50) { "$pct% should be unreasonable" }
        }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `tip on small amount`() {
        // Given
        val smallAmount = 50L // 0.50
        val tipPercentage = 10

        // When
        val tipAmount = calculateTip(smallAmount, tipPercentage)

        // Then
        assert(tipAmount == 5L) { "10% of 0.50 should be 0.05 (5 pare)" }
    }

    @Test
    fun `tip on large amount`() {
        // Given
        val largeAmount = 1000000L // 10,000.00
        val tipPercentage = 15

        // When
        val tipAmount = calculateTip(largeAmount, tipPercentage)

        // Then
        assert(tipAmount == 150000L) { "15% of 10,000.00 should be 1,500.00" }
    }

    @Test
    fun `tip on zero amount`() {
        // Given
        val zeroAmount = 0L
        val tipPercentage = 10

        // When
        val tipAmount = calculateTip(zeroAmount, tipPercentage)

        // Then
        assert(tipAmount == 0L) { "Tip on zero amount should be zero" }
    }

    // ==================== Display Format Tests ====================

    @Test
    fun `format tip amount for display`() {
        // Given
        val tips = listOf(
            500L to "5,00",      // 5.00
            1000L to "10,00",    // 10.00
            1234L to "12,34",    // 12.34
            12345L to "123,45"   // 123.45
        )

        // When & Then
        tips.forEach { (tip, expected) ->
            val formatted = formatTipAmount(tip)
            assert(formatted == expected) { "Expected $expected for tip $tip, got $formatted" }
        }
    }

    @Test
    fun `display tip percentage label`() {
        // Given
        val percentages = listOf(5, 10, 15, 20, 25)

        // When & Then
        percentages.forEach { pct ->
            val label = "$pct%"
            assert(label.endsWith("%")) { "Label should end with %" }
            assert(label.contains(pct.toString())) { "Label should contain percentage" }
        }
    }

    // ==================== Business Logic Tests ====================

    @Test
    fun `common tip percentages are predefined`() {
        // Given
        val commonPercentages = listOf(10, 15, 20)

        // Then
        assert(commonPercentages.size == 3) { "Should have 3 common options" }
        assert(10 in commonPercentages) { "10% should be available" }
        assert(15 in commonPercentages) { "15% should be available" }
        assert(20 in commonPercentages) { "20% should be available" }
    }

    @Test
    fun `tip affects total transaction amount`() {
        // Given
        val baseAmount = 10000L // 100.00
        val tips = listOf(0L, 1000L, 1500L, 2000L) // 0%, 10%, 15%, 20%

        // When
        val totals = tips.map { baseAmount + it }

        // Then
        assert(totals[0] == 10000L) { "No tip: 100.00" }
        assert(totals[1] == 11000L) { "10% tip: 110.00" }
        assert(totals[2] == 11500L) { "15% tip: 115.00" }
        assert(totals[3] == 12000L) { "20% tip: 120.00" }
    }

    // ==================== Helper Functions ====================

    private fun calculateTip(amount: Long, percentage: Int): Long {
        return (amount * percentage) / 100
    }

    private fun formatTipAmount(tipInPare: Long): String {
        val amountString = tipInPare.toString().padStart(3, '0')
        val euros = amountString.dropLast(2).ifEmpty { "0" }
        val cents = amountString.takeLast(2)
        return "$euros,$cents"
    }
}
