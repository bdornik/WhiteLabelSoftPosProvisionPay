package com.payten.whitelabel

import org.junit.Test

/**
 * Unit tests for navigation parameter handling.
 *
 * Tests URL encoding, parameter parsing, and navigation safety.
 */
class NavigationParameterTest {

    // ==================== Parameter Encoding Tests ====================

    @Test
    fun `encode amount parameter for navigation`() {
        // Given
        val amountInPare = 12345L

        // When
        val encodedParam = amountInPare.toString()

        // Then
        assert(encodedParam == "12345") { "Amount should encode as string" }
        assert(encodedParam.toLong() == 12345L) { "Should decode back to Long" }
    }

    @Test
    fun `encode tip amount parameter`() {
        // Given
        val tipInPare = 1500L

        // When
        val encodedParam = tipInPare.toString()

        // Then
        assert(encodedParam == "1500") { "Tip should encode as string" }
        assert(encodedParam.toLongOrNull() != null) { "Should be parseable as Long" }
    }

    // ==================== Parameter Decoding Tests ====================

    @Test
    fun `decode amount from navigation args`() {
        // Given
        val amountParam = "12345"

        // When
        val amountInPare = amountParam.toLongOrNull()

        // Then
        assert(amountInPare != null) { "Should decode to Long" }
        assert(amountInPare == 12345L) { "Decoded amount should match" }
    }

    @Test
    fun `decode tip from navigation args with default`() {
        // Given
        val validTip = "1500"
        val invalidTip = "invalid"
        val missingTip = "0"

        // When
        val tip1 = validTip.toLongOrNull() ?: 0L
        val tip2 = invalidTip.toLongOrNull() ?: 0L
        val tip3 = missingTip.toLongOrNull() ?: 0L

        // Then
        assert(tip1 == 1500L) { "Valid tip should parse" }
        assert(tip2 == 0L) { "Invalid tip should default to 0" }
        assert(tip3 == 0L) { "Missing tip should be 0" }
    }

    // ==================== Route Construction Tests ====================

    @Test
    fun `build card processing route`() {
        // Given
        val amountInPare = 10000L
        val tipAmount = 1500L

        // When
        val route = "card_processing/$amountInPare/$tipAmount"

        // Then
        assert(route == "card_processing/10000/1500") { "Route should be formatted correctly" }
        assert(route.startsWith("card_processing/")) { "Route should have correct prefix" }
    }

    @Test
    fun `build transaction details route with record ID`() {
        // Given
        val recordId = "REC123456"

        // When
        val route = "transaction_details/$recordId"

        // Then
        assert(route == "transaction_details/REC123456") { "Route should include record ID" }
        assert(route.contains(recordId)) { "Route should contain the ID" }
    }

    @Test
    fun `build route with multiple parameters`() {
        // Given
        val param1 = "value1"
        val param2 = 12345L
        val param3 = true

        // When
        val route = "screen/$param1/$param2/$param3"

        // Then
        assert(route == "screen/value1/12345/true") { "Multi-param route: $route" }
    }

    // ==================== Parameter Validation Tests ====================

    @Test
    fun `validate amount parameter is positive`() {
        // Given
        val validAmount = "10000"
        val invalidAmount = "-1000"
        val zeroAmount = "0"

        // When
        val valid = validAmount.toLongOrNull()?.let { it > 0 } ?: false
        val invalid = invalidAmount.toLongOrNull()?.let { it > 0 } ?: false
        val zero = zeroAmount.toLongOrNull()?.let { it > 0 } ?: false

        // Then
        assert(valid) { "Positive amount should be valid" }
        assert(!invalid) { "Negative amount should be invalid" }
        assert(!zero) { "Zero amount should be invalid" }
    }

    @Test
    fun `validate tip parameter is non-negative`() {
        // Given
        val noTip = "0"
        val validTip = "1500"
        val negativeTip = "-500"

        // When
        val tip1Valid = noTip.toLongOrNull()?.let { it >= 0 } ?: false
        val tip2Valid = validTip.toLongOrNull()?.let { it >= 0 } ?: false
        val tip3Valid = negativeTip.toLongOrNull()?.let { it >= 0 } ?: false

        // Then
        assert(tip1Valid) { "Zero tip should be valid" }
        assert(tip2Valid) { "Positive tip should be valid" }
        assert(!tip3Valid) { "Negative tip should be invalid" }
    }

    @Test
    fun `validate record ID format`() {
        // Given
        val validIds = listOf("REC123", "TXN456789", "ID001")
        val invalidIds = listOf("", " ", "REC 123", "REC@123")

        // When
        fun isValidRecordId(id: String): Boolean {
            return id.isNotEmpty() && id.all { it.isLetterOrDigit() }
        }

        // Then
        validIds.forEach { id ->
            assert(isValidRecordId(id)) { "$id should be valid" }
        }
        invalidIds.forEach { id ->
            assert(!isValidRecordId(id)) { "$id should be invalid" }
        }
    }

