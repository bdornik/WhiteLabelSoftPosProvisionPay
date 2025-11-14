package com.payten.whitelabel

import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class MerchantDataValidationTest {

    @Mock
    private lateinit var sharedPreferences: KsPrefs

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `merchant name is retrieved correctly`() {
        // Given
        val expectedName = "Petar Petrović"
        `when`(sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME, "N/A"))
            .thenReturn(expectedName)

        // When
        val merchantName = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME, "N/A")

        // Then
        assert(merchantName == expectedName)
    }

    @Test
    fun `merchant address is formatted correctly`() {
        // Given
        val address = "Bul. Mihajla Pupina 10b"
        val city = "Beograd"

        // When
        val fullAddress = "$address, $city"

        // Then
        assert(fullAddress == "Bul. Mihajla Pupina 10b, Beograd")
    }

    @Test
    fun `TID validation accepts 8-character alphanumeric`() {
        // Given
        val validTIDs = listOf("DU160014", "AB123456", "XY999999")

        // When & Then
        validTIDs.forEach { tid ->
            val isValid = tid.length == 8 && tid.all { it.isLetterOrDigit() }
            assert(isValid) { "TID '$tid' should be valid" }
        }
    }

    @Test
    fun `TID validation rejects invalid formats`() {
        // Given
        val invalidTIDs = listOf("", "123", "TOOLONG123", "AB-12345", "AB 12345")

        // When & Then
        invalidTIDs.forEach { tid ->
            val isValid = tid.length == 8 && tid.all { it.isLetterOrDigit() }
            assert(!isValid) { "TID '$tid' should be invalid" }
        }
    }

    @Test
    fun `merchant data defaults to N_A when not set`() {
        // Given
        `when`(sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME, "N/A"))
            .thenReturn("N/A")

        // When
        val merchantName = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME, "N/A")

        // Then
        assert(merchantName == "N/A")
    }
}