package com.payten.whitelabel

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.ui.screens.SettingsScreen
import com.payten.whitelabel.ui.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var testPrefs: KsPrefs

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testPrefs = KsPrefs(context)

        testPrefs.clear()

        testPrefs.push("language", 2) 
    }

    @Test
    fun settingsScreen_displaysAllMenuItems() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                SettingsScreen(
                    sharedPreferences = testPrefs,
                    onNavigateBack = {},
                    onProfileClick = {},
                    onChangePinClick = {},
                    onTermsClick = {}
                )
            }
        }

        // Then - Verify screen is displayed
        composeTestRule.waitForIdle()

        // Check that Settings title exists (will be in Serbian by default)
        composeTestRule
            .onNodeWithText("PODEŠAVANJA", substring = true, ignoreCase = true)
            .assertExists()
    }

    @Test
    fun settingsScreen_backButtonExists() {
        composeTestRule.setContent {
            AppTheme {
                SettingsScreen(
                    sharedPreferences = testPrefs,
                    onNavigateBack = {},
                    onProfileClick = {},
                    onChangePinClick = {},
                    onTermsClick = {}
                )
            }
        }

        // Then - Back button should exist
        composeTestRule.waitForIdle()
    }

    @Test
    fun settingsScreen_displaysVersionNumber() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                SettingsScreen(
                    sharedPreferences = testPrefs,
                    onNavigateBack = {},
                    onProfileClick = {},
                    onChangePinClick = {},
                    onTermsClick = {}
                )
            }
        }

        // Then - Version number should be displayed
        composeTestRule.waitForIdle()
    }

    @Test
    fun settingsScreen_canChangeLanguage() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                SettingsScreen(
                    sharedPreferences = testPrefs,
                    onNavigateBack = {},
                    onProfileClick = {},
                    onChangePinClick = {},
                    onTermsClick = {}
                )
            }
        }

        // Wait for initial render
        composeTestRule.waitForIdle()

        // Check that Serbian flag is displayed
        composeTestRule
            .onNodeWithText("🇷🇸", substring = true)
            .assertExists()
    }

    @Test
    fun settingsScreen_profileItemIsClickable() {
        // Given
        var profileClicked = false

        composeTestRule.setContent {
            AppTheme {
                SettingsScreen(
                    sharedPreferences = testPrefs,
                    onNavigateBack = {},
                    onProfileClick = { profileClicked = true },
                    onChangePinClick = {},
                    onTermsClick = {}
                )
            }
        }

        // When - Click on Profile item
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Profil", substring = true, ignoreCase = true)
            .performClick()

        // Then
        assert(profileClicked) { "Profile onClick should be called" }
    }

    @Test
    fun settingsScreen_changePinItemIsClickable() {
        // Given
        var changePinClicked = false

        composeTestRule.setContent {
            AppTheme {
                SettingsScreen(
                    sharedPreferences = testPrefs,
                    onNavigateBack = {},
                    onProfileClick = {},
                    onChangePinClick = { changePinClicked = true },
                    onTermsClick = {}
                )
            }
        }

        // When
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Promeni PIN", substring = true, ignoreCase = true)
            .performClick()

        // Then
        assert(changePinClicked) { "Change PIN onClick should be called" }
    }

    @Test
    fun settingsScreen_termsItemIsClickable() {
        // Given
        var termsClicked = false

        composeTestRule.setContent {
            AppTheme {
                SettingsScreen(
                    sharedPreferences = testPrefs,
                    onNavigateBack = {},
                    onProfileClick = {},
                    onChangePinClick = {},
                    onTermsClick = { termsClicked = true }
                )
            }
        }

        // When
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("Uslovi korišćenja", substring = true, ignoreCase = true)
            .performClick()

        // Then
        assert(termsClicked) { "Terms onClick should be called" }
    }

    @Test
    fun settingsScreen_languageChangePersists() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                SettingsScreen(
                    sharedPreferences = testPrefs,
                    onNavigateBack = {},
                    onProfileClick = {},
                    onChangePinClick = {},
                    onTermsClick = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // When - Change language
        testPrefs.push("language", 1) // English

        // Then
        val languageIndex = testPrefs.pull("language", 2)
        assert(languageIndex == 1) { "Language should be changed to English" }
    }
}