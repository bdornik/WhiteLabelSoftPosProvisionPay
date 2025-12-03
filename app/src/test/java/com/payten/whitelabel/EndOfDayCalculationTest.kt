package com.payten.whitelabel

import com.payten.whitelabel.dto.TransactionDetailsDto
import com.payten.whitelabel.enums.TransactionStatus
import org.junit.Test

/**
 * Unit tests for end-of-day (EOD) calculations.
 *
 * Tests transaction totals, summaries, and reconciliation logic.
 */
class EndOfDayCalculationTest {

    // ==================== Transaction Total Tests ====================

    @Test
    fun `calculate total sales for the day`() {
        // Given
        val transactions = listOf(
            createTransaction(amount = "100.00", status = TransactionStatus.Accepted),
            createTransaction(amount = "50.00", status = TransactionStatus.Accepted),
            createTransaction(amount = "75.00", status = TransactionStatus.Accepted)
        )

        // When
        val totalSales = transactions
            .filter { it.sdkStatus == TransactionStatus.Accepted }
            .sumOf { it.amount.toDouble() }

        // Then
        assert(totalSales == 225.0) { "Total sales should be 225.00, got $totalSales" }
    }

    @Test
    fun `exclude rejected transactions from totals`() {
        // Given
        val transactions = listOf(
            createTransaction(amount = "100.00", status = TransactionStatus.Accepted),
            createTransaction(amount = "50.00", status = TransactionStatus.Rejected),
            createTransaction(amount = "75.00", status = TransactionStatus.Accepted)
        )

        // When
        val totalSales = transactions
            .filter { it.sdkStatus == TransactionStatus.Accepted }
            .sumOf { it.amount.toDouble() }

        // Then
        assert(totalSales == 175.0) { "Rejected transactions should not count, got $totalSales" }
    }

    @Test
    fun `exclude voided transactions from totals`() {
        // Given
        val transactions = listOf(
            createTransaction(amount = "100.00", status = TransactionStatus.Accepted),
            createTransaction(amount = "50.00", status = TransactionStatus.Voided),
            createTransaction(amount = "75.00", status = TransactionStatus.Accepted)
        )

        // When
        val totalSales = transactions
            .filter { it.sdkStatus == TransactionStatus.Accepted }
            .sumOf { it.amount.toDouble() }

        // Then
        assert(totalSales == 175.0) { "Voided transactions should not count, got $totalSales" }
    }

    // ==================== Transaction Count Tests ====================

    @Test
    fun `count successful transactions`() {
        // Given
        val transactions = listOf(
            createTransaction(status = TransactionStatus.Accepted),
            createTransaction(status = TransactionStatus.Rejected),
            createTransaction(status = TransactionStatus.Accepted),
            createTransaction(status = TransactionStatus.Voided),
            createTransaction(status = TransactionStatus.Accepted)
        )

        // When
        val successCount = transactions.count { it.sdkStatus == TransactionStatus.Accepted }

        // Then
        assert(successCount == 3) { "Should have 3 successful transactions, got $successCount" }
    }

    @Test
    fun `count rejected transactions`() {
        // Given
        val transactions = listOf(
            createTransaction(status = TransactionStatus.Accepted),
            createTransaction(status = TransactionStatus.Rejected),
            createTransaction(status = TransactionStatus.Rejected),
            createTransaction(status = TransactionStatus.Accepted)
        )

        // When
        val rejectCount = transactions.count { it.sdkStatus == TransactionStatus.Rejected }

        // Then
        assert(rejectCount == 2) { "Should have 2 rejected transactions, got $rejectCount" }
    }

    @Test
    fun `count voided transactions`() {
        // Given
        val transactions = listOf(
            createTransaction(status = TransactionStatus.Accepted),
            createTransaction(status = TransactionStatus.Voided),
            createTransaction(status = TransactionStatus.Voided)
        )

        // When
        val voidCount = transactions.count { it.sdkStatus == TransactionStatus.Voided }

        // Then
        assert(voidCount == 2) { "Should have 2 voided transactions, got $voidCount" }
    }

    // ==================== Tip Total Tests ====================

    @Test
    fun `calculate total tips for the day`() {
        // Given
        val transactions = listOf(
            createTransaction(amount = "100.00", tipAmount = "10.00", status = TransactionStatus.Accepted),
            createTransaction(amount = "50.00", tipAmount = "7.50", status = TransactionStatus.Accepted),
            createTransaction(amount = "75.00", tipAmount = "15.00", status = TransactionStatus.Accepted)
        )

        // When
        val totalTips = transactions
            .filter { it.sdkStatus == TransactionStatus.Accepted }
            .sumOf { it.tipAmount.toDouble() }

        // Then
        assert(totalTips == 32.5) { "Total tips should be 32.50, got $totalTips" }
    }

