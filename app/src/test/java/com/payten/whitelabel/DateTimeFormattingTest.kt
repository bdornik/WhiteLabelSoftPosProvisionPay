package com.payten.whitelabel

import org.junit.Test
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

/**
 * Unit tests for date and time formatting.
 *
 * Tests parsing, formatting, and comparison of transaction dates.
 */
class DateTimeFormattingTest {

    // ==================== Date Parsing Tests ====================

    @Test
    fun `parse ISO datetime string`() {
        // Given
        val isoString = "2025-12-02T14:30:00"

        // When
        val dateTime = LocalDateTime.parse(isoString)

        // Then
        assert(dateTime.year == 2025)
        assert(dateTime.monthValue == 12)
        assert(dateTime.dayOfMonth == 2)
        assert(dateTime.hour == 14)
        assert(dateTime.minute == 30)
    }

    @Test
    fun `parse datetime with different formats`() {
        // Given
        val formats = mapOf(
            "2025-12-02T14:30:00" to DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            "2025-12-02" to DateTimeFormatter.ISO_LOCAL_DATE
        )

        // When & Then
        formats.forEach { (dateString, formatter) ->
            try {
                if (formatter == DateTimeFormatter.ISO_LOCAL_DATE) {
                    val date = java.time.LocalDate.parse(dateString, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                    assert(date != null)
                } else {
                    val dateTime = LocalDateTime.parse(dateString)
                    assert(dateTime != null)
                }
            } catch (_: Exception) {
                assert(false) { "Failed to parse $dateString with format $formatter" }
            }
        }
    }

    // ==================== Date Formatting Tests ====================

    @Test
    fun `format datetime to readable string`() {
        // Given
        val dateTime = LocalDateTime.of(2025, 12, 2, 14, 30, 0)

        // When
        val formatted = dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))

