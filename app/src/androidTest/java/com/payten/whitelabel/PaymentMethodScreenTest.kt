package com.payten.whitelabel

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.payten.whitelabel.ui.screens.PaymentMethod
import com.payten.whitelabel.ui.screens.PaymentMethodScreen
import com.payten.whitelabel.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for PaymentMethodScreen.
 * Tests payment method selection and user interactions.
 */
class PaymentMethodScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==================== Rendering Tests ====================

    @Test
    fun paymentMethodScreen_displaysTitle() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(amountInPare = 100000L)
            }
        }

        // Then
        composeTestRule.onNodeWithText("NAČIN PLAĆANJA").assertIsDisplayed()
    }

    @Test
    fun paymentMethodScreen_displaysAmount() {
        // Given - 234000 pare = 2340,00 RSD
        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(amountInPare = 234000L)
            }
        }

        // Then
        composeTestRule.onNodeWithText("2.340,00").assertIsDisplayed()
    }

    @Test
    fun paymentMethodScreen_formatsAmountCorrectly() {
        // Given - 12345678 pare = 123456,78 RSD
        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(amountInPare = 12345678L)
            }
        }

        // Then - Should display with thousand separators
        composeTestRule.onNodeWithText("123.456,78").assertIsDisplayed()
    }

    @Test
    fun paymentMethodScreen_displaysSelectionLabel() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(amountInPare = 100000L)
            }
        }

        // Then
        composeTestRule.onNodeWithText("IZABERITE NAČIN PLAĆANJA").assertIsDisplayed()
    }

    @Test
    fun paymentMethodScreen_displaysCardOption() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(amountInPare = 100000L)
            }
        }

        // Then
        composeTestRule.onNodeWithText("Plaćanje karticom").assertIsDisplayed()
    }

    @Test
    fun paymentMethodScreen_displaysIPSOption() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(amountInPare = 100000L)
            }
        }

        // Then
        composeTestRule.onNodeWithText("IPS paćanje").assertIsDisplayed()
    }

    @Test
    fun paymentMethodScreen_displaysContinueButton() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(amountInPare = 100000L)
            }
        }

        // Then
        composeTestRule.onNodeWithText("NASTAVI").assertIsDisplayed()
    }

    @Test
    fun paymentMethodScreen_continueButtonIsDisabledInitially() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(amountInPare = 100000L)
            }
        }

        // Then - Button should be disabled
        composeTestRule.onNodeWithText("NASTAVI").assertIsNotEnabled()
    }

    @Test
    fun paymentMethodScreen_displayBackButton() {
        // Given
        var backClicked = false

        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(
                    amountInPare = 100000L,
                    onNavigateBack = { backClicked = true }
                )
            }
        }

        // Then - BackButton should be clickable (test by verifying it exists and can be clicked)
        composeTestRule.onAllNodes(hasClickAction())[0].assertExists()
    }

    // ==================== Interaction Tests ====================

    @Test
    fun paymentMethodScreen_backButtonTriggersCallback() {
        // Given
        var backClicked = false

        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(
                    amountInPare = 100000L,
                    onNavigateBack = { backClicked = true }
                )
            }
        }

        // When - Click the first clickable element (back button)
        composeTestRule.onAllNodes(hasClickAction())[0].performClick()

        // Then
        assert(backClicked)
    }

    @Test
    fun paymentMethodScreen_selectingCardEnablesContinueButton() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(amountInPare = 100000L)
            }
        }

        // When - Click card payment option
        composeTestRule.onNodeWithText("Plaćanje karticom").performClick()

        // Then - Continue button should be enabled
        composeTestRule.onNodeWithText("NASTAVI").assertIsEnabled()
    }

    @Test
    fun paymentMethodScreen_selectingIPSEnablesContinueButton() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(amountInPare = 100000L)
            }
        }

        // When - Click IPS payment option
        composeTestRule.onNodeWithText("IPS paćanje").performClick()

        // Then - Continue button should be enabled
        composeTestRule.onNodeWithText("NASTAVI").assertIsEnabled()
    }

    @Test
    fun paymentMethodScreen_continueWithCardCallsCallback() {
        // Given
        var selectedMethod: PaymentMethod? = null

        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(
                    amountInPare = 100000L,
                    onContinue = { method -> selectedMethod = method }
                )
            }
        }

        // When - Select card and click continue
        composeTestRule.onNodeWithText("Plaćanje karticom").performClick()
        composeTestRule.onNodeWithText("NASTAVI").performClick()

        // Then
        assert(selectedMethod == PaymentMethod.CARD)
    }

    @Test
    fun paymentMethodScreen_continueWithIPSCallsCallback() {
        // Given
        var selectedMethod: PaymentMethod? = null

        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(
                    amountInPare = 100000L,
                    onContinue = { method -> selectedMethod = method }
                )
            }
        }

        // When - Select IPS and click continue
        composeTestRule.onNodeWithText("IPS paćanje").performClick()
        composeTestRule.onNodeWithText("NASTAVI").performClick()

        // Then
        assert(selectedMethod == PaymentMethod.IPS)
    }

    @Test
    fun paymentMethodScreen_switchingSelectionUpdatesState() {
        // Given
        var selectedMethod: PaymentMethod? = null

        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(
                    amountInPare = 100000L,
                    onContinue = { method -> selectedMethod = method }
                )
            }
        }

        // When - Select card, then switch to IPS, then continue
        composeTestRule.onNodeWithText("Plaćanje karticom").performClick()
        composeTestRule.onNodeWithText("IPS paćanje").performClick()
        composeTestRule.onNodeWithText("NASTAVI").performClick()

        // Then - Should continue with IPS (last selected)
        assert(selectedMethod == PaymentMethod.IPS)
    }

    @Test
    fun paymentMethodScreen_cannotContinueWithoutSelection() {
        // Given
        var continueClicked = false

        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(
                    amountInPare = 100000L,
                    onContinue = { continueClicked = true }
                )
            }
        }

        // When - Try to click continue without selecting payment method
        composeTestRule.onNodeWithText("NASTAVI").performClick()

        // Then - Callback should not be invoked
        assert(!continueClicked)
    }

    // ==================== Edge Cases ====================

    @Test
    fun paymentMethodScreen_withZeroAmount() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(amountInPare = 0L)
            }
        }

        // Then - Should display 0,00
        composeTestRule.onNodeWithText("0,00").assertIsDisplayed()
    }

    @Test
    fun paymentMethodScreen_withSmallAmount() {
        // Given - 50 pare = 0,50 RSD
        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(amountInPare = 50L)
            }
        }

        // Then
        composeTestRule.onNodeWithText("0,50").assertIsDisplayed()
    }

    @Test
    fun paymentMethodScreen_withLargeAmount() {
        // Given - 999999999 pare = 9999999,99 RSD
        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(amountInPare = 999999999L)
            }
        }

        // Then - Should display with thousand separators
        composeTestRule.onNodeWithText("9.999.999,99").assertIsDisplayed()
    }

    @Test
    fun paymentMethodScreen_multipleSelectionChanges() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(amountInPare = 100000L)
            }
        }

        // When - Select card
        composeTestRule.onNodeWithText("Plaćanje karticom").performClick()

        // Then - Continue should be enabled
        composeTestRule.onNodeWithText("NASTAVI").assertIsEnabled()

        // When - Switch to IPS
        composeTestRule.onNodeWithText("IPS paćanje").performClick()

        // Then - Continue should still be enabled
        composeTestRule.onNodeWithText("NASTAVI").assertIsEnabled()

        // When - Switch back to card
        composeTestRule.onNodeWithText("Plaćanje karticom").performClick()

        // Then - Continue should still be enabled
        composeTestRule.onNodeWithText("NASTAVI").assertIsEnabled()
    }

    @Test
    fun paymentMethodScreen_allCallbacksAreIndependent() {
        // Given
        var backClicked = false
        var continueClicked = false

        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(
                    amountInPare = 100000L,
                    onNavigateBack = { backClicked = true },
                    onContinue = { continueClicked = true }
                )
            }
        }

        // When - Click back button
        composeTestRule.onAllNodes(hasClickAction())[0].performClick()

        // Then - Only back callback should be triggered
        assert(backClicked)
        assert(!continueClicked)
    }

    @Test
    fun paymentMethodScreen_amountFormattingWithSingleDigitPare() {
        // Given - 1205 pare = 12,05 RSD
        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(amountInPare = 1205L)
            }
        }

        // Then - Should pad pare to 2 digits
        composeTestRule.onNodeWithText("12,05").assertIsDisplayed()
    }

    @Test
    fun paymentMethodScreen_bothPaymentOptionsAreClickable() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PaymentMethodScreen(amountInPare = 100000L)
            }
        }

        // Then - Both options should be clickable
        composeTestRule.onNodeWithText("Plaćanje karticom").assertHasClickAction()
        composeTestRule.onNodeWithText("IPS paćanje").assertHasClickAction()
    }
}
