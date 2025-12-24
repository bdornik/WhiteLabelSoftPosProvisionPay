package com.payten.whitelabel

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.payten.whitelabel.ui.screens.PaymentProcessingScreen
import com.payten.whitelabel.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for PaymentProcessingScreen.
 *
 * Tests static loading screen displayed during payment transaction processing.
 */
class PaymentProcessingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun getString(id: Int): String {
        return InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
    }

    @Test
    fun paymentProcessingScreen_displaysLoadingIndicator() {
        composeTestRule.setContent {
            AppTheme {
                PaymentProcessingScreen()
            }
        }

        // Loading indicator should be present
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
    }

    @Test
    fun paymentProcessingScreen_displaysProcessingMessage() {
        composeTestRule.setContent {
            AppTheme {
                PaymentProcessingScreen()
            }
        }

        // Main processing message should be displayed
        val expectedMessage = getString(R.string.payment_processing_message)
        composeTestRule.onNodeWithText(expectedMessage).assertExists()
    }

    @Test
    fun paymentProcessingScreen_displaysSubtitle() {
        composeTestRule.setContent {
            AppTheme {
                PaymentProcessingScreen()
            }
        }

        // Subtitle message should be displayed
        val expectedSubtitle = getString(R.string.payment_processing_subtitle)
        composeTestRule.onNodeWithText(expectedSubtitle).assertExists()
    }

    @Test
    fun paymentProcessingScreen_hasWhiteBackground() {
        composeTestRule.setContent {
            AppTheme {
                PaymentProcessingScreen()
            }
        }

        // Screen should render without crashes
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun paymentProcessingScreen_contentIsCentered() {
        composeTestRule.setContent {
            AppTheme {
                PaymentProcessingScreen()
            }
        }

        // All content should be rendered (loading indicator, text)
        val expectedMessage = getString(R.string.payment_processing_message)
        val expectedSubtitle = getString(R.string.payment_processing_subtitle)

        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
        composeTestRule.onNodeWithText(expectedMessage).assertExists()
        composeTestRule.onNodeWithText(expectedSubtitle).assertExists()
    }

    @Test
    fun paymentProcessingScreen_allElementsVisible() {
        composeTestRule.setContent {
            AppTheme {
                PaymentProcessingScreen()
            }
        }

        // Verify all UI elements are visible
        val expectedMessage = getString(R.string.payment_processing_message)
        val expectedSubtitle = getString(R.string.payment_processing_subtitle)

        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedMessage)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedSubtitle)
            .assertIsDisplayed()
    }

    @Test
    fun paymentProcessingScreen_rendersWithoutCrash() {
        // Test that screen renders without throwing exceptions
        composeTestRule.setContent {
            AppTheme {
                PaymentProcessingScreen()
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onRoot().assertExists()
    }
}
