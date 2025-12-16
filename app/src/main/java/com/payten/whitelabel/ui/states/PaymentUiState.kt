package com.payten.whitelabel.ui.states

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LedState(
    val led1: Boolean = false,
    val led2: Boolean = false,
    val led3: Boolean = false,
    val led4: Boolean = false
)

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

    fun reset() {
        pLedOn = 0x00
        _ledState.value = LedState() // All false
        _showProcessingScreen.value = false
        _showAnimationScreen.value = null
        _isActivityComplete.value = false
    }

    fun setActivityComplete(complete: Boolean) {
        _isActivityComplete.value = complete
    }

    fun setAnimationStarted() {
        _isActivityComplete.value = true
    }

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

    fun setProcessingScreen(show: Boolean) {
        _showProcessingScreen.value = show
    }

    fun setAnimationScreen(cardType: String?) {
        _showAnimationScreen.value = cardType
    }
}