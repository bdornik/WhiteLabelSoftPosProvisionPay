package com.payten.whitelabel

import com.payten.whitelabel.dto.TransactionDetailsDto
import com.payten.whitelabel.enums.TransactionStatus
import org.junit.Test

/**
 * Simple test to verify TransactionDetailsDto creation works correctly.
 */
class SimpleTransactionTest {

    @Test
    fun `can create transaction DTO`() {
        // Given
        val transaction = TransactionDetailsDto(
            bankName = "Test Bank",
            dateTime = "2025-12-02T10:00:00",
            merchantId = "M001",
            terminalId = "T001",
            merchantName = "Test Merchant",
            cardNumber = "1234****5678",
            authorizationCode = "AUTH001",
            operationName = "Sale",
            status = "A",
            response = "00",
            message = "Approved",
            rrn = "RRN001",
            code = "CODE001",
            applicationLabel = "VISA",
            aid = "AID001",
            amount = "100.00",
            isIps = false,
            sdkStatus = TransactionStatus.Accepted,
            billStatus = null,
            recordId = "REC001",
            listName = "",
            color = -1,
            tipAmount = "0.00"
        )

        // Then
        assert(transaction.isIps == false)
        assert(transaction.sdkStatus == TransactionStatus.Accepted)
        assert(transaction.amount == "100.00")
    }

    @Test
    fun `can filter transactions by isIps`() {
        // Given
        val transactions = listOf(
            TransactionDetailsDto(
                bankName = "Bank", dateTime = "2025-12-02T10:00:00",
                merchantId = "M", terminalId = "T", merchantName = "Merchant",
                cardNumber = "****", authorizationCode = "AUTH", operationName = "Sale",
                status = "A", response = "00", message = "OK", rrn = "RRN", code = "C",
                applicationLabel = "VISA", aid = "AID", amount = "100", isIps = false,
                sdkStatus = TransactionStatus.Accepted, billStatus = null,
                recordId = "1", listName = "", color = -1, tipAmount = "0"
            ),
            TransactionDetailsDto(
                bankName = "Bank", dateTime = "2025-12-02T10:00:00",
                merchantId = "M", terminalId = "T", merchantName = "Merchant",
                cardNumber = "****", authorizationCode = "AUTH", operationName = "Sale",
                status = "A", response = "00", message = "OK", rrn = "RRN", code = "C",
                applicationLabel = "VISA", aid = "AID", amount = "100", isIps = true,
                sdkStatus = TransactionStatus.Accepted, billStatus = null,
                recordId = "2", listName = "", color = -1, tipAmount = "0"
            )
        )

        // When
        val posOnly = transactions.filter { !it.isIps }
        val ipsOnly = transactions.filter { it.isIps }

        // Then
        assert(posOnly.size == 1)
        assert(ipsOnly.size == 1)
        assert(posOnly[0].recordId == "1")
        assert(ipsOnly[0].recordId == "2")
    }
}
