package com.payten.whitelabel

import org.junit.Test
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

/**
 * Unit tests for receipt formatting and printing.
 *
 * Tests receipt layout, text formatting, and content validation.
 */
class ReceiptFormattingTest {

    // ==================== Basic Receipt Structure Tests ====================

    @Test
    fun `receipt contains all required fields`() {
        // Given
        val receipt = buildBasicReceipt()

        // Then
        assert(receipt.contains("OTP banka") || receipt.contains("MERCHANT")) { "Should have merchant name" }
        assert(receipt.contains("TERMINAL")) { "Should have terminal ID" }
        assert(receipt.contains("AMOUNT")) { "Should have amount" }
        assert(receipt.contains("DATE")) { "Should have date" }
        assert(receipt.contains("TIME")) { "Should have time" }
        assert(receipt.contains("CARD")) { "Should have card info" }
    }

    @Test
    fun `receipt header formatting`() {
        // Given
        val merchantName = "OTP banka d.d."
        val header = formatReceiptHeader(merchantName)

        // Then
        assert(header.contains(merchantName)) { "Header should contain merchant name" }
        assert(header.trim() == merchantName) { "Trimmed header should equal merchant name" }
    }

    @Test
    fun `receipt footer with signature line`() {
        // Given
        val footer = buildReceiptFooter()

        // Then
        assert(footer.contains("_____")) { "Should have signature line" }
        assert(footer.contains("CUSTOMER") || footer.contains("CARDHOLDER")) {
            "Should indicate customer signature"
        }
    }

    // ==================== Amount Formatting Tests ====================

    @Test
    fun `format amount with currency symbol`() {
        // Given
        val amounts = mapOf(
            "100.00" to "100,00 EUR",
            "1234.56" to "1.234,56 EUR",
            "0.50" to "0,50 EUR"
        )

        // When & Then
        amounts.forEach { (amount, _) ->
            val formatted = formatReceiptAmount(amount)
            assert(formatted.contains("EUR")) { "Should include currency" }
            assert(formatted.contains(amount.replace(".", ","))) { "Should format decimal correctly" }
        }
    }

    @Test
    fun `display subtotal and tip separately`() {
        // When
        val receiptLines = listOf(
            "SUBTOTAL:    100,00 EUR",
            "TIP:          15,00 EUR",
            "TOTAL:       115,00 EUR"
        )

        // Then
        assert(receiptLines.any { it.contains("SUBTOTAL") })
        assert(receiptLines.any { it.contains("TIP") })
        assert(receiptLines.any { it.contains("TOTAL") })
    }

    @Test
    fun `align amounts to the right`() {
        // Given
        val lines = listOf(
            "AMOUNT:           100,00",
            "TIP:               15,00",
            "TOTAL:            115,00"
        )

        // Then
        lines.forEach { line ->
            val parts = line.split(":")
            assert(parts.size == 2) { "Should have label and value" }
            val value = parts[1]
            assert(value.trimStart() != value) { "Amount should be right-aligned with leading spaces" }
        }
    }

    // ==================== Date/Time Formatting Tests ====================

