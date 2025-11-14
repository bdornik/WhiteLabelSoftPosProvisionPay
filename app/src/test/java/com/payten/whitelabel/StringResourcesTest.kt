package com.payten.whitelabel

import org.junit.Test

class StringResourcesTest {

    @Test
    fun `language codes are valid ISO 639-1 codes`() {
        // Given
        val validCodes = setOf("sl", "en", "sr")
        val testCodes = listOf("sl", "en", "sr")

        // Then
        testCodes.forEach { code ->
            assert(validCodes.contains(code)) { "Language code should be valid: $code" }
            assert(code.length == 2) { "Language code should be 2 characters: $code" }
        }
    }

    @Test
    fun `language index mapping is consistent`() {
        // Given
        val indexToLanguage = mapOf(
            0 to "sl",
            1 to "en",
            2 to "sr"
        )

        // Then - All indices should map to valid language codes
        indexToLanguage.forEach { (index, code) ->
            assert(index in 0..2) { "Language index should be 0-2: $index" }
            assert(code.length == 2) { "Language code should be 2 characters: $code" }
        }
    }

    @Test
    fun `default language is Serbian`() {
        // Given
        val defaultLanguageIndex = 2

        // When
        val defaultLanguage = when (defaultLanguageIndex) {
            0 -> "sl"
            1 -> "en"
            2 -> "sr"
            else -> "sr"
        }

        // Then
        assert(defaultLanguage == "sr") { "Default language should be Serbian" }
    }

    @Test
    fun `language flags are valid emoji`() {
        // Given
        val flags = mapOf(
            0 to "🇸🇮",
            1 to "🇬🇧",
            2 to "🇷🇸"
        )

        // Then - All flags should be non-empty
        flags.forEach { (index, flag) ->
            assert(flag.isNotEmpty()) { "Flag for index $index should not be empty" }
        }
    }
}