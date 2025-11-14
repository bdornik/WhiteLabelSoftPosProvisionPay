package com.payten.whitelabel

import org.junit.Test

class PhoneNumberValidationTest {

    @Test
    fun `phone number formatting removes commas and spaces`() {
        // Given
        val phoneNumber = "011 123 4567, 011 765 4321"

        // When
        val formatted = phoneNumber.replace(",", "").replace(" ", "")

        // Then
        assert(formatted == "01112345670117654321")
    }

    @Test
    fun `valid Serbian phone number format`() {
        // Given
        val validNumbers = listOf(
            "011 123 4567",
            "064 123 4567",
            "+381 11 123 4567"
        )

        // When & Then
        validNumbers.forEach { number ->
            val cleaned = number.replace("+", "").replace(" ", "")
            assert(cleaned.all { it.isDigit() }) { "$number should contain only digits after cleaning" }
        }
    }

    @Test
    fun `phone number with multiple numbers separated by comma`() {
        // Given
        val phoneNumbers = "011 123 4567, 064 987 6543"

        // When
        val numbers = phoneNumbers.split(",").map { it.trim() }

        // Then
        assert(numbers.size == 2)
        assert(numbers[0] == "011 123 4567")
        assert(numbers[1] == "064 987 6543")
    }

    @Test
    fun `empty phone number handling`() {
        // Given
        val emptyNumber = ""

        // When
        val formatted = emptyNumber.replace(",", "").replace(" ", "")

        // Then
        assert(formatted.isEmpty())
    }
}