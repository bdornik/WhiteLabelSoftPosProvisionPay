package com.payten.whitelabel.ui.states

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents the LED indicator state displayed during payment processing.
 *
 * This data class tracks the on/off state of four LED indicators shown on the
 * CardProcessingScreen to provide visual feedback during NFC card reading.
 *
 * @property led1 First LED indicator (0x01 bit mask) - transaction idle/ready
 * @property led2 Second LED indicator (0x02 bit mask) - card detected/processing
 * @property led3 Third LED indicator (0x04 bit mask) - card read successfully
 * @property led4 Fourth LED indicator (0x08 bit mask) - transaction complete
 */
data class LedState(
    val led1: Boolean = false,
    val led2: Boolean = false,
    val led3: Boolean = false,
    val led4: Boolean = false
)

/**
 * PaymentUiBridge provides state synchronization between HeadlessPaymentActivity
 * and CardProcessingScreen.
 *
 * Since HeadlessPaymentActivity runs as a separate activity on top of SplashActivity,
 * this singleton object uses StateFlow to communicate UI state changes between the
 * two activities. This enables:
 * - LED indicator updates during card reading
 * - Screen transitions (card tap → processing → animation)
 * - Hiding CardProcessingScreen when payment completes to prevent visual flashing
 *
 * ## State Management Flow:
 * 1. HeadlessPaymentActivity calls updateLedState() → CardProcessingScreen observes and updates UI
 * 2. HeadlessPaymentActivity calls setAnimationStarted() → CardProcessingScreen hides itself
 * 3. New payment starts → reset() clears all state for fresh transaction
 *
 * ## Thread Safety:
 * All state updates use MutableStateFlow which is thread-safe and can be safely
 * updated from background threads (SDK callbacks) via runOnUiThread().
 */
object PaymentUiBridge {
    private val _ledState = MutableStateFlow(LedState())
    val ledState = _ledState.asStateFlow()

    private val _showProcessingScreen = MutableStateFlow(false)
    val showProcessingScreen = _showProcessingScreen.asStateFlow()

    private val _showAnimationScreen = MutableStateFlow<String?>(null)
    val showAnimationScreen = _showAnimationScreen.asStateFlow()

    private val _isActivityComplete = MutableStateFlow(false)
    val isActivityComplete = _isActivityComplete.asStateFlow()

    private var pLedOn = 0x00

    /**
     * Resets all payment UI state to initial values.
     *
     * This should be called when:
     * - A new payment transaction starts (in HeadlessPaymentActivity.onCreate)
     * - Navigating to the card_tap screen for a fresh payment
     *
     * Clears:
     * - All LED states (all off)
     * - Processing screen flag
     * - Animation screen flag
     * - Activity complete flag
     */
    fun reset() {
        pLedOn = 0x00
        _ledState.value = LedState() // All false
        _showProcessingScreen.value = false
        _showAnimationScreen.value = null
        _isActivityComplete.value = false
    }

    /**
     * Sets the activity completion state.
     *
     * When true, CardProcessingScreen will hide itself to prevent
     * visual flashing during activity transitions.
     *
     * @param complete True when HeadlessPaymentActivity is finishing
     */
    fun setActivityComplete(complete: Boolean) {
        _isActivityComplete.value = complete
    }

    /**
     * Signals that the payment success animation has started.
     *
     * This immediately hides CardProcessingScreen to prevent it from being
     * visible during the transition from HeadlessPaymentActivity (showing animation)
     * back to SplashActivity and then to TransactionScreen.
     *
     * Called when Visa/Mastercard animation begins in HeadlessPaymentActivity.
     */
    fun setAnimationStarted() {
        _isActivityComplete.value = true
    }

    /**
     * Updates LED indicator states using bit mask operations.
     *
     * LED indicators provide visual feedback during NFC card reading:
     * - 0x01: LED 1 - Transaction idle/ready to read
     * - 0x02: LED 2 - Card detected/processing
     * - 0x04: LED 3 - Card read successfully
     * - 0x08: LED 4 - Transaction complete
     * - 0x0F: All LEDs (combination of above bits)
     *
     * @param ledOn Bit mask indicating which LEDs to update (0x01, 0x02, 0x04, 0x08, or 0x0F)
     * @param isLedON True to turn on the specified LEDs, false to turn all off
     */
    fun updateLedState(ledOn: Int, isLedON: Boolean) {
        pLedOn = if (!isLedON) {
            0
        } else {
            pLedOn or ledOn
        }

        _ledState.value = LedState(
            led1 = (pLedOn and 0x01) == 0x01,
            led2 = (pLedOn and 0x02) == 0x02,
            led3 = (pLedOn and 0x04) == 0x04,
            led4 = (pLedOn and 0x08) == 0x08
        )
    }

    /**
     * Controls visibility of the PaymentProcessingScreen.
     *
     * @param show True to show processing screen (during PIN entry), false to hide
     */
    fun setProcessingScreen(show: Boolean) {
        _showProcessingScreen.value = show
    }

    /**
     * Sets the card type for animation display.
     *
     * @param cardType Card brand name or null to clear
     */
    fun setAnimationScreen(cardType: String?) {
        _showAnimationScreen.value = cardType
    }
}