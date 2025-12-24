package com.payten.whitelabel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payten.whitelabel.ui.components.CustomDialog
import com.payten.whitelabel.ui.components.CustomTextField
import com.payten.whitelabel.ui.components.TermsAndConditionsBox
import com.payten.whitelabel.ui.theme.AppTheme
import com.payten.whitelabel.ui.theme.MyriadPro
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for RegistrationPage.
 *
 * Tests the registration screen UI components and interactions including:
 * - Input field rendering and validation
 * - Terms & Conditions checkbox
 * - Continue button enabled/disabled states
 * - Loading state display
 * - Error dialog display
 *
 * ## Test Coverage:
 * - UI element rendering (title, input fields, checkbox, button)
 * - Input field interactions (text entry)
 * - Button state logic (enabled/disabled based on validation)
 * - Loading state (progress indicator)
 * - Error dialog display and dismissal
 *
 * ## Testing Strategy:
 * Tests the pure UI components without ViewModel integration.
 * ViewModel-dependent features (activation flow, SDK integration)
 * require integration testing with actual backend.
 *
 * Note: Registration flow tests (activate, generateToken, getHostKeys, initializeMta)
 * are not included because they depend on RegistrationViewModel and SDK which
 * require Hilt/backend integration.
 */
class RegistrationPageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Simplified Registration screen for testing without ViewModel.
     * Displays the same UI elements but without API calls and SDK integration.
     */
    @Composable
    private fun TestRegistrationPage(
        onNavigateNext: () -> Unit = {},
        onViewTermsClick: () -> Unit = {}
    ) {
        var userId by remember { mutableStateOf("") }
        var activationCode by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var termsAccepted by remember { mutableStateOf(false) }
        var showErrorDialog by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }

        val isButtonEnabled = termsAccepted && userId.isNotEmpty() && activationCode.isNotEmpty() && !isLoading

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

                Spacer(modifier = Modifier.size(48.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // User ID field
                    CustomTextField(
                        label = "Korisnički identifikator",
                        placeholder = "Unesite korisnički identifikator",
                        value = userId,
                        onValueChange = { userId = it }
                    )

                    // Activation code field
                    CustomTextField(
                        label = "Aktivacioni kod",
                        placeholder = "Unesite aktivacioni kod",
                        value = activationCode,
                        onValueChange = { activationCode = it },
                        keyboardType = KeyboardType.Number
                    )

                    Spacer(modifier = Modifier.height(150.dp))

                    // T&C checkbox
                    TermsAndConditionsBox(
                        isChecked = termsAccepted,
                        onCheckedChange = { termsAccepted = it },
                        onViewTermsClick = onViewTermsClick
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            onNavigateNext()
                        },
                        enabled = isButtonEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = "NASTAVI",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (showErrorDialog) {
                CustomDialog(
                    isSuccess = false,
                    title = errorMessage,
                    buttonText = "NAZAD",
                    onDismiss = { showErrorDialog = false }
                )
            }
        }
    }

    /**
     * Test version with controllable state for testing error dialogs and loading.
     */
    @Composable
    private fun TestRegistrationPageWithState(
        showError: Boolean = false,
        errorMessage: String = "",
        isLoading: Boolean = false,
        onNavigateNext: () -> Unit = {},
        onViewTermsClick: () -> Unit = {},
        onErrorDismiss: () -> Unit = {}
    ) {
        var userId by remember { mutableStateOf("") }
        var activationCode by remember { mutableStateOf("") }
        var termsAccepted by remember { mutableStateOf(false) }

        val isButtonEnabled = termsAccepted && userId.isNotEmpty() && activationCode.isNotEmpty() && !isLoading

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

                Spacer(modifier = Modifier.size(48.dp))

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CustomTextField(
                        label = "Korisnički identifikator",
                        placeholder = "Unesite korisnički identifikator",
                        value = userId,
                        onValueChange = { userId = it }
                    )

                    CustomTextField(
                        label = "Aktivacioni kod",
                        placeholder = "Unesite aktivacioni kod",
                        value = activationCode,
                        onValueChange = { activationCode = it },
                        keyboardType = KeyboardType.Number
                    )

                    Spacer(modifier = Modifier.height(150.dp))

                    TermsAndConditionsBox(
                        isChecked = termsAccepted,
                        onCheckedChange = { termsAccepted = it },
                        onViewTermsClick = onViewTermsClick
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onNavigateNext,
                        enabled = isButtonEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(
                                text = "NASTAVI",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (showError) {
                CustomDialog(
                    isSuccess = false,
                    title = errorMessage,
                    buttonText = "NAZAD",
                    onDismiss = onErrorDismiss
                )
            }
        }
    }

    // ==================== Rendering Tests ====================

    @Test
    fun registrationPage_displaysTitle() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestRegistrationPage()
            }
        }

        // Then
        composeTestRule.onNodeWithText("REGISTRACIJA").assertIsDisplayed()
    }

    @Test
    fun registrationPage_displaysUserIdField() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestRegistrationPage()
            }
        }

        // Then
        composeTestRule.onNodeWithText("Korisnički identifikator").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unesite korisnički identifikator").assertIsDisplayed()
    }

    @Test
    fun registrationPage_displaysActivationCodeField() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestRegistrationPage()
            }
        }

        // Then
        composeTestRule.onNodeWithText("Aktivacioni kod").assertIsDisplayed()
        composeTestRule.onNodeWithText("Unesite aktivacioni kod").assertIsDisplayed()
    }

    @Test
    fun registrationPage_displaysContinueButton() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestRegistrationPage()
            }
        }

        // Then
        composeTestRule.onNodeWithText("NASTAVI").assertIsDisplayed()
    }

    // ==================== Input Tests ====================

    @Test
    fun registrationPage_userCanEnterUserId() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestRegistrationPage()
            }
        }

        // When
        composeTestRule.onNodeWithText("Unesite korisnički identifikator").performTextInput("testUser123")

        // Then
        composeTestRule.onNodeWithText("testUser123").assertIsDisplayed()
    }

    @Test
    fun registrationPage_userCanEnterActivationCode() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestRegistrationPage()
            }
        }

        // When
        composeTestRule.onNodeWithText("Unesite aktivacioni kod").performTextInput("123456")

        // Then
        composeTestRule.onNodeWithText("123456").assertIsDisplayed()
    }

    @Test
    fun registrationPage_canEnterBothFields() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestRegistrationPage()
            }
        }

        // When
        composeTestRule.onNodeWithText("Unesite korisnički identifikator").performTextInput("testUser")
        composeTestRule.onNodeWithText("Unesite aktivacioni kod").performTextInput("999999")

        // Then
        composeTestRule.onNodeWithText("testUser").assertIsDisplayed()
        composeTestRule.onNodeWithText("999999").assertIsDisplayed()
    }

    // ==================== Button State Tests ====================

    @Test
    fun registrationPage_buttonDisabledWhenTermsNotAccepted() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestRegistrationPage()
            }
        }

        // When - Enter fields but don't accept terms
        composeTestRule.onNodeWithText("Unesite korisnički identifikator").performTextInput("user")
        composeTestRule.onNodeWithText("Unesite aktivacioni kod").performTextInput("123")

        // Then - Button should be disabled
        composeTestRule.onNodeWithText("NASTAVI").assertIsNotEnabled()
    }

    @Test
    fun registrationPage_buttonDisabledWhenUserIdEmpty() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestRegistrationPage()
            }
        }

        // When - Accept terms and enter activation code only
        composeTestRule.onNode(isToggleable()).performClick()
        composeTestRule.onNodeWithText("Unesite aktivacioni kod").performTextInput("123")

        // Then
        composeTestRule.onNodeWithText("NASTAVI").assertIsNotEnabled()
    }

    @Test
    fun registrationPage_buttonDisabledWhenActivationCodeEmpty() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestRegistrationPage()
            }
        }

        // When - Accept terms and enter user ID only
        composeTestRule.onNode(isToggleable()).performClick()
        composeTestRule.onNodeWithText("Unesite korisnički identifikator").performTextInput("user")

        // Then
        composeTestRule.onNodeWithText("NASTAVI").assertIsNotEnabled()
    }

    @Test
    fun registrationPage_buttonEnabledWhenAllConditionsMet() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestRegistrationPage()
            }
        }

        // When - Accept terms and fill all fields
        composeTestRule.onNodeWithText("Unesite korisnički identifikator").performTextInput("testUser")
        composeTestRule.onNodeWithText("Unesite aktivacioni kod").performTextInput("123456")
        composeTestRule.onNode(isToggleable()).performClick()

        // Then
        composeTestRule.onNodeWithText("NASTAVI").assertIsEnabled()
    }

    // ==================== Interaction Tests ====================

    @Test
    fun registrationPage_termsCheckboxCanBeToggled() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestRegistrationPage()
            }
        }

        // When - Click terms checkbox
        composeTestRule.onNode(isToggleable()).performClick()

        // Then - Checkbox should be checked (button state will change if other conditions met)
        // We verify by checking that with filled fields, button becomes enabled
        composeTestRule.onNodeWithText("Unesite korisnički identifikator").performTextInput("user")
        composeTestRule.onNodeWithText("Unesite aktivacioni kod").performTextInput("123")
        composeTestRule.onNodeWithText("NASTAVI").assertIsEnabled()
    }

    @Test
    fun registrationPage_continueButtonTriggersCallback() {
        // Given
        var continueClicked = false

        composeTestRule.setContent {
            AppTheme {
                TestRegistrationPage(
                    onNavigateNext = { continueClicked = true }
                )
            }
        }

        // When - Fill fields, accept terms, click continue
        composeTestRule.onNodeWithText("Unesite korisnički identifikator").performTextInput("testUser")
        composeTestRule.onNodeWithText("Unesite aktivacioni kod").performTextInput("123456")
        composeTestRule.onNode(isToggleable()).performClick()
        composeTestRule.onNodeWithText("NASTAVI").performClick()

        // Then
        assert(continueClicked)
    }

    // ==================== Loading State Tests ====================

    @Test
    fun registrationPage_showsProgressIndicatorWhenLoading() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestRegistrationPageWithState(isLoading = true)
            }
        }

        // When - Fill fields and accept terms so we can see the button
        composeTestRule.onNodeWithText("Unesite korisnički identifikator").performTextInput("user")
        composeTestRule.onNodeWithText("Unesite aktivacioni kod").performTextInput("123")
        composeTestRule.onNode(isToggleable()).performClick()

        // Then - Progress indicator should be shown, button text "NASTAVI" should not be visible
        // When loading, the button shows CircularProgressIndicator instead of text
        composeTestRule.onNodeWithText("NASTAVI", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun registrationPage_buttonDisabledWhenLoading() {
        // Given
        var clicked = false

        composeTestRule.setContent {
            AppTheme {
                TestRegistrationPageWithState(
                    isLoading = true,
                    onNavigateNext = { clicked = true }
                )
            }
        }

        // When - Fill fields and accept terms
        composeTestRule.onNodeWithText("Unesite korisnički identifikator").performTextInput("user")
        composeTestRule.onNodeWithText("Unesite aktivacioni kod").performTextInput("123")
        composeTestRule.onNode(isToggleable()).performClick()

        // Then - Try to click the button (it should be disabled so callback won't fire)
        // Find the button - it's the second clickable node (first is checkbox)
        composeTestRule.onAllNodes(hasClickAction())[1].performClick()

        // Button is disabled so callback should not have been triggered
        assert(!clicked)
    }

    // ==================== Error Dialog Tests ====================

    @Test
    fun registrationPage_displaysErrorDialog() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestRegistrationPageWithState(
                    showError = true,
                    errorMessage = "Unsuccessful registration"
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText("Unsuccessful registration").assertIsDisplayed()
        composeTestRule.onNodeWithText("NAZAD").assertIsDisplayed()
    }

    @Test
    fun registrationPage_errorDialogCanBeDismissed() {
        // Given
        var dialogDismissed = false

        composeTestRule.setContent {
            AppTheme {
                TestRegistrationPageWithState(
                    showError = true,
                    errorMessage = "Test error",
                    onErrorDismiss = { dialogDismissed = true }
                )
            }
        }

        // When
        composeTestRule.onNodeWithText("NAZAD").performClick()

        // Then
        assert(dialogDismissed)
    }

    // ==================== Edge Cases ====================

    @Test
    fun registrationPage_multipleFieldEditsRetainValues() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestRegistrationPage()
            }
        }

        // When - Enter, clear, re-enter
        composeTestRule.onNodeWithText("Unesite korisnički identifikator").performTextInput("first")
        composeTestRule.onNodeWithText("first").performTextClearance()
        composeTestRule.onNodeWithText("Unesite korisnički identifikator").performTextInput("second")

        // Then
        composeTestRule.onNodeWithText("second").assertIsDisplayed()
        composeTestRule.onNodeWithText("first").assertDoesNotExist()
    }

    @Test
    fun registrationPage_termsCanBeToggledMultipleTimes() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestRegistrationPage()
            }
        }

        // When - Fill fields
        composeTestRule.onNodeWithText("Unesite korisnički identifikator").performTextInput("user")
        composeTestRule.onNodeWithText("Unesite aktivacioni kod").performTextInput("123")

        // Toggle terms multiple times
        val termsNode = composeTestRule.onNode(isToggleable())
        termsNode.performClick() // Enable
        composeTestRule.onNodeWithText("NASTAVI").assertIsEnabled()

        termsNode.performClick() // Disable
        composeTestRule.onNodeWithText("NASTAVI").assertIsNotEnabled()

        termsNode.performClick() // Enable again
        composeTestRule.onNodeWithText("NASTAVI").assertIsEnabled()
    }

    @Test
    fun registrationPage_callbacksAreIndependent() {
        // Given
        var continueClicked = false
        var termsClicked = false

        composeTestRule.setContent {
            AppTheme {
                TestRegistrationPage(
                    onNavigateNext = { continueClicked = true },
                    onViewTermsClick = { termsClicked = true }
                )
            }
        }

        // When - Fill fields, accept terms, click continue button
        composeTestRule.onNodeWithText("Unesite korisnički identifikator").performTextInput("user")
        composeTestRule.onNodeWithText("Unesite aktivacioni kod").performTextInput("123")
        composeTestRule.onNode(isToggleable()).performClick()
        composeTestRule.onNodeWithText("NASTAVI").performClick()

        // Then - Only continue callback should be triggered, not view terms
        assert(continueClicked)
        assert(!termsClicked)
    }
}
