package com.payten.whitelabel

import com.payten.whitelabel.ui.states.LedState
import com.payten.whitelabel.ui.states.PaymentUiBridge
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PaymentUiBridge.
 *
 * Tests LED state management and processing screen visibility during payment flows.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentUiBridgeTest {

    @Before
    fun setup() {
        // Reset state before each test
        PaymentUiBridge.reset()
    }

    // ==================== Reset Tests ====================

    @Test
    fun `reset clears all LED states`() = runTest {
        // Given - some LEDs are on
        PaymentUiBridge.updateLedState(0x0F, true) // All LEDs on

        // When
        PaymentUiBridge.reset()

        // Then
        val state = PaymentUiBridge.ledState.first()
        assert(!state.led1) { "LED 1 should be off after reset" }
        assert(!state.led2) { "LED 2 should be off after reset" }
        assert(!state.led3) { "LED 3 should be off after reset" }
        assert(!state.led4) { "LED 4 should be off after reset" }
    }

    @Test
    fun `reset clears processing screen visibility`() = runTest {
        // Given
        PaymentUiBridge.setProcessingScreen(true)

        // When
        PaymentUiBridge.reset()

        // Then
        val showProcessing = PaymentUiBridge.showProcessingScreen.first()
        assert(!showProcessing) { "Processing screen should be hidden after reset" }
    }

    // ==================== LED State Tests ====================

    @Test
    fun `updateLedState turns on individual LEDs`() = runTest {
        // Given
        val testCases = mapOf(
            0x01 to LedState(led1 = true, led2 = false, led3 = false, led4 = false),
            0x02 to LedState(led1 = false, led2 = true, led3 = false, led4 = false),
            0x04 to LedState(led1 = false, led2 = false, led3 = true, led4 = false),
            0x08 to LedState(led1 = false, led2 = false, led3 = false, led4 = true)
        )

        testCases.forEach { (mask, expectedState) ->
            // When
            PaymentUiBridge.reset()
            PaymentUiBridge.updateLedState(mask, true)

            // Then
            val actualState = PaymentUiBridge.ledState.first()
            assert(actualState == expectedState) {
                "LED state for mask $mask should be $expectedState, got $actualState"
            }
        }
    }

    @Test
    fun `updateLedState turns on multiple LEDs`() = runTest {
        // When - Turn on LED 1
        PaymentUiBridge.updateLedState(0x01, true)
        // Then add LED 2
        PaymentUiBridge.updateLedState(0x02, true)

        // Then
        val state = PaymentUiBridge.ledState.first()
        assert(state.led1) { "LED 1 should be on" }
        assert(state.led2) { "LED 2 should be on" }
        assert(!state.led3) { "LED 3 should be off" }
        assert(!state.led4) { "LED 4 should be off" }
    }

    @Test
    fun `updateLedState with false turns off all LEDs`() = runTest {
        // Given - some LEDs are on
        PaymentUiBridge.updateLedState(0x0F, true)

        // When
        PaymentUiBridge.updateLedState(0x00, false)

        // Then
        val state = PaymentUiBridge.ledState.first()
        assert(!state.led1) { "LED 1 should be off" }
        assert(!state.led2) { "LED 2 should be off" }
        assert(!state.led3) { "LED 3 should be off" }
        assert(!state.led4) { "LED 4 should be off" }
    }

    @Test
    fun `updateLedState all LEDs on`() = runTest {
        // When
        PaymentUiBridge.updateLedState(0x0F, true) // 0x0F = 0b1111 = all LEDs

        // Then
        val state = PaymentUiBridge.ledState.first()
        assert(state.led1) { "LED 1 should be on" }
        assert(state.led2) { "LED 2 should be on" }
        assert(state.led3) { "LED 3 should be on" }
        assert(state.led4) { "LED 4 should be on" }
    }

    @Test
    fun `updateLedState preserves previously set LEDs`() = runTest {
        // When - Sequentially turn on LEDs
        PaymentUiBridge.updateLedState(0x01, true) // LED 1 on
        PaymentUiBridge.updateLedState(0x02, true) // LED 2 on (LED 1 stays on)
        PaymentUiBridge.updateLedState(0x04, true) // LED 3 on (LED 1,2 stay on)

        // Then
        val state = PaymentUiBridge.ledState.first()
        assert(state.led1) { "LED 1 should stay on" }
        assert(state.led2) { "LED 2 should stay on" }
        assert(state.led3) { "LED 3 should be on" }
        assert(!state.led4) { "LED 4 should be off" }
    }

    // ==================== Processing Screen Tests ====================

    @Test
    fun `setProcessingScreen shows processing screen`() = runTest {
        // When
        PaymentUiBridge.setProcessingScreen(true)

        // Then
        val showProcessing = PaymentUiBridge.showProcessingScreen.first()
        assert(showProcessing) { "Processing screen should be visible" }
    }

    @Test
    fun `setProcessingScreen hides processing screen`() = runTest {
        // Given
        PaymentUiBridge.setProcessingScreen(true)

        // When
        PaymentUiBridge.setProcessingScreen(false)

        // Then
        val showProcessing = PaymentUiBridge.showProcessingScreen.first()
        assert(!showProcessing) { "Processing screen should be hidden" }
    }

    @Test
    fun `setProcessingScreen can toggle multiple times`() = runTest {
        // When & Then
        PaymentUiBridge.setProcessingScreen(true)
        assert(PaymentUiBridge.showProcessingScreen.first())

        PaymentUiBridge.setProcessingScreen(false)
        assert(!PaymentUiBridge.showProcessingScreen.first())

        PaymentUiBridge.setProcessingScreen(true)
        assert(PaymentUiBridge.showProcessingScreen.first())
    }

    // ==================== Integration Tests ====================

    @Test
    fun `typical payment flow LED sequence`() = runTest {
        // Simulate typical payment transaction LED sequence

        // Step 1: Transaction idle
        PaymentUiBridge.updateLedState(0x01, true)
        var state = PaymentUiBridge.ledState.first()
        assert(state.led1 && !state.led2 && !state.led3 && !state.led4)

        // Step 2: Transaction processing
        PaymentUiBridge.updateLedState(0x02, true)
        state = PaymentUiBridge.ledState.first()
        assert(state.led1 && state.led2 && !state.led3 && !state.led4)

        // Step 3: Card read
        PaymentUiBridge.updateLedState(0x04, true)
        state = PaymentUiBridge.ledState.first()
        assert(state.led1 && state.led2 && state.led3 && !state.led4)

        // Step 4: Transaction complete
        PaymentUiBridge.updateLedState(0x0F, true)
        state = PaymentUiBridge.ledState.first()
        assert(state.led1 && state.led2 && state.led3 && state.led4)
    }

    @Test
    fun `processing screen lifecycle during payment`() = runTest {
        // Initial state
        assert(!PaymentUiBridge.showProcessingScreen.first())

        // Card tapped - after delay, show processing
        PaymentUiBridge.setProcessingScreen(true)
        assert(PaymentUiBridge.showProcessingScreen.first())

        // PIN required - hide processing for PIN dialog
        PaymentUiBridge.setProcessingScreen(false)
        assert(!PaymentUiBridge.showProcessingScreen.first())

        // PIN entered - show processing again
        PaymentUiBridge.setProcessingScreen(true)
        assert(PaymentUiBridge.showProcessingScreen.first())

        // Transaction complete - should stay visible until navigation
        assert(PaymentUiBridge.showProcessingScreen.first())

        // Navigation complete - reset
        PaymentUiBridge.reset()
        assert(!PaymentUiBridge.showProcessingScreen.first())
    }

    // ==================== Edge Cases ====================

    @Test
    fun `multiple resets are safe`() = runTest {
        // When
        PaymentUiBridge.reset()
        PaymentUiBridge.reset()
        PaymentUiBridge.reset()

        // Then - should not crash
        val state = PaymentUiBridge.ledState.first()
        assert(!state.led1 && !state.led2 && !state.led3 && !state.led4)
    }

    @Test
    fun `updateLedState with zero mask and true does not clear LEDs`() = runTest {
        // Given - All LEDs on
        PaymentUiBridge.updateLedState(0x0F, true)

        // When - OR with 0x00 doesn't change anything
        PaymentUiBridge.updateLedState(0x00, true)

        // Then - LEDs stay on because pLedOn = pLedOn OR 0x00 = pLedOn
        val state = PaymentUiBridge.ledState.first()
        assert(state.led1 && state.led2 && state.led3 && state.led4) { "LEDs should stay on when OR-ing with 0x00" }
    }
}
