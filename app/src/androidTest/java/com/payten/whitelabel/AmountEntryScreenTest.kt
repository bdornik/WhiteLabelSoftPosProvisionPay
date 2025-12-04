package com.payten.whitelabel

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.payten.whitelabel.ui.screens.AmountEntryScreen
import com.payten.whitelabel.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for AmountEntryScreen.
 * Tests amount entry and numeric keypad interactions.
 */
class AmountEntryScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==================== Rendering Tests ====================

    @Test
    fun amountEntryScreen_displaysTitle() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen()
            }
        }

        // Then
        composeTestRule.onNodeWithText("UNESITE IZNOS").assertIsDisplayed()
    }

    @Test
    fun amountEntryScreen_displaysCurrencyLabel() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen()
            }
        }

        // Then
        composeTestRule.onNodeWithText("RSD").assertIsDisplayed()
    }

    @Test
    fun amountEntryScreen_displaysInitialAmount() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen()
            }
        }

        // Then - Initial amount should be 0,00
        composeTestRule.onNodeWithText("0,00").assertIsDisplayed()
    }

    @Test
    fun amountEntryScreen_displaysContinueButton() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen()
            }
        }

        // Then
        composeTestRule.onNodeWithText("NASTAVI").assertIsDisplayed()
    }

    @Test
    fun amountEntryScreen_continueButtonIsDisabledInitially() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen()
            }
        }

        // Then
        composeTestRule.onNodeWithText("NASTAVI").assertIsNotEnabled()
    }

    @Test
    fun amountEntryScreen_displaysAllNumericKeys() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen()
            }
        }

        // Then - All digits 0-9 should be displayed
        for (i in 0..9) {
            composeTestRule.onNodeWithText(i.toString()).assertIsDisplayed()
        }
    }

    // ==================== Numeric Input Tests ====================

    @Test
    fun amountEntryScreen_clickingNumberUpdatesAmount() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen()
            }
        }

        // When - Click number 5
        composeTestRule.onNodeWithText("5").performClick()

        // Then - Amount should be 0,05
        composeTestRule.onNodeWithText("0,05").assertIsDisplayed()
    }

    @Test
    fun amountEntryScreen_enteringMultipleDigits() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen()
            }
        }

        // When - Enter 1, 2, 3, 4
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()

        // Then - Amount should be 12,34
        composeTestRule.onNodeWithText("12,34").assertIsDisplayed()
    }

    @Test
    fun amountEntryScreen_formatsLargeAmountWithThousandSeparators() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen()
            }
        }

        // When - Enter 1234567 (12345,67)
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()
        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.onNodeWithText("6").performClick()
        composeTestRule.onNodeWithText("7").performClick()

        // Then - Should display with thousand separator
        composeTestRule.onNodeWithText("12.345,67").assertIsDisplayed()
    }

    @Test
    fun amountEntryScreen_enteringZeroFirst() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen()
            }
        }

        // When - Click 0, 5, 0
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.onNodeWithText("0").performClick()

        // Then - Amount should be 0,50
        composeTestRule.onNodeWithText("0,50").assertIsDisplayed()
    }

    @Test
    fun amountEntryScreen_backspaceRemovesLastDigit() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen()
            }
        }

        // When - Enter 1, 2, 3 (creates 123 pare = 1,23 RSD)
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()

        // Verify we have 1,23
        composeTestRule.onNodeWithText("1,23").assertIsDisplayed()

        // Find and click backspace - it's clickable but has no text (only image)
        // Back button is index 0, numbers are 1-10, backspace is index 11
        composeTestRule.onAllNodes(hasClickAction())[11].performClick()

        // Then - Amount should be 0,12 (12 pare after backspace)
        composeTestRule.onNodeWithText("0,12").assertIsDisplayed()
    }

    @Test
    fun amountEntryScreen_backspaceToZero() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen()
            }
        }

        // When - Enter 5, then backspace
        composeTestRule.onNodeWithText("5").performClick()

        // Backspace button at index 11
        composeTestRule.onAllNodes(hasClickAction())[11].performClick()

        // Then - Amount should be 0,00
        composeTestRule.onNodeWithText("0,00").assertIsDisplayed()
    }

    @Test
    fun amountEntryScreen_multipleBackspaces() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen()
            }
        }

        // When - Enter 1, 2, 3, 4, then backspace twice
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()

        // Backspace button at index 11
        val backspaceButton = composeTestRule.onAllNodes(hasClickAction())[11]
        backspaceButton.performClick()
        backspaceButton.performClick()

        // Then - Amount should be 0,12
        composeTestRule.onNodeWithText("0,12").assertIsDisplayed()
    }

    // ==================== Button State Tests ====================

    @Test
    fun amountEntryScreen_continueEnabledAfterEnteringAmount() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen()
            }
        }

        // When - Enter amount 1
        composeTestRule.onNodeWithText("1").performClick()

        // Then - Continue button should be enabled
        composeTestRule.onNodeWithText("NASTAVI").assertIsEnabled()
    }

    @Test
    fun amountEntryScreen_continueDisabledAfterBackspacingToZero() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen()
            }
        }

        // When - Enter 5, then backspace to zero
        composeTestRule.onNodeWithText("5").performClick()

        // Verify button is enabled first
        composeTestRule.onNodeWithText("NASTAVI").assertIsEnabled()

        // Backspace button at index 11
        composeTestRule.onAllNodes(hasClickAction())[11].performClick()

        // Then - Continue button should be disabled
        composeTestRule.onNodeWithText("NASTAVI").assertIsNotEnabled()
    }

    // ==================== Callback Tests ====================

    @Test
    fun amountEntryScreen_backButtonTriggersCallback() {
        // Given
        var backClicked = false

        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen(onNavigateBack = { backClicked = true })
            }
        }

        // When - Click back button (first clickable)
        composeTestRule.onAllNodes(hasClickAction())[0].performClick()

        // Then
        assert(backClicked)
    }

    @Test
    fun amountEntryScreen_continuePassesCorrectAmount() {
        // Given
        var receivedAmount: Long? = null

        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen(onContinue = { amount -> receivedAmount = amount })
            }
        }

        // When - Enter 1, 2, 3, 4 (12,34) and click continue
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()
        composeTestRule.onNodeWithText("NASTAVI").performClick()

        // Then - Should pass 1234 pare
        assert(receivedAmount == 1234L)
    }

    @Test
    fun amountEntryScreen_continueNotCalledWhenDisabled() {
        // Given
        var continueClicked = false

        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen(onContinue = { continueClicked = true })
            }
        }

        // When - Try to click continue without entering amount
        composeTestRule.onNodeWithText("NASTAVI").performClick()

        // Then - Callback should not be invoked
        assert(!continueClicked)
    }

    // ==================== Edge Cases ====================

    @Test
    fun amountEntryScreen_maxAmountLimit() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen()
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
    fun amountEntryScreen_allNumbersAreClickable() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen()
            }
        }

        // Then - All digit buttons should be clickable
        for (i in 0..9) {
            composeTestRule.onNodeWithText(i.toString()).assertHasClickAction()
        }
    }

    @Test
    fun amountEntryScreen_complexAmountEntry() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen()
            }
        }

        // When - Enter 5, 0, 0, 0, 0 (50,00)
        // This creates: 5 -> 50 -> 500 -> 5000 -> 50000 pare = 500,00 RSD
        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("0").performClick()

        // Then - Should display 500,00 (not 50,00 - we entered 5 digits)
        composeTestRule.onNodeWithText("500,00").assertIsDisplayed()
    }

    @Test
    fun amountEntryScreen_sequentialOperations() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen()
            }
        }

        // When - Enter 1, 2, backspace, 3
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()

        // Backspace button at index 11
        composeTestRule.onAllNodes(hasClickAction())[11].performClick()

        composeTestRule.onNodeWithText("3").performClick()

        // Then - Should display 0,13
        composeTestRule.onNodeWithText("0,13").assertIsDisplayed()
    }

    @Test
    fun amountEntryScreen_paddingZerosInPare() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen()
            }
        }

        // When - Enter just 1 (0,01)
        composeTestRule.onNodeWithText("1").performClick()

        // Then - Should display 0,01 (with padded zero)
        composeTestRule.onNodeWithText("0,01").assertIsDisplayed()
    }

    @Test
    fun amountEntryScreen_allCallbacksAreIndependent() {
        // Given
        var backClicked = false
        var continueClicked = false

        composeTestRule.setContent {
            AppTheme {
                AmountEntryScreen(
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
}
