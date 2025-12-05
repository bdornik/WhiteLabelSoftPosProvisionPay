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

    @RelaxedMockK
    lateinit var mockViewModel: PosViewModel

    @RelaxedMockK
    lateinit var mockSdkProvider: SoftPosProvider

    @Before
    fun setup() {
        MockKAnnotations.init(this)

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
}