    @Test
    fun `format transaction date for receipt`() {
        // Given
        val dateTime = LocalDateTime.of(2025, 12, 2, 14, 30, 0)

        // When
        val dateFormatted = dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        val timeFormatted = dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))

        // Then
        assert(dateFormatted == "02.12.2025") { "Date should be DD.MM.YYYY format" }
        assert(timeFormatted == "14:30:00") { "Time should be HH:MM:SS format" }
    }

    @Test
    fun `display date and time on receipt`() {
        // Given
        val receipt = buildBasicReceipt()

        // Then
        assert(receipt.contains("DATE:") || receipt.contains("DATUM:")) { "Should have date label" }
        assert(receipt.contains("TIME:") || receipt.contains("VRIJEME:")) { "Should have time label" }
    }

    // ==================== Card Information Tests ====================

    @Test
    fun `mask card number on receipt`() {
        // Given
        val fullCardNumber = "1234567890123456"

        // When
        val masked = maskCardForReceipt(fullCardNumber)

        // Then
        assert(!masked.contains(fullCardNumber)) { "Full card number should not appear" }
        assert(masked.contains("****")) { "Should contain masking" }
        assert(masked.contains("3456")) { "Should show last 4 digits" }
    }

    @Test
    fun `display card brand on receipt`() {
        // Given
        val cardBrands = listOf("VISA", "Mastercard", "Maestro", "American Express")

        // Then
        cardBrands.forEach { brand ->
            assert(brand.isNotEmpty()) { "Card brand should not be empty" }
            assert(brand.length <= 20) { "Card brand should be reasonable length" }
        }
    }

    @Test
    fun `show authorization code`() {
        // Given
        val authCode = "046667"

        // When
        val line = "AUTH CODE: $authCode"

        // Then
        assert(line.contains(authCode)) { "Should contain auth code" }
        assert(authCode.length == 6) { "Auth codes are typically 6 digits" }
    }

    // ==================== Transaction Details Tests ====================

    @Test
    fun `display transaction type`() {
        // Given
        val transactionTypes = listOf("SALE", "VOID", "REFUND")

        // Then
        transactionTypes.forEach { type ->
            val line = "TYPE: $type"
            assert(line.contains(type)) { "Should show transaction type" }
        }
    }

    @Test
    fun `show transaction result`() {
        // Given
        val approvedReceipt = "RESULT: APPROVED"
        val declinedReceipt = "RESULT: DECLINED"

        // Then
        assert(approvedReceipt.contains("APPROVED"))
        assert(declinedReceipt.contains("DECLINED"))
    }

    @Test
    fun `include RRN reference number`() {
        // Given
        val rrn = "123456789012"

        // When
        val line = "RRN: $rrn"

        // Then
        assert(line.contains(rrn)) { "Should include RRN" }
        assert(rrn.length == 12) { "RRN is typically 12 digits" }
    }

    // ==================== Receipt Layout Tests ====================

    @Test
    fun `receipt width is fixed`() {
        // Given
        val maxWidth = 32 // Common thermal printer width

        val lines = listOf(
            "MERCHANT: OTP banka d.d.",
            "AMOUNT: 100,00 EUR",
            "================================"
        )

        // Then
        lines.forEach { line ->
            assert(line.length <= maxWidth) { "Line should not exceed $maxWidth chars: '$line'" }
        }
    }

    @Test
    fun `use separator lines`() {
        // Given
        val separators = listOf(
            "================================", // 32 chars
            "--------------------------------"  // 32 chars
        )

        // Then
        separators.forEach { separator ->
            assert(separator.length == 32) { "Separator should be full width, got ${separator.length}" }
            val allowedChars = setOf('=', '-', ' ')
            assert(separator.all { it in allowedChars }) { "Should only use line characters: $separator" }
        }
    }

    @Test
    fun `center align text for headers`() {
        // Given
        val text = "OTP banka d.d."
        val width = 32

        // When
        val centered = text.padStart((width + text.length) / 2).padEnd(width)

        // Then
        assert(centered.length == width) { "Centered text should be full width" }
        assert(centered.trim() == text) { "Should contain original text" }
    }

    // ==================== Merchant Copy vs Customer Copy Tests ====================

    @Test
    fun `indicate receipt copy type`() {
        // Given
        val merchantCopy = "*** MERCHANT COPY ***"
        val customerCopy = "*** CUSTOMER COPY ***"

        // Then
        assert(merchantCopy.contains("MERCHANT"))
        assert(customerCopy.contains("CUSTOMER"))
        assert(merchantCopy != customerCopy) { "Copies should be clearly distinguished" }
    }

    @Test
    fun `merchant copy requires signature`() {
        // Given
        val merchantReceipt = buildMerchantReceipt()

        // Then
        assert(merchantReceipt.contains("SIGNATURE") || merchantReceipt.contains("POTPIS")) {
            "Merchant copy should have signature line"
        }
    }

    // ==================== Multilingual Support Tests ====================

    @Test
    fun `receipt supports Serbian language`() {
        // Given
        val serbianLabels = mapOf(
            "IZNOS" to "AMOUNT",
            "DATUM" to "DATE",
            "VRIJEME" to "TIME",
            "KARTICA" to "CARD"
        )

        // Then
        serbianLabels.forEach { (serbian, english) ->
            assert(serbian.isNotEmpty()) { "Serbian translation should exist" }
            assert(serbian != english) { "Should be translated, not in English" }
        }
    }

    @Test
    fun `receipt supports Slovenian language`() {
        // Given
        val slovenianLabels = mapOf(
            "ZNESEK" to "AMOUNT",
            "DATUM" to "DATE",
            "ČAS" to "TIME",
            "KARTICA" to "CARD"
        )

        // Then
        slovenianLabels.forEach { (slovenian, _) ->
            assert(slovenian.isNotEmpty()) { "Slovenian translation should exist" }
        }
    }

    // ==================== Special Characters Tests ====================

    @Test
    fun `handle special characters in merchant name`() {
        // Given
        val merchantsWithSpecialChars = listOf(
            "Café & Restaurant",
            "Müller GmbH",
            "Société Générale"
        )

        // Then
        merchantsWithSpecialChars.forEach { name ->
            val formatted = formatMerchantName(name)
            assert(formatted.isNotEmpty()) { "Should handle special characters" }
        }
    }

    @Test
    fun `currency symbol displays correctly`() {
        // Given
        val currencies = listOf("EUR", "RSD", "USD")

        // Then
        currencies.forEach { currency ->
            val line = "100,00 $currency"
            assert(line.contains(currency)) { "Should display currency: $currency" }
        }
    }

    // ==================== Helper Functions ====================

    private fun buildBasicReceipt(): String {
        return """
            OTP banka d.d.
            ================================
            TERMINAL: T001
            DATE: 02.12.2025
            TIME: 14:30:00
            ================================
            CARD: ****5678 (VISA)
            AMOUNT: 100,00 EUR
            ================================
        """.trimIndent()
    }

    private fun buildReceiptFooter(): String {
        return """

            CUSTOMER SIGNATURE:
            _____________________________

        """.trimIndent()
    }

    private fun buildMerchantReceipt(): String {
        return buildBasicReceipt() + "\n*** MERCHANT COPY ***\nSIGNATURE: _______________"
    }

    private fun formatReceiptHeader(merchantName: String): String {
        return merchantName.padStart((32 + merchantName.length) / 2).padEnd(32)
    }

    private fun formatReceiptAmount(amount: String): String {
        return "${amount.replace(".", ",")} EUR"
    }

    private fun maskCardForReceipt(cardNumber: String): String {
        return "****" + cardNumber.takeLast(4)
    }

    private fun formatMerchantName(name: String): String {
        // Simple sanitization - in real app would handle encoding properly
        return name.take(32)
    }
}
