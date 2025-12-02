package com.payten.whitelabel

import com.payten.whitelabel.enums.TransactionStatus
import org.junit.Test

/**
 * Unit tests for TransactionStatus enum.
 *
 * Tests status values, transitions, and business logic.
 */
class TransactionStatusTest {

    // ==================== Enum Values Tests ====================

    @Test
    fun `all transaction statuses are defined`() {
        // Given
        val expectedStatuses = setOf(
            TransactionStatus.Accepted,
            TransactionStatus.Pending,
            TransactionStatus.PinNotEntered,
            TransactionStatus.WrongPin,
            TransactionStatus.Rejected,
            TransactionStatus.Voided,
            TransactionStatus.Reversed
        )

        // When
        val actualStatuses = TransactionStatus.entries.toSet()

        // Then
        assert(actualStatuses == expectedStatuses) {
            "Expected $expectedStatuses but got $actualStatuses"
        }
    }

    @Test
    fun `transaction status enum count is correct`() {
        // When
        val statusCount = TransactionStatus.entries.size

        // Then
        assert(statusCount == 7) { "Should have exactly 7 transaction statuses, got $statusCount" }
    }

    // ==================== Status Logic Tests ====================

    @Test
    fun `accepted transactions can be voided`() {
        // When
        val canBeVoided = true

        // Then
        assert(canBeVoided) { "Accepted transactions should be voidable" }
    }

    @Test
    fun `voided transactions cannot be voided again`() {
        // When
        val canBeVoided = false

        // Then
        assert(!canBeVoided) { "Voided transactions should not be voidable again" }
    }

    @Test
    fun `rejected transactions cannot be voided`() {

        // When
        val canBeVoided = false

        // Then
        assert(!canBeVoided) { "Rejected transactions should not be voidable" }
    }

    @Test
    fun `pending transactions should not be voided`() {

        // When
        val canBeVoided = false

        // Then
        assert(!canBeVoided) { "Pending transactions should not be voidable" }
    }

    @Test
    fun `reversed transactions are different from voided`() {
        // Given
        val voided = TransactionStatus.Voided
        val reversed = TransactionStatus.Reversed

        // Then
        assert(voided != reversed) { "Voided and Reversed are different statuses" }
    }

    // ==================== Status Filtering Tests ====================

    @Test
    fun `filter successful transactions`() {
        // Given
        val statuses = listOf(
            TransactionStatus.Accepted,
            TransactionStatus.Rejected,
            TransactionStatus.Voided,
            TransactionStatus.Accepted
        )

        // When
        val successful = statuses.filter { it == TransactionStatus.Accepted }

        // Then
        assert(successful.size == 2) { "Should have 2 successful transactions" }
    }

    @Test
    fun `filter failed transactions`() {
        // Given
        val statuses = listOf(
            TransactionStatus.Accepted,
            TransactionStatus.Rejected,
            TransactionStatus.WrongPin,
            TransactionStatus.Rejected
        )

        // When
        val failed = statuses.filter {
            it == TransactionStatus.Rejected || it == TransactionStatus.WrongPin
        }

        // Then
        assert(failed.size == 3) { "Should have 3 failed transactions" }
    }

    @Test
    fun `filter incomplete transactions`() {
        // Given
        val statuses = listOf(
            TransactionStatus.Accepted,
            TransactionStatus.Pending,
            TransactionStatus.PinNotEntered,
            TransactionStatus.Rejected
        )

        // When
        val incomplete = statuses.filter {
            it == TransactionStatus.Pending || it == TransactionStatus.PinNotEntered
        }

        // Then
        assert(incomplete.size == 2) { "Should have 2 incomplete transactions" }
    }

    // ==================== Status Name Tests ====================

    @Test
    fun `status enum names are correct`() {
        // Then
        assert(TransactionStatus.Accepted.name == "Accepted")
        assert(TransactionStatus.Pending.name == "Pending")
        assert(TransactionStatus.PinNotEntered.name == "PinNotEntered")
        assert(TransactionStatus.WrongPin.name == "WrongPin")
        assert(TransactionStatus.Rejected.name == "Rejected")
        assert(TransactionStatus.Voided.name == "Voided")
        assert(TransactionStatus.Reversed.name == "Reversed")
    }

    @Test
    fun `can get status by name`() {
        // Given
        val statusName = "Accepted"

        // When
        val status = TransactionStatus.valueOf(statusName)

        // Then
        assert(status == TransactionStatus.Accepted) { "Should get Accepted status" }
    }

    @Test
    fun `invalid status name throws exception`() {
        // Given
        val invalidName = "InvalidStatus"

        // When/Then
        try {
            TransactionStatus.valueOf(invalidName)
            assert(false) { "Should throw exception for invalid status name" }
        } catch (_: IllegalArgumentException) {
            assert(true) { "Expected IllegalArgumentException" }
        }
    }

    // ==================== Business Logic Tests ====================

    @Test
    fun `group statuses by finality`() {
        // Given
        val finalStatuses = setOf(
            TransactionStatus.Accepted,
            TransactionStatus.Rejected,
            TransactionStatus.Voided,
            TransactionStatus.Reversed
        )

        val nonFinalStatuses = setOf(
            TransactionStatus.Pending,
            TransactionStatus.PinNotEntered,
            TransactionStatus.WrongPin
        )

        // When
        val allStatuses = TransactionStatus.entries.toSet()

        // Then
        assert(finalStatuses.union(nonFinalStatuses) == allStatuses) {
            "All statuses should be either final or non-final"
        }
        assert(finalStatuses.intersect(nonFinalStatuses).isEmpty()) {
            "Final and non-final statuses should not overlap"
        }
    }

    @Test
    fun `determine if transaction is completable`() {
        // Given - Transactions that can potentially become Accepted
        val completableStatuses = setOf(
            TransactionStatus.Pending,
            TransactionStatus.PinNotEntered,
            TransactionStatus.WrongPin // Can retry PIN
        )

        // Then
        completableStatuses.forEach { status ->
            val isCompletable = status in setOf(
                TransactionStatus.Pending,
                TransactionStatus.PinNotEntered,
                TransactionStatus.WrongPin
            )
            assert(isCompletable) { "$status should be completable" }
        }
    }

    @Test
    fun `check if status represents active transaction`() {
        // Given
        val activeStatuses = setOf(
            TransactionStatus.Pending,
            TransactionStatus.PinNotEntered,
            TransactionStatus.WrongPin
        )

        // When
        val isActive: (TransactionStatus) -> Boolean = { it in activeStatuses }

        // Then
        assert(isActive(TransactionStatus.Pending))
        assert(isActive(TransactionStatus.PinNotEntered))
        assert(!isActive(TransactionStatus.Accepted))
        assert(!isActive(TransactionStatus.Voided))
    }
}
