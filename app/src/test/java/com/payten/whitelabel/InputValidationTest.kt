package com.payten.whitelabel

import org.junit.Test

class InputValidationTest {

    @Test
    fun `empty string validation`() {
        // Given
        val emptyInputs = listOf("", "   ", "\n", "\t")

        // Then
        emptyInputs.forEach { input ->
            val isEmpty = input.trim().isEmpty()
            assert(isEmpty) { "Input should be considered empty: '$input'" }
        }
    }

    @Test
    fun `alphanumeric validation accepts valid input`() {
        // Given
        val validInputs = listOf("abc123", "ABC", "123", "Test123")

        // Then
        validInputs.forEach { input ->
            val isValid = input.all { it.isLetterOrDigit() }
            assert(isValid) { "Input should be valid alphanumeric: $input" }
        }
    }

    @Test
    fun `alphanumeric validation rejects invalid input`() {
        // Given
        val invalidInputs = listOf("abc-123", "test@123", "hello world", "test_123")

        // Then
        invalidInputs.forEach { input ->
            val isValid = input.all { it.isLetterOrDigit() }
            assert(!isValid) { "Input should be invalid alphanumeric: $input" }
        }
    }

    @Test
    fun `numeric validation accepts digits only`() {
        // Given
        val validInputs = listOf("123", "0", "999999")

        // Then
        validInputs.forEach { input ->
            val isValid = input.all { it.isDigit() }
            assert(isValid) { "Input should be valid numeric: $input" }
        }
    }

    @Test
    fun `numeric validation rejects non-digits`() {
        // Given
        val invalidInputs = listOf("12a", "1.5", "12-34", "")

        // Then
        invalidInputs.forEach { input ->
            val isValid = input.isNotEmpty() && input.all { it.isDigit() }
            assert(!isValid) { "Input should be invalid numeric: $input" }
        }
    }

    @Test
    fun `length validation for fixed-length fields`() {
        // Given - TID должен бити 8 карактера
        val validTIDs = listOf("DU160014", "AB123456")
        val invalidTIDs = listOf("SHORT", "TOOLONGVALUE", "")

        // Then
        validTIDs.forEach { tid ->
            assert(tid.length == 8) { "TID should be 8 characters: $tid" }
        }

        invalidTIDs.forEach { tid ->
            assert(tid.length != 8) { "TID should not be 8 characters: $tid" }
        }
    }

    @Test
    fun `whitespace trimming works correctly`() {
        // Given
        val inputs = mapOf(
            "  test  " to "test",
            "\nvalue\n" to "value",
            "  spaced words  " to "spaced words"
        )

        // Then
        inputs.forEach { (input, expected) ->
            assert(input.trim() == expected) { "Trimmed value should be: $expected" }
        }
    }
}