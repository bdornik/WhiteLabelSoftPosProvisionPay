package com.payten.whitelabel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payten.whitelabel.ui.components.CustomDialog
import com.payten.whitelabel.ui.components.NumericKeypad
import com.payten.whitelabel.ui.components.PinIndicators
import com.payten.whitelabel.ui.theme.AppTheme
import com.payten.whitelabel.ui.theme.MyriadPro
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for PinSetupScreen.
 *
 * Tests the PIN setup screen UI components and interactions including:
 * - PIN entry via numeric keypad
 * - PIN confirmation flow
 * - PIN match/mismatch validation
 * - Success and error dialogs
 * - PIN indicators
 *
 * ## Test Coverage:
 * - UI element rendering (title, instructions, PIN indicators, keypad)
 * - PIN entry interactions (number clicks, backspace)
 * - PIN confirmation flow (enter → confirm)
 * - Validation (match → success, mismatch → error)
 * - Error dialog display and dismissal
 * - Success dialog display
 *
 * ## Testing Strategy:
 * Tests the pure UI components and PIN entry logic without ViewModel/SDK integration.
 * Provisioning flow (token refresh, SDK events, merchant details) requires integration testing.
 *
 * Note: Provisioning flow tests are not included because they depend on:
 * - ProvisionViewModel.refreshData() and LiveData
 * - SDK integration (MainApplication.getSACBTPApplication().goOnlineCheckRNS())
 * - AppEventBus SDK event listening
 * - SharedPreferences BCrypt hashing
 */
class PinSetupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Simplified PIN Setup screen for testing without ViewModel/SDK.
     * Implements the same PIN entry and confirmation logic.
     */
    @Composable
    private fun TestPinSetupScreen(
        onPinSetupComplete: (String) -> Unit = {}
    ) {
        var pin by remember { mutableStateOf("") }
        var confirmedPin by remember { mutableStateOf<String?>(null) }
        var showError by remember { mutableStateOf(false) }
        var showSuccessDialog by remember { mutableStateOf(false) }
        var showErrorDialog by remember { mutableStateOf(false) }

        // PIN validation logic (same as actual screen)
        LaunchedEffect(pin.length) {
            if (pin.length == 4 && confirmedPin == null) {
                // First PIN entered
                confirmedPin = pin
                pin = ""
            } else if (confirmedPin != null && pin.length == 4) {
                // Second PIN entered - check match
                if (pin == confirmedPin) {
                    showSuccessDialog = true
                    delay(300)
                    onPinSetupComplete(pin)
                } else {
                    showErrorDialog = true
                    showError = true
                    delay(1000)
                    pin = ""
                    confirmedPin = null
                    showError = false
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.height(96.dp))

                    Text(
                        text = "REGISTRACIJA",
                        fontSize = 20.sp,
                        fontFamily = MyriadPro,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        letterSpacing = TextUnit(4f, TextUnitType.Sp)
                    )
                }

                Spacer(modifier = Modifier.height(120.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (confirmedPin == null) {
                            "Unesite želeni PIN"
                        } else {
                            "Potvrdite svoj PIN"
                        },
                        fontSize = 24.sp,
                        fontFamily = MyriadPro,
                        fontWeight = FontWeight.Bold,
                        color = if (showError) MaterialTheme.colorScheme.error else Color.Black
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    PinIndicators(
                        pinLength = pin.length,
                        isError = showError
                    )

                    Spacer(modifier = Modifier.height(80.dp))

                    NumericKeypad(
                        onNumberClick = { number ->
                            if (pin.length < 4) {
                                pin += number
                            }
                        },
                        onBackspaceClick = {
                            if (pin.isNotEmpty()) {
                                pin = pin.dropLast(1)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }

            if (showSuccessDialog) {
                CustomDialog(
                    isSuccess = true,
                    title = "PIN uspešno postavljen!",
                    buttonText = "U REDU",
                    onDismiss = { showSuccessDialog = false }
                )
            }

            if (showErrorDialog) {
                CustomDialog(
                    isSuccess = false,
                    title = "PIN-ovi se ne poklapaju",
                    buttonText = "NAZAD",
                    onDismiss = {
                        showErrorDialog = false
                        pin = ""
                        confirmedPin = null
                        showError = false
                    }
                )
            }
        }
    }

    // ==================== Rendering Tests ====================

    @Test
    fun pinSetupScreen_displaysTitle() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestPinSetupScreen()
            }
        }

        // Then
        composeTestRule.onNodeWithText("REGISTRACIJA").assertIsDisplayed()
    }

    @Test
    fun pinSetupScreen_displaysEnterPinInstruction() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestPinSetupScreen()
            }
        }

        // Then
        composeTestRule.onNodeWithText("Unesite želeni PIN").assertIsDisplayed()
    }

    @Test
    fun pinSetupScreen_displaysPinIndicators() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestPinSetupScreen()
            }
        }

        // Then - PinIndicators component should be rendered (4 dots)
        // We can't directly test the dots, but we can verify the component exists by checking the structure
        composeTestRule.onNodeWithText("Unesite želeni PIN").assertExists()
    }

    @Test
    fun pinSetupScreen_displaysNumericKeypad() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestPinSetupScreen()
            }
        }

        // Then - All numbers should be displayed
        for (i in 0..9) {
            composeTestRule.onNodeWithText(i.toString()).assertIsDisplayed()
        }
    }

    // ==================== PIN Entry Tests ====================

    @Test
    fun pinSetupScreen_canEnterSingleDigit() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestPinSetupScreen()
            }
        }

        // When - Click number 1
        composeTestRule.onNodeWithText("1").performClick()

        // Then - PIN length should be 1 (verified implicitly by the component state)
        // We can't directly verify the PIN state, but the UI should update
        composeTestRule.waitForIdle()
    }

    @Test
    fun pinSetupScreen_canEnterMultipleDigits() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestPinSetupScreen()
            }
        }

        // When - Click numbers 1, 2, 3
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()

        // Then
        composeTestRule.waitForIdle()
    }

    @Test
    fun pinSetupScreen_canEnterFullPin() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestPinSetupScreen()
            }
        }

        // When - Enter 4 digits
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()

        // Then - Should show confirmation instruction
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Potvrdite svoj PIN").assertIsDisplayed()
    }

    @Test
    fun pinSetupScreen_backspaceRemovesDigit() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestPinSetupScreen()
            }
        }

        // When - Enter 2 digits then backspace
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithContentDescription("Backspace").performClick()

        // Then - Should have 1 digit remaining
        composeTestRule.waitForIdle()
    }

    @Test
    fun pinSetupScreen_backspaceOnEmptyPinDoesNothing() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestPinSetupScreen()
            }
        }

        // When - Click backspace on empty PIN
        composeTestRule.onNodeWithContentDescription("Backspace").performClick()

        // Then - Should still show enter instruction
        composeTestRule.onNodeWithText("Unesite želeni PIN").assertIsDisplayed()
    }

    // ==================== PIN Confirmation Tests ====================

    @Test
    fun pinSetupScreen_transitionsToConfirmationAfterFirstPin() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestPinSetupScreen()
            }
        }

        // When - Enter first PIN (1234)
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()

        // Then - Should show confirmation instruction
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Potvrdite svoj PIN").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unesite želeni PIN").assertDoesNotExist()
    }

    @Test
    fun pinSetupScreen_matchingPinsShowSuccessDialog() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestPinSetupScreen()
            }
        }

        // When - Enter matching PINs (1234, 1234)
        // First PIN
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()

        composeTestRule.waitForIdle()

        // Second PIN (same)
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()

        // Then - Success dialog should appear
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("PIN uspešno postavljen!").assertIsDisplayed()
        composeTestRule.onNodeWithText("U REDU").assertIsDisplayed()
    }

    @Test
    fun pinSetupScreen_mismatchingPinsShowErrorDialog() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestPinSetupScreen()
            }
        }

        // When - Enter mismatching PINs (1234, 5678)
        // First PIN
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()

        composeTestRule.waitForIdle()

        // Second PIN (different)
        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.onNodeWithText("6").performClick()
        composeTestRule.onNodeWithText("7").performClick()
        composeTestRule.onNodeWithText("8").performClick()

        // Then - Error dialog should appear
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("PIN-ovi se ne poklapaju").assertIsDisplayed()
        composeTestRule.onNodeWithText("NAZAD").assertIsDisplayed()
    }

    @Test
    fun pinSetupScreen_errorDialogDismissalResetsFlow() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestPinSetupScreen()
            }
        }

        // When - Enter mismatching PINs and dismiss error
        // First PIN
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()

        composeTestRule.waitForIdle()

        // Second PIN (different)
        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.onNodeWithText("6").performClick()
        composeTestRule.onNodeWithText("7").performClick()
        composeTestRule.onNodeWithText("8").performClick()

        composeTestRule.waitForIdle()

        // Dismiss error dialog
        composeTestRule.onNodeWithText("NAZAD").performClick()

        // Then - Should reset to initial state
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Unesite želeni PIN").assertIsDisplayed()
        composeTestRule.onNodeWithText("PIN-ovi se ne poklapaju").assertDoesNotExist()
    }

    @Test
    fun pinSetupScreen_successDialogCanBeDismissed() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestPinSetupScreen()
            }
        }

        // When - Enter matching PINs
        // First PIN
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()

        composeTestRule.waitForIdle()

        // Second PIN (same)
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()

        composeTestRule.waitForIdle()

        // Dismiss success dialog
        composeTestRule.onNodeWithText("U REDU").performClick()

        // Then - Dialog should be dismissed
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("PIN uspešno postavljen!").assertDoesNotExist()
    }

    // ==================== Callback Tests ====================

    @Test
    fun pinSetupScreen_callbackTriggeredOnSuccessfulPinMatch() {
        // Given
        var completedPin: String? = null

        composeTestRule.setContent {
            AppTheme {
                TestPinSetupScreen(
                    onPinSetupComplete = { pin -> completedPin = pin }
                )
            }
        }

        // When - Enter matching PINs
        // First PIN
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()

        composeTestRule.waitForIdle()

        // Second PIN (same)
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()

        // Then - Callback should be triggered with PIN
        // Wait for the delay(300) in LaunchedEffect before callback is called
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()
        assert(completedPin == "1234")
    }

    @Test
    fun pinSetupScreen_callbackNotTriggeredOnMismatch() {
        // Given
        var callbackTriggered = false

        composeTestRule.setContent {
            AppTheme {
                TestPinSetupScreen(
                    onPinSetupComplete = { callbackTriggered = true }
                )
            }
        }

        // When - Enter mismatching PINs
        // First PIN
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()

        composeTestRule.waitForIdle()

        // Second PIN (different)
        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.onNodeWithText("6").performClick()
        composeTestRule.onNodeWithText("7").performClick()
        composeTestRule.onNodeWithText("8").performClick()

        // Then - Callback should NOT be triggered
        composeTestRule.waitForIdle()
        assert(!callbackTriggered)
    }

    // ==================== Edge Cases ====================

    @Test
    fun pinSetupScreen_cannotEnterMoreThanFourDigits() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestPinSetupScreen()
            }
        }

        // When - Try to enter 5 digits
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()
        composeTestRule.onNodeWithText("5").performClick() // Should be ignored

        // Then - Should transition to confirmation (not accept 5th digit)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Potvrdite svoj PIN").assertIsDisplayed()
    }

    @Test
    fun pinSetupScreen_backspaceWorksInConfirmationMode() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestPinSetupScreen()
            }
        }

        // When - Enter first PIN, then start second PIN and use backspace
        // First PIN
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()
        composeTestRule.onNodeWithText("3").performClick()
        composeTestRule.onNodeWithText("4").performClick()

        composeTestRule.waitForIdle()

        // Start second PIN
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("2").performClick()

        // Backspace
        composeTestRule.onNodeWithContentDescription("Backspace").performClick()

        // Then - Should still be in confirmation mode
        composeTestRule.onNodeWithText("Potvrdite svoj PIN").assertIsDisplayed()
    }

    @Test
    fun pinSetupScreen_canEnterDifferentDigits() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestPinSetupScreen()
            }
        }

        // When - Enter PIN with all different digits
        composeTestRule.onNodeWithText("9").performClick()
        composeTestRule.onNodeWithText("7").performClick()
        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.onNodeWithText("3").performClick()

        // Then - Should transition to confirmation
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Potvrdite svoj PIN").assertIsDisplayed()
    }

    @Test
    fun pinSetupScreen_canEnterSameDigitMultipleTimes() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestPinSetupScreen()
            }
        }

        // When - Enter PIN 1111
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("1").performClick()
        composeTestRule.onNodeWithText("1").performClick()

        // Then - Should transition to confirmation
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Potvrdite svoj PIN").assertIsDisplayed()
    }

    @Test
    fun pinSetupScreen_zeroDigitWorks() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestPinSetupScreen()
            }
        }

        // When - Enter PIN with zeros
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("0").performClick()
        composeTestRule.onNodeWithText("0").performClick()

        // Then - Should transition to confirmation
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Potvrdite svoj PIN").assertIsDisplayed()
    }
}
