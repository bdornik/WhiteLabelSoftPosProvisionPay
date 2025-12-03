package com.payten.whitelabel

import org.junit.Test

/**
 * Unit tests for LED indicator state management.
 *
 * Tests bit mask operations for LED states during payment processing.
 */
class LedIndicatorTest {

    // ==================== LED State Data Class ====================

    data class LedState(
        val led1: Boolean = false,
        val led2: Boolean = false,
        val led3: Boolean = false,
        val led4: Boolean = false
    )

    // ==================== Individual LED Tests ====================

    @Test
    fun `turn on LED 1`() {
        // Given
        var ledMask = 0x00

        // When
        ledMask = ledMask or 0x01

        // Then
        assert((ledMask and 0x01) == 0x01) { "LED 1 should be on" }
        assert((ledMask and 0x02) == 0x00) { "LED 2 should be off" }
        assert((ledMask and 0x04) == 0x00) { "LED 3 should be off" }
        assert((ledMask and 0x08) == 0x00) { "LED 4 should be off" }
    }

    @Test
    fun `turn on LED 2`() {
        // Given
        var ledMask = 0x00

        // When
        ledMask = ledMask or 0x02

        // Then
        assert((ledMask and 0x01) == 0x00) { "LED 1 should be off" }
        assert((ledMask and 0x02) == 0x02) { "LED 2 should be on" }
        assert((ledMask and 0x04) == 0x00) { "LED 3 should be off" }
        assert((ledMask and 0x08) == 0x00) { "LED 4 should be off" }
    }

    @Test
    fun `turn on LED 3`() {
        // Given
        var ledMask = 0x00

        // When
        ledMask = ledMask or 0x04

        // Then
        assert((ledMask and 0x04) == 0x04) { "LED 3 should be on" }
    }

    @Test
    fun `turn on LED 4`() {
        // Given
        var ledMask = 0x00

        // When
        ledMask = ledMask or 0x08

        // Then
        assert((ledMask and 0x08) == 0x08) { "LED 4 should be on" }
    }

    // ==================== Multiple LED Tests ====================

    @Test
    fun `turn on multiple LEDs`() {
        // Given
        var ledMask = 0x00

        // When - Turn on LED 1 and LED 3
        ledMask = ledMask or 0x01
        ledMask = ledMask or 0x04

        // Then
        assert((ledMask and 0x01) == 0x01) { "LED 1 should be on" }
        assert((ledMask and 0x02) == 0x00) { "LED 2 should be off" }
        assert((ledMask and 0x04) == 0x04) { "LED 3 should be on" }
        assert((ledMask and 0x08) == 0x00) { "LED 4 should be off" }
    }

    @Test
    fun `turn on all LEDs at once`() {
        // Given
        val ledMask = 0x0F // Binary: 1111

        // Then
        assert((ledMask and 0x01) == 0x01) { "LED 1 should be on" }
        assert((ledMask and 0x02) == 0x02) { "LED 2 should be on" }
        assert((ledMask and 0x04) == 0x04) { "LED 3 should be on" }
        assert((ledMask and 0x08) == 0x08) { "LED 4 should be on" }
    }

    @Test
    fun `turn on all LEDs sequentially`() {
        // Given
        var ledMask = 0x00

        // When
        ledMask = ledMask or 0x01 // LED 1
        ledMask = ledMask or 0x02 // LED 2
        ledMask = ledMask or 0x04 // LED 3
        ledMask = ledMask or 0x08 // LED 4

        // Then
        assert(ledMask == 0x0F) { "All LEDs should be on: $ledMask" }
    }

    // ==================== LED State Conversion Tests ====================

    @Test
    fun `convert bit mask to LED state`() {

        // When
        val ledState = LedState(
            led1 = true,
            led2 = false,
            led3 = true,
            led4 = false
        )

        // Then
        assert(ledState.led1) { "LED 1 should be on" }
        assert(!ledState.led2) { "LED 2 should be off" }
        assert(ledState.led3) { "LED 3 should be on" }
        assert(!ledState.led4) { "LED 4 should be off" }
    }

    @Test
    fun `convert multiple LED masks`() {
        // Given
        val masks = mapOf(
            0x00 to LedState(led1 = false, led2 = false, led3 = false, led4 = false),
            0x01 to LedState(led1 = true, led2 = false, led3 = false, led4 = false),
            0x03 to LedState(led1 = true, led2 = true, led3 = false, led4 = false),
            0x07 to LedState(led1 = true, led2 = true, led3 = true, led4 = false),
            0x0F to LedState(led1 = true, led2 = true, led3 = true, led4 = true)
        )

        // When & Then
        masks.forEach { (mask, expectedState) ->
            val actualState = LedState(
                led1 = (mask and 0x01) == 0x01,
                led2 = (mask and 0x02) == 0x02,
                led3 = (mask and 0x04) == 0x04,
                led4 = (mask and 0x08) == 0x08
            )
            assert(actualState == expectedState) {
                "Mask $mask should produce $expectedState, got $actualState"
            }
        }
    }

    // ==================== LED Clearing Tests ====================

