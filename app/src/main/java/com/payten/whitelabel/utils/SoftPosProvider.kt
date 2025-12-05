package com.payten.whitelabel.utils

import android.app.Activity
import com.payten.whitelabel.activities.HeadlessPaymentActivity
import com.simcore.api.interfaces.PaymentData
import com.icmp10.cvms.api.CVMSListener
import com.simant.MainApplication
import com.simant.sample.SimantApplication
import com.simcore.api.SoftPOSSDK
import com.simant.softpos.api.CVMTransactionApi
import com.simant.softpos.api.TransactionApi
import com.simcore.api.interfaces.TransactionResultListener

/**
 * SoftPosProvider.kt
 *
 * Defines the contract for interacting with the underlying SoftPOS SDK and Hardware.
 * This interface acts as a "Bridge" or "Facade" to decouple the UI/Activity logic
 * from the concrete, obfuscated SDK implementation.
 *
 * Purpose:
 * 1. To encapsulate all static calls to `SoftPOSSDK` and `MainApplication`.
 * 2. To allow Unit Tests to inject a Mock provider, preventing crashes caused by
 * loading obfuscated SDK classes in a test environment.
 */
interface SoftPosProvider {
    /**
     * Initializes the SDK for a new transaction.
     *
     * @param amount The transaction amount as a string.
     * @param tip The tip amount.
     * @param additionalData JSON string containing merchant specific data.
     * @return true if initialization was successful and SDK status is OK, false otherwise.
     */
    fun initializeSdk(amount: String, tip: String, additionalData: String): Boolean

    /**
     * Checks if the internal NFC reader is enabled.
     *
     * @return true if NFC is enabled, false if disabled or if an error occurred.
     */
    fun checkNfcEnabled(): Boolean

    /**
     * Registers the necessary SDK listeners (MTMS, CVMS, Transaction, Loyalty)
     * to the provided activity.
     *
     * @param activity The [HeadlessPaymentActivity] that implements the listener interfaces.
     */
    fun registerListeners(activity: HeadlessPaymentActivity)

    /**
     * Unregisters or re-registers listeners.
     * Typically called during onPause or cleanup.
     *
     * @param activity The activity to detach/reattach.
     */
    fun unregisterListeners(activity: HeadlessPaymentActivity)

    /**
     * Initiates the actual payment transaction using the PaymentData configured during initialization.
     *
     * @param activity The context required by the SDK to start the transaction.
     */
    fun startTransaction(activity: Activity)

    /**
     * Cancels any currently active transaction in the SDK.
     * Safe to call even if no transaction is running (exceptions are caught internally).
     */
    fun cancelTransaction()

    /**
     * Sets the SDK's auto-mode configuration.
     *
     * @param enabled True to enable auto-mode, false to disable.
     */
    fun setAutoMode(enabled: Boolean)

    /**
     * Sets the cancellation flag in the SDK.
     * Used during PIN timeouts or manual cancellations.
     *
     * @param cancelled True if the transaction is cancelled.
     */
    fun setCancelled(cancelled: Boolean)

    /**
     * Resets the internal reader outcome state.
     * Essential cleanup step after a transaction ends or fails.
     */
    fun resetReaderOutcome()

    /**
     * Initiates the PIN entry flow.
     *
     * @param listener The [CVMSListener] that handles PIN entry events.
     * @param transactionType The integer identifier for the transaction type.
     */
    fun startPinEntry(listener: CVMSListener, transactionType: Int)
}

/**
 * RealSoftPosProvider
 *
 * The concrete implementation of [SoftPosProvider] for Production use.
 *
 * WARNING: This class contains references to obfuscated SDK classes (`com.simcore...`)
 * and static singletons (`MainApplication`). Attempting to load this class inside
 * a standard Robolectric/JUnit test will cause `NoClassDefFoundError` or crashes.
 *
 * This class should only be instantiated by Hilt in the actual Android App,
 * or by the Activity at runtime.
 */
class RealSoftPosProvider : SoftPosProvider {

    override fun initializeSdk(amount: String, tip: String, additionalData: String): Boolean {
        return try {
            // Cancel previous
            try { SoftPOSSDK.getInstance().transactionInterface.cancelTransaction() } catch (_: Exception) {}

            // Set Transaction Type
            MainApplication.getInstance().paymentData.transactionType = PaymentData.TransactionType.GOODS.internalType

            // Check SDK Status
            if (SimantApplication.getSDKStatus() != 0) return false

            // Set Amount
            MainApplication.getInstance().setPaymentAmount(AmountUtil.setAmount(amount))

            // Set Additional Data
            MainApplication.getInstance().paymentData.merchantAdditionalData =
                additionalData.ifEmpty { "None" }

            true
        } catch (_: Exception) {
            false
        }
    }

    override fun checkNfcEnabled(): Boolean {
        return try {
            SoftPOSSDK.getCardCommunicationProvider().isEnabled
        } catch (_: Exception) {
            false
        }
    }

    override fun registerListeners(activity: HeadlessPaymentActivity) {
        MainApplication.getInstance().mtmsListener.setListener(activity)
        MainApplication.getInstance().cvmsListener.setListener(activity)
        MainApplication.getInstance().transactionOutcomeObserver.transactionResultListener = activity
        MainApplication.getInstance().loyaltyObserver.loyaltyActionListener = activity
        MainApplication.getInstance().configurationInterface.setDisplayInterface(activity)

        if (MainApplication.getInstance().cardCommunicationProvider != null) {
            MainApplication.getInstance().cardCommunicationProvider.connectReader(activity)
        }
    }

    override fun unregisterListeners(activity: HeadlessPaymentActivity) {
        // Re-register to activity (standard pattern in this app seems to be just setting them again)
        MainApplication.getInstance().mtmsListener.setListener(activity)
        MainApplication.getInstance().cvmsListener.setListener(activity)
        MainApplication.getInstance().transactionOutcomeObserver.transactionResultListener = activity
        MainApplication.getInstance().loyaltyObserver.loyaltyActionListener = activity
        MainApplication.getInstance().configurationInterface.setDisplayInterface(activity)
    }

    override fun startTransaction(activity: Activity) {
        val paymentData = MainApplication.getInstance().paymentData
        TransactionApi.doTransaction(activity as TransactionResultListener?, paymentData)
    }

    override fun cancelTransaction() {
        try { SoftPOSSDK.getInstance().transactionInterface.cancelTransaction() } catch (_: Exception) {}
    }

    override fun setAutoMode(enabled: Boolean) {
        try { SoftPOSSDK.setAutoMode(enabled) } catch (_: Exception) {}
    }

    override fun setCancelled(cancelled: Boolean) {
        try { SoftPOSSDK.setCancelled(cancelled) } catch (_: Exception) {}
    }

    override fun resetReaderOutcome() {
        try { SoftPOSSDK.resetReaderOutcome() } catch (_: Exception) {}
    }

    override fun startPinEntry(listener: CVMSListener, transactionType: Int) {
        CVMTransactionApi.doTransactionPCPOC(listener as TransactionResultListener?, transactionType.toString())
    }
}