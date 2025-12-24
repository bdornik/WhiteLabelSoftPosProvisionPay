package com.payten.whitelabel

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.enums.TransactionSortType
import com.payten.whitelabel.enums.TransactionSource
import com.payten.whitelabel.enums.TransactionStatusFilterType
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.screens.FilterScreen
import com.payten.whitelabel.ui.theme.AppTheme
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for FilterScreen.
 *
 * Tests filter selection, active filter chips, clear functionality, and apply callback.
 */
class FilterScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockSharedPreferences: KsPrefs
    private var applyFilterCalled = false
    private var navigateBackCalled = false

    @Before
    fun setup() {
        mockSharedPreferences = mockk(relaxed = true)
        applyFilterCalled = false
        navigateBackCalled = false

        // Default filter values
        every { mockSharedPreferences.pull(SharedPreferencesKeys.FILTER_TYPE, any<Int>()) } returns TransactionSource.POS.ordinal
        every { mockSharedPreferences.pull(SharedPreferencesKeys.FILTER_STATUS, any<Int>()) } returns TransactionStatusFilterType.ALL.ordinal
        every { mockSharedPreferences.pull(SharedPreferencesKeys.FILTER_SORT, any<Int>()) } returns TransactionSortType.DateDesc.ordinal
        every { mockSharedPreferences.pull(SharedPreferencesKeys.DATE_FROM, any<String>()) } returns ""
        every { mockSharedPreferences.pull(SharedPreferencesKeys.DATE_TO, any<String>()) } returns ""
    }

    @Test
    fun filterScreen_displaysHeaderWithBackButton() {
        composeTestRule.setContent {
            AppTheme {
                FilterScreen(
                    sharedPreferences = mockSharedPreferences,
                    onApplyFilter = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").assertExists()
        composeTestRule.onNodeWithText("FILTER").assertExists()
    }

    @Test
    fun filterScreen_backButton_triggersCallback() {
        composeTestRule.setContent {
            AppTheme {
                FilterScreen(
                    sharedPreferences = mockSharedPreferences,
                    onApplyFilter = {},
                    onNavigateBack = { navigateBackCalled = true }
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()

        assert(navigateBackCalled)
    }

    @Test
    fun filterScreen_displaysAllFilterSections() {
        composeTestRule.setContent {
            AppTheme {
                FilterScreen(
                    sharedPreferences = mockSharedPreferences,
                    onApplyFilter = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Datum").assertExists()
        composeTestRule.onNodeWithText("Tip transakcije").assertExists()
        composeTestRule.onNodeWithText("Status transakcije").assertExists()
        composeTestRule.onNodeWithText("Razvrstajte").assertExists()
    }

    @Test
    fun filterScreen_dateSection_isExpandedByDefault() {
        composeTestRule.setContent {
            AppTheme {
                FilterScreen(
                    sharedPreferences = mockSharedPreferences,
                    onApplyFilter = {},
                    onNavigateBack = {}
                )
            }
        }

        // Date section should show date pickers
        composeTestRule.onNodeWithText("Od").assertExists()
        composeTestRule.onNodeWithText("Do").assertExists()
    }

    @Test
    fun filterScreen_typeSection_canExpand() {
        composeTestRule.setContent {
            AppTheme {
                FilterScreen(
                    sharedPreferences = mockSharedPreferences,
                    onApplyFilter = {},
                    onNavigateBack = {}
                )
            }
        }

        // Click to expand type section
        composeTestRule.onNodeWithText("Tip transakcije").performClick()
        composeTestRule.waitForIdle()

        // Should show POS and IPS options
        composeTestRule.onNodeWithText("POS").assertExists()
        composeTestRule.onNodeWithText("IPS").assertExists()
    }

    @Test
    fun filterScreen_statusSection_canExpand() {
        composeTestRule.setContent {
            AppTheme {
                FilterScreen(
                    sharedPreferences = mockSharedPreferences,
                    onApplyFilter = {},
                    onNavigateBack = {}
                )
            }
        }

        // Click to expand status section
        composeTestRule.onNodeWithText("Status transakcije").performClick()
        composeTestRule.waitForIdle()

        // Should show all status options
        composeTestRule.onNodeWithText("Sve").assertExists()
        composeTestRule.onNodeWithText("Odobrene").assertExists()
        composeTestRule.onNodeWithText("Odbijene").assertExists()
        composeTestRule.onNodeWithText("Stornirane").assertExists()
    }

    @Test
    fun filterScreen_sortSection_canExpand() {
        composeTestRule.setContent {
            AppTheme {
                FilterScreen(
                    sharedPreferences = mockSharedPreferences,
                    onApplyFilter = {},
                    onNavigateBack = {}
                )
            }
        }

        // Click to expand sort section
        composeTestRule.onNodeWithText("Razvrstajte").performClick()
        composeTestRule.waitForIdle()

        // Should show all sort options
        composeTestRule.onNodeWithText("Opadajuće po datumu").assertExists()
        composeTestRule.onNodeWithText("Uzlazno po datumu").assertExists()
        composeTestRule.onNodeWithText("Uzlazno po iznosu").assertExists()
        composeTestRule.onNodeWithText("Opadajuće po iznosu").assertExists()
    }

    @Test
    fun filterScreen_applyButton_triggersCallback() {
        composeTestRule.setContent {
            AppTheme {
                FilterScreen(
                    sharedPreferences = mockSharedPreferences,
                    onApplyFilter = { applyFilterCalled = true },
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("PRIMENI FILTERE").performClick()
        composeTestRule.waitForIdle()

        assert(applyFilterCalled)
    }

    @Test
    fun filterScreen_applyButton_savesToSharedPreferences() {
        composeTestRule.setContent {
            AppTheme {
                FilterScreen(
                    sharedPreferences = mockSharedPreferences,
                    onApplyFilter = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("PRIMENI FILTERE").performClick()
        composeTestRule.waitForIdle()

        // Verify SharedPreferences were updated
        verify {
            mockSharedPreferences.push(SharedPreferencesKeys.FILTER_TYPE, any<Int>())
            mockSharedPreferences.push(SharedPreferencesKeys.FILTER_STATUS, any<Int>())
            mockSharedPreferences.push(SharedPreferencesKeys.FILTER_SORT, any<Int>())
        }
    }

    @Test
    fun filterScreen_withActiveFilters_displaysActiveFiltersSection() {
        // Set up active filters
        every { mockSharedPreferences.pull(SharedPreferencesKeys.DATE_FROM, any<String>()) } returns "01-01-2025 00:00"
        every { mockSharedPreferences.pull(SharedPreferencesKeys.FILTER_STATUS, any<Int>()) } returns TransactionStatusFilterType.ACCEPTED.ordinal

        composeTestRule.setContent {
            AppTheme {
                FilterScreen(
                    sharedPreferences = mockSharedPreferences,
                    onApplyFilter = {},
                    onNavigateBack = {}
                )
            }
        }

        // Should show active filters section
        composeTestRule.onNodeWithText("Izabrani filteri").assertExists()
        composeTestRule.onNodeWithText("Izbriši sve filtere").assertExists()
    }

    @Test
    fun filterScreen_activeFilters_displaysDateFromChip() {
        every { mockSharedPreferences.pull(SharedPreferencesKeys.DATE_FROM, any<String>()) } returns "01-01-2025 00:00"

        composeTestRule.setContent {
            AppTheme {
                FilterScreen(
                    sharedPreferences = mockSharedPreferences,
                    onApplyFilter = {},
                    onNavigateBack = {}
                )
            }
        }

        // Should show date from chip
        composeTestRule.onNode(hasText("Datum od", substring = true)).assertExists()
    }

    @Test
    fun filterScreen_activeFilters_displaysDateToChip() {
        every { mockSharedPreferences.pull(SharedPreferencesKeys.DATE_TO, any<String>()) } returns "31-01-2025 23:59"

        composeTestRule.setContent {
            AppTheme {
                FilterScreen(
                    sharedPreferences = mockSharedPreferences,
                    onApplyFilter = {},
                    onNavigateBack = {}
                )
            }
        }

        // Should show date to chip
        composeTestRule.onNodeWithText("Do 31-01-2025").assertExists()
    }

    @Test
    fun filterScreen_activeFilters_displaysStatusChip() {
        every { mockSharedPreferences.pull(SharedPreferencesKeys.FILTER_STATUS, any<Int>()) } returns TransactionStatusFilterType.ACCEPTED.ordinal

        composeTestRule.setContent {
            AppTheme {
                FilterScreen(
                    sharedPreferences = mockSharedPreferences,
                    onApplyFilter = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Odobrene").assertExists()
    }

    @Test
    fun filterScreen_activeFilters_displaysSortChip() {
        every { mockSharedPreferences.pull(SharedPreferencesKeys.FILTER_SORT, any<Int>()) } returns TransactionSortType.AmountAsc.ordinal

        composeTestRule.setContent {
            AppTheme {
                FilterScreen(
                    sharedPreferences = mockSharedPreferences,
                    onApplyFilter = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Uzlazno po iznosu").assertExists()
    }

    @Test
    fun filterScreen_activeFilters_displaysTypeChip() {
        every { mockSharedPreferences.pull(SharedPreferencesKeys.FILTER_TYPE, any<Int>()) } returns TransactionSource.IPS.ordinal

        composeTestRule.setContent {
            AppTheme {
                FilterScreen(
                    sharedPreferences = mockSharedPreferences,
                    onApplyFilter = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("IPS").assertExists()
    }

    @Test
    fun filterScreen_clearAllButton_removesAllFilters() {
        // Set up multiple active filters
        every { mockSharedPreferences.pull(SharedPreferencesKeys.DATE_FROM, any<String>()) } returns "01-01-2025 00:00"
        every { mockSharedPreferences.pull(SharedPreferencesKeys.DATE_TO, any<String>()) } returns "31-01-2025 23:59"
        every { mockSharedPreferences.pull(SharedPreferencesKeys.FILTER_STATUS, any<Int>()) } returns TransactionStatusFilterType.ACCEPTED.ordinal

        composeTestRule.setContent {
            AppTheme {
                FilterScreen(
                    sharedPreferences = mockSharedPreferences,
                    onApplyFilter = {},
                    onNavigateBack = {}
                )
            }
        }

        // Click clear all
        composeTestRule.onNodeWithText("Izbriši sve filtere").performClick()
        composeTestRule.waitForIdle()

        // Verify all filters were removed
        verify {
            mockSharedPreferences.remove(SharedPreferencesKeys.FILTER_TYPE)
            mockSharedPreferences.remove(SharedPreferencesKeys.FILTER_STATUS)
            mockSharedPreferences.remove(SharedPreferencesKeys.FILTER_SORT)
            mockSharedPreferences.remove(SharedPreferencesKeys.DATE_FROM)
            mockSharedPreferences.remove(SharedPreferencesKeys.DATE_TO)
        }
    }

    @Test
    fun filterScreen_noActiveFilters_hidesActiveFiltersSection() {
        composeTestRule.setContent {
            AppTheme {
                FilterScreen(
                    sharedPreferences = mockSharedPreferences,
                    onApplyFilter = {},
                    onNavigateBack = {}
                )
            }
        }

        // Should NOT show active filters section
        composeTestRule.onNodeWithText("Izabrani filteri").assertDoesNotExist()
    }

    @Test
    fun filterScreen_selectTypeIPS_updatesSelection() {
        composeTestRule.setContent {
            AppTheme {
                FilterScreen(
                    sharedPreferences = mockSharedPreferences,
                    onApplyFilter = {},
                    onNavigateBack = {}
                )
            }
        }

        // Expand type section
        composeTestRule.onNodeWithText("Tip transakcije").performClick()
        composeTestRule.waitForIdle()

        // Select IPS
        composeTestRule.onAllNodesWithText("IPS")[0].performClick()
        composeTestRule.waitForIdle()

        // Apply to trigger save
        composeTestRule.onNodeWithText("PRIMENI FILTERE").performClick()
        composeTestRule.waitForIdle()

        verify {
            mockSharedPreferences.push(SharedPreferencesKeys.FILTER_TYPE, TransactionSource.IPS.ordinal)
        }
    }

    @Test
    fun filterScreen_selectStatusRejected_updatesSelection() {
        composeTestRule.setContent {
            AppTheme {
                FilterScreen(
                    sharedPreferences = mockSharedPreferences,
                    onApplyFilter = {},
                    onNavigateBack = {}
                )
            }
        }

        // Expand status section
        composeTestRule.onNodeWithText("Status transakcije").performClick()
        composeTestRule.waitForIdle()

        // Select Rejected
        composeTestRule.onNodeWithText("Odbijene").performClick()
        composeTestRule.waitForIdle()

        // Apply to trigger save
        composeTestRule.onNodeWithText("PRIMENI FILTERE").performClick()
        composeTestRule.waitForIdle()

        verify {
            mockSharedPreferences.push(SharedPreferencesKeys.FILTER_STATUS, TransactionStatusFilterType.REJECTED.ordinal)
        }
    }

    @Test
    fun filterScreen_selectSortAmountDesc_updatesSelection() {
        composeTestRule.setContent {
            AppTheme {
                FilterScreen(
                    sharedPreferences = mockSharedPreferences,
                    onApplyFilter = {},
                    onNavigateBack = {}
                )
            }
        }

        // Expand sort section
        composeTestRule.onNodeWithText("Razvrstajte").performClick()
        composeTestRule.waitForIdle()

        // Select Amount Descending
        composeTestRule.onNodeWithText("Opadajuće po iznosu").performClick()
        composeTestRule.waitForIdle()

        // Apply to trigger save
        composeTestRule.onNodeWithText("PRIMENI FILTERE").performClick()
        composeTestRule.waitForIdle()

        verify {
            mockSharedPreferences.push(SharedPreferencesKeys.FILTER_SORT, TransactionSortType.AmountDesc.ordinal)
        }
    }
}
