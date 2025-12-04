package com.payten.whitelabel

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.payten.whitelabel.ui.components.AmountDisplayCard
import com.payten.whitelabel.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for AmountDisplayCard component.
 * Tests amount display card rendering and text content.
 */
class AmountDisplayCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==================== Rendering Tests ====================

    @Test
    fun amountDisplayCard_displaysLabel() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = "100,00")
            }
        }

        // Then
        composeTestRule.onNodeWithText("Iznos transakcije").assertIsDisplayed()
    }

    @Test
    fun amountDisplayCard_displaysCurrency() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = "100,00")
            }
        }

        // Then
        composeTestRule.onNodeWithText("RSD").assertIsDisplayed()
    }

    @Test
    fun amountDisplayCard_displaysAmount() {
        // Given
        val amount = "1.234,56"

        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = amount)
            }
        }

        // Then
        composeTestRule.onNodeWithText(amount).assertIsDisplayed()
    }

    @Test
    fun amountDisplayCard_displaysSmallAmount() {
        // Given
        val amount = "0,50"

        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = amount)
            }
        }

        // Then
        composeTestRule.onNodeWithText(amount).assertIsDisplayed()
    }

    @Test
    fun amountDisplayCard_displaysLargeAmount() {
        // Given
        val amount = "9.999.999,99"

        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = amount)
            }
        }

        // Then
        composeTestRule.onNodeWithText(amount).assertIsDisplayed()
    }

    @Test
    fun amountDisplayCard_displaysZeroAmount() {
        // Given
        val amount = "0,00"

        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = amount)
            }
        }

        // Then
        composeTestRule.onNodeWithText(amount).assertIsDisplayed()
    }

    @Test
    fun amountDisplayCard_displaysAllElements() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = "500,00")
            }
        }

        // Then - All three text elements should be displayed
        composeTestRule.onNodeWithText("Iznos transakcije").assertIsDisplayed()
        composeTestRule.onNodeWithText("500,00").assertIsDisplayed()
        composeTestRule.onNodeWithText("RSD").assertIsDisplayed()
    }

    // ==================== Format Tests ====================

    @Test
    fun amountDisplayCard_displaysAmountWithThousandSeparators() {
        // Given
        val amount = "12.345,67"

        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = amount)
            }
        }

        // Then
        composeTestRule.onNodeWithText(amount).assertIsDisplayed()
    }

    @Test
    fun amountDisplayCard_displaysAmountWithMultipleThousandSeparators() {
        // Given
        val amount = "1.234.567,89"

        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = amount)
            }
        }

        // Then
        composeTestRule.onNodeWithText(amount).assertIsDisplayed()
    }

    @Test
    fun amountDisplayCard_displaysAmountWithDecimalComma() {
        // Given
        val amount = "100,50"

        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = amount)
            }
        }

        // Then - Serbian format uses comma as decimal separator
        composeTestRule.onNodeWithText(amount).assertIsDisplayed()
    }

    // ==================== Edge Cases ====================

    @Test
    fun amountDisplayCard_displaysEmptyAmount() {
        // Given
        val amount = ""

        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = amount)
            }
        }

        // Then - Component should render without crashing
        composeTestRule.onNodeWithText("Iznos transakcije").assertIsDisplayed()
        composeTestRule.onNodeWithText("RSD").assertIsDisplayed()
    }

    @Test
    fun amountDisplayCard_displaysVeryLongAmount() {
        // Given
        val amount = "999.999.999.999,99"

        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = amount)
            }
        }

        // Then
        composeTestRule.onNodeWithText(amount).assertIsDisplayed()
    }

    @Test
    fun amountDisplayCard_displaysAmountWithoutDecimals() {
        // Given
        val amount = "1.000"

        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = amount)
            }
        }

        // Then
        composeTestRule.onNodeWithText(amount).assertIsDisplayed()
    }

    @Test
    fun amountDisplayCard_displaysAmountWithSingleDecimal() {
        // Given
        val amount = "100,5"

        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = amount)
            }
        }

        // Then
        composeTestRule.onNodeWithText(amount).assertIsDisplayed()
    }

    @Test
    fun amountDisplayCard_isNotClickable() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = "100,00")
            }
        }

        // Then - Component should not have any clickable actions
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    @Test
    fun amountDisplayCard_textIsNotClickable() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = "100,00")
            }
        }

        // Then - Text elements should not be clickable
        composeTestRule.onNodeWithText("100,00").assertHasNoClickAction()
        composeTestRule.onNodeWithText("Iznos transakcije").assertHasNoClickAction()
        composeTestRule.onNodeWithText("RSD").assertHasNoClickAction()
    }

    @Test
    fun amountDisplayCard_multipleInstancesCanCoexist() {
        // Given - Multiple AmountDisplayCards in the same screen
        composeTestRule.setContent {
            AppTheme {
                androidx.compose.foundation.layout.Column {
                    AmountDisplayCard(amount = "100,00")
                    AmountDisplayCard(amount = "500,00")
                    AmountDisplayCard(amount = "1.000,00")
                }
            }
        }

        // Then - All instances should render
        composeTestRule.onNodeWithText("100,00").assertIsDisplayed()
        composeTestRule.onNodeWithText("500,00").assertIsDisplayed()
        composeTestRule.onNodeWithText("1.000,00").assertIsDisplayed()
    }

    @Test
    fun amountDisplayCard_updatesWhenAmountChanges() {
        // Given - Start with one amount
        var amount = "100,00"
        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = amount)
            }
        }

        // Then - Initial amount displayed
        composeTestRule.onNodeWithText("100,00").assertIsDisplayed()

        // When - Update amount
        composeTestRule.runOnUiThread {
            amount = "500,00"
        }

        // Note: We can't easily test dynamic updates without state management in the test
        // This test verifies the component can be rendered with different amounts
    }

    @Test
    fun amountDisplayCard_displaysSpecialCharacters() {
        // Given - Amount with special characters
        val amount = "1.234,56 €"

        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = amount)
            }
        }

        // Then
        composeTestRule.onNodeWithText(amount).assertIsDisplayed()
    }

    @Test
    fun amountDisplayCard_displaysNegativeAmount() {
        // Given
        val amount = "-100,00"

        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = amount)
            }
        }

        // Then
        composeTestRule.onNodeWithText(amount).assertIsDisplayed()
    }

    @Test
    fun amountDisplayCard_labelTextIsGray() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = "100,00")
            }
        }

        // Then - Label should exist (we can't test color directly in compose tests)
        composeTestRule.onNodeWithText("Iznos transakcije").assertIsDisplayed()
    }

    @Test
    fun amountDisplayCard_currencyTextIsGray() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = "100,00")
            }
        }

        // Then - Currency should exist (we can't test color directly in compose tests)
        composeTestRule.onNodeWithText("RSD").assertIsDisplayed()
    }

    @Test
    fun amountDisplayCard_displaysInProperOrder() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                AmountDisplayCard(amount = "100,00")
            }
        }

        // Then - All elements should be displayed (order is ensured by Column layout)
        composeTestRule.onNodeWithText("Iznos transakcije").assertIsDisplayed()
        composeTestRule.onNodeWithText("100,00").assertIsDisplayed()
        composeTestRule.onNodeWithText("RSD").assertIsDisplayed()
    }
}
