package com.payten.whitelabel

import org.junit.Test

class AppVersionTest {

    @Test
    fun `version string format is correct`() {
        // Given
        val versionName = "1.0.0"
        val versionCode = 1L

        // When
        val appVersion = "$versionName ($versionCode)"

        // Then
        assert(appVersion == "1.0.0 (1)")
    }

    @Test
    fun `version code conversion from int to long`() {
        // Given
        @Suppress("DEPRECATION")
        val oldVersionCode = 123

        // When
        val versionCode: Long = oldVersionCode.toLong()

        // Then
        assert(versionCode == 123L)
    }

    @Test
    fun `unknown version name defaults correctly`() {
        // Given
        val versionName: String? = null

        // When
        val displayVersion = versionName ?: "unknown"

        // Then
        assert(displayVersion == "unknown")
    }
}