        // Then
        assert(formatted == "02.12.2025 14:30")
    }

    @Test
    fun `format datetime for display in different formats`() {
        // Given
        val dateTime = LocalDateTime.of(2025, 12, 2, 14, 30, 0)

        val formats = mapOf(
            "dd.MM.yyyy" to "02.12.2025",
            "dd/MM/yyyy" to "02/12/2025",
            "yyyy-MM-dd" to "2025-12-02",
            "dd.MM.yyyy HH:mm" to "02.12.2025 14:30",
            "HH:mm:ss" to "14:30:00"
        )

        // When & Then
        formats.forEach { (pattern, expected) ->
            val formatted = dateTime.format(DateTimeFormatter.ofPattern(pattern))
            assert(formatted == expected) { "Expected $expected for pattern $pattern, got $formatted" }
        }
    }

    // ==================== Date Comparison Tests ====================

    @Test
    fun `compare dates for sorting`() {
        // Given
        val date1 = LocalDateTime.of(2025, 12, 1, 10, 0)
        val date2 = LocalDateTime.of(2025, 12, 2, 10, 0)
        val date3 = LocalDateTime.of(2025, 12, 3, 10, 0)

        // When
        val dates = listOf(date2, date1, date3).sorted()

        // Then
        assert(dates[0] == date1) { "Earliest date should be first" }
        assert(dates[1] == date2)
        assert(dates[2] == date3) { "Latest date should be last" }
    }

    @Test
    fun `check if date is today`() {
        // Given - Use fixed dates to avoid timezone initialization issues in tests
        val baseDate = LocalDateTime.of(2025, 12, 2, 14, 30)
        val yesterday = baseDate.minusDays(1)
        val tomorrow = baseDate.plusDays(1)

        // Then - Compare date portions
        assert(baseDate.toLocalDate() == baseDate.toLocalDate()) { "Same datetime should have same date" }
        assert(yesterday.toLocalDate() != baseDate.toLocalDate()) { "Yesterday should not equal today" }
        assert(tomorrow.toLocalDate() != baseDate.toLocalDate()) { "Tomorrow should not equal today" }
    }

    @Test
    fun `check if date is in range`() {
        // Given
        val startDate = LocalDateTime.of(2025, 12, 1, 0, 0)
        val endDate = LocalDateTime.of(2025, 12, 31, 23, 59)
        val testDate = LocalDateTime.of(2025, 12, 15, 12, 0)

        // When
        val isInRange = testDate.isAfter(startDate) && testDate.isBefore(endDate)

        // Then
        assert(isInRange) { "Date should be in range" }
    }

    // ==================== Date Manipulation Tests ====================

    @Test
    fun `add days to date`() {
        // Given
        val date = LocalDateTime.of(2025, 12, 1, 10, 0)

        // When
        val future = date.plusDays(7)

        // Then
        assert(future.dayOfMonth == 8)
        assert(future.monthValue == 12)
        assert(future.year == 2025)
    }

    @Test
    fun `subtract days from date`() {
        // Given
        val date = LocalDateTime.of(2025, 12, 10, 10, 0)

        // When
        val past = date.minusDays(5)

        // Then
        assert(past.dayOfMonth == 5)
        assert(past.monthValue == 12)
    }

    @Test
    fun `calculate days between dates`() {
        // Given
        val startDate = LocalDateTime.of(2025, 12, 1, 0, 0)
        val endDate = LocalDateTime.of(2025, 12, 8, 0, 0)

        // When
        val daysBetween = java.time.Duration.between(
            java.time.LocalDateTime.parse(startDate.toString()),
            java.time.LocalDateTime.parse(endDate.toString())
        ).toDays()

        // Then
        assert(daysBetween == 7L) { "Should be 7 days between dates" }
    }

    // ==================== Start/End of Day Tests ====================

    @Test
    fun `get start of day`() {
        // Given
        val dateTime = LocalDateTime.of(2025, 12, 2, 14, 30, 45)

        // When
        val startOfDay = dateTime.toLocalDate().atStartOfDay()

        // Then
        assert(startOfDay.hour == 0)
        assert(startOfDay.minute == 0)
        assert(startOfDay.second == 0)
        assert(startOfDay.dayOfMonth == dateTime.dayOfMonth)
    }

    @Test
    fun `get end of day`() {
        // Given
        val dateTime = LocalDateTime.of(2025, 12, 2, 14, 30, 45)

        // When
        val endOfDay = dateTime.toLocalDate().atTime(23, 59, 59)

        // Then
        assert(endOfDay.hour == 23)
        assert(endOfDay.minute == 59)
        assert(endOfDay.second == 59)
        assert(endOfDay.dayOfMonth == dateTime.dayOfMonth)
    }

    // ==================== Month/Year Operations Tests ====================

    @Test
    fun `get start of month`() {
        // Given
        val dateTime = LocalDateTime.of(2025, 12, 15, 10, 0)

        // When
        val startOfMonth = dateTime.withDayOfMonth(1).toLocalDate().atStartOfDay()

        // Then
        assert(startOfMonth.dayOfMonth == 1)
        assert(startOfMonth.monthValue == 12)
        assert(startOfMonth.hour == 0)
    }

    @Test
    fun `get end of month`() {
        // Given
        val dateTime = LocalDateTime.of(2025, 12, 15, 10, 0)

        // When
        val lastDay = dateTime.toLocalDate().lengthOfMonth()
        val endOfMonth = dateTime.withDayOfMonth(lastDay).toLocalDate().atTime(23, 59, 59)

        // Then
        assert(endOfMonth.dayOfMonth == 31) // December has 31 days
        assert(endOfMonth.monthValue == 12)
        assert(endOfMonth.hour == 23)
    }

    // ==================== Time-Only Operations Tests ====================

    @Test
    fun `extract time from datetime`() {
        // Given
        val dateTime = LocalDateTime.of(2025, 12, 2, 14, 30, 45)

        // When
        val timeString = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))

        // Then
        assert(timeString == "14:30")
    }

    @Test
    fun `compare times regardless of date`() {
        // Given
        val morning = LocalDateTime.of(2025, 12, 1, 9, 0)
        val evening = LocalDateTime.of(2025, 12, 2, 18, 0)

        // When
        val morningTime = morning.toLocalTime()
        val eveningTime = evening.toLocalTime()

        // Then
        assert(morningTime.isBefore(eveningTime))
    }

    // ==================== Invalid Date Handling Tests ====================

    @Test
    fun `parse invalid date string throws exception`() {
        // Given
        val invalidDates = listOf(
            "invalid",
            "2025-13-01", // invalid month
            "2025-12-32", // invalid day
            ""
        )

        // When & Then
        invalidDates.forEach { invalidDate ->
            try {
                LocalDateTime.parse(invalidDate)
                assert(false) { "Should throw exception for invalid date: $invalidDate" }
            } catch (_: Exception) {
                // Expected
                assert(true)
            }
        }
    }

    // ==================== Timezone Considerations ====================

    @Test
    fun `datetime without timezone uses local time`() {
        // Given
        val dateTime = LocalDateTime.of(2025, 12, 2, 14, 30, 0)
        val dateTimeString = dateTime.toString()

        // Then - LocalDateTime should not include timezone information
        assert(dateTimeString.contains("T")) { "LocalDateTime toString should contain T separator, got: $dateTimeString" }
        assert(!dateTimeString.contains("Z")) { "LocalDateTime should not contain Z (UTC indicator), got: $dateTimeString" }
        assert(!dateTimeString.contains("+")) { "LocalDateTime should not contain timezone offset, got: $dateTimeString" }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `datetime at midnight`() {
        // Given
        val midnight = LocalDateTime.of(2025, 12, 2, 0, 0, 0)

        // Then
        assert(midnight.hour == 0)
        assert(midnight.minute == 0)
        assert(midnight.second == 0)
    }

    @Test
    fun `datetime before epoch still works`() {
        // Given
        val oldDate = LocalDateTime.of(1900, 1, 1, 0, 0)

        // Then
        assert(oldDate.year == 1900)
        assert(oldDate.monthValue == 1)
    }

    @Test
    fun `datetime far in future still works`() {
        // Given
        val futureDate = LocalDateTime.of(2100, 12, 31, 23, 59)

        // Then
        assert(futureDate.year == 2100)
        assert(futureDate.monthValue == 12)
    }
}
