package com.payten.whitelabel

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.payten.whitelabel.ui.screens.PaymentMethod
import com.payten.whitelabel.ui.screens.TipSelectionScreen
import com.payten.whitelabel.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for TipSelectionScreen.
 * Tests tip selection and calculation logic.
 */
class TipSelectionScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==================== Rendering Tests ====================

    @Test
    fun tipSelectionScreen_displaysTitle() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 100000L,
                    paymentMethod = PaymentMethod.CARD
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("NAPOJNICA").assertIsDisplayed()
    }

    @Test
    fun tipSelectionScreen_displaysAmount() {
        // Given - 234000 pare = 2340,00 RSD
        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 234000L,
                    paymentMethod = PaymentMethod.CARD
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("2.340,00").assertIsDisplayed()
    }

    @Test
    fun tipSelectionScreen_displaysSelectionLabel() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 100000L,
                    paymentMethod = PaymentMethod.CARD
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("IZABERITE NAPOJNICU").assertIsDisplayed()
    }

    @Test
    fun tipSelectionScreen_displaysAllTipOptions() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 100000L,
                    paymentMethod = PaymentMethod.CARD
                )
            }
        }

        // Then - All tip options should be displayed
        composeTestRule.onNodeWithText("5%").assertIsDisplayed()
        composeTestRule.onNodeWithText("10%").assertIsDisplayed()
        composeTestRule.onNodeWithText("15%").assertIsDisplayed()
        composeTestRule.onNodeWithText("20%").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unos po izboru").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bez napojnice").assertIsDisplayed()
    }

    @Test
    fun tipSelectionScreen_displaysConfirmButton() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 100000L,
                    paymentMethod = PaymentMethod.CARD
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("POTVRDI").assertIsDisplayed()
    }

    @Test
    fun tipSelectionScreen_confirmButtonIsDisabledInitially() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 100000L,
                    paymentMethod = PaymentMethod.CARD
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("POTVRDI").assertIsNotEnabled()
    }

    // ==================== Selection Tests ====================

    @Test
    fun tipSelectionScreen_selecting5PercentEnablesButton() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 100000L,
                    paymentMethod = PaymentMethod.CARD
                )
            }
        }

        // When - Click 5% option
        composeTestRule.onNodeWithText("5%").performClick()

        // Then
        composeTestRule.onNodeWithText("POTVRDI").assertIsEnabled()
    }

    @Test
    fun tipSelectionScreen_selecting10PercentEnablesButton() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 100000L,
                    paymentMethod = PaymentMethod.CARD
                )
            }
        }

        // When - Click 10% option
        composeTestRule.onNodeWithText("10%").performClick()

        // Then
        composeTestRule.onNodeWithText("POTVRDI").assertIsEnabled()
    }

    @Test
    fun tipSelectionScreen_selecting15PercentEnablesButton() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 100000L,
                    paymentMethod = PaymentMethod.CARD
                )
            }
        }

        // When - Click 15% option
        composeTestRule.onNodeWithText("15%").performClick()

        // Then
        composeTestRule.onNodeWithText("POTVRDI").assertIsEnabled()
    }

    @Test
    fun tipSelectionScreen_selecting20PercentEnablesButton() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 100000L,
                    paymentMethod = PaymentMethod.CARD
                )
            }
        }

        // When - Click 20% option
        composeTestRule.onNodeWithText("20%").performClick()

        // Then
        composeTestRule.onNodeWithText("POTVRDI").assertIsEnabled()
    }

    @Test
    fun tipSelectionScreen_selectingNoTipEnablesButton() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 100000L,
                    paymentMethod = PaymentMethod.CARD
                )
            }
        }

        // When - Click no tip option
        composeTestRule.onNodeWithText("Bez napojnice").performClick()

        // Then
        composeTestRule.onNodeWithText("POTVRDI").assertIsEnabled()
    }

    // ==================== Callback Tests - Card Payment ====================

    @Test
    fun tipSelectionScreen_cardPaymentWith5PercentTip() {
        // Given - 10000 pare = 100,00 RSD, 5% tip = 500 pare
        var receivedTipAmount: Long? = null

        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 10000L,
                    paymentMethod = PaymentMethod.CARD,
                    onContinueCard = { tipAmount -> receivedTipAmount = tipAmount }
                )
            }
        }

        // When - Select 5% and confirm
        composeTestRule.onNodeWithText("5%").performClick()
        composeTestRule.onNodeWithText("POTVRDI").performClick()

        // Then - Tip should be 500 pare (5% of 10000)
        assert(receivedTipAmount == 500L)
    }

    @Test
    fun tipSelectionScreen_cardPaymentWith10PercentTip() {
        // Given - 10000 pare = 100,00 RSD, 10% tip = 1000 pare
        var receivedTipAmount: Long? = null

        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 10000L,
                    paymentMethod = PaymentMethod.CARD,
                    onContinueCard = { tipAmount -> receivedTipAmount = tipAmount }
                )
            }
        }

        // When - Select 10% and confirm
        composeTestRule.onNodeWithText("10%").performClick()
        composeTestRule.onNodeWithText("POTVRDI").performClick()

        // Then - Tip should be 1000 pare (10% of 10000)
        assert(receivedTipAmount == 1000L)
    }

    @Test
    fun tipSelectionScreen_cardPaymentWith15PercentTip() {
        // Given - 10000 pare = 100,00 RSD, 15% tip = 1500 pare
        var receivedTipAmount: Long? = null

        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 10000L,
                    paymentMethod = PaymentMethod.CARD,
                    onContinueCard = { tipAmount -> receivedTipAmount = tipAmount }
                )
            }
        }

        // When - Select 15% and confirm
        composeTestRule.onNodeWithText("15%").performClick()
        composeTestRule.onNodeWithText("POTVRDI").performClick()

        // Then - Tip should be 1500 pare (15% of 10000)
        assert(receivedTipAmount == 1500L)
    }

    @Test
    fun tipSelectionScreen_cardPaymentWith20PercentTip() {
        // Given - 10000 pare = 100,00 RSD, 20% tip = 2000 pare
        var receivedTipAmount: Long? = null

        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 10000L,
                    paymentMethod = PaymentMethod.CARD,
                    onContinueCard = { tipAmount -> receivedTipAmount = tipAmount }
                )
            }
        }

        // When - Select 20% and confirm
        composeTestRule.onNodeWithText("20%").performClick()
        composeTestRule.onNodeWithText("POTVRDI").performClick()

        // Then - Tip should be 2000 pare (20% of 10000)
        assert(receivedTipAmount == 2000L)
    }

    @Test
    fun tipSelectionScreen_cardPaymentWithNoTip() {
        // Given
        var receivedTipAmount: Long? = null

        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 10000L,
                    paymentMethod = PaymentMethod.CARD,
                    onContinueCard = { tipAmount -> receivedTipAmount = tipAmount }
                )
            }
        }

        // When - Select no tip and confirm
        composeTestRule.onNodeWithText("Bez napojnice").performClick()
        composeTestRule.onNodeWithText("POTVRDI").performClick()

        // Then - Tip should be 0
        assert(receivedTipAmount == 0L)
    }

    // ==================== Callback Tests - IPS Payment ====================

    @Test
    fun tipSelectionScreen_ipsPaymentWith10PercentTip() {
        // Given - 10000 pare = 100,00 RSD, 10% tip = 1000 pare, total = 11000 pare
        var receivedTotalAmount: Long? = null

        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 10000L,
                    paymentMethod = PaymentMethod.IPS,
                    onContinueIps = { totalAmount -> receivedTotalAmount = totalAmount }
                )
            }
        }

        // When - Select 10% and confirm
        composeTestRule.onNodeWithText("10%").performClick()
        composeTestRule.onNodeWithText("POTVRDI").performClick()

        // Then - Total should be 11000 pare (10000 + 1000)
        assert(receivedTotalAmount == 11000L)
    }

    @Test
    fun tipSelectionScreen_ipsPaymentWithNoTip() {
        // Given
        var receivedTotalAmount: Long? = null

        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 10000L,
                    paymentMethod = PaymentMethod.IPS,
                    onContinueIps = { totalAmount -> receivedTotalAmount = totalAmount }
                )
            }
        }

        // When - Select no tip and confirm
        composeTestRule.onNodeWithText("Bez napojnice").performClick()
        composeTestRule.onNodeWithText("POTVRDI").performClick()

        // Then - Total should be original amount
        assert(receivedTotalAmount == 10000L)
    }

    // ==================== Custom Tip Tests ====================

    @Test
    fun tipSelectionScreen_customTipTriggersCallback() {
        // Given
        var customTipClicked = false

        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 10000L,
                    paymentMethod = PaymentMethod.CARD,
                    onCustomTipClick = { customTipClicked = true }
                )
            }
        }

        // When - Click custom tip option
        composeTestRule.onNodeWithText("Unos po izboru").performClick()

        // Then
        assert(customTipClicked)
    }

    @Test
    fun tipSelectionScreen_externalCustomTipEnablesButton() {
        // Given - External custom tip of 500 pare
        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 10000L,
                    paymentMethod = PaymentMethod.CARD,
                    externalCustomTip = 500L
                )
            }
        }

        // Then - Button should be enabled
        composeTestRule.onNodeWithText("POTVRDI").assertIsEnabled()
    }

    @Test
    fun tipSelectionScreen_cardPaymentWithExternalCustomTip() {
        // Given - 10000 pare amount, 500 pare custom tip
        var receivedTipAmount: Long? = null

        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 10000L,
                    paymentMethod = PaymentMethod.CARD,
                    externalCustomTip = 500L,
                    onContinueCard = { tipAmount -> receivedTipAmount = tipAmount }
                )
            }
        }

        // When - Confirm with custom tip
        composeTestRule.onNodeWithText("POTVRDI").performClick()

        // Then - Tip should be 500 pare
        assert(receivedTipAmount == 500L)
    }

    // ==================== Edge Cases ====================

    @Test
    fun tipSelectionScreen_backButtonTriggersCallback() {
        // Given
        var backClicked = false

        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 10000L,
                    paymentMethod = PaymentMethod.CARD,
                    onNavigateBack = { backClicked = true }
                )
            }
        }

        // When - Click back button (first clickable)
        composeTestRule.onAllNodes(hasClickAction())[0].performClick()

        // Then
        assert(backClicked)
    }

    @Test
    fun tipSelectionScreen_switchingBetweenOptions() {
        // Given
        var receivedTipAmount: Long? = null

        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 10000L,
                    paymentMethod = PaymentMethod.CARD,
                    onContinueCard = { tipAmount -> receivedTipAmount = tipAmount }
                )
            }
        }

        // When - Select 5%, then switch to 10%, then confirm
        composeTestRule.onNodeWithText("5%").performClick()
        composeTestRule.onNodeWithText("10%").performClick()
        composeTestRule.onNodeWithText("POTVRDI").performClick()

        // Then - Should use last selected (10%)
        assert(receivedTipAmount == 1000L)
    }

    @Test
    fun tipSelectionScreen_cannotConfirmWithoutSelection() {
        // Given
        var confirmClicked = false

        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 10000L,
                    paymentMethod = PaymentMethod.CARD,
                    onContinueCard = { confirmClicked = true }
                )
            }
        }

        // When - Try to click confirm without selecting tip
        composeTestRule.onNodeWithText("POTVRDI").performClick()

        // Then - Callback should not be invoked
        assert(!confirmClicked)
    }

    @Test
    fun tipSelectionScreen_allTipOptionsAreClickable() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 10000L,
                    paymentMethod = PaymentMethod.CARD
                )
            }
        }

        // Then - All options should be clickable
        composeTestRule.onNodeWithText("5%").assertHasClickAction()
        composeTestRule.onNodeWithText("10%").assertHasClickAction()
        composeTestRule.onNodeWithText("15%").assertHasClickAction()
        composeTestRule.onNodeWithText("20%").assertHasClickAction()
        composeTestRule.onNodeWithText("Unos po izboru").assertHasClickAction()
        composeTestRule.onNodeWithText("Bez napojnice").assertHasClickAction()
    }

    @Test
    fun tipSelectionScreen_tipCalculationWithLargeAmount() {
        // Given - 100000000 pare = 1,000,000.00 RSD, 10% = 10000000 pare
        var receivedTipAmount: Long? = null

        composeTestRule.setContent {
            AppTheme {
                TipSelectionScreen(
                    amountInPare = 100000000L,
                    paymentMethod = PaymentMethod.CARD,
                    onContinueCard = { tipAmount -> receivedTipAmount = tipAmount }
                )
            }
        }

        // When - Select 10% and confirm
        composeTestRule.onNodeWithText("10%").performClick()
        composeTestRule.onNodeWithText("POTVRDI").performClick()

        // Then - Tip should be 10000000 pare
        assert(receivedTipAmount == 10000000L)
    }
}
