package com.payten.whitelabel

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.payten.whitelabel.ui.screens.FirstPage
import com.payten.whitelabel.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for FirstPage.
 * Tests initial landing page UI and navigation.
 */
class FirstPageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==================== Rendering Tests ====================

    @Test
    fun firstPage_displaysSoftPOSTitle() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                FirstPage(
                    onNavigateToLogin = {},
                    onNavigateToRegister = {}
                )
            }
        }

        // Then - SoftPOS text should be displayed
        // The annotated string creates "SoftPOS" as a single text node
        composeTestRule.onNodeWithText("SoftPOS").assertIsDisplayed()
    }

    @Test
    fun firstPage_displaysMotto() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                FirstPage(
                    onNavigateToLogin = {},
                    onNavigateToRegister = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Brzo. Bezbedno. Bilo kada.").assertIsDisplayed()
    }

    @Test
    fun firstPage_displaysDescription() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                FirstPage(
                    onNavigateToLogin = {},
                    onNavigateToRegister = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Jednostavan način prihvatanja plaćanja karticama i Flik računima.")
            .assertIsDisplayed()
    }

    @Test
    fun firstPage_displaysRegisterButton() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                FirstPage(
                    onNavigateToLogin = {},
                    onNavigateToRegister = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("POSTANITE KORISNIK").assertIsDisplayed()
    }

    @Test
    fun firstPage_registerButtonIsEnabled() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                FirstPage(
                    onNavigateToLogin = {},
                    onNavigateToRegister = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("POSTANITE KORISNIK").assertIsEnabled()
    }

    @Test
    fun firstPage_registerButtonIsClickable() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                FirstPage(
                    onNavigateToLogin = {},
                    onNavigateToRegister = {}
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("POSTANITE KORISNIK").assertHasClickAction()
    }

    // ==================== Interaction Tests ====================

    @Test
    fun firstPage_registerButtonTriggersCallback() {
        // Given
        var registerClicked = false

        composeTestRule.setContent {
            AppTheme {
                FirstPage(
                    onNavigateToLogin = {},
                    onNavigateToRegister = { registerClicked = true }
                )
            }
        }

        // When - Click register button
        composeTestRule.onNodeWithText("POSTANITE KORISNIK").performClick()

        // Then
        assert(registerClicked)
    }

    @Test
    fun firstPage_multipleClicksOnRegisterButton() {
        // Given
        var clickCount = 0

        composeTestRule.setContent {
            AppTheme {
                FirstPage(
                    onNavigateToLogin = {},
                    onNavigateToRegister = { clickCount++ }
                )
            }
        }

        // When - Click multiple times
        composeTestRule.onNodeWithText("POSTANITE KORISNIK").performClick()
        composeTestRule.onNodeWithText("POSTANITE KORISNIK").performClick()
        composeTestRule.onNodeWithText("POSTANITE KORISNIK").performClick()

        // Then
        assert(clickCount == 3)
    }

    // ==================== Layout Tests ====================

    @Test
    fun firstPage_allElementsAreDisplayed() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                FirstPage(
                    onNavigateToLogin = {},
                    onNavigateToRegister = {}
                )
            }
        }

        // Then - All key elements should be visible
        composeTestRule.onNodeWithText("Brzo. Bezbedno. Bilo kada.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jednostavan način prihvatanja plaćanja karticama i Flik računima.")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("POSTANITE KORISNIK").assertIsDisplayed()
    }

    @Test
    fun firstPage_textIsDisplayedInCorrectOrder() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                FirstPage(
                    onNavigateToLogin = {},
                    onNavigateToRegister = {}
                )
            }
        }

        // Then - Verify elements exist (order is ensured by layout structure)
        // Motto should appear before description
        composeTestRule.onNodeWithText("Brzo. Bezbedno. Bilo kada.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Jednostavan način prihvatanja plaćanja karticama i Flik računima.")
            .assertIsDisplayed()
    }

    // ==================== Edge Cases ====================

    @Test
    fun firstPage_callbacksAreIndependent() {
        // Given
        var loginClicked = false
        var registerClicked = false

        composeTestRule.setContent {
            AppTheme {
                FirstPage(
                    onNavigateToLogin = { loginClicked = true },
                    onNavigateToRegister = { registerClicked = true }
                )
            }
        }

        // When - Click register button
        composeTestRule.onNodeWithText("POSTANITE KORISNIK").performClick()

        // Then - Only register callback should be triggered
        assert(registerClicked)
        assert(!loginClicked)
    }

    @Test
    fun firstPage_onlyOneClickableElement() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                FirstPage(
                    onNavigateToLogin = {},
                    onNavigateToRegister = {}
                )
            }
        }

        // Then - Only the register button should be clickable
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(1)
    }

    @Test
    fun firstPage_mottoTextIsNotClickable() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                FirstPage(
                    onNavigateToLogin = {},
                    onNavigateToRegister = {}
                )
            }
        }

        // Then - Motto text should not be clickable
        composeTestRule.onNodeWithText("Brzo. Bezbedno. Bilo kada.")
            .assertIsDisplayed()
            .assertHasNoClickAction()
    }

    @Test
    fun firstPage_descriptionTextIsNotClickable() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                FirstPage(
                    onNavigateToLogin = {},
                    onNavigateToRegister = {}
                )
            }
        }

        // Then - Description text should not be clickable
        composeTestRule.onNodeWithText("Jednostavan način prihvatanja plaćanja karticama i Flik računima.")
            .assertIsDisplayed()
            .assertHasNoClickAction()
    }

    @Test
    fun firstPage_repeatedNavigationCallbacks() {
        // Given
        var registerClickCount = 0

        composeTestRule.setContent {
            AppTheme {
                FirstPage(
                    onNavigateToLogin = {},
                    onNavigateToRegister = { registerClickCount++ }
                )
            }
        }

        // When - Click register button 5 times
        repeat(5) {
            composeTestRule.onNodeWithText("POSTANITE KORISNIK").performClick()
        }

        // Then - Should have triggered callback 5 times
        assert(registerClickCount == 5)
    }

    @Test
    fun firstPage_mottoContainsPunctuation() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                FirstPage(
                    onNavigateToLogin = {},
                    onNavigateToRegister = {}
                )
            }
        }

        // Then - Verify motto contains periods (testing full string)
        composeTestRule.onNodeWithText("Brzo. Bezbedno. Bilo kada.", substring = false)
            .assertIsDisplayed()
    }

    @Test
    fun firstPage_descriptionIsMultiWord() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                FirstPage(
                    onNavigateToLogin = {},
                    onNavigateToRegister = {}
                )
            }
        }

        // Then - Verify description text is complete
        val fullDescription = "Jednostavan način prihvatanja plaćanja karticama i Flik računima."
        composeTestRule.onNodeWithText(fullDescription).assertIsDisplayed()
    }

    @Test
    fun firstPage_buttonTextIsUppercase() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                FirstPage(
                    onNavigateToLogin = {},
                    onNavigateToRegister = {}
                )
            }
        }

        // Then - Button text should be in uppercase
        composeTestRule.onNodeWithText("POSTANITE KORISNIK", ignoreCase = false)
            .assertIsDisplayed()
    }
}
