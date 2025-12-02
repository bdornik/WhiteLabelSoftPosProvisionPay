package com.payten.whitelabel

import com.payten.whitelabel.dto.TransactionDetailsDto
import com.payten.whitelabel.enums.TransactionStatus
import org.junit.Test
import org.threeten.bp.LocalDateTime

/**
 * Unit tests for transaction filtering logic.
 *
 * Tests filtering by date range, transaction type, status, and sorting.
 */
class TransactionFilterTest {

    // ==================== Type Filtering Tests ====================

    @Test
    fun `filter by POS transactions only`() {
        // Given
        val tx1 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Accepted, null, "1", "", -1, "0")
        val tx2 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100", true, TransactionStatus.Accepted, null, "2", "", -1, "0")
        val tx3 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Accepted, null, "3", "", -1, "0")

        val transactions = listOf(tx1, tx2, tx3)

        // When
        val filtered = transactions.filter { !it.isIps }

        // Then
        assert(filtered.size == 2) { "Expected 2 POS transactions, got ${filtered.size}" }
        assert(filtered.all { !it.isIps }) { "All should be POS transactions" }
    }

    @Test
    fun `filter by IPS transactions only`() {
        // Given
        val tx1 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Accepted, null, "1", "", -1, "0")
        val tx2 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100", true, TransactionStatus.Accepted, null, "2", "", -1, "0")
        val tx3 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100", true, TransactionStatus.Accepted, null, "3", "", -1, "0")

        val transactions = listOf(tx1, tx2, tx3)

        // When
        val filtered = transactions.filter { it.isIps }

        // Then
        assert(filtered.size == 2) { "Expected 2 IPS transactions, got ${filtered.size}" }
        assert(filtered.all { it.isIps }) { "All should be IPS transactions" }
    }

    // ==================== Status Filtering Tests ====================

    @Test
    fun `filter by approved transactions`() {
        // Given
        val tx1 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Accepted, null, "1", "", -1, "0")
        val tx2 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "R", "01", "Declined", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Rejected, null, "2", "", -1, "0")
        val tx3 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Accepted, null, "3", "", -1, "0")

        val transactions = listOf(tx1, tx2, tx3)

        // When
        val filtered = transactions.filter { it.sdkStatus == TransactionStatus.Accepted }

        // Then
        assert(filtered.size == 2) { "Expected 2 approved transactions, got ${filtered.size}" }
        assert(filtered.all { it.sdkStatus == TransactionStatus.Accepted }) { "All should be accepted" }
    }

    @Test
    fun `filter by declined transactions`() {
        // Given
        val tx1 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Accepted, null, "1", "", -1, "0")
        val tx2 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "R", "01", "Declined", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Rejected, null, "2", "", -1, "0")
        val tx3 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "R", "01", "Declined", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Rejected, null, "3", "", -1, "0")

        val transactions = listOf(tx1, tx2, tx3)

        // When
        val filtered = transactions.filter { it.sdkStatus == TransactionStatus.Rejected }

        // Then
        assert(filtered.size == 2) { "Expected 2 declined transactions, got ${filtered.size}" }
    }

    @Test
    fun `filter by voided transactions`() {
        // Given
        val tx1 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Accepted, null, "1", "", -1, "0")
        val tx2 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "V", "00", "Voided", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Voided, null, "2", "", -1, "0")
        val tx3 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "V", "00", "Voided", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Voided, null, "3", "", -1, "0")

        val transactions = listOf(tx1, tx2, tx3)

        // When
        val filtered = transactions.filter { it.sdkStatus == TransactionStatus.Voided }

        // Then
        assert(filtered.size == 2) { "Expected 2 voided transactions, got ${filtered.size}" }
    }

    // ==================== Date Filtering Tests ====================

    @Test
    fun `filter transactions within date range`() {
        // Given - Use fixed dates to avoid timezone initialization issues in tests
        val baseDate = LocalDateTime.of(2025, 12, 2, 14, 30)
        val yesterday = baseDate.minusDays(1)
        val tomorrow = baseDate.plusDays(1)

        val tx1 = TransactionDetailsDto("Bank", yesterday.toString(), "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Accepted, null, "1", "", -1, "0")
        val tx2 = TransactionDetailsDto("Bank", baseDate.toString(), "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Accepted, null, "2", "", -1, "0")
        val tx3 = TransactionDetailsDto("Bank", tomorrow.toString(), "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Accepted, null, "3", "", -1, "0")

        val transactions = listOf(tx1, tx2, tx3)

        // When - filter for base date only
        val filtered = transactions.filter {
            val txDate = LocalDateTime.parse(it.dateTime)
            txDate.toLocalDate() == baseDate.toLocalDate()
        }

        // Then
        assert(filtered.size == 1) { "Expected 1 transaction from base date, got ${filtered.size}" }
        assert(filtered[0].recordId == "2") { "Expected transaction 2, got ${filtered[0].recordId}" }
    }

    // ==================== Amount Filtering Tests ====================

    @Test
    fun `filter transactions by minimum amount`() {
        // Given
        val tx1 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "50.00", false, TransactionStatus.Accepted, null, "1", "", -1, "0")
        val tx2 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100.00", false, TransactionStatus.Accepted, null, "2", "", -1, "0")
        val tx3 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "150.00", false, TransactionStatus.Accepted, null, "3", "", -1, "0")

        val transactions = listOf(tx1, tx2, tx3)

        // When
        val minAmount = 100.0
        val filtered = transactions.filter { it.amount.toDouble() >= minAmount }

        // Then
        assert(filtered.size == 2) { "Expected 2 transactions >= 100, got ${filtered.size}" }
    }

    @Test
    fun `filter transactions by amount range`() {
        // Given
        val tx1 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "25.00", false, TransactionStatus.Accepted, null, "1", "", -1, "0")
        val tx2 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "50.00", false, TransactionStatus.Accepted, null, "2", "", -1, "0")
        val tx3 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "75.00", false, TransactionStatus.Accepted, null, "3", "", -1, "0")
        val tx4 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100.00", false, TransactionStatus.Accepted, null, "4", "", -1, "0")

        val transactions = listOf(tx1, tx2, tx3, tx4)

        // When
        val filtered = transactions.filter {
            val amt = it.amount.toDouble()
            amt in 50.0..75.0
        }

        // Then
        assert(filtered.size == 2) { "Expected 2 transactions in range 50-75, got ${filtered.size}" }
    }

    // ==================== Sorting Tests ====================

    @Test
    fun `sort transactions by amount ascending`() {
        // Given
        val tx1 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100.00", false, TransactionStatus.Accepted, null, "1", "", -1, "0")
        val tx2 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "50.00", false, TransactionStatus.Accepted, null, "2", "", -1, "0")
        val tx3 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "150.00", false, TransactionStatus.Accepted, null, "3", "", -1, "0")

        val transactions = listOf(tx1, tx2, tx3)

        // When
        val sorted = transactions.sortedBy { it.amount.toDouble() }

        // Then
        assert(sorted[0].recordId == "2") { "Smallest amount first, got ${sorted[0].recordId}" }
        assert(sorted[1].recordId == "1")
        assert(sorted[2].recordId == "3") { "Largest amount last, got ${sorted[2].recordId}" }
    }

    @Test
    fun `sort transactions by amount descending`() {
        // Given
        val tx1 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100.00", false, TransactionStatus.Accepted, null, "1", "", -1, "0")
        val tx2 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "50.00", false, TransactionStatus.Accepted, null, "2", "", -1, "0")
        val tx3 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "150.00", false, TransactionStatus.Accepted, null, "3", "", -1, "0")

        val transactions = listOf(tx1, tx2, tx3)

        // When
        val sorted = transactions.sortedByDescending { it.amount.toDouble() }

        // Then
        assert(sorted[0].recordId == "3") { "Largest amount first, got ${sorted[0].recordId}" }
        assert(sorted[2].recordId == "2") { "Smallest amount last, got ${sorted[2].recordId}" }
    }

    // ==================== Combined Filter Tests ====================

    @Test
    fun `filter POS transactions that are approved`() {
        // Given
        val tx1 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Accepted, null, "1", "", -1, "0")
        val tx2 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100", true, TransactionStatus.Accepted, null, "2", "", -1, "0")
        val tx3 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "R", "01", "Declined", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Rejected, null, "3", "", -1, "0")
        val tx4 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Accepted, null, "4", "", -1, "0")

        val transactions = listOf(tx1, tx2, tx3, tx4)

        // When
        val filtered = transactions.filter {
            !it.isIps && it.sdkStatus == TransactionStatus.Accepted
        }

        // Then
        assert(filtered.size == 2) { "Expected 2 approved POS transactions, got ${filtered.size}" }
        assert(filtered.all { !it.isIps && it.sdkStatus == TransactionStatus.Accepted }) { "All should be approved POS" }
    }

    @Test
    fun `filter and sort transactions`() {
        // Given
        val tx1 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100.00", false, TransactionStatus.Accepted, null, "1", "", -1, "0")
        val tx2 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "R", "01", "Declined", "RRN", "C", "VISA", "AID", "50.00", false, TransactionStatus.Rejected, null, "2", "", -1, "0")
        val tx3 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "150.00", false, TransactionStatus.Accepted, null, "3", "", -1, "0")
        val tx4 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "75.00", false, TransactionStatus.Accepted, null, "4", "", -1, "0")

        val transactions = listOf(tx1, tx2, tx3, tx4)

        // When - filter approved and sort by amount descending
        val result = transactions
            .filter { it.sdkStatus == TransactionStatus.Accepted }
            .sortedByDescending { it.amount.toDouble() }

        // Then
        assert(result.size == 3) { "Expected 3 approved transactions, got ${result.size}" }
        assert(result[0].recordId == "3") { "Highest approved amount first, got ${result[0].recordId}" }
        assert(result[1].recordId == "1")
        assert(result[2].recordId == "4") { "Lowest approved amount last, got ${result[2].recordId}" }
    }

    // ==================== Edge Cases ====================

    @Test
    fun `filter that matches no transactions returns empty list`() {
        // Given
        val tx1 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Accepted, null, "1", "", -1, "0")
        val tx2 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Accepted, null, "2", "", -1, "0")

        val transactions = listOf(tx1, tx2)

        // When
        val filtered = transactions.filter { it.isIps }

        // Then
        assert(filtered.isEmpty()) { "Expected empty list when no matches, got ${filtered.size}" }
    }

    @Test
    fun `filter that matches all transactions returns all`() {
        // Given
        val tx1 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Accepted, null, "1", "", -1, "0")
        val tx2 = TransactionDetailsDto("Bank", "2025-12-02T10:00:00", "M001", "T001", "Merchant", "****", "AUTH", "Sale", "A", "00", "OK", "RRN", "C", "VISA", "AID", "100", false, TransactionStatus.Accepted, null, "2", "", -1, "0")

        val transactions = listOf(tx1, tx2)

        // When
        val filtered = transactions.filter { it.sdkStatus == TransactionStatus.Accepted }

        // Then
        assert(filtered.size == transactions.size) { "Expected all transactions, got ${filtered.size} out of ${transactions.size}" }
    }
}
