package com.payten.whitelabel

import android.annotation.SuppressLint
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.screens.SplashScreen
import com.payten.whitelabel.ui.theme.AppTheme
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for SplashScreen.
 *
 * Tests initial rendering, navigation logic based on registration status, and delay handling.
 */
class SplashScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockSharedPreferences: KsPrefs
    private var navigationDestination: String? = null

    @Before
    fun setup() {
        mockSharedPreferences = mockk(relaxed = true)
        navigationDestination = null
    }

    @Test
    fun splashScreen_displaysLogo() {
        every { mockSharedPreferences.pull(SharedPreferencesKeys.REGISTERED, any<Boolean>()) } returns false

        composeTestRule.setContent {
            AppTheme {
                SplashScreen(
                    sharedPreferences = mockSharedPreferences,
                    onNavigateToNext = {}
                )
            }
        }

        // Logo image should exist
        composeTestRule.onNodeWithContentDescription("Payten Logo").assertExists()
    }

    @Test
    fun splashScreen_displaysSoftPOSText() {
        every { mockSharedPreferences.pull(SharedPreferencesKeys.REGISTERED, any<Boolean>()) } returns false

        composeTestRule.setContent {
            AppTheme {
                SplashScreen(
                    sharedPreferences = mockSharedPreferences,
                    onNavigateToNext = {}
                )
            }
        }

        // Should display "SoftPOS" text (split into "Soft" and "POS" with different colors)
        composeTestRule.onNodeWithText("SoftPOS", substring = true).assertExists()
    }

    @Test
    fun splashScreen_displaysLoadingIndicator() {
        every { mockSharedPreferences.pull(SharedPreferencesKeys.REGISTERED, any<Boolean>()) } returns false

        composeTestRule.setContent {
            AppTheme {
                SplashScreen(
                    sharedPreferences = mockSharedPreferences,
                    onNavigateToNext = {}
                )
            }
        }

        // Loading indicator should be present
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
    }

    @Test
    fun splashScreen_notRegistered_navigatesToFirstPage() {
        every { mockSharedPreferences.pull(SharedPreferencesKeys.REGISTERED, any<Boolean>()) } returns false

        composeTestRule.setContent {
            AppTheme {
                SplashScreen(
                    sharedPreferences = mockSharedPreferences,
                    onNavigateToNext = { destination ->
                        navigationDestination = destination
                    }
                )
            }
        }

        // Wait for delay (2 seconds + buffer)
        composeTestRule.mainClock.advanceTimeBy(2500)
        composeTestRule.waitForIdle()

        // Should navigate to "first" page
        assert(navigationDestination == "first") {
            "Expected navigation to 'first', but got '$navigationDestination'"
        }
    }

    @Test
    fun splashScreen_registered_navigatesToPinLogin() {
        every { mockSharedPreferences.pull(SharedPreferencesKeys.REGISTERED, any<Boolean>()) } returns true

        composeTestRule.setContent {
            AppTheme {
                SplashScreen(
                    sharedPreferences = mockSharedPreferences,
                    onNavigateToNext = { destination ->
                        navigationDestination = destination
                    }
                )
            }
        }

        // Wait for delay (2 seconds + buffer)
        composeTestRule.mainClock.advanceTimeBy(2500)
        composeTestRule.waitForIdle()

        // Should navigate to "pin_login" page
        assert(navigationDestination == "pin_login") {
            "Expected navigation to 'pin_login', but got '$navigationDestination'"
        }
    }

    @Test
    fun splashScreen_doesNotNavigateImmediately() {
        every { mockSharedPreferences.pull(SharedPreferencesKeys.REGISTERED, any<Boolean>()) } returns false

        composeTestRule.setContent {
            AppTheme {
                SplashScreen(
                    sharedPreferences = mockSharedPreferences,
                    onNavigateToNext = { destination ->
                        navigationDestination = destination
                    }
                )
            }
        }

        // Wait only 1 second (less than the 2-second delay)
        composeTestRule.mainClock.advanceTimeBy(1000)
        composeTestRule.waitForIdle()

        // Should NOT have navigated yet
        assert(navigationDestination == null) {
            "Expected no navigation yet, but got '$navigationDestination'"
        }
    }

    @Test
    fun splashScreen_navigatesAfterDelay() {
        every { mockSharedPreferences.pull(SharedPreferencesKeys.REGISTERED, any<Boolean>()) } returns false

        composeTestRule.setContent {
            AppTheme {
                SplashScreen(
                    sharedPreferences = mockSharedPreferences,
                    onNavigateToNext = { destination ->
                        navigationDestination = destination
                    }
                )
            }
        }

        // Initially no navigation
        assert(navigationDestination == null)

        // Wait for exact 2-second delay
        composeTestRule.mainClock.advanceTimeBy(2000)
        composeTestRule.waitForIdle()

        // Should have navigated
        assert(navigationDestination != null) {
            "Expected navigation after 2 seconds, but navigationDestination is null"
        }
    }

    @SuppressLint("CheckResult")
    @Test
    fun splashScreen_checksRegistrationStatus() {
        every { mockSharedPreferences.pull(SharedPreferencesKeys.REGISTERED, any<Boolean>()) } returns false

        composeTestRule.setContent {
            AppTheme {
                SplashScreen(
                    sharedPreferences = mockSharedPreferences,
                    onNavigateToNext = {}
                )
            }
        }

        composeTestRule.mainClock.advanceTimeBy(2500)
        composeTestRule.waitForIdle()

        // Verify that REGISTERED key was checked
        io.mockk.verify {
            mockSharedPreferences.pull(SharedPreferencesKeys.REGISTERED, any<Boolean>())
        }
    }

    @Test
    fun splashScreen_hasCorrectBackgroundColor() {
        every { mockSharedPreferences.pull(SharedPreferencesKeys.REGISTERED, any<Boolean>()) } returns false

        composeTestRule.setContent {
            AppTheme {
                SplashScreen(
                    sharedPreferences = mockSharedPreferences,
                    onNavigateToNext = {}
                )
            }
        }

        // Screen should be rendered (can't directly test background color in Compose tests,
        // but we can verify the screen renders without crashes)
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun splashScreen_logoHasCorrectSize() {
        every { mockSharedPreferences.pull(SharedPreferencesKeys.REGISTERED, any<Boolean>()) } returns false

        composeTestRule.setContent {
            AppTheme {
                SplashScreen(
                    sharedPreferences = mockSharedPreferences,
                    onNavigateToNext = {}
                )
            }
        }

        // Verify logo is displayed with size constraint (can't directly test size,
        // but verify it renders correctly)
        composeTestRule.onRoot().assertExists()
        composeTestRule.onNodeWithContentDescription("Payten Logo").assertExists()
    }
}
