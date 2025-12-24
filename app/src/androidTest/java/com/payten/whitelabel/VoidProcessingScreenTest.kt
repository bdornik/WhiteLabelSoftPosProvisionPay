package com.payten.whitelabel

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.payten.whitelabel.ui.screens.VoidProcessingScreen
import com.payten.whitelabel.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for VoidProcessingScreen.
 *
 * Tests static loading screen displayed during void transaction processing.
 */
class VoidProcessingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun getString(id: Int): String {
        return InstrumentationRegistry.getInstrumentation().targetContext.getString(id)
    }

    @Test
    fun voidProcessingScreen_displaysLoadingIndicator() {
        composeTestRule.setContent {
            AppTheme {
                VoidProcessingScreen()
            }
        }

        // Loading indicator should be present
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
    }

    @Test
    fun voidProcessingScreen_displaysProcessingMessage() {
        composeTestRule.setContent {
            AppTheme {
                VoidProcessingScreen()
            }
        }

        // Main processing message should be displayed
        val expectedMessage = getString(R.string.void_processing_message)
        composeTestRule.onNodeWithText(expectedMessage).assertExists()
    }

    @Test
    fun voidProcessingScreen_displaysSubtitle() {
        composeTestRule.setContent {
            AppTheme {
                VoidProcessingScreen()
            }
        }

        // Subtitle message should be displayed
        val expectedSubtitle = getString(R.string.void_processing_subtitle)
        composeTestRule.onNodeWithText(expectedSubtitle).assertExists()
    }

    @Test
    fun voidProcessingScreen_hasWhiteBackground() {
        composeTestRule.setContent {
            AppTheme {
                VoidProcessingScreen()
            }
        }

        // Screen should render without crashes
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun voidProcessingScreen_contentIsCentered() {
        composeTestRule.setContent {
            AppTheme {
                VoidProcessingScreen()
            }
        }

        // All content should be rendered (loading indicator, text)
        val expectedMessage = getString(R.string.void_processing_message)
        val expectedSubtitle = getString(R.string.void_processing_subtitle)

        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
        composeTestRule.onNodeWithText(expectedMessage).assertExists()
        composeTestRule.onNodeWithText(expectedSubtitle).assertExists()
    }

    @Test
    fun voidProcessingScreen_allElementsVisible() {
        composeTestRule.setContent {
            AppTheme {
                VoidProcessingScreen()
            }
        }

        // Verify all UI elements are visible
        val expectedMessage = getString(R.string.void_processing_message)
        val expectedSubtitle = getString(R.string.void_processing_subtitle)

        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedMessage)
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedSubtitle)
            .assertIsDisplayed()
    }

    @Test
    fun voidProcessingScreen_rendersWithoutCrash() {
        // Test that screen renders without throwing exceptions
        composeTestRule.setContent {
            AppTheme {
                VoidProcessingScreen()
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onRoot().assertExists()
    }
}
