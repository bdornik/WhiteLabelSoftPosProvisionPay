package com.payten.whitelabel

import at.favre.lib.crypto.bcrypt.BCrypt
import org.junit.Test

class PinValidationTest {

    @Test
    fun `bcrypt verifies correct PIN`() {
        val pin = "1234"
        val hashedPin = BCrypt.withDefaults().hashToString(12, pin.toCharArray())

        val result = BCrypt.verifyer().verify(pin.toCharArray(), hashedPin)

        assert(result.verified)
    }

    @Test
    fun `bcrypt rejects incorrect PIN`() {
        val correctPin = "1234"
        val wrongPin = "5678"
        val hashedPin = BCrypt.withDefaults().hashToString(12, correctPin.toCharArray())

        val result = BCrypt.verifyer().verify(wrongPin.toCharArray(), hashedPin)

        assert(!result.verified)
    }

    @Test
    fun `pin validation accepts 4-digit PIN`() {
        val pin = "1234"

        val isValid = pin.all { it.isDigit() }

        assert(isValid)
    }

    @Test
    fun `pin validation rejects non-4-digit PIN`() {
        val pins = listOf("123", "12345", "abcd", "12a4")

        pins.forEach { pin ->
            val isValid = pin.length == 4 && pin.all { it.isDigit() }
            assert(!isValid) { "PIN '$pin' should be invalid" }
        }
    }
}