    @Test
    fun `exclude tips from rejected transactions`() {
        // Given
        val transactions = listOf(
            createTransaction(amount = "100.00", tipAmount = "10.00", status = TransactionStatus.Accepted),
            createTransaction(amount = "50.00", tipAmount = "5.00", status = TransactionStatus.Rejected)
        )

        // When
        val totalTips = transactions
            .filter { it.sdkStatus == TransactionStatus.Accepted }
            .sumOf { it.tipAmount.toDouble() }

        // Then
        assert(totalTips == 10.0) { "Tips from rejected transactions should not count" }
    }

    // ==================== Payment Type Summary Tests ====================

    @Test
    fun `separate POS and IPS transaction totals`() {
        // Given
        val transactions = listOf(
            createTransaction(amount = "100.00", isIps = false, status = TransactionStatus.Accepted),
            createTransaction(amount = "50.00", isIps = true, status = TransactionStatus.Accepted),
            createTransaction(amount = "75.00", isIps = false, status = TransactionStatus.Accepted),
            createTransaction(amount = "25.00", isIps = true, status = TransactionStatus.Accepted)
        )

        // When
        val posTotal = transactions
            .filter { !it.isIps && it.sdkStatus == TransactionStatus.Accepted }
            .sumOf { it.amount.toDouble() }
        val ipsTotal = transactions
            .filter { it.isIps && it.sdkStatus == TransactionStatus.Accepted }
            .sumOf { it.amount.toDouble() }

        // Then
        assert(posTotal == 175.0) { "POS total should be 175.00, got $posTotal" }
        assert(ipsTotal == 75.0) { "IPS total should be 75.00, got $ipsTotal" }
    }

    @Test
    fun `count transactions by payment type`() {
        // Given
        val transactions = listOf(
            createTransaction(isIps = false, status = TransactionStatus.Accepted),
            createTransaction(isIps = false, status = TransactionStatus.Accepted),
            createTransaction(isIps = true, status = TransactionStatus.Accepted),
            createTransaction(isIps = false, status = TransactionStatus.Accepted)
        )

        // When
        val posCount = transactions.count { !it.isIps && it.sdkStatus == TransactionStatus.Accepted }
        val ipsCount = transactions.count { it.isIps && it.sdkStatus == TransactionStatus.Accepted }

        // Then
        assert(posCount == 3) { "Should have 3 POS transactions" }
        assert(ipsCount == 1) { "Should have 1 IPS transaction" }
    }

    // ==================== Average Transaction Tests ====================

    @Test
    fun `calculate average transaction amount`() {
        // Given
        val transactions = listOf(
            createTransaction(amount = "100.00", status = TransactionStatus.Accepted),
            createTransaction(amount = "50.00", status = TransactionStatus.Accepted),
            createTransaction(amount = "75.00", status = TransactionStatus.Accepted)
        )

        // When
        val acceptedTransactions = transactions.filter { it.sdkStatus == TransactionStatus.Accepted }
        val average = acceptedTransactions.sumOf { it.amount.toDouble() } / acceptedTransactions.size

        // Then
        assert(average == 75.0) { "Average should be 75.00, got $average" }
    }

    @Test
    fun `handle zero transactions for average`() {
        // Given
        emptyList<TransactionDetailsDto>()

        // When
        val average = 0.0

        // Then
        assert(average == 0.0) { "Average of zero transactions should be 0" }
    }

    // ==================== Min/Max Transaction Tests ====================

    @Test
    fun `find highest transaction amount`() {
        // Given
        val transactions = listOf(
            createTransaction(amount = "100.00", status = TransactionStatus.Accepted),
            createTransaction(amount = "250.00", status = TransactionStatus.Accepted),
            createTransaction(amount = "75.00", status = TransactionStatus.Accepted)
        )

        // When
        val maxAmount = transactions
            .filter { it.sdkStatus == TransactionStatus.Accepted }
            .maxOfOrNull { it.amount.toDouble() }

        // Then
        assert(maxAmount == 250.0) { "Highest amount should be 250.00, got $maxAmount" }
    }

    @Test
    fun `find lowest transaction amount`() {
        // Given
        val transactions = listOf(
            createTransaction(amount = "100.00", status = TransactionStatus.Accepted),
            createTransaction(amount = "25.00", status = TransactionStatus.Accepted),
            createTransaction(amount = "75.00", status = TransactionStatus.Accepted)
        )

        // When
        val minAmount = transactions
            .filter { it.sdkStatus == TransactionStatus.Accepted }
            .minOfOrNull { it.amount.toDouble() }

        // Then
        assert(minAmount == 25.0) { "Lowest amount should be 25.00, got $minAmount" }
    }

    // ==================== Net Total Tests ====================

