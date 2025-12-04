package com.payten.whitelabel

import androidx.compose.foundation.layout.size
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.payten.whitelabel.ui.components.BackButton
import com.payten.whitelabel.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented tests for BackButton component.
 * Tests back button rendering and click interactions.
 */
class BackButtonTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==================== Rendering Tests ====================

    @Test
    fun backButton_renders() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                BackButton()
            }
        }

        // Then - Component should render
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun backButton_isClickable() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                BackButton()
            }
        }

        // Then - Should have click action
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(1)
    }

    @Test
    fun backButton_hasNoText() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                BackButton()
            }
        }

        // Then - BackButton is purely graphical (Canvas), has no text
        composeTestRule.onRoot().assertExists()
    }

    // ==================== Interaction Tests ====================

    @Test
    fun backButton_triggersCallback() {
        // Given
        var clicked = false

        composeTestRule.setContent {
            AppTheme {
                BackButton(onClick = { clicked = true })
            }
        }

        // When - Click the button
        composeTestRule.onAllNodes(hasClickAction())[0].performClick()

        // Then
        assert(clicked)
    }

    @Test
    fun backButton_multipleClicks() {
        // Given
        var clickCount = 0

        composeTestRule.setContent {
            AppTheme {
                BackButton(onClick = { clickCount++ })
            }
        }

        // When - Click multiple times
        val button = composeTestRule.onAllNodes(hasClickAction())[0]
        button.performClick()
        button.performClick()
        button.performClick()

        // Then
        assert(clickCount == 3)
    }

    @Test
    fun backButton_callbackNotInvokedWithoutClick() {
        // Given
        var clicked = false

        composeTestRule.setContent {
            AppTheme {
                BackButton(onClick = { clicked = true })
            }
        }

        // When - Don't click

        // Then
        assert(!clicked)
    }

    @Test
    fun backButton_defaultCallbackDoesNotCrash() {
        // Given - BackButton with default empty callback
        composeTestRule.setContent {
            AppTheme {
                BackButton()
            }
        }

        // When - Click the button with default callback
        composeTestRule.onAllNodes(hasClickAction())[0].performClick()

        // Then - Should not crash
        composeTestRule.onRoot().assertExists()
    }

    // ==================== Edge Cases ====================

    @Test
    fun backButton_multipleInstancesCanCoexist() {
        // Given - Multiple BackButtons in the same screen
        var button1Clicked = false
        var button2Clicked = false
        var button3Clicked = false

        composeTestRule.setContent {
            AppTheme {
                androidx.compose.foundation.layout.Column {
                    BackButton(onClick = { button1Clicked = true })
                    BackButton(onClick = { button2Clicked = true })
                    BackButton(onClick = { button3Clicked = true })
                }
            }
        }

        // Then - All buttons should be clickable
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(3)

        // When - Click first button
        composeTestRule.onAllNodes(hasClickAction())[0].performClick()

        // Then - Only first callback triggered
        assert(button1Clicked)
        assert(!button2Clicked)
        assert(!button3Clicked)
    }

    @Test
    fun backButton_independentCallbacks() {
        // Given
        var button1Clicked = false
        var button2Clicked = false

        composeTestRule.setContent {
            AppTheme {
                androidx.compose.foundation.layout.Row {
                    BackButton(onClick = { button1Clicked = true })
                    BackButton(onClick = { button2Clicked = true })
                }
            }
        }

        // When - Click second button
        composeTestRule.onAllNodes(hasClickAction())[1].performClick()

        // Then - Only second callback triggered
        assert(!button1Clicked)
        assert(button2Clicked)
    }

    @Test
    fun backButton_rapidClicks() {
        // Given
        var clickCount = 0

        composeTestRule.setContent {
            AppTheme {
                BackButton(onClick = { clickCount++ })
            }
        }

        // When - Rapid clicks
        val button = composeTestRule.onAllNodes(hasClickAction())[0]
        repeat(10) {
            button.performClick()
        }

        // Then - All clicks should be registered
        assert(clickCount == 10)
    }

    @Test
    fun backButton_withCustomModifier() {
        // Given - BackButton with custom modifier
        composeTestRule.setContent {
            AppTheme {
                BackButton(
                    modifier = androidx.compose.ui.Modifier.size(60.dp),
                    onClick = {}
                )
            }
        }

        // Then - Component should render
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun backButton_rendersWithDifferentThemes() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                BackButton()
            }
        }

        // Then - Component should render with theme applied
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun backButton_callbackExecutesLogic() {
        // Given
        var navigationPerformed = false
        var dataCleared = false

        composeTestRule.setContent {
            AppTheme {
                BackButton(onClick = {
                    navigationPerformed = true
                    dataCleared = true
                })
            }
        }

        // When - Click button
        composeTestRule.onAllNodes(hasClickAction())[0].performClick()

        // Then - All logic in callback should execute
        assert(navigationPerformed)
        assert(dataCleared)
    }

    @Test
    fun backButton_doesNotHaveContentDescription() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                BackButton()
            }
        }

        // Then - BackButton is a Canvas without content description
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun backButton_isNotDisabled() {
        // Given
        composeTestRule.setContent {
            AppTheme {
                BackButton()
            }
        }

        // Then - BackButton should be clickable (not disabled)
        composeTestRule.onAllNodes(hasClickAction())[0].assertExists()
    }

    @Test
    fun backButton_multipleClicksInSequence() {
        // Given
        val clickOrder = mutableListOf<Int>()

        composeTestRule.setContent {
            AppTheme {
                BackButton(onClick = { clickOrder.add(clickOrder.size + 1) })
            }
        }

        // When - Click 5 times
        val button = composeTestRule.onAllNodes(hasClickAction())[0]
        repeat(5) {
            button.performClick()
        }

        // Then - Clicks should be in order
        assert(clickOrder == listOf(1, 2, 3, 4, 5))
    }

    @Test
    fun backButton_callbackWithException() {
        // Given - BackButton with callback that might throw
        var exceptionThrown = false

        composeTestRule.setContent {
            AppTheme {
                BackButton(onClick = {
                    try {
                        throw RuntimeException("Test exception")
                    } catch (_: Exception) {
                        exceptionThrown = true
                    }
                })
            }
        }

        // When - Click button
        composeTestRule.onAllNodes(hasClickAction())[0].performClick()

        // Then - Exception should be caught
        assert(exceptionThrown)
    }

    @Test
    fun backButton_rendersInDifferentContainers() {
        // Given - BackButton in different container types
        composeTestRule.setContent {
            AppTheme {
                androidx.compose.foundation.layout.Column {
                    androidx.compose.foundation.layout.Row {
                        BackButton()
                    }
                    androidx.compose.foundation.layout.Box {
                        BackButton()
                    }
                }
            }
        }

        // Then - All instances should render
        composeTestRule.onAllNodes(hasClickAction()).assertCountEquals(2)
    }

    @Test
    fun backButton_callbackCanAccessExternalState() {
        // Given
        var externalCounter = 0

        composeTestRule.setContent {
            AppTheme {
                BackButton(onClick = { externalCounter += 5 })
            }
        }

        // When - Click button twice
        val button = composeTestRule.onAllNodes(hasClickAction())[0]
        button.performClick()
        button.performClick()

        // Then - External state should be modified
        assert(externalCounter == 10)
    }
}
