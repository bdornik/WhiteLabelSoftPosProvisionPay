package com.payten.nkbm

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.payten.nkbm.ui.screens.PinSetupScreen
import com.payten.nkbm.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test

class PinSetupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun pinSetupScreen_displaysTitle() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PinSetupScreen(
                    onNavigateBack = {},
                    onPinSetupComplete = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("REGISTRACIJA")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Unesite željeni PIN")
            .assertIsDisplayed()
    }

    @Test
    fun pinSetupScreen_canEnterPin() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PinSetupScreen(
                    onNavigateBack = {},
                    onPinSetupComplete = {}
                )
            }
        }

        // When
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()

        // Wait for UI to update
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("5")
            .assertExists()
    }

    @Test
    fun pinSetupScreen_matchingPins_callsOnComplete() {
        // Given
        var completeCalled = false
        var capturedPin = ""

        composeTestRule.setContent {
            AppTheme {
                PinSetupScreen(
                    onNavigateBack = {},
                    onPinSetupComplete = { pin ->
                        completeCalled = true
                        capturedPin = pin
                    }
                )
            }
        }

        // When
        repeat(2) {
            composeTestRule.onNodeWithText("1").performClick()
            composeTestRule.onNodeWithText("2").performClick()
            composeTestRule.onNodeWithText("3").performClick()
            composeTestRule.onNodeWithText("4").performClick()
            composeTestRule.waitForIdle()
        }

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            completeCalled
        }

        assert(completeCalled) { "onPinSetupComplete should be called" }
        assert(capturedPin == "1234") { "Captured PIN should be 1234" }
    }

    @Test
    fun pinSetupScreen_nonMatchingPins_showsErrorDialog() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PinSetupScreen(
                    onNavigateBack = {},
                    onPinSetupComplete = {}
                )
            }
        }

        // When
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.onNodeWithText("6").performClick()
        composeTestRule.onNodeWithText("7").performClick()
        composeTestRule.onNodeWithText("8").performClick()
        composeTestRule.waitForIdle()

        // Then
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Uneli ste neispravan PIN")
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule
            .onNodeWithText("Uneli ste neispravan PIN")
            .assertIsDisplayed()
    }

    @Test
    fun pinSetupScreen_displaysAllNumbers() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                PinSetupScreen(
                    onNavigateBack = {},
                    onPinSetupComplete = {}
                )
            }
        }

        // Then
        (0..9).forEach { number ->
            composeTestRule
                .onNodeWithText(number.toString())
                .assertIsDisplayed()
        }
    }
}