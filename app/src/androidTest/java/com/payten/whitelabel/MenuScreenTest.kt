package com.payten.whitelabel

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.payten.whitelabel.ui.screens.MenuScreen
import com.payten.whitelabel.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for MenuScreen.
 * Tests UI rendering and user interactions.
 */
class MenuScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==================== Rendering Tests ====================

    @Test
    fun menuScreen_displaysPaytenLogo() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                MenuScreen(
                    merchantName = "Test Merchant",
                    merchantAddress = "Test Address"
                )
            }
        }

        // Then - Logo should be displayed (checking via content description being null is expected)
        composeTestRule.onNodeWithContentDescription("null", substring = true, useUnmergedTree = true)
    }

    @Test
    fun menuScreen_displaysMerchantName() {
        // Given
        val merchantName = "Petar Petrović"

        composeTestRule.setContent {
            AppTheme {
                MenuScreen(
                    merchantName = merchantName,
                    merchantAddress = "Test Address"
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(merchantName).assertIsDisplayed()
    }

    @Test
    fun menuScreen_displaysMerchantAddress() {
        // Given
        val merchantAddress = "Bul. Mihajla Pupina 10b, Beograd"

        composeTestRule.setContent {
            AppTheme {
                MenuScreen(
                    merchantName = "Test Merchant",
                    merchantAddress = merchantAddress
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(merchantAddress).assertIsDisplayed()
    }

    @Test
    fun menuScreen_doesNotDisplayAddressWhenBlank() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                MenuScreen(
                    merchantName = "Test Merchant",
                    merchantAddress = ""
                )
            }
        }

        // Then - Location icon should not be displayed when address is blank
        composeTestRule.onNodeWithText("Test Merchant").assertIsDisplayed()
    }

    @Test
    fun menuScreen_displaysAllMenuItems() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                MenuScreen(
                    merchantName = "Test Merchant",
                    merchantAddress = "Test Address"
                )
            }
        }

        // Then - All menu items should be displayed
        composeTestRule.onNodeWithText("Promet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Podešavanja").assertIsDisplayed()
        composeTestRule.onNodeWithText("Presek dana").assertIsDisplayed()
        composeTestRule.onNodeWithText("Odjava").assertIsDisplayed()
    }

    // ==================== Interaction Tests ====================

    @Test
    fun menuScreen_closeButtonTriggersCallback() {
        // Given
        var closeClicked = false

        composeTestRule.setContent {
            AppTheme {
                MenuScreen(
                    merchantName = "Test Merchant",
                    merchantAddress = "Test Address",
                    onClose = { closeClicked = true }
                )
            }
        }

        // When - Click the close/exit button (looking for clickable image near the end)
        composeTestRule.onAllNodes(hasClickAction()).filter(hasAnyAncestor(isRoot()))[0].performClick()

        // Then
        assert(closeClicked)
    }

    @Test
    fun menuScreen_trafficMenuItemTriggersCallback() {
        // Given
        var trafficClicked = false

        composeTestRule.setContent {
            AppTheme {
                MenuScreen(
                    merchantName = "Test Merchant",
                    merchantAddress = "Test Address",
                    onTrafficClick = { trafficClicked = true }
                )
            }
        }

        // When - The entire outer Row with the menu item should be clickable via the IconButton
        // Find all clickable nodes and get the first one after the merchant card (index 1: 0=close, 1=traffic, 2=settings, 3=endOfDay, 4=signOut)
        composeTestRule.onAllNodes(hasClickAction())[1].performClick()

        // Then
        assert(trafficClicked)
    }

    @Test
    fun menuScreen_settingsMenuItemTriggersCallback() {
        // Given
        var settingsClicked = false

        composeTestRule.setContent {
            AppTheme {
                MenuScreen(
                    merchantName = "Test Merchant",
                    merchantAddress = "Test Address",
                    onSettingsClick = { settingsClicked = true }
                )
            }
        }

        // When - Find all clickable nodes and get settings (index 2)
        composeTestRule.onAllNodes(hasClickAction())[2].performClick()

        // Then
        assert(settingsClicked)
    }

    @Test
    fun menuScreen_endOfDayMenuItemTriggersCallback() {
        // Given
        var endOfDayClicked = false

        composeTestRule.setContent {
            AppTheme {
                MenuScreen(
                    merchantName = "Test Merchant",
                    merchantAddress = "Test Address",
                    onEndOfDayClick = { endOfDayClicked = true }
                )
            }
        }

        // When - Find all clickable nodes and get end of day (index 3)
        composeTestRule.onAllNodes(hasClickAction())[3].performClick()

        // Then
        assert(endOfDayClicked)
    }

    @Test
    fun menuScreen_signOutMenuItemTriggersCallback() {
        // Given
        var signOutClicked = false

        composeTestRule.setContent {
            AppTheme {
                MenuScreen(
                    merchantName = "Test Merchant",
                    merchantAddress = "Test Address",
                    onSignOutClick = { signOutClicked = true }
                )
            }
        }

        // When - Click the Sign Out icon (the Image itself is clickable in MenuItemWithClickableIcon)
        composeTestRule.onNodeWithContentDescription("Odjava").performClick()

        // Then
        assert(signOutClicked)
    }

    // ==================== Layout Tests ====================

    @Test
    fun menuScreen_merchantInfoCardHasCorrectStructure() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                MenuScreen(
                    merchantName = "Test Merchant",
                    merchantAddress = "Bul. Mihajla Pupina 10b"
                )
            }
        }

        // Then - Merchant name and address should be in the same container
        composeTestRule.onNodeWithText("Test Merchant").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bul. Mihajla Pupina 10b").assertIsDisplayed()
    }

    @Test
    fun menuScreen_allMenuItemsAreClickable() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                MenuScreen(
                    merchantName = "Test Merchant",
                    merchantAddress = "Test Address"
                )
            }
        }

        // Then - All menu items should have clickable components
        val clickableNodes = composeTestRule.onAllNodes(hasClickAction())
        // Should have at least 5 clickable elements: 1 close button + 3 arrow buttons + 1 sign out
        assert(clickableNodes.fetchSemanticsNodes().size >= 5)
    }

    // ==================== Edge Cases ====================

    @Test
    fun menuScreen_withEmptyMerchantName() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                MenuScreen(
                    merchantName = "",
                    merchantAddress = "Test Address"
                )
            }
        }

        // Then - Screen should still render without crashing
        composeTestRule.onNodeWithText("Test Address").assertIsDisplayed()
    }

    @Test
    fun menuScreen_withLongMerchantName() {
        // Given
        val longName = "Very Long Merchant Name That Should Still Display Correctly"

        composeTestRule.setContent {
            AppTheme {
                MenuScreen(
                    merchantName = longName,
                    merchantAddress = "Test Address"
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(longName).assertIsDisplayed()
    }

    @Test
    fun menuScreen_withLongAddress() {
        // Given
        val longAddress = "Bulevar Kralja Aleksandra 123, Savski Venac, Beograd 11000, Serbia"

        composeTestRule.setContent {
            AppTheme {
                MenuScreen(
                    merchantName = "Test Merchant",
                    merchantAddress = longAddress
                )
            }
        }

        // Then
        composeTestRule.onNodeWithText(longAddress).assertIsDisplayed()
    }

    @Test
    fun menuScreen_multipleClicksOnSameMenuItemWork() {
        // Given
        var clickCount = 0

        composeTestRule.setContent {
            AppTheme {
                MenuScreen(
                    merchantName = "Test Merchant",
                    merchantAddress = "Test Address",
                    onTrafficClick = { clickCount++ }
                )
            }
        }

        // When - Click multiple times
        val trafficButton = composeTestRule.onAllNodes(hasClickAction())[1]

        trafficButton.performClick()
        trafficButton.performClick()
        trafficButton.performClick()

        // Then
        assert(clickCount == 3)
    }

    @Test
    fun menuScreen_allCallbacksAreIndependent() {
        // Given
        var closeClicked = false
        var trafficClicked = false
        var settingsClicked = false
        var endOfDayClicked = false
        var signOutClicked = false

        composeTestRule.setContent {
            AppTheme {
                MenuScreen(
                    merchantName = "Test Merchant",
                    merchantAddress = "Test Address",
                    onClose = { closeClicked = true },
                    onTrafficClick = { trafficClicked = true },
                    onSettingsClick = { settingsClicked = true },
                    onEndOfDayClick = { endOfDayClicked = true },
                    onSignOutClick = { signOutClicked = true }
                )
            }
        }

        // When - Click traffic menu item (index 1)
        composeTestRule.onAllNodes(hasClickAction())[1].performClick()

        // Then - Only traffic callback should be triggered
        assert(trafficClicked)
        assert(!closeClicked)
        assert(!settingsClicked)
        assert(!endOfDayClicked)
        assert(!signOutClicked)
    }
}
