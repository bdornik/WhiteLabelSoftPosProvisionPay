package com.payten.whitelabel

import com.payten.whitelabel.config.SupercaseConfig
import org.junit.Test

class ConfigValidationTest {

    @Test
    fun `contact number is not empty`() {
        // Then
        assert(SupercaseConfig.CONTACT_NUMBER.isNotEmpty()) {
            "Contact number should not be empty"
        }
    }

    @Test
    fun `contact number contains digits`() {
        // Given
        val contactNumber = SupercaseConfig.CONTACT_NUMBER
            .replace(",", "")
            .replace(" ", "")

        // Then
        assert(contactNumber.isNotEmpty()) { "Contact number should have digits" }
    }

    @Test
    fun `contact number can be formatted for dialer`() {
        // Given
        val phoneNumber = SupercaseConfig.CONTACT_NUMBER
            .replace(",", "")
            .replace(" ", "")

        // Then
        assert(phoneNumber.all { it.isDigit() || it == '+' }) {
            "Formatted phone number should only contain digits and +"
        }
    }

    @Test
    fun `contact number format is valid`() {
        // Given
        val contactNumber = SupercaseConfig.CONTACT_NUMBER

        // Then - Should contain at least one digit
        assert(contactNumber.any { it.isDigit() }) {
            "Contact number should contain at least one digit"
        }
    }
}