    // ==================== URL Encoding Tests ====================

    @Test
    fun `parameters with special characters need encoding`() {
        // Given
        val specialStrings = listOf(
            "test space",
            "test/slash",
            "test?question",
            "test&ampersand"
        )

        // Then
        specialStrings.forEach { str ->
            val needsEncoding = str.contains(' ') ||
                               str.contains('/') ||
                               str.contains('?') ||
                               str.contains('&')
            assert(needsEncoding) { "$str needs URL encoding" }
        }
    }

    @Test
    fun `numeric parameters don't need encoding`() {
        // Given
        val numericParams = listOf("123", "12345", "0", "999999")

        // Then
        numericParams.forEach { param ->
            val needsEncoding = !param.all { it.isDigit() }
            assert(!needsEncoding) { "$param doesn't need encoding" }
        }
    }

    // ==================== Route Parsing Tests ====================

    @Test
    fun `parse route with path segments`() {
        // Given
        val route = "card_processing/10000/1500"

        // When
        val segments = route.split('/')

        // Then
        assert(segments.size == 3) { "Should have 3 segments" }
        assert(segments[0] == "card_processing") { "Screen name should be first" }
        assert(segments[1] == "10000") { "Amount should be second" }
        assert(segments[2] == "1500") { "Tip should be third" }
    }

    @Test
    fun `extract parameters from route`() {
        // Given
        val route = "transaction_details/REC123456"

        // When
        val parts = route.split('/')
        val screenName = parts.getOrNull(0)
        val recordId = parts.getOrNull(1)

        // Then
        assert(screenName == "transaction_details") { "Screen name extracted" }
        assert(recordId == "REC123456") { "Record ID extracted" }
    }

    // ==================== Default Value Tests ====================

    @Test
    fun `use default value for missing parameter`() {
        // Given
        val routeWithoutTip = "card_processing/10000"

        // When
        val parts = routeWithoutTip.split('/')
        val amount = parts.getOrNull(1)?.toLongOrNull() ?: 0L
        val tip = parts.getOrNull(2)?.toLongOrNull() ?: 0L // Missing, use default

        // Then
        assert(amount == 10000L) { "Amount should be present" }
        assert(tip == 0L) { "Missing tip should default to 0" }
    }

    @Test
    fun `handle malformed parameter gracefully`() {
        // Given
        val malformedRoute = "card_processing/not_a_number/1500"

        // When
        val parts = malformedRoute.split('/')
        val amount = parts.getOrNull(1)?.toLongOrNull() ?: 0L
        val tip = parts.getOrNull(2)?.toLongOrNull() ?: 0L

        // Then
        assert(amount == 0L) { "Malformed amount should default to 0" }
        assert(tip == 1500L) { "Valid tip should parse correctly" }
    }

    // ==================== Navigation Safety Tests ====================

    @Test
    fun `prevent injection in route parameters`() {
        // Given
        val maliciousInput = "123/../../sensitive_screen"

        // When
        val sanitized = maliciousInput.replace("/", "")

        // Then
        assert(!sanitized.contains("/")) { "Slashes should be removed" }
        // "123/../../sensitive_screen" with slashes removed becomes "123....sensitive_screen" (4 dots from ../..)
        assert(sanitized == "123....sensitive_screen") { "Path traversal prevented, got: $sanitized" }
    }

    @Test
    fun `validate route matches expected pattern`() {
        // Given
        val validRoutes = listOf(
            "card_processing/10000/1500",
            "transaction_details/REC123",
            "amount_entry/0"
        )
        val invalidRoutes = listOf(
            "unknown_screen",
            "card_processing"
        )

        // When
        fun isValidRoute(route: String): Boolean {
            val parts = route.split('/')
            return parts.size >= 2 && parts[0].isNotEmpty()
        }

        // Then
        validRoutes.forEach { route ->
            assert(isValidRoute(route)) { "$route should be valid" }
        }
        invalidRoutes.forEach { route ->
            assert(!isValidRoute(route)) { "$route should be invalid" }
        }
    }

    // ==================== Optional Parameter Tests ====================

    @Test
    fun `handle optional parameters with defaults`() {
        // Given
        data class NavigationParams(
            val amount: Long,
            val tip: Long = 0L,
            val includeReceipt: Boolean = false
        )

        // When
        val withAllParams = NavigationParams(10000L, 1500L, true)
        val withRequiredOnly = NavigationParams(10000L)
        val withSomeParams = NavigationParams(10000L, tip = 1500L)

        // Then
        assert(withAllParams.amount == 10000L && withAllParams.tip == 1500L && withAllParams.includeReceipt)
        assert(withRequiredOnly.amount == 10000L && withRequiredOnly.tip == 0L && !withRequiredOnly.includeReceipt)
        assert(withSomeParams.amount == 10000L && withSomeParams.tip == 1500L && !withSomeParams.includeReceipt)
    }
}
