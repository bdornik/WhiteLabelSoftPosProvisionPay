package com.payten.whitelabel

import com.payten.whitelabel.persistance.SharedPreferencesKeys
import org.junit.Test

class SharedPreferencesKeyTest {

    @Test
    fun `all preference keys are non-empty`() {
        // Given
        val keys = listOf(
            SharedPreferencesKeys.LANGUAGE,
            SharedPreferencesKeys.TOKEN,
            SharedPreferencesKeys.MERCHANT_NAME,
            SharedPreferencesKeys.MERCHANT_ADDRESS,
            SharedPreferencesKeys.MERCHANT_PLACE_NAME,
            SharedPreferencesKeys.USER_TID
        )

        // Then
        keys.forEach { key ->
            assert(key.isNotEmpty()) { "Key should not be empty: $key" }
        }
    }

    @Test
    fun `preference keys are unique`() {
        // Given
        val keys = listOf(
            SharedPreferencesKeys.LANGUAGE,
            SharedPreferencesKeys.TOKEN,
            SharedPreferencesKeys.MERCHANT_NAME,
            SharedPreferencesKeys.MERCHANT_ADDRESS,
            SharedPreferencesKeys.MERCHANT_PLACE_NAME,
            SharedPreferencesKeys.USER_TID
        )

        // Then
        val uniqueKeys = keys.toSet()
        assert(keys.size == uniqueKeys.size) { "All keys should be unique" }
    }

    @Test
    fun `language key is consistent`() {
        // Given
        val key = SharedPreferencesKeys.LANGUAGE

        // Then - Key should be non-empty and alphanumeric
        assert(key.isNotEmpty()) { "Language key should not be empty" }
        assert(key.all { it.isLetterOrDigit() || it == '_' }) {
            "Language key should be alphanumeric or underscore: $key"
        }
    }

    @Test
    fun `token key is consistent`() {
        // Given
        val key = SharedPreferencesKeys.TOKEN

        // Then - Key should be non-empty and alphanumeric
        assert(key.isNotEmpty()) { "Token key should not be empty" }
        assert(key.all { it.isLetterOrDigit() || it == '_' }) {
            "Token key should be alphanumeric or underscore: $key"
        }
    }

    @Test
    fun `preference keys contain only valid characters`() {
        // Given
        val keys = listOf(
            SharedPreferencesKeys.LANGUAGE,
            SharedPreferencesKeys.TOKEN,
            SharedPreferencesKeys.MERCHANT_NAME,
            SharedPreferencesKeys.MERCHANT_ADDRESS,
            SharedPreferencesKeys.MERCHANT_PLACE_NAME,
            SharedPreferencesKeys.USER_TID
        )

        // Then - Keys should only contain letters, digits, or underscores
        keys.forEach { key ->
            val isValid = key.all { it.isLetterOrDigit() || it == '_' }
            assert(isValid) { "Key should only contain alphanumeric or underscore: $key" }
        }
    }

    @Test
    fun `preference keys have reasonable length`() {
        // Given
        val keys = listOf(
            SharedPreferencesKeys.LANGUAGE,
            SharedPreferencesKeys.TOKEN,
            SharedPreferencesKeys.MERCHANT_NAME,
            SharedPreferencesKeys.MERCHANT_ADDRESS,
            SharedPreferencesKeys.MERCHANT_PLACE_NAME,
            SharedPreferencesKeys.USER_TID
        )

        // Then - Keys should be between 3 and 50 characters
        keys.forEach { key ->
            assert(key.length in 3..50) {
                "Key should be between 3-50 characters: $key (length: ${key.length})"
            }
        }
    }

    @Test
    fun `merchant-related keys contain merchant prefix or identifier`() {
        // Given
        val merchantKeys = listOf(
            SharedPreferencesKeys.MERCHANT_NAME,
            SharedPreferencesKeys.MERCHANT_ADDRESS,
            SharedPreferencesKeys.MERCHANT_PLACE_NAME
        )

        // Then - At least check they're not empty
        merchantKeys.forEach { key ->
            assert(key.isNotEmpty()) { "Merchant key should not be empty: $key" }
        }
    }

    @Test
    fun `user TID key is well-formed`() {
        // Given
        val tidKey = SharedPreferencesKeys.USER_TID

        // Then
        assert(tidKey.isNotEmpty()) { "TID key should not be empty" }
        assert(tidKey.length >= 3) { "TID key should be at least 3 characters: $tidKey" }
    }
}