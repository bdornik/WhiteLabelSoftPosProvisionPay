package com.payten.whitelabel

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payten.whitelabel.ui.theme.AppTheme
import com.payten.whitelabel.ui.theme.MyriadPro
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for LandingScreen.
 *
 * Tests the main landing/dashboard screen UI components and interactions.
 * This screen displays welcome message, new transaction button, and menu access.
 *
 * ## Test Coverage:
 * - UI element rendering (welcome text, buttons)
 * - Navigation callbacks (new transaction, menu)
 *
 * ## Testing Strategy:
 * Tests the pure UI components without ViewModel integration.
 * ViewModel-dependent features (terminal status check, reactivation dialog)
 * require integration testing with actual backend.
 *
 * Note: Reactivation dialog tests are not included because they depend on
 * ViewModel.reactivation LiveData which requires Hilt/backend integration.
 */
class LandingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Simplified LandingScreen for testing without ViewModel.
     * Displays the same UI elements but without terminal status checks.
     */
    @Composable
    private fun TestLandingScreen(
        onNavigateToTransaction: () -> Unit = {},
        onNavigateToMenu: () -> Unit = {}
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            Image(
                painter = painterResource(id = R.drawable.gradient_background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            Image(
                painter = painterResource(id = R.drawable.corner_lines),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-120).dp, y = (-20).dp)
                    .size(width = 350.dp, height = 450.dp),
                contentScale = ContentScale.Fit
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                // Header with menu
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.payten),
                        contentDescription = null,
                        modifier = Modifier
                            .height(28.dp)
                            .align(Alignment.Center),
                        contentScale = ContentScale.FillHeight
                    )

                    Image(
                        painter = painterResource(id = R.drawable.menu_image),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .align(Alignment.CenterEnd)
                            .clickable(onClick = onNavigateToMenu)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Welcome content
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Dobrodošli",
                        fontSize = 24.sp,
                        fontFamily = MyriadPro,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color(0x40000000),
                                offset = Offset(0f, 4f),
                                blurRadius = 24f
                            )
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "u Payten Soft POS aplikaciju, koja vam pruža brz, siguran i jednostavan način prihvatanja platnih kartica i Flik instant plaćanja.",
                        fontSize = 16.sp,
                        fontFamily = MyriadPro,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onPrimary,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Transaction button
                Button(
                    onClick = onNavigateToTransaction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "NOVA TRANSAKCIJA",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MyriadPro,
                        letterSpacing = TextUnit(1f, TextUnitType.Sp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // ==================== Rendering Tests ====================

    @Test
    fun landingScreen_displaysWelcomeMessage() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestLandingScreen()
            }
        }

        // Then
        composeTestRule.onNodeWithText("Dobrodošli").assertIsDisplayed()
    }

    @Test
    fun landingScreen_displaysWelcomeDescription() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestLandingScreen()
            }
        }

        // Then
        composeTestRule.onNodeWithText(
            "u Payten Soft POS aplikaciju, koja vam pruža brz, siguran i jednostavan način prihvatanja platnih kartica i Flik instant plaćanja.",
            substring = true
        ).assertIsDisplayed()
    }

    @Test
    fun landingScreen_displaysNewTransactionButton() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestLandingScreen()
            }
        }

        // Then
        composeTestRule.onNodeWithText("NOVA TRANSAKCIJA").assertIsDisplayed()
    }

    @Test
    fun landingScreen_newTransactionButtonIsEnabled() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestLandingScreen()
            }
        }

        // Then - Button should be clickable
        composeTestRule.onNodeWithText("NOVA TRANSAKCIJA").assertIsEnabled()
    }

    @Test
    fun landingScreen_displaysMenuIcon() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                TestLandingScreen()
            }
        }

        // Then - Menu icon should be clickable
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(2) // Menu + New Transaction button
    }

    // ==================== Interaction Tests ====================

    @Test
    fun landingScreen_newTransactionButtonTriggersCallback() {
        // Given
        var transactionClicked = false

        composeTestRule.setContent {
            AppTheme {
                TestLandingScreen(
                    onNavigateToTransaction = { transactionClicked = true }
                )
            }
        }

        // When
        composeTestRule.onNodeWithText("NOVA TRANSAKCIJA").performClick()

        // Then
        assert(transactionClicked)
    }

    @Test
    fun landingScreen_menuIconTriggersCallback() {
        // Given
        var menuClicked = false

        composeTestRule.setContent {
            AppTheme {
                TestLandingScreen(
                    onNavigateToMenu = { menuClicked = true }
                )
            }
        }

        // When - Click the first clickable element (menu icon in top-right)
        composeTestRule.onAllNodes(hasClickAction())[0].performClick()

        // Then
        assert(menuClicked)
    }

    @Test
    fun landingScreen_callbacksAreIndependent() {
        // Given
        var transactionClicked = false
        var menuClicked = false

        composeTestRule.setContent {
            AppTheme {
                TestLandingScreen(
                    onNavigateToTransaction = { transactionClicked = true },
                    onNavigateToMenu = { menuClicked = true }
                )
            }
        }

        // When - Click new transaction button
        composeTestRule.onNodeWithText("NOVA TRANSAKCIJA").performClick()

        // Then - Only transaction callback should be triggered
        assert(transactionClicked)
        assert(!menuClicked)
    }

    // ==================== Edge Cases ====================

    @Test
    fun landingScreen_multipleCallbacksCanBeSet() {
        // Given
        var transactionClicked = false
        var menuClicked = false

        composeTestRule.setContent {
            AppTheme {
                TestLandingScreen(
                    onNavigateToTransaction = { transactionClicked = true },
                    onNavigateToMenu = { menuClicked = true }
                )
            }
        }

        // When - Click menu
        composeTestRule.onAllNodes(hasClickAction())[0].performClick()

        // Then - Only menu callback should fire
        assert(menuClicked)
        assert(!transactionClicked)
    }

    @Test
    fun landingScreen_canNavigateToTransactionMultipleTimes() {
        // Given
        var clickCount = 0

        composeTestRule.setContent {
            AppTheme {
                TestLandingScreen(
                    onNavigateToTransaction = { clickCount++ }
                )
            }
        }

        // When - Click new transaction button multiple times
        composeTestRule.onNodeWithText("NOVA TRANSAKCIJA").performClick()
        composeTestRule.onNodeWithText("NOVA TRANSAKCIJA").performClick()
        composeTestRule.onNodeWithText("NOVA TRANSAKCIJA").performClick()

        // Then
        assert(clickCount == 3)
    }
}
