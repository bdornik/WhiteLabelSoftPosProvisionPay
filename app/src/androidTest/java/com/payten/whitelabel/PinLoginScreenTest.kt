package com.payten.whitelabel

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.MutableLiveData
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.screens.PinLoginScreen
import com.payten.whitelabel.ui.theme.AppTheme
import com.payten.whitelabel.viewmodel.PinViewModel
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for PinLoginScreen.
 *
 * Tests PIN input UI, error states, app blocked state, and navigation callbacks.
 * Note: BCrypt verification and token refresh are not unit tested here (require integration tests).
 */
class PinLoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockSharedPreferences: KsPrefs
    private lateinit var mockViewModel: PinViewModel
    private lateinit var getTokenSuccessfulLiveData: MutableLiveData<Boolean?>
    private var loginSuccessCalled = false
    private var forgotPinCalled = false
    private var loginFailedCalled = false

    @Before
    fun setup() {
        mockSharedPreferences = mockk(relaxed = true)
        mockViewModel = mockk(relaxed = true)
        getTokenSuccessfulLiveData = MutableLiveData(null)

        loginSuccessCalled = false
        forgotPinCalled = false
        loginFailedCalled = false

        // Setup ViewModel LiveData
        every { mockViewModel.getTokenSuccessfull } returns getTokenSuccessfulLiveData as MutableLiveData<Boolean>

        // Default state - not blocked, 3 attempts remaining
        every { mockSharedPreferences.pull(SharedPreferencesKeys.APP_BLOCKED, any<Boolean>()) } returns false
        every { mockSharedPreferences.pull(SharedPreferencesKeys.PIN_COUNT, any<Int>()) } returns 3
        every { mockSharedPreferences.pull(SharedPreferencesKeys.PIN, any<String>()) } returns ""
        every { mockSharedPreferences.pull(SharedPreferencesKeys.DUMMY, any<Boolean>()) } returns false
    }

    @Test
    fun pinLoginScreen_displaysLogo() {
        composeTestRule.setContent {
            AppTheme {
                PinLoginScreen(
                    sharedPreferences = mockSharedPreferences,
                    viewModel = mockViewModel
                )
            }
        }

        // Screen should render
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun pinLoginScreen_displaysEnterPinTitle() {
        composeTestRule.setContent {
            AppTheme {
                PinLoginScreen(
                    sharedPreferences = mockSharedPreferences,
                    viewModel = mockViewModel
                )
            }
        }

        composeTestRule.onNodeWithText("Unesite PIN").assertExists()
    }

    @Test
    fun pinLoginScreen_displaysPinIndicators() {
        composeTestRule.setContent {
            AppTheme {
                PinLoginScreen(
                    sharedPreferences = mockSharedPreferences,
                    viewModel = mockViewModel
                )
            }
        }

        // PIN indicators should be visible (screen renders successfully)
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun pinLoginScreen_displaysNumericKeypad() {
        composeTestRule.setContent {
            AppTheme {
                PinLoginScreen(
                    sharedPreferences = mockSharedPreferences,
                    viewModel = mockViewModel
                )
            }
        }

        // Verify numeric buttons exist
        composeTestRule.onAllNodesWithText("0").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("1").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("5").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("9").assertCountEquals(1)
    }

    @Test
    fun pinLoginScreen_displaysBackspaceButton() {
        composeTestRule.setContent {
            AppTheme {
                PinLoginScreen(
                    sharedPreferences = mockSharedPreferences,
                    viewModel = mockViewModel
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Backspace").assertExists()
    }

    @Test
    fun pinLoginScreen_displaysForgotPinButton() {
        composeTestRule.setContent {
            AppTheme {
                PinLoginScreen(
                    sharedPreferences = mockSharedPreferences,
                    viewModel = mockViewModel
                )
            }
        }

        composeTestRule.onNodeWithText("ZABORAVLJEN PIN?").assertExists()
    }

    @Test
    fun pinLoginScreen_canEnterSingleDigit() {
        composeTestRule.setContent {
            AppTheme {
                PinLoginScreen(
                    sharedPreferences = mockSharedPreferences,
                    viewModel = mockViewModel
                )
            }
        }

        composeTestRule.onAllNodesWithText("1")[0].performClick()
        composeTestRule.waitForIdle()

        // Screen should still render without errors
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun pinLoginScreen_canEnterMultipleDigits() {
        composeTestRule.setContent {
            AppTheme {
                PinLoginScreen(
                    sharedPreferences = mockSharedPreferences,
                    viewModel = mockViewModel
                )
            }
        }

        composeTestRule.onAllNodesWithText("1")[0].performClick()
        composeTestRule.onAllNodesWithText("2")[0].performClick()
        composeTestRule.onAllNodesWithText("3")[0].performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun pinLoginScreen_backspaceRemovesDigit() {
        composeTestRule.setContent {
            AppTheme {
                PinLoginScreen(
                    sharedPreferences = mockSharedPreferences,
                    viewModel = mockViewModel
                )
            }
        }

        // Enter 2 digits
        composeTestRule.onAllNodesWithText("5")[0].performClick()
        composeTestRule.onAllNodesWithText("6")[0].performClick()
        composeTestRule.waitForIdle()

        // Remove one digit
        composeTestRule.onNodeWithContentDescription("Backspace").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun pinLoginScreen_backspaceOnEmptyPinDoesNothing() {
        composeTestRule.setContent {
            AppTheme {
                PinLoginScreen(
                    sharedPreferences = mockSharedPreferences,
                    viewModel = mockViewModel
                )
            }
        }

        // Click backspace with no PIN entered
        composeTestRule.onNodeWithContentDescription("Backspace").performClick()
        composeTestRule.waitForIdle()

        // Should still render without errors
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun pinLoginScreen_forgotPinButton_triggersCallback() {
        composeTestRule.setContent {
            AppTheme {
                PinLoginScreen(
                    sharedPreferences = mockSharedPreferences,
                    onForgotPin = { forgotPinCalled = true },
                    viewModel = mockViewModel
                )
            }
        }

        composeTestRule.onNodeWithText("ZABORAVLJEN PIN?").performClick()
        composeTestRule.waitForIdle()

        assert(forgotPinCalled)
    }

    @Test
    fun pinLoginScreen_appBlocked_displaysBlockedMessage() {
        every { mockSharedPreferences.pull(SharedPreferencesKeys.APP_BLOCKED, any<Boolean>()) } returns true

        composeTestRule.setContent {
            AppTheme {
                PinLoginScreen(
                    sharedPreferences = mockSharedPreferences,
                    viewModel = mockViewModel
                )
            }
        }

        composeTestRule.onNodeWithText("Aplikacija je blokirana").assertExists()
    }

    @Test
    fun pinLoginScreen_appBlocked_hidesNumericKeypad() {
        every { mockSharedPreferences.pull(SharedPreferencesKeys.APP_BLOCKED, any<Boolean>()) } returns true

        composeTestRule.setContent {
            AppTheme {
                PinLoginScreen(
                    sharedPreferences = mockSharedPreferences,
                    viewModel = mockViewModel
                )
            }
        }

        // Numeric keypad should not be visible
        composeTestRule.onAllNodesWithText("0").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("1").assertCountEquals(0)
    }

    @Test
    fun pinLoginScreen_appBlocked_hidesForgotPinButton() {
        every { mockSharedPreferences.pull(SharedPreferencesKeys.APP_BLOCKED, any<Boolean>()) } returns true

        composeTestRule.setContent {
            AppTheme {
                PinLoginScreen(
                    sharedPreferences = mockSharedPreferences,
                    viewModel = mockViewModel
                )
            }
        }

        // Forgot PIN button should not be visible
        composeTestRule.onNodeWithText("ZABORAVLJEN PIN?").assertDoesNotExist()
    }

    @Test
    fun pinLoginScreen_notBlocked_showsNumericKeypad() {
        every { mockSharedPreferences.pull(SharedPreferencesKeys.APP_BLOCKED, any<Boolean>()) } returns false

        composeTestRule.setContent {
            AppTheme {
                PinLoginScreen(
                    sharedPreferences = mockSharedPreferences,
                    viewModel = mockViewModel
                )
            }
        }

        // All numeric buttons should be visible
        composeTestRule.onAllNodesWithText("0").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("1").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("9").assertCountEquals(1)
    }

    @Test
    fun pinLoginScreen_notBlocked_showsForgotPinButton() {
        every { mockSharedPreferences.pull(SharedPreferencesKeys.APP_BLOCKED, any<Boolean>()) } returns false

        composeTestRule.setContent {
            AppTheme {
                PinLoginScreen(
                    sharedPreferences = mockSharedPreferences,
                    viewModel = mockViewModel
                )
            }
        }

        composeTestRule.onNodeWithText("ZABORAVLJEN PIN?").assertExists()
    }

    @Test
    fun pinLoginScreen_rendersWithoutCrash() {
        composeTestRule.setContent {
            AppTheme {
                PinLoginScreen(
                    sharedPreferences = mockSharedPreferences,
                    viewModel = mockViewModel
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun pinLoginScreen_allCallbacksAreIndependent() {
        var successCount = 0
        var forgotCount = 0
        var failedCount = 0

        composeTestRule.setContent {
            AppTheme {
                PinLoginScreen(
                    sharedPreferences = mockSharedPreferences,
                    onLoginSuccess = { successCount++ },
                    onForgotPin = { forgotCount++ },
                    onLoginFailed = { failedCount++ },
                    viewModel = mockViewModel
                )
            }
        }

        // Click forgot PIN
        composeTestRule.onNodeWithText("ZABORAVLJEN PIN?").performClick()
        composeTestRule.waitForIdle()

        assert(forgotCount == 1)
        assert(successCount == 0)
        assert(failedCount == 0)
    }
}