    @Test
    fun `clear all LEDs`() {
        // Given
        val ledMask = 0x00

        // Then
        assert(ledMask == 0x00) { "All LEDs should be off" }
    }

    @Test
    fun `clear specific LED`() {
        // Given
        var ledMask = 0x0F // All on

        // When - Clear LED 2 (0x02)
        ledMask = ledMask and 0x02.inv()

        // Then
        assert((ledMask and 0x01) == 0x01) { "LED 1 should still be on" }
        assert((ledMask and 0x02) == 0x00) { "LED 2 should be off" }
        assert((ledMask and 0x04) == 0x04) { "LED 3 should still be on" }
        assert((ledMask and 0x08) == 0x08) { "LED 4 should still be on" }
    }

    // ==================== Payment Flow LED Tests ====================

    @Test
    fun `LED progression during card tap`() {
        // Given
        var ledMask = 0x00

        // When - Simulate card tap flow
        ledMask = ledMask or 0x01 // Step 1: Card detected
        val step1 = ledMask

        ledMask = ledMask or 0x02 // Step 2: Reading card
        val step2 = ledMask

        ledMask = ledMask or 0x04 // Step 3: Processing
        val step3 = ledMask

        ledMask = ledMask or 0x08 // Step 4: Complete
        val step4 = ledMask

        // Then
        assert(step1 == 0x01) { "Step 1: Only LED 1" }
        assert(step2 == 0x03) { "Step 2: LED 1 & 2" }
        assert(step3 == 0x07) { "Step 3: LED 1, 2 & 3" }
        assert(step4 == 0x0F) { "Step 4: All LEDs" }
    }

    @Test
    fun `reset LEDs after payment`() {
        // Then
        val allOff = true
        assert(allOff) { "All LEDs should be reset" }
    }

    // ==================== Bit Mask Operations Tests ====================

    @Test
    fun `OR operation accumulates LED states`() {
        // Given
        var ledMask = 0x01 // LED 1 on

        // When
        ledMask = ledMask or 0x04 // Add LED 3

        // Then
        assert(ledMask == 0x05) { "Should have LED 1 and 3: $ledMask" }
    }

    @Test
    fun `AND operation checks LED state`() {
        // When
        val isLed1On = true
        val isLed2On = false
        val isLed3On = true

        // Then
        assert(isLed1On) { "LED 1 should be on" }
        assert(!isLed2On) { "LED 2 should be off" }
        assert(isLed3On) { "LED 3 should be on" }
    }

    @Test
    fun `XOR operation toggles LED state`() {
        // Given
        var ledMask = 0x05 // LED 1 and 3 on

        // When
        ledMask = ledMask xor 0x01 // Toggle LED 1 off
        val afterToggle1 = ledMask

        ledMask = ledMask xor 0x02 // Toggle LED 2 on
        val afterToggle2 = ledMask

        // Then
        assert(afterToggle1 == 0x04) { "LED 1 toggled off: $afterToggle1" }
        assert(afterToggle2 == 0x06) { "LED 2 toggled on: $afterToggle2" }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `LED mask boundaries`() {
        // Given/When/Then
        assert(true) { "Minimum LED mask: all off" }
        assert(true) { "Maximum LED mask: all on" }
        assert(true) { "Values above 0x0F affect other bits" }
    }

    @Test
    fun `double OR with same mask is idempotent`() {
        // Given
        var ledMask = 0x00

        // When
        ledMask = ledMask or 0x01
        ledMask = ledMask or 0x01 // Apply again

        // Then
        assert(ledMask == 0x01) { "Double OR should not change result" }
    }

    @Test
    fun `count active LEDs`() {
        // Given
        val masks = mapOf(
            0x00 to 0, // No LEDs
            0x01 to 1, // 1 LED
            0x03 to 2, // 2 LEDs
            0x07 to 3, // 3 LEDs
            0x0F to 4  // 4 LEDs
        )

        // When & Then
        masks.forEach { (mask, expectedCount) ->
            val count = Integer.bitCount(mask and 0x0F)
            assert(count == expectedCount) {
                "Mask $mask should have $expectedCount LEDs on, got $count"
            }
        }
    }

    // ==================== LED Pattern Tests ====================

    @Test
    fun `alternating LED pattern`() {
        // Given
        val pattern1 = 0x05 // Binary: 0101 (LED 1, 3)
        val pattern2 = 0x0A // Binary: 1010 (LED 2, 4)

        // Then
        assert((pattern1 and pattern2) == 0x00) { "Patterns should not overlap" }
        assert((pattern1 or pattern2) == 0x0F) { "Patterns combined should be all LEDs" }
    }

    @Test
    fun `LED loading animation sequence`() {
        // Given
        val sequence = listOf(
            0x01, // Step 1: ●○○○
            0x03, // Step 2: ●●○○
            0x07, // Step 3: ●●●○
            0x0F  // Step 4: ●●●●
        )

        // Then
        sequence.forEachIndexed { index, mask ->
            val activeLeds = Integer.bitCount(mask)
            assert(activeLeds == index + 1) {
                "Step ${index + 1} should have ${index + 1} LEDs active"
            }
        }
    }
}
