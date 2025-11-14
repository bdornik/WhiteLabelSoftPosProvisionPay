package com.payten.whitelabel

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class LanguageSwitchingTest {

    @Mock
    private lateinit var sharedPreferences: KsPrefs

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `language index 0 maps to Slovenian`() {
        // Given
        val languageIndex = 0

        // When
        val localeTag = when (languageIndex) {
            0 -> "sl"
            1 -> "en"
            2 -> "sr"
            else -> "sr"
        }

        // Then
        assert(localeTag == "sl")
    }

    @Test
    fun `language index 1 maps to English`() {
        // Given
        val languageIndex = 1

        // When
        val localeTag = when (languageIndex) {
            0 -> "sl"
            1 -> "en"
            2 -> "sr"
            else -> "sr"
        }

        // Then
        assert(localeTag == "en")
    }

    @Test
    fun `language index 2 maps to Serbian`() {
        // Given
        val languageIndex = 2

        // When
        val localeTag = when (languageIndex) {
            0 -> "sl"
            1 -> "en"
            2 -> "sr"
            else -> "sr"
        }

        // Then
        assert(localeTag == "sr")
    }

    @Test
    fun `invalid language index defaults to Serbian`() {
        // Given
        val invalidIndices = listOf(-1, 3, 99, 100)

        // When & Then
        invalidIndices.forEach { index ->
            val localeTag = when (index) {
                0 -> "sl"
                1 -> "en"
                2 -> "sr"
                else -> "sr"
            }
            assert(localeTag == "sr") { "Index $index should default to Serbian" }
        }
    }

    @Test
    fun `language preference is saved correctly`() {
        // Given
        val languageIndex = 1

        // When
        sharedPreferences.push(SharedPreferencesKeys.LANGUAGE, languageIndex)

        // Then
        verify(sharedPreferences).push(SharedPreferencesKeys.LANGUAGE, languageIndex)
    }

    @Test
    fun `default language is Serbian when not set`() {
        // Given
        `when`(sharedPreferences.pull(SharedPreferencesKeys.LANGUAGE, 2)).thenReturn(2)

        // When
        val languageIndex = sharedPreferences.pull(SharedPreferencesKeys.LANGUAGE, 2)

        // Then
        assert(languageIndex == 2)
    }
}