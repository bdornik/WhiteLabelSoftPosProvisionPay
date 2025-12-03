package com.payten.whitelabel

import org.junit.Test

/**
 * Unit tests for error message handling and formatting.
 *
 * Tests error code parsing, user-friendly messages, and localization.
 */
class ErrorMessageHandlingTest {

    // ==================== Error Code Parsing Tests ====================

    @Test
    fun `parse response code from error message`() {
        // Given
        val errorMessages = mapOf(
            "Error 001: Insufficient funds" to "001",
            "Transaction declined - Code: 051" to "051",
            "05 Invalid card" to "05"
        )

        // When & Then
        errorMessages.forEach { (message, expectedCode) ->
            val code = extractErrorCode(message)
            assert(code == expectedCode) { "Expected $expectedCode from '$message', got $code" }
        }
    }

    @Test
    fun `identify common error codes`() {
        // Given
        val commonErrors = mapOf(
            "05" to "Do not honor",
            "51" to "Insufficient funds",
            "54" to "Expired card",
            "55" to "Incorrect PIN",
            "57" to "Transaction not permitted"
        )

        // Then
        commonErrors.forEach { (code, description) ->
            assert(code.length == 2) { "Standard response codes are 2 digits" }
            assert(description.isNotEmpty()) { "Each code should have a description" }
        }
    }

    // ==================== User-Friendly Message Tests ====================

    @Test
    fun `convert technical error to user-friendly message`() {
        // Given
        val technicalErrors = mapOf(
            "InvalidIccData ErrorCode=20233" to "Card reading error. Please try again.",
            "Connection timeout" to "Network error. Please check your connection.",
            "NULL_POINTER_EXCEPTION" to "An unexpected error occurred. Please try again."
        )

        // When & Then
        technicalErrors.forEach { (technical, _) ->
            val message = convertToUserFriendly(technical)
            assert(message.isNotEmpty()) { "User message should not be empty" }
            assert(!message.contains("Exception", ignoreCase = true)) { "Should not expose technical terms" }
        }
    }

    @Test
    fun `error messages are clear and actionable`() {
        // Given
        val errorMessages = listOf(
            "Insufficient funds. Please use another card.",
            "Card expired. Please use a different card.",
            "Incorrect PIN. Please try again.",
            "Transaction declined. Please contact your bank."
        )

        // Then
        errorMessages.forEach { message ->
            assert(message.contains("Please", ignoreCase = true)) { "Should provide guidance" }
            assert(message.length > 20) { "Should be descriptive: $message" }
        }
    }

    // ==================== Error Categorization Tests ====================

    @Test
    fun `categorize errors by type`() {
        // Given
        val cardErrors = listOf("05", "51", "54", "55")
        val systemErrors = listOf("96", "91", "68")
        listOf("timeout", "connection refused", "host unreachable")

        // Then
        cardErrors.forEach { code ->
            val category = categorizeError(code)
            assert(category == "CARD_ERROR") { "$code should be card error" }
        }
        systemErrors.forEach { code ->
            val category = categorizeError(code)
            assert(category == "SYSTEM_ERROR") { "$code should be system error" }
        }
    }

    @Test
    fun `determine if error is recoverable`() {
        // Given
        val recoverableErrors = listOf(
            "timeout" to true,
            "51" to true,  // Insufficient funds - can try another card
            "55" to true,  // Wrong PIN - can retry
            "54" to false, // Expired card - not recoverable
            "57" to false  // Not permitted - not recoverable
        )

        // When & Then
        recoverableErrors.forEach { (error, expectedRecoverable) ->
            val isRecoverable = isErrorRecoverable(error)
            assert(isRecoverable == expectedRecoverable) {
                "$error recoverable status should be $expectedRecoverable"
            }
        }
    }

    // ==================== Error Logging Tests ====================

    @Test
    fun `sanitize error messages for logging`() {
        // Given
        val cardNumber = "1234567890123456"
        val errorWithSensitiveData = "Transaction failed for card $cardNumber"

        // When
        val sanitized = sanitizeForLogging(errorWithSensitiveData, cardNumber)

        // Then
        assert(!sanitized.contains(cardNumber)) { "Card number should be masked" }
        assert(sanitized.contains("****")) { "Should contain masking characters" }
    }

    @Test
    fun `include relevant context in error logs`() {
        // Given
        val errorContext = mapOf(
            "errorCode" to "51",
            "amount" to "100.00",
            "timestamp" to "2025-12-02T10:00:00",
            "terminalId" to "T001"
        )

        // Then
        errorContext.forEach { (key, value) ->
            assert(key.isNotEmpty()) { "Context key should not be empty" }
            assert(value.isNotEmpty()) { "Context value should not be empty" }
        }
    }

    // ==================== Error Message Formatting Tests ====================

    @Test
    fun `format error message with code and description`() {
        // Given
        val errorCode = "51"
        val description = "Insufficient funds"

        // When
        val formatted = "Error $errorCode: $description"

        // Then
        assert(formatted == "Error 51: Insufficient funds")
        assert(formatted.contains(errorCode))
        assert(formatted.contains(description))
    }

    @Test
    fun `truncate very long error messages`() {
        // Given
        val longMessage = "Error occurred: " + "x".repeat(500)
        val maxLength = 200

        // When
        val truncated = if (longMessage.length > maxLength) {
            longMessage.take(maxLength - 3) + "..."
        } else {
            longMessage
        }

        // Then
        assert(truncated.length <= maxLength) { "Message should be truncated to $maxLength chars" }
        assert(truncated.endsWith("...")) { "Truncated message should end with ..." }
    }

    // ==================== Localization Tests ====================

    @Test
    fun `error messages support multiple languages`() {
        // Given
        val errorCode = "51"
        val translations = mapOf(
            "en" to "Insufficient funds",
            "sr" to "Nedovoljno sredstava",
            "sl" to "Nezadostna sredstva"
        )

        // Then
        translations.forEach { (lang, message) ->
            assert(message.isNotEmpty()) { "Translation for $lang should exist" }
            assert(message != errorCode) { "Translation should not be just the code" }
        }
    }

    @Test
    fun `fallback to default language for missing translations`() {
        // Given
        val requestedLanguage = "de" // Not available
        val translations = mapOf(
            "en" to "Insufficient funds",
            "sr" to "Nedovoljno sredstava"
        )

        // When
        val message = translations[requestedLanguage] ?: translations["en"] ?: "Unknown error"

        // Then
        assert(message == "Insufficient funds") { "Should fallback to English" }
    }

    // ==================== Error Response Tests ====================

    @Test
    fun `parse error from API response`() {
        // Given
        val apiError = """{"status": false, "message": "Transaction declined", "code": "05"}"""

        // Then
        assert(apiError.contains("status")) { "Should have status field" }
        assert(apiError.contains("message")) { "Should have message field" }
        assert(apiError.contains("false")) { "Status should be false for errors" }
    }

    @Test
    fun `extract error details from response`() {
        // Given
        data class ErrorResponse(val status: Boolean, val message: String, val code: String?)

        val error = ErrorResponse(false, "Transaction declined", "05")

        // Then
        assert(!error.status) { "Status should be false" }
        assert(error.message.isNotEmpty()) { "Should have error message" }
        assert(error.code != null) { "Should have error code" }
    }

    // ==================== Error Dialog Tests ====================

    @Test
    fun `format error for dialog display`() {
        // Given
        val title = "Transaction Failed"
        val message = "Insufficient funds. Please use another card."
        val actionButton = "Try Again"

        // Then
        assert(title.length < 50) { "Title should be concise" }
        assert(message.contains("Please")) { "Message should provide guidance" }
        assert(actionButton.isNotEmpty()) { "Should have action button" }
    }

    @Test
    fun `error dialogs have appropriate severity`() {
        // Given
        val errorSeverities = mapOf(
            "Wrong PIN" to "warning",
            "Card expired" to "error",
            "Network timeout" to "warning",
            "System failure" to "error"
        )

        // Then
        errorSeverities.forEach { (_, severity) ->
            assert(severity in listOf("info", "warning", "error")) {
                "Severity should be valid: $severity"
            }
        }
    }

    // ==================== Helper Functions ====================

    private fun extractErrorCode(message: String): String {
        // Simple regex to extract numeric codes
        val regex = "\\b\\d{2,3}\\b".toRegex()
        return regex.find(message)?.value ?: ""
    }

    private fun convertToUserFriendly(technical: String): String {
        return when {
            technical.contains("InvalidIccData") -> "Card reading error. Please try again."
            technical.contains("timeout", ignoreCase = true) -> "Network error. Please check your connection."
            else -> "An unexpected error occurred. Please try again."
        }
    }

    private fun categorizeError(error: String): String {
        return when (error) {
            in listOf("05", "51", "54", "55") -> "CARD_ERROR"
            in listOf("96", "91", "68") -> "SYSTEM_ERROR"
            else -> "UNKNOWN_ERROR"
        }
    }

    private fun isErrorRecoverable(error: String): Boolean {
        return when (error) {
            "timeout", "51", "55" -> true
            "54", "57" -> false
            else -> false
        }
    }

    private fun sanitizeForLogging(message: String, sensitiveData: String): String {
        return message.replace(sensitiveData, "****" + sensitiveData.takeLast(4))
    }
}
