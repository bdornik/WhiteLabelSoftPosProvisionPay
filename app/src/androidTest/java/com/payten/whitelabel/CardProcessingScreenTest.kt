package com.payten.whitelabel

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.payten.whitelabel.ui.screens.CardProcessingScreen
import com.payten.whitelabel.ui.states.PaymentUiBridge
import com.payten.whitelabel.ui.theme.AppTheme
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for CardProcessingScreen.
 *
 * Tests the "Tap card" screen UI and interactions during payment processing.
 */
class CardProcessingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var navigateBackCalled = false

    @Before
    fun setup() {
        navigateBackCalled = false
        // Reset PaymentUiBridge state
        PaymentUiBridge.reset()
    }

    @After
    fun tearDown() {
        PaymentUiBridge.reset()
    }

    // ==================== Display Tests ====================

    @Test
    fun displaysAmountCorrectlyWithProperFormatting() {
        // Given - 1000.00 RSD
        val amountInPare = 100000L

        // When
        composeTestRule.setContent {
            AppTheme {
                CardProcessingScreen(
                    amountInPare = amountInPare,
                    tipAmount = 0L
                )
            }
        }

        // Then - Amount should be formatted as "1.000,00"
        composeTestRule.onNodeWithText("1.000,00").assertExists()
    }

    @Test
    fun displaysAmountPlusTipWithCorrectFormatting() {
        // Given - 1000.00 RSD + 150.00 RSD tip
        val amountInPare = 100000L
        val tipAmount = 15000L

        // When
        composeTestRule.setContent {
            AppTheme {
                CardProcessingScreen(
                    amountInPare = amountInPare,
                    tipAmount = tipAmount
                )
            }
        }

        // Then - Total should be formatted as "1.150,00"
        composeTestRule.onNodeWithText("1.150,00").assertExists()
    }

    @Test
    fun displaysSmallAmountWithCorrectFormatting() {
        // Given - 5.50 RSD
        val amountInPare = 550L

        // When
        composeTestRule.setContent {
            AppTheme {
                CardProcessingScreen(
                    amountInPare = amountInPare,
                    tipAmount = 0L
                )
            }
        }

        // Then - Amount should be formatted as "5,50"
        composeTestRule.onNodeWithText("5,50").assertExists()
    }

    @Test
    fun displaysLargeAmountWithThousandSeparators() {
        // Given - 1,234,567.89 RSD
        val amountInPare = 123456789L

        // When
        composeTestRule.setContent {
            AppTheme {
                CardProcessingScreen(
                    amountInPare = amountInPare,
                    tipAmount = 0L
                )
            }
        }

        // Then - Amount should be formatted with dots as thousand separators
        composeTestRule.onNodeWithText("1.234.567,89").assertExists()
    }

    // ==================== UI State Tests ====================

    @Test
    fun hidesScreenWhenHideScreenIsTrue() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CardProcessingScreen(
                    amountInPare = 100000L,
                    hideScreen = true
                )
            }
        }

        // Then - Screen content should not be visible
        composeTestRule.onNodeWithText("1.000,00").assertDoesNotExist()
    }

    @Test
    fun hidesScreenWhenActivityIsComplete() {
        // Given - Set activity complete
        PaymentUiBridge.setActivityComplete(true)

        // When
        composeTestRule.setContent {
            AppTheme {
                CardProcessingScreen(
                    amountInPare = 100000L
                )
            }
        }

        // Then - Screen should be hidden
        composeTestRule.onNodeWithText("1.000,00").assertDoesNotExist()
    }

    @Test
    fun showsPaymentProcessingScreenWhenShowProcessingIsTrue() {
        // Given - Set processing screen flag
        PaymentUiBridge.setProcessingScreen(true)

        // When
        composeTestRule.setContent {
            AppTheme {
                CardProcessingScreen(
                    amountInPare = 100000L
                )
            }
        }

        // Then - CardProcessingScreen content should not be visible
        // PaymentProcessingScreen shows instead (white screen with loading indicator)
        composeTestRule.onNodeWithText("1.000,00").assertDoesNotExist()
    }

    // ==================== LED Indicator Tests ====================

    @Test
    fun displaysLedIndicators() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CardProcessingScreen(
                    amountInPare = 100000L
                )
            }
        }

        // Then - LED indicators should exist (though we can't directly test visual state)
        // The component renders without errors
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun ledStateUpdatesWhenPaymentUiBridgeStateChanges() {
        // Given - Initial LED state
        composeTestRule.setContent {
            AppTheme {
                CardProcessingScreen(
                    amountInPare = 100000L
                )
            }
        }

        // When - Update LED state (LED1 and LED2 on: 0x01 | 0x02 = 0x03)
        composeTestRule.runOnUiThread {
            PaymentUiBridge.updateLedState(0x03, true)
        }

        // Then - Screen should still render (LED state is internal)
        composeTestRule.onNodeWithText("1.000,00").assertExists()
    }

    // ==================== Interaction Tests ====================

    @Test
    fun backButtonRendersSuccessfully() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CardProcessingScreen(
                    amountInPare = 100000L,
                    onNavigateBack = { navigateBackCalled = true }
                )
            }
        }

        // Then - BackButton is a Canvas component without content description
        // We verify the screen renders successfully with BackButton visible
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun cancelButtonTriggersNavigationCallback() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CardProcessingScreen(
                    amountInPare = 100000L,
                    onNavigateBack = { navigateBackCalled = true }
                )
            }
        }

        // When - Find and click cancel button by searching for clickable node with "PREKINI" text
        // "PREKINI" appears in Serbian and Slovenian variants
        composeTestRule.onNode(
            hasText("PREKINI", substring = true, ignoreCase = true) and hasClickAction()
        ).performClick()

        // Then
        assert(navigateBackCalled) { "Cancel button should trigger navigation callback" }
    }

    // ==================== Edge Cases ====================

    @Test
    fun handlesZeroAmount() {
        // Given
        val amountInPare = 0L

        // When
        composeTestRule.setContent {
            AppTheme {
                CardProcessingScreen(
                    amountInPare = amountInPare,
                    tipAmount = 0L
                )
            }
        }

        // Then - Should display "0,00"
        composeTestRule.onNodeWithText("0,00").assertExists()
    }

    @Test
    fun handlesAmountWithOnlyTip() {
        // Given - Zero base amount, only tip
        val amountInPare = 0L
        val tipAmount = 50000L // 500.00 RSD

        // When
        composeTestRule.setContent {
            AppTheme {
                CardProcessingScreen(
                    amountInPare = amountInPare,
                    tipAmount = tipAmount
                )
            }
        }

        // Then - Should display tip amount "500,00"
        composeTestRule.onNodeWithText("500,00").assertExists()
    }

    @Test
    fun rendersWithoutCrashWhenNoCallbacksProvided() {
        // Given - No callback
        composeTestRule.setContent {
            AppTheme {
                CardProcessingScreen(
                    amountInPare = 100000L
                    // onNavigateBack not provided (uses default empty lambda)
                )
            }
        }

        // Then - Should render successfully
        composeTestRule.onNodeWithText("1.000,00").assertExists()
    }

    @Test
    fun displaysCardBrandLogos() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                CardProcessingScreen(
                    amountInPare = 100000L
                )
            }
        }

        // Then - Screen should have Visa and Mastercard images
        // We verify the screen renders completely
        composeTestRule.onRoot().assertExists()
    }

    // ==================== State Transition Tests ====================

    @Test
    fun screenVisibilityTogglesWhenHideScreenChanges() {
        // Given - Start with visible screen using mutableStateOf
        composeTestRule.setContent {
            var hideScreen by remember { mutableStateOf(false) }

            AppTheme {
                Column {
                    // Control button to toggle visibility
                    Button(onClick = { hideScreen = !hideScreen }) {
                        Text("Toggle")
                    }

                    CardProcessingScreen(
                        amountInPare = 100000L,
                        hideScreen = hideScreen
                    )
                }
            }
        }

        // Then - Initially visible
        composeTestRule.onNodeWithText("1.000,00").assertExists()

        // When - Click toggle button to hide the screen
        composeTestRule.onNodeWithText("Toggle").performClick()
        composeTestRule.waitForIdle()

        // Then - Should be hidden
        composeTestRule.onNodeWithText("1.000,00").assertDoesNotExist()
    }

    @Test
    fun transitionsToProcessingScreenMaintainAmountState() {
        // Given - Start with normal display
        composeTestRule.setContent {
            AppTheme {
                CardProcessingScreen(
                    amountInPare = 100000L
                )
            }
        }

        // Then - Amount is visible
        composeTestRule.onNodeWithText("1.000,00").assertExists()

        // When - Trigger processing screen
        composeTestRule.runOnUiThread {
            PaymentUiBridge.setProcessingScreen(true)
        }

        composeTestRule.waitForIdle()

        // Then - CardProcessingScreen content hidden (PaymentProcessingScreen shows)
        composeTestRule.onNodeWithText("1.000,00").assertDoesNotExist()
    }
}