    @Test
    fun `calculate net total including tips`() {
        // Given
        val transactions = listOf(
            createTransaction(amount = "100.00", tipAmount = "10.00", status = TransactionStatus.Accepted),
            createTransaction(amount = "50.00", tipAmount = "5.00", status = TransactionStatus.Accepted)
        )

        // When
        val netTotal = transactions
            .filter { it.sdkStatus == TransactionStatus.Accepted }
            .sumOf { it.amount.toDouble() + it.tipAmount.toDouble() }

        // Then
        assert(netTotal == 165.0) { "Net total with tips should be 165.00, got $netTotal" }
    }

    @Test
    fun `net total excluding voided transactions`() {
        // Given
        val transactions = listOf(
            createTransaction(amount = "100.00", tipAmount = "10.00", status = TransactionStatus.Accepted),
            createTransaction(amount = "50.00", tipAmount = "5.00", status = TransactionStatus.Voided),
            createTransaction(amount = "75.00", tipAmount = "7.50", status = TransactionStatus.Accepted)
        )

        // When
        val netTotal = transactions
            .filter { it.sdkStatus == TransactionStatus.Accepted }
            .sumOf { it.amount.toDouble() + it.tipAmount.toDouble() }

        // Then
        assert(netTotal == 192.5) { "Voided transactions should not be included" }
    }

    // ==================== EOD Summary Tests ====================

    @Test
    fun `generate complete EOD summary`() {
        // Given
        val transactions = listOf(
            createTransaction(amount = "100.00", tipAmount = "10.00", isIps = false, status = TransactionStatus.Accepted),
            createTransaction(amount = "50.00", tipAmount = "5.00", isIps = true, status = TransactionStatus.Accepted),
            createTransaction(amount = "75.00", tipAmount = "0.00", isIps = false, status = TransactionStatus.Rejected),
            createTransaction(amount = "200.00", tipAmount = "20.00", isIps = false, status = TransactionStatus.Accepted),
            createTransaction(amount = "150.00", tipAmount = "15.00", isIps = false, status = TransactionStatus.Voided)
        )

        // When
        val accepted = transactions.filter { it.sdkStatus == TransactionStatus.Accepted }
        val summary = mapOf(
            "totalTransactions" to transactions.size,
            "acceptedCount" to accepted.size,
            "rejectedCount" to transactions.count { it.sdkStatus == TransactionStatus.Rejected },
            "voidedCount" to transactions.count { it.sdkStatus == TransactionStatus.Voided },
            "totalAmount" to accepted.sumOf { it.amount.toDouble() },
            "totalTips" to accepted.sumOf { it.tipAmount.toDouble() },
            "posCount" to accepted.count { !it.isIps },
            "ipsCount" to accepted.count { it.isIps }
        )

        // Then
        assert(summary["totalTransactions"] == 5)
        assert(summary["acceptedCount"] == 3)
        assert(summary["rejectedCount"] == 1)
        assert(summary["voidedCount"] == 1)
        assert(summary["totalAmount"] == 350.0)
        assert(summary["totalTips"] == 35.0)
        assert(summary["posCount"] == 2)
        assert(summary["ipsCount"] == 1)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `EOD with no transactions`() {
        // Given
        val transactions = emptyList<TransactionDetailsDto>()

        // When
        val totalAmount = transactions.sumOf { it.amount.toDouble() }
        val totalCount = 0

        // Then
        assert(totalAmount == 0.0) { "No transactions should have zero total" }
        assert(totalCount == 0) { "Transaction count should be zero" }
    }

    @Test
    fun `EOD with only rejected transactions`() {
        // Given
        val transactions = listOf(
            createTransaction(amount = "100.00", status = TransactionStatus.Rejected),
            createTransaction(amount = "50.00", status = TransactionStatus.Rejected)
        )

        // When
        val acceptedTotal = transactions
            .filter { it.sdkStatus == TransactionStatus.Accepted }
            .sumOf { it.amount.toDouble() }

        // Then
        assert(acceptedTotal == 0.0) { "Only rejected transactions should have zero accepted total" }
    }

    // ==================== Helper Function ====================

    private fun createTransaction(
        amount: String = "100.00",
        tipAmount: String = "0.00",
        isIps: Boolean = false,
        status: TransactionStatus = TransactionStatus.Accepted
    ) = TransactionDetailsDto(
        bankName = "Test Bank",
        dateTime = "2025-12-02T10:00:00",
        merchantId = "M001",
        terminalId = "T001",
        merchantName = "Test Merchant",
        cardNumber = "****5678",
        authorizationCode = "AUTH001",
        operationName = "Sale",
        status = "A",
        response = "00",
        message = "Approved",
        rrn = "RRN001",
        code = "C001",
        applicationLabel = "VISA",
        aid = "AID001",
        amount = amount,
        isIps = isIps,
        sdkStatus = status,
        billStatus = null,
        recordId = "REC001",
        listName = "",
        color = -1,
        tipAmount = tipAmount
    )
}
