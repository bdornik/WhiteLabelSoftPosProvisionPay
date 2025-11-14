package com.payten.whitelabel

import org.junit.Test

class ErrorHandlingTest {

    @Test
    fun `null safety with elvis operator`() {
        // Given
        val nullValue: String? = null
        val validValue: String? = "test"

        // When
        val result1 = nullValue ?: "default"
        val result2 = validValue ?: "default"

        // Then
        assert(result1 == "default") { "Null should return default value" }
        assert(result2 == "test") { "Valid value should be returned" }
    }

    @Test
    fun `safe call operator handles null`() {
        // Given
        val nullString: String? = null
        val validString: String? = "test"

        // When
        val result1 = nullString?.length
        val result2 = validString?.length

        // Then
        assert(result1 == null) { "Null string should return null length" }
        assert(result2 == 4) { "Valid string should return correct length" }
    }

    @Test
    fun `exception message is not empty`() {
        // Given
        val exception = RuntimeException("Test error")

        // Then
        assert(exception.message?.isNotEmpty() == true) {
            "Exception should have a message"
        }
    }

    @Test
    fun `empty list handling`() {
        // Given
        val emptyList = emptyList<String>()
        val nonEmptyList = listOf("item")

        // Then
        assert(emptyList.isEmpty()) { "Empty list should be detected" }
        assert(nonEmptyList.isNotEmpty()) { "Non-empty list should be detected" }
    }
}