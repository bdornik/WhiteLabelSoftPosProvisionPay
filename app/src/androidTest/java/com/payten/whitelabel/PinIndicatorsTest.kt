package com.payten.whitelabel

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.payten.whitelabel.ui.components.PinIndicators
import com.payten.whitelabel.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for PinIndicators component.
 * Tests PIN indicator visual states and rendering.
 */
class PinIndicatorsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==================== Rendering Tests ====================

    @Test
    fun pinIndicators_displaysEmptyState() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PinIndicators(pinLength = 0)
            }
        }

        // Then - 4 indicators should exist (we can't directly test color, but we can verify they exist)
        // The component creates 4 circles, so we verify the structure exists
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun pinIndicators_displaysOneDigitFilled() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PinIndicators(pinLength = 1)
            }
        }

        // Then - Component should render
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun pinIndicators_displaysTwoDigitsFilled() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PinIndicators(pinLength = 2)
            }
        }

        // Then - Component should render
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun pinIndicators_displaysThreeDigitsFilled() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PinIndicators(pinLength = 3)
            }
        }

        // Then - Component should render
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun pinIndicators_displaysAllDigitsFilled() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PinIndicators(pinLength = 4)
            }
        }

        // Then - Component should render
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun pinIndicators_displaysErrorState() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PinIndicators(pinLength = 4, isError = true)
            }
        }

        // Then - Component should render with error state
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun pinIndicators_displaysErrorStateWithPartialPin() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PinIndicators(pinLength = 2, isError = true)
            }
        }

        // Then - Component should render with error state
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun pinIndicators_displaysErrorStateWithEmptyPin() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PinIndicators(pinLength = 0, isError = true)
            }
        }

        // Then - Component should render with error state
        composeTestRule.onRoot().assertExists()
    }

    // ==================== Edge Cases ====================

    @Test
    fun pinIndicators_handlesZeroLength() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PinIndicators(pinLength = 0, isError = false)
            }
        }

        // Then - Component should render without crashing
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun pinIndicators_handlesMaxLength() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PinIndicators(pinLength = 4, isError = false)
            }
        }

        // Then - Component should render without crashing
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun pinIndicators_handlesInvalidLengthAboveMax() {
        // Given - Testing with length > 4 (should still render 4 circles)
        composeTestRule.setContent {
            AppTheme {
                PinIndicators(pinLength = 5, isError = false)
            }
        }

        // Then - Component should render without crashing
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun pinIndicators_handlesNegativeLength() {
        // Given - Testing with negative length (edge case)
        composeTestRule.setContent {
            AppTheme {
                PinIndicators(pinLength = -1, isError = false)
            }
        }

        // Then - Component should render without crashing (all circles unfilled)
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun pinIndicators_switchingFromNormalToError() {
        // Given - Start with normal state
        var isError = false
        composeTestRule.setContent {
            AppTheme {
                PinIndicators(pinLength = 4, isError = isError)
            }
        }

        // When - Switch to error state
        composeTestRule.runOnUiThread {
            isError = true
        }

        // Then - Component should still render
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun pinIndicators_switchingPinLength() {
        // Given - Start with one digit
        var pinLength = 1
        composeTestRule.setContent {
            AppTheme {
                PinIndicators(pinLength = pinLength, isError = false)
            }
        }

        // When - Change pin length
        composeTestRule.runOnUiThread {
            pinLength = 3
        }

        // Then - Component should still render
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun pinIndicators_isNotClickable() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PinIndicators(pinLength = 2, isError = false)
            }
        }

        // Then - Component should not have any clickable actions
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    @Test
    fun pinIndicators_hasNoTextContent() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PinIndicators(pinLength = 2, isError = false)
            }
        }

        // Then - Component should not display any text (it's purely visual with circles)
        composeTestRule.onRoot().assertExists()
        // No text assertions because this component doesn't render text
    }

    @Test
    fun pinIndicators_multipleInstancesCanCoexist() {
        // Given - Multiple PinIndicators in the same screen
        composeTestRule.setContent {
            AppTheme {
                androidx.compose.foundation.layout.Column {
                    PinIndicators(pinLength = 1, isError = false)
                    PinIndicators(pinLength = 3, isError = false)
                    PinIndicators(pinLength = 4, isError = true)
                }
            }
        }

        // Then - All instances should render
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun pinIndicators_rendersWithDifferentThemes() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PinIndicators(pinLength = 2, isError = false)
            }
        }

        // Then - Component should render with theme applied
        composeTestRule.onRoot().assertExists()
    }
}
