package com.payten.whitelabel.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.payten.whitelabel.dto.TransactionDetailsDto
import com.payten.whitelabel.enums.TransactionStatus
import com.simant.softpos.api.TransactionApi
import com.simcore.api.interfaces.TransactionResultListener
import dagger.hilt.android.lifecycle.HiltViewModel
import mu.KotlinLogging
import javax.inject.Inject

sealed class VoidTransactionState {
    object Idle : VoidTransactionState()
    object Processing : VoidTransactionState()
    data class Success(val transactionData: TransactionDetailsDto) : VoidTransactionState()
    data class Failed(val message: String) : VoidTransactionState()
    object Cancelled : VoidTransactionState()
}

@HiltViewModel
class VoidTransactionViewModel @Inject constructor(
) : ViewModel(), TransactionResultListener {

    private val logger = KotlinLogging.logger {}

    private val _voidState = MutableLiveData<VoidTransactionState>(VoidTransactionState.Idle)
    val voidState: LiveData<VoidTransactionState> = _voidState

    private var currentTransactionData: TransactionDetailsDto? = null

    fun startVoidTransaction(transactionData: TransactionDetailsDto) {
        logger.info { "Starting void transaction for recordId: ${transactionData.recordId}" }

        currentTransactionData = transactionData
        _voidState.value = VoidTransactionState.Processing

        try {
            TransactionApi.doTransactionVoid(this, transactionData.recordId)
        } catch (e: Exception) {
            logger.error { "Failed to start void transaction: ${e.message}" }
            _voidState.value = VoidTransactionState.Failed("Failed to start transaction: ${e.message}")
        }
    }

    fun resetState() {
        _voidState.value = VoidTransactionState.Idle
        currentTransactionData = null
    }

    // TransactionResultListener implementations
    override fun onTransactionProcessing() {
        logger.info { "Void transaction processing" }
        _voidState.postValue(VoidTransactionState.Processing)
    }

    override fun onTransactionSuccessful() {
        logger.info { "Void transaction successful" }

        // Build successful void transaction data
        val voidedTransaction = currentTransactionData?.copy(
            operationName = "Void",
            response = "00",
            status = "v",
            message = "Voided",
            sdkStatus = TransactionStatus.Voided
        )

        if (voidedTransaction != null) {
            _voidState.postValue(VoidTransactionState.Success(voidedTransaction))
        } else {
            _voidState.postValue(VoidTransactionState.Failed("Failed to create void transaction data"))
        }
    }

    override fun onTransactionDeclined() {
        logger.info { "Void transaction declined" }
        _voidState.postValue(VoidTransactionState.Failed("Transaction declined"))
    }

    override fun onTransactionEnded(message: String?) {
        logger.info { "Void transaction ended: $message" }

        val voidedTransaction = currentTransactionData?.copy(
            operationName = "Void",
            response = "06",
            status = "f",
            message = message ?: "Transaction ended",
            sdkStatus = TransactionStatus.Rejected
        )

        if (voidedTransaction != null) {
            _voidState.postValue(VoidTransactionState.Failed(message ?: "Transaction ended"))
        }
    }

    override fun onTransactionCancelled() {
        logger.info { "Void transaction cancelled" }
        _voidState.postValue(VoidTransactionState.Cancelled)
    }

    override fun onTransactionNotStarted(message: String?) {
        logger.error { "Void transaction not started: $message" }
        _voidState.postValue(VoidTransactionState.Failed(message ?: "Transaction not started"))
    }

    override fun onTransactionOnline() {
        logger.info { "Void transaction online" }
        _voidState.postValue(VoidTransactionState.Processing)
    }

    override fun onOnlineRequest(): com.sacbpp.core.bytes.ByteArray? {
        logger.info { "Void onOnlineRequest" }
        return null
    }

    override fun onBatchApproval() {
        logger.info { "Void batch approval" }
    }

    override fun onBatchDeclined() {
        logger.info { "Void batch declined" }
        _voidState.postValue(VoidTransactionState.Failed("Batch declined"))
    }

    override fun onTransactionIdle() {
        logger.info { "Transaction idle" }
    }

    override fun onTransactionReadyToRead() {
        logger.info { "Transaction ready to read" }
    }
}