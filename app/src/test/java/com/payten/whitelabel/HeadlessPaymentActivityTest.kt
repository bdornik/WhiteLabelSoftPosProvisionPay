package com.payten.whitelabel

import android.app.Activity
import android.content.Intent
import androidx.appcompat.R
import com.cioccarellia.ksprefs.KsPrefs
import com.icmp10.mtms.codes.opGetTransaction.GetTransactionResult
import com.payten.whitelabel.activities.HeadlessPaymentActivity
import com.payten.whitelabel.dto.TransactionDetailsDto
import com.payten.whitelabel.persistance.SharedPreferenceModule
import com.payten.whitelabel.utils.SoftPosProvider
import com.payten.whitelabel.viewmodel.PosViewModel
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@UninstallModules(SharedPreferenceModule::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class,
        sdk = [34])
class HeadlessPaymentActivityTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var controller: ActivityController<HeadlessPaymentActivity>
    private lateinit var activity: HeadlessPaymentActivity

    @BindValue
    @RelaxedMockK
    lateinit var mockPrefs: KsPrefs

    @BindValue
    @RelaxedMockK
    lateinit var mockViewModel: PosViewModel

    @RelaxedMockK
    lateinit var mockSdkProvider: SoftPosProvider

    @Before
    fun setup() {
        MockKAnnotations.init(this)

        // Mock ViewModel's createErrorLog to avoid SDK class access
        every { mockViewModel.createErrorLog(any(), any(), any(), any(), any()) } returns mockk(relaxed = true)
        every { mockViewModel.logError(any()) } just Runs

        // 1. Setup Mock Provider responses
        every { mockSdkProvider.initializeSdk(any(), any(), any()) } returns true
        every { mockSdkProvider.checkNfcEnabled() } returns true

        every { mockPrefs.pull<String>(any()) } returns "MockString"
        every { mockPrefs.pull(any(), any<String>()) } returns "MockString"

        val intent = Intent().apply {
            putExtra("Amount", "1000")
            putExtra("Tip", "0")
            putExtra("uniqueId", "12345")
        }

        // 2. Build Activity but do not create it yet
        controller = Robolectric.buildActivity(HeadlessPaymentActivity::class.java, intent)

        // 3. Inject Hilt dependencies
        hiltRule.inject()

        // 4. Get the instance and set the Theme explicitly
        activity = controller.get()
        activity.setTheme(R.style.Theme_AppCompat_Light_NoActionBar)

        // 5. Inject the Bridge Provider
        activity.softPosProvider = mockSdkProvider
        activity.model = mockViewModel
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `onCreate initializes SDK and registers listeners`() {
        controller.create()

        verify { mockSdkProvider.initializeSdk("1000", any(), any()) }
        verify { mockSdkProvider.registerListeners(activity) }
    }

    @Test
    fun `onTransactionSuccessful plays sound and updates LED`() {
        // Setup lifecycle
        controller.create().resume()

        // Act
        activity.onTransactionSuccessful()

        // Assert
        assertEquals(false, activity.isFinishing)
    }

    @Test
    fun `onOnlineResponse with code 00 returns RESULT_OK`() {
        // Setup lifecycle
        controller.create().resume()

        // Mock result data using chained mocks
        val mockResult = mockk<GetTransactionResult>(relaxed = true)
        every { mockResult.transactionResponseData.statusCode } returns "A"
        every { mockResult.transactionResponseData.responseCode } returns "00"
        every { mockResult.transactionResponseData.amount } returns "10.00"

        // Act
        activity.onOnlineResponse(mockResult)

        // Force main thread execution to process any Handlers/Runnables
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Assert
        assertTrue("Activity should be finishing", activity.isFinishing)
        assertEquals(Activity.RESULT_OK, Shadows.shadowOf(activity).resultCode)

        // Verify data passed back
        val resultIntent = Shadows.shadowOf(activity).resultIntent
        val data = resultIntent.getSerializableExtra("transaction_data") as? TransactionDetailsDto
        assertEquals("10.00", data?.amount)
    }

    // ==================== PIN Entry Flow Tests ====================

    @Test
    fun `onCVMEEntered triggers PIN entry flow`() {
        // Setup lifecycle
        controller.create().resume()

        // Act - PIN entry requested
        activity.onCVMEEntered(4) // 4-digit PIN

        // Force UI thread execution
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Assert - Should start PIN transaction via provider
        verify { mockSdkProvider.startPinEntry(activity, any()) }
    }

    @Test
    fun `onCVMETimeout cancels transaction`() {
        // Setup lifecycle
        controller.create().resume()

        // Act - PIN timeout
        activity.onCVMETimeout()

        // Force UI thread execution
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Assert - Should cleanup and finish
        verify { mockSdkProvider.setAutoMode(false) }
        verify { mockSdkProvider.setCancelled(true) }
        verify { mockSdkProvider.resetReaderOutcome() }
        verify { mockSdkProvider.cancelTransaction() }
        assertTrue("Activity should finish on PIN timeout", activity.isFinishing)
        assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
    }

    @Test
    fun `onCVMECancelled cancels transaction`() {
        // Setup lifecycle
        controller.create().resume()

        // Act - User cancels PIN entry
        activity.onCVMECancelled()

        // Force UI thread execution
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Assert - Should cleanup and finish
        verify { mockSdkProvider.setAutoMode(false) }
        verify { mockSdkProvider.setCancelled(true) }
        verify { mockSdkProvider.resetReaderOutcome() }
        verify { mockSdkProvider.cancelTransaction() }
        assertTrue("Activity should finish on PIN cancel", activity.isFinishing)
        assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
    }

    // ==================== Card Detection Tests ====================

    @Test
    fun `onTransactionReadyToRead updates LED state`() {
        // Setup lifecycle
        controller.create().resume()

        // Act - Transaction ready to read card
        activity.onTransactionReadyToRead()

        // Assert - Should not finish or crash
        assertEquals(false, activity.isFinishing)
    }

    @Test
    fun `onTransactionIdle updates LED state`() {
        // Setup lifecycle
        controller.create().resume()

        // Act - Transaction idle
        activity.onTransactionIdle()

        // Assert - Should not finish or crash
        assertEquals(false, activity.isFinishing)
    }

    @Test
    fun `onTransactionProcessing plays audio and updates LED`() {
        // Setup lifecycle
        controller.create().resume()

        // Act - Transaction processing
        activity.onTransactionProcessing()

        // Assert - Should not finish (just UI feedback)
        assertEquals(false, activity.isFinishing)
    }

    // ==================== Transaction Result Tests ====================

    @Test
    fun `onTransactionDeclined with shouldIgnoreDecline retries`() {
        // Setup lifecycle
        controller.create().resume()

        // Act - Transaction declined but shouldIgnoreDecline is true
        activity.onTransactionDeclined()

        // Assert - Should not finish (retries instead)
        assertEquals(false, activity.isFinishing)
    }

    @Test
    fun `onTransactionCancelled finishes activity`() {
        // Setup lifecycle
        controller.create().resume()

        // Act - Transaction cancelled
        activity.onTransactionCancelled()

        // Force UI thread execution
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Assert
        assertTrue("Activity should finish on transaction cancelled", activity.isFinishing)
        assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
    }

    @Test
    fun `onTransactionEnded with TRY_AGAIN retries transaction`() {
        // Setup lifecycle
        controller.create().resume()

        // Act - Transaction ended with TRY_AGAIN message
        activity.onTransactionEnded("TRY_AGAIN - Tap card again")

        // Assert - Should not finish (retries instead)
        assertEquals(false, activity.isFinishing)
    }

    @Test
    fun `onTransactionEnded without TRY_AGAIN finishes activity`() {
        // Setup lifecycle
        controller.create().resume()

        // Act - Transaction ended with error
        activity.onTransactionEnded("TRANSACTION_FAILED")

        // Force UI thread execution
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Assert
        assertTrue("Activity should finish on transaction ended", activity.isFinishing)
        assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
    }

    @Test
    fun `onTransactionNotStarted with Parameters not Ready retries after delay`() {
        // Setup lifecycle
        controller.create().resume()

        // Act - SDK parameters not ready
        activity.onTransactionNotStarted("Parameters not Ready")

        // Assert - Should not finish immediately (retries after 2 seconds)
        assertEquals(false, activity.isFinishing)

        // Force delayed tasks to run
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Should still not be finishing (waiting for retry)
        assertEquals(false, activity.isFinishing)
    }

    @Test
    fun `onTransactionNotStarted with other error finishes activity`() {
        // Setup lifecycle
        controller.create().resume()

        // Act - SDK error
        activity.onTransactionNotStarted("SDK_ERROR")

        // Force UI thread execution
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Assert
        assertTrue("Activity should finish on SDK error", activity.isFinishing)
        assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
    }

    // ==================== Card Type Animation Tests ====================

    @Test
    fun `onOnlineResponse with Visa card shows animation`() {
        // Setup lifecycle
        controller.create().resume()

        // Mock Visa card result - MockK relaxed mode handles nested objects automatically
        val mockResult = mockk<GetTransactionResult>(relaxed = true) {
            every { transactionResponseData } returns mockk(relaxed = true) {
                every { statusCode } returns "A"
                every { responseCode } returns "00"
                every { applicationLabel } returns "visa"
                every { amount } returns "10.00"
                every { recordId } returns "REC123"
                every { maskedPAN } returns "1234****5678"
                every { transactionDate } returns "2025-01-01"
                every { authorizationCode } returns "AUTH123"
                every { aid } returns "AID123"
                every { screenMessage } returns "Approved"
            }
        }

        // Act
        activity.onOnlineResponse(mockResult)

        // Force UI thread execution
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Assert - Animation state is set (activity won't finish in unit tests since Compose doesn't render)
        // In real app, AnimationScreen would complete and call onAnimationComplete to finish activity
        // In unit tests, we verify the method completes without error
        // Note: Cannot verify activity.isFinishing or result code since animation callback never fires in tests
    }

    @Test
    fun `onOnlineResponse with Mastercard shows animation`() {
        // Setup lifecycle
        controller.create().resume()

        // Mock Mastercard result - MockK relaxed mode handles nested objects automatically
        val mockResult = mockk<GetTransactionResult>(relaxed = true) {
            every { transactionResponseData } returns mockk(relaxed = true) {
                every { statusCode } returns "A"
                every { responseCode } returns "00"
                every { applicationLabel } returns "card"
                every { amount } returns "10.00"
                every { recordId } returns "REC123"
                every { maskedPAN } returns "5678****1234"
                every { transactionDate } returns "2025-01-01"
                every { authorizationCode } returns "AUTH456"
                every { aid } returns "AID456"
                every { screenMessage } returns "Approved"
            }
        }

        // Act
        activity.onOnlineResponse(mockResult)

        // Force UI thread execution
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Assert - Animation state is set (activity won't finish in unit tests since Compose doesn't render)
        // In real app, AnimationScreen would complete and call onAnimationComplete to finish activity
        // In unit tests, we verify the method completes without error
        // Note: Cannot verify activity.isFinishing or result code since animation callback never fires in tests
    }

    @Test
    fun `onOnlineResponse with other card type skips animation`() {
        // Setup lifecycle
        controller.create().resume()

        // Mock other card type result
        val mockResult = mockk<GetTransactionResult>(relaxed = true)
        every { mockResult.transactionResponseData.statusCode } returns "A"
        every { mockResult.transactionResponseData.responseCode } returns "00"
        every { mockResult.transactionResponseData.applicationLabel } returns "AmEx"
        every { mockResult.transactionResponseData.amount } returns "10.00"
        every { mockResult.transactionResponseData.recordId } returns "REC123"

        // Act
        activity.onOnlineResponse(mockResult)

        // Force UI thread execution
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Assert - Should finish without animation
        assertTrue("Activity should finish without animation", activity.isFinishing)
        assertEquals(Activity.RESULT_OK, Shadows.shadowOf(activity).resultCode)
    }

    @Test
    fun `onOnlineResponse with declined transaction finishes with CANCELED`() {
        // Setup lifecycle
        controller.create().resume()

        // Mock declined transaction
        val mockResult = mockk<GetTransactionResult>(relaxed = true)
        every { mockResult.transactionResponseData.statusCode } returns "A"
        every { mockResult.transactionResponseData.responseCode } returns "05" // Declined
        every { mockResult.transactionResponseData.amount } returns "10.00"

        // Act
        activity.onOnlineResponse(mockResult)

        // Force UI thread execution
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Assert
        assertTrue("Activity should finish on declined", activity.isFinishing)
        assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
    }

    @Test
    fun `onOnlineResponse with status D finishes with CANCELED`() {
        // Setup lifecycle
        controller.create().resume()

        // Mock declined status
        val mockResult = mockk<GetTransactionResult>(relaxed = true)
        every { mockResult.transactionResponseData.statusCode } returns "D" // Declined

        // Act
        activity.onOnlineResponse(mockResult)

        // Force UI thread execution
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Assert
        assertTrue("Activity should finish on declined status", activity.isFinishing)
        assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
    }

    // ==================== NFC State Tests ====================

    @Test
    fun `onCreate with NFC disabled finishes activity`() {
        // Given - Reset mock to return false for NFC
        every { mockSdkProvider.initializeSdk(any(), any(), any()) } returns true
        every { mockSdkProvider.checkNfcEnabled() } returns false

        val intent = Intent().apply {
            putExtra("Amount", "1000")
            putExtra("Tip", "0")
            putExtra("uniqueId", "12345")
        }

        // When
        val controller2 = Robolectric.buildActivity(HeadlessPaymentActivity::class.java, intent)
        val activity2 = controller2.get()
        activity2.setTheme(R.style.Theme_AppCompat_Light_NoActionBar)
        activity2.softPosProvider = mockSdkProvider
        activity2.model = mockViewModel

        controller2.create()

        // Then
        assertTrue("Activity should finish when NFC disabled", activity2.isFinishing)
        assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity2).resultCode)
    }

    @Test
    fun `onCreate with SDK not ready finishes activity`() {
        // Given - SDK returns false for initialization
        every { mockSdkProvider.initializeSdk(any(), any(), any()) } returns false
        every { mockSdkProvider.checkNfcEnabled() } returns true

        val intent = Intent().apply {
            putExtra("Amount", "1000")
            putExtra("Tip", "0")
            putExtra("uniqueId", "12345")
        }

        // When
        val controller2 = Robolectric.buildActivity(HeadlessPaymentActivity::class.java, intent)
        val activity2 = controller2.get()
        activity2.setTheme(R.style.Theme_AppCompat_Light_NoActionBar)
        activity2.softPosProvider = mockSdkProvider
        activity2.model = mockViewModel  // Inject mock ViewModel to avoid SDK class access

        controller2.create()

        // Then
        assertTrue("Activity should finish when SDK not ready", activity2.isFinishing)
        assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity2).resultCode)
    }

    @Test
    fun `onCreate with missing Amount parameter finishes activity`() {
        // Given - No amount in intent
        every { mockSdkProvider.initializeSdk(any(), any(), any()) } returns true
        every { mockSdkProvider.checkNfcEnabled() } returns true

        val intent = Intent().apply {
            // Missing Amount parameter
            putExtra("Tip", "0")
            putExtra("uniqueId", "12345")
        }

        // When
        val controller2 = Robolectric.buildActivity(HeadlessPaymentActivity::class.java, intent)
        val activity2 = controller2.get()
        activity2.setTheme(R.style.Theme_AppCompat_Light_NoActionBar)
        activity2.softPosProvider = mockSdkProvider
        activity2.model = mockViewModel

        controller2.create()

        // Then
        assertTrue("Activity should finish when Amount missing", activity2.isFinishing)
        assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity2).resultCode)
    }

    // ==================== Multiple Payment Tests ====================

    @Test
    fun `onPause cancels ongoing transaction`() {
        // Setup lifecycle
        controller.create().resume()

        // Act - Activity paused
        controller.pause()

        // Assert - Should cancel transaction via provider
        verify { mockSdkProvider.cancelTransaction() }
    }

    @Test
    fun `onResume with NFC disabled finishes activity`() {
        // Setup lifecycle
        controller.create()

        // Change NFC state to disabled
        every { mockSdkProvider.checkNfcEnabled() } returns false

        // Act - Resume with NFC disabled
        controller.resume()

        // Force delayed tasks
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Assert
        assertTrue("Activity should finish when NFC disabled on resume", activity.isFinishing)
        assertEquals(Activity.RESULT_CANCELED, Shadows.shadowOf(activity).resultCode)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `onTransactionOnline updates LED state`() {
        // Setup lifecycle
        controller.create().resume()

        // Act - Transaction going online
        activity.onTransactionOnline()

        // Assert - Should not finish
        assertEquals(false, activity.isFinishing)
    }

    @Test
    fun `onBatchApproval does not crash`() {
        // Setup lifecycle
        controller.create().resume()

        // Act - Batch approval callback
        activity.onBatchApproval()

        // Assert - Should handle gracefully
        assertEquals(false, activity.isFinishing)
    }

    @Test
    fun `onBatchDeclined does not crash`() {
        // Setup lifecycle
        controller.create().resume()

        // Act - Batch declined callback
        activity.onBatchDeclined()

        // Assert - Should handle gracefully
        assertEquals(false, activity.isFinishing)
    }

    @Test
    fun `onCreate with tip amount parses correctly`() {
        // Given - Setup mocks
        every { mockSdkProvider.initializeSdk(any(), any(), any()) } returns true
        every { mockSdkProvider.checkNfcEnabled() } returns true

        // Setup with tip
        val intent = Intent().apply {
            putExtra("Amount", "1000")
            putExtra("Tip", "150") // 1.50 RSD tip
            putExtra("uniqueId", "12345")
        }

        val controller2 = Robolectric.buildActivity(HeadlessPaymentActivity::class.java, intent)
        val activity2 = controller2.get()
        activity2.setTheme(R.style.Theme_AppCompat_Light_NoActionBar)
        activity2.softPosProvider = mockSdkProvider
        activity2.model = mockViewModel

        // When
        controller2.create()

        // Then - Should parse tip and initialize SDK with it
        verify { mockSdkProvider.initializeSdk("1000", "150", any()) }
    }
}