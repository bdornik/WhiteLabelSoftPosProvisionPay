package com.payten.whitelabel

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.payten.whitelabel.ui.screens.CustomTipEntryScreen
import com.payten.whitelabel.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for CustomTipEntryScreen.
 * Tests custom tip amount entry and numeric keypad interactions.
 */
class CustomTipEntryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==================== Rendering Tests ====================

    @Test
    fun customTipEntryScreen_displaysTitle() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen()
            }
        }

        // Then
        composeTestRule.onNodeWithText("UNESITE NAPOJNICU").assertIsDisplayed()
    }

    @Test
    fun customTipEntryScreen_displaysCurrencyLabel() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen()
            }
        }

        // Then
        composeTestRule.onNodeWithText("RSD").assertIsDisplayed()
    }

    @Test
    fun customTipEntryScreen_displaysInitialAmount() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen()
            }
        }

        // Then - Initial amount should be 0,00
        composeTestRule.onNodeWithText("0,00").assertIsDisplayed()
    }

    @Test
    fun customTipEntryScreen_displaysContinueButton() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen()
            }
        }

        // Then
        composeTestRule.onNodeWithText("POTVRDI").assertIsDisplayed()
    }

    @Test
    fun customTipEntryScreen_continueButtonIsEnabledInitially() {
        // Given - Note: Unlike AmountEntryScreen, this allows zero tip
        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen()
            }
        }

        // Then - Button should be enabled even with 0
        composeTestRule.onNodeWithText("POTVRDI").assertIsEnabled()
    }

    @Test
    fun customTipEntryScreen_displaysAllNumericKeys() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen()
            }
        }

        // Then - All digits 0-9 should be displayed
        for (i in 0..9) {
            composeTestRule.onNodeWithText(i.toString()).assertIsDisplayed()
        }
    }

    // ==================== Numeric Input Tests ====================

    @Test
    fun customTipEntryScreen_clickingNumberUpdatesAmount() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen()
            }
        }

        // When - Click number 5
        composeTestRule.onNodeWithText("5").performClick()

        // Then - Amount should be 0,05
        composeTestRule.onNodeWithText("0,05").assertIsDisplayed()
    }

    @Test
    fun customTipEntryScreen_enteringMultipleDigits() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen()
            }
        }

        // When - Enter 2, 5, 0
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.onNodeWithText("0").performClick()

        // Then - Amount should be 2,50
        composeTestRule.onNodeWithText("2,50").assertIsDisplayed()
    }

    @Test
    fun customTipEntryScreen_formatsLargeAmountWithThousandSeparators() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen()
            }
        }

        // When - Enter 5000000 (50000,00)
        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("0").performClick()

        // Then - Should display with thousand separator
        composeTestRule.onNodeWithText("50.000,00").assertIsDisplayed()
    }

    @Test
    fun customTipEntryScreen_backspaceRemovesLastDigit() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen()
            }
        }

        // When - Enter 1, 0, 0, then backspace
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("0").performClick()

        // Verify we have 1,00
        composeTestRule.onNodeWithText("1,00").assertIsDisplayed()

        // Backspace button at index 11
        composeTestRule.onAllNodes(hasClickAction())[11].performClick()

        // Then - Amount should be 0,10
        composeTestRule.onNodeWithText("0,10").assertIsDisplayed()
    }

    @Test
    fun customTipEntryScreen_backspaceToZero() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen()
            }
        }

        // When - Enter 7, then backspace
        composeTestRule.onNodeWithText("7").performClick()

        // Backspace button at index 11
        composeTestRule.onAllNodes(hasClickAction())[11].performClick()

        // Then - Amount should be 0,00
        composeTestRule.onNodeWithText("0,00").assertIsDisplayed()
    }

    @Test
    fun customTipEntryScreen_multipleBackspaces() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen()
            }
        }

        // When - Enter 5, 0, 0, 0 (creates 5000 pare = 50,00 RSD), then backspace twice
        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("0").performClick()

        // Verify we have 50,00
        composeTestRule.onNodeWithText("50,00").assertIsDisplayed()

        // Backspace button at index 11
        val backspaceButton = composeTestRule.onAllNodes(hasClickAction())[11]
        backspaceButton.performClick() // 5000 / 10 = 500 pare = 5,00
        backspaceButton.performClick() // 500 / 10 = 50 pare = 0,50

        // Then - Amount should be 0,50
        composeTestRule.onNodeWithText("0,50").assertIsDisplayed()
    }

    // ==================== Button State Tests ====================

    @Test
    fun customTipEntryScreen_continueEnabledWithZeroTip() {
        // Given - This is different from AmountEntryScreen - zero tip is allowed
        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen()
            }
        }

        // Then - Continue button should be enabled at 0,00
        composeTestRule.onNodeWithText("POTVRDI").assertIsEnabled()
    }

    @Test
    fun customTipEntryScreen_continueEnabledAfterEnteringAmount() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen()
            }
        }

        // When - Enter amount 5
        composeTestRule.onNodeWithText("5").performClick()

        // Then - Continue button should be enabled
        composeTestRule.onNodeWithText("POTVRDI").assertIsEnabled()
    }

    @Test
    fun customTipEntryScreen_continueEnabledAfterBackspacingToZero() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen()
            }
        }

        // When - Enter 3, then backspace to zero
        composeTestRule.onNodeWithText("3").performClick()

        // Backspace button at index 11
        composeTestRule.onAllNodes(hasClickAction())[11].performClick()

        // Then - Continue button should still be enabled (zero tip allowed)
        composeTestRule.onNodeWithText("POTVRDI").assertIsEnabled()
    }

    // ==================== Callback Tests ====================

    @Test
    fun customTipEntryScreen_backButtonTriggersCallback() {
        // Given
        var backClicked = false

        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen(onNavigateBack = { backClicked = true })
            }
        }

        // When - Click back button (first clickable)
        composeTestRule.onAllNodes(hasClickAction())[0].performClick()

        // Then
        assert(backClicked)
    }

    @Test
    fun customTipEntryScreen_continueWithZeroTip() {
        // Given
        var receivedAmount: Long? = null

        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen(onContinue = { amount -> receivedAmount = amount })
            }
        }

        // When - Click continue without entering tip
        composeTestRule.onNodeWithText("POTVRDI").performClick()

        // Then - Should pass 0 pare
        assert(receivedAmount == 0L)
    }

    @Test
    fun customTipEntryScreen_continuePassesCorrectAmount() {
        // Given
        var receivedAmount: Long? = null

        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen(onContinue = { amount -> receivedAmount = amount })
            }
        }

        // When - Enter 5, 0, 0 (5,00 = 500 pare) and click continue
        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("POTVRDI").performClick()

        // Then - Should pass 500 pare
        assert(receivedAmount == 500L)
    }

    @Test
    fun customTipEntryScreen_continuePassesSmallAmount() {
        // Given
        var receivedAmount: Long? = null

        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen(onContinue = { amount -> receivedAmount = amount })
            }
        }

        // When - Enter 2, 5 (0,25 = 25 pare) and click continue
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.onNodeWithText("POTVRDI").performClick()

        // Then - Should pass 25 pare
        assert(receivedAmount == 25L)
    }

    // ==================== Edge Cases ====================

    @Test
    fun customTipEntryScreen_maxAmountLimit() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen()
            }
        }

        // When - Try to enter more than max (999,999,999 pare = 9,999,999.99)
        // Enter 10 digits: 9999999999
        for (i in 1..10) {
            composeTestRule.onNodeWithText("9").performClick()
        }

        // Then - Should stop at 9.999.999,99 (999999999 pare, 9 digits)
        composeTestRule.onNodeWithText("9.999.999,99").assertIsDisplayed()
    }

    @Test
    fun customTipEntryScreen_allNumbersAreClickable() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen()
            }
        }

        // Then - All digit buttons should be clickable
        for (i in 0..9) {
            composeTestRule.onNodeWithText(i.toString()).assertHasClickAction()
        }
    }

    @Test
    fun customTipEntryScreen_sequentialOperations() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen()
            }
        }

        // When - Enter 1, 0, backspace, 5
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("0").performClick()

        // Backspace button at index 11
        composeTestRule.onAllNodes(hasClickAction())[11].performClick()

        composeTestRule.onNodeWithText("5").performClick()

        // Then - Should display 0,15
        composeTestRule.onNodeWithText("0,15").assertIsDisplayed()
    }

    @Test
    fun customTipEntryScreen_paddingZerosInPare() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen()
            }
        }

        // When - Enter just 3 (0,03)
        composeTestRule.onNodeWithText("3").performClick()

        // Then - Should display 0,03 (with padded zero)
        composeTestRule.onNodeWithText("0,03").assertIsDisplayed()
    }

    @Test
    fun customTipEntryScreen_allCallbacksAreIndependent() {
        // Given
        var backClicked = false
        var continueClicked = false

        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen(
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
    fun customTipEntryScreen_enteringZeroFirst() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen()
            }
        }

        // When - Click 0, 1, 0
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("0").performClick()

        // Then - Amount should be 0,10
        composeTestRule.onNodeWithText("0,10").assertIsDisplayed()
    }

    @Test
    fun customTipEntryScreen_largeAmountWithMultipleDigits() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CustomTipEntryScreen()
            }
        }

        // When - Enter 12345 (123,45)
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()
        composeTestRule.onNodeWithText("5").performClick()

        // Then - Should display 123,45
        composeTestRule.onNodeWithText("123,45").assertIsDisplayed()
    }
}
