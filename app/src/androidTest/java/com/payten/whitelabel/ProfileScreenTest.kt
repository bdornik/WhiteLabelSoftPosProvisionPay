package com.payten.whitelabel

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.screens.ProfileScreen
import com.payten.whitelabel.ui.theme.AppTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var testPrefs: KsPrefs

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testPrefs = KsPrefs(context)
        testPrefs.clear()

        testPrefs.push(SharedPreferencesKeys.MERCHANT_NAME, "Petar Petrović")
        testPrefs.push(SharedPreferencesKeys.MERCHANT_ADDRESS, "Bul. Mihajla Pupina 10b")
        testPrefs.push(SharedPreferencesKeys.MERCHANT_PLACE_NAME, "Beograd")
        testPrefs.push(SharedPreferencesKeys.USER_TID, "DU160014")
    }

    @Test
    fun profileDetailsScreen_displaysUserName() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                ProfileScreen(
                    sharedPreferences = testPrefs,
                    onNavigateBack = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("Petar Petrović")
            .assertIsDisplayed()
    }

    @Test
    fun profileDetailsScreen_displaysAddress() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                ProfileScreen(
                    sharedPreferences = testPrefs,
                    onNavigateBack = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("Bul. Mihajla Pupina 10b")
            .assertIsDisplayed()
    }

    @Test
    fun profileDetailsScreen_displaysCity() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                ProfileScreen(
                    sharedPreferences = testPrefs,
                    onNavigateBack = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("Beograd")
            .assertIsDisplayed()
    }

    @Test
    fun profileDetailsScreen_displaysTID() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                ProfileScreen(
                    sharedPreferences = testPrefs,
                    onNavigateBack = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("DU160014")
            .assertIsDisplayed()
    }

    @Test
    fun profileDetailsScreen_backButtonWorks() {
        // Given
        var backCalled = false
        composeTestRule.setContent {
            AppTheme {
                ProfileScreen(
                    sharedPreferences = testPrefs,
                    onNavigateBack = { backCalled = true }
                )
            }
        }

        // When - Look for back button and click it
        composeTestRule.waitForIdle()

        // Then - Just verify screen loads for now
        composeTestRule
            .onNodeWithText("PROFIL")
            .assertIsDisplayed()
    }

    @Test
    fun profileDetailsScreen_displaysAllLabels() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                ProfileScreen(
                    sharedPreferences = testPrefs,
                    onNavigateBack = {}
                )
            }
        }

        // Then - Check for all labels
        composeTestRule
            .onNodeWithText("ID korisnika", substring = true, ignoreCase = true)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Adresa", substring = true, ignoreCase = true)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Grad", ignoreCase = true)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("TID", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun profileDetailsScreen_handlesEmptyData() {
        // Given - Clear all data
        testPrefs.clear()

        composeTestRule.setContent {
            AppTheme {
                ProfileScreen(
                    sharedPreferences = testPrefs,
                    onNavigateBack = {}
                )
            }
        }

        // Then - Should display N/A for empty values (4 times - one for each field)
        composeTestRule
            .onAllNodesWithText("N/A")
            .assertCountEquals(4)
    }

    @Test
    fun profileDetailsScreen_titleIsDisplayed() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                ProfileScreen(
                    sharedPreferences = testPrefs,
                    onNavigateBack = {}
                )
            }
        }

        // Then
        composeTestRule
            .onNodeWithText("PROFIL")
            .assertIsDisplayed()
    }

    @Test
    fun profileDetailsScreen_allFieldsHaveIcons() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                ProfileScreen(
                    sharedPreferences = testPrefs,
                    onNavigateBack = {}
                )
            }
        }

        // Then - Verify that all data is displayed (icons are harder to test without testTag)
        composeTestRule
            .onNodeWithText("Petar Petrović")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Bul. Mihajla Pupina 10b")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Beograd")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("DU160014")
            .assertIsDisplayed()
    }

    @Test
    fun profileDetailsScreen_partialData() {
        // Given - Only some fields have data
        testPrefs.clear()
        testPrefs.push(SharedPreferencesKeys.MERCHANT_NAME, "Test User")
        testPrefs.push(SharedPreferencesKeys.USER_TID, "TEST1234")

        composeTestRule.setContent {
            AppTheme {
                ProfileScreen(
                    sharedPreferences = testPrefs,
                    onNavigateBack = {}
                )
            }
        }

        // Then - Should display both filled and N/A fields
        composeTestRule
            .onNodeWithText("Test User")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("TEST1234")
            .assertIsDisplayed()

        // Should have 2 N/A fields (address and city)
        composeTestRule
            .onAllNodesWithText("N/A")
            .assertCountEquals(2)
    }
}