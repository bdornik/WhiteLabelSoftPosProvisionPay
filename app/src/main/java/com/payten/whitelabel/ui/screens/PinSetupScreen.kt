package com.payten.whitelabel.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import at.favre.lib.crypto.bcrypt.BCrypt
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.ui.components.CustomDialog
import com.payten.whitelabel.ui.components.NumericKeypad
import com.payten.whitelabel.ui.components.PinIndicators
import com.payten.whitelabel.ui.theme.MyriadPro
import com.payten.whitelabel.R
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.viewmodel.ProvisionViewModel
import com.sacbpp.remotemanagement.SACBPPNotificationManager
import com.simant.MainApplication
import com.simant.utils.AppEvent
import com.simant.utils.AppEventBus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * PIN Setup screen for creating a new PIN.
 *
 * User enters a 4-digit PIN using the numeric keypad.
 *  After PIN is confirmed, performs provision.
 *
 * @param sharedPreferences SharedPreferences instance.
 * @param onNavigateBack Callback when back button is clicked.
 * @param onPinSetupComplete Callback when PIN is successfully set up (PIN entered, confirmed and provisioned).
 * @param viewModel ProvisionViewModel for provisioning logic.
 */
@Composable
fun PinSetupScreen(
    sharedPreferences: KsPrefs,
    onNavigateBack: () -> Unit = {},
    onPinSetupComplete: (String) -> Unit = {},
    viewModel: ProvisionViewModel = hiltViewModel()
) {
    val TAG = "PinSetupScreen"
    val scope = rememberCoroutineScope()

    var pin by remember { mutableStateOf("") }
    var confirmedPin by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorDialogMessage by remember { mutableStateOf("") }

    var isProvisioning by remember { mutableStateOf(false) }
    var provisioningStep by remember { mutableStateOf("") }
    var provisionComplete by remember { mutableStateOf(false) }
    var provisionError by remember { mutableStateOf(false) }

    val eventDisposables = remember { CompositeDisposable() }

    val tokenRefreshSuccess by viewModel.getDetailsSuccessfull.observeAsState()

    // Start provisioning after PIN is confirmed
    fun startProvisioning(finalPin: String) {
        Log.d(TAG, "Starting provisioning flow...")
        isProvisioning = true
        provisioningStep = "Saving PIN..."

        // Save PIN to SharedPreferences
        val encryptedPin = BCrypt
            .withDefaults()
            .hashToString(12, finalPin.toCharArray())
        sharedPreferences.push(SharedPreferencesKeys.PIN, encryptedPin)
        sharedPreferences.push(SharedPreferencesKeys.REGISTERED, true)

        // Check if dummy mode
        val isDummy = sharedPreferences.pull(SharedPreferencesKeys.DUMMY, false)

        if (isDummy) {
            Log.d(TAG, "Dummy mode - skipping provision")
            isProvisioning = false
            onPinSetupComplete(pin)
            return
        }

        // Start listening to SDK events
        provisioningStep = "Initializing SDK..."
        val disposable = AppEventBus.listen(AppEvent::class.java)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { event ->
                Log.d(TAG, "SDK Event: ${event.process}")
                if (event.process == SACBPPNotificationManager.END_OF_REMOTE_PROCESS) {
                    Log.d(TAG, "SDK provisioning complete!")
                    provisionComplete = true
                    isProvisioning = false
                    onPinSetupComplete(pin)
                }
            }
        eventDisposables.add(disposable)

        // Start timeout timer
        scope.launch {
            var secondsElapsed = 0
            while (secondsElapsed < 15 && !provisionComplete && !provisionError) {
                delay(1000)
                secondsElapsed++
                Log.d(TAG, "Provision timeout: $secondsElapsed/15 seconds")
            }

            if (!provisionComplete && !provisionError) {
                Log.e(TAG, "Provision timeout after 15 seconds")
                // Retry SDK provisioning on timeout
                try {
                    MainApplication.getSACBTPApplication().goOnlineCheckRNS()
                    Log.d(TAG, "Retrying SDK provisioning after timeout")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to retry SDK provisioning", e)
                    isProvisioning = false
                    errorDialogMessage = "Provisioning failed. Please try again."
                    showErrorDialog = true
                }
            }
        }

        provisioningStep = "Refreshing token..."
        viewModel.refreshData()
    }

    // Handle token refresh + merchant details response
    LaunchedEffect(tokenRefreshSuccess) {
        tokenRefreshSuccess?.let { success ->
            Log.d(TAG, "Token refresh + details fetch result: $success")

            if (success) {
                // Token refreshed and merchant details fetched successfully
                provisioningStep = "Configuring terminal..."

                // Trigger SDK remote provisioning
                try {
                    MainApplication.getSACBTPApplication().goOnlineCheckRNS()
                    Log.d(TAG, "SDK remote provisioning started")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start SDK provisioning", e)
                    provisionError = true
                    isProvisioning = false
                    errorDialogMessage = "SDK provisioning failed. Please try again."
                    showErrorDialog = true
                }
            } else {
                // Token refresh or merchant details fetch failed
                Log.e(TAG, "Token refresh or merchant details fetch failed")
                provisionError = true
                isProvisioning = false
                errorDialogMessage = "Failed to fetch merchant details. Please try again."
                showErrorDialog = true
            }
        }
    }

    // Check the validity of the PIN
    LaunchedEffect(pin.length) {
        if (pin.length == 4 && confirmedPin == null) {
            confirmedPin = pin
            pin = ""
        } else if (confirmedPin != null && pin.length == 4) {
            if (pin == confirmedPin) {
                showSuccessDialog = true
                delay(300)
                startProvisioning(pin)
            } else {
                showErrorDialog = true
                delay(1000)
                pin = ""
                confirmedPin = null
                showError = false
            }
        }
    }
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "Cleaning up SDK event listeners")
            eventDisposables.clear()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (isProvisioning) {
            ProvisioningOverlay(provisioningStep = provisioningStep)
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.height((96).dp))

                    Text(
                        text = stringResource(R.string.registration_title),
                        fontSize = 20.sp,
                        fontFamily = MyriadPro,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        letterSpacing = TextUnit(4f, TextUnitType.Sp)
                    )
                }

                Spacer(modifier = Modifier.height(120.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (confirmedPin == null) {
                            stringResource(R.string.pin_setup_enter_desired_pin)
                        } else {
                            stringResource(R.string.pin_setup_confirm_your_pin)
                        },
                        fontSize = 24.sp,
                        fontFamily = MyriadPro,
                        fontWeight = FontWeight.Bold,
                        color = if (showError) MaterialTheme.colorScheme.error else Color.Black
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    PinIndicators(
                        pinLength = pin.length,
                        isError = showError
                    )

                    Spacer(modifier = Modifier.height(80.dp))

                    NumericKeypad(
                        onNumberClick = { number ->
                            if (pin.length < 4) {
                                pin += number
                            }
                        },
                        onBackspaceClick = {
                            if (pin.isNotEmpty()) {
                                pin = pin.dropLast(1)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }

        if (showSuccessDialog && !isProvisioning) {
            CustomDialog(
                isSuccess = true,
                title = stringResource(R.string.pin_setup_success_dialog_title),
                buttonText = stringResource(R.string.dialog_button_ok_default),
                onDismiss = {
                    showSuccessDialog = false
                }
            )
        }

        if (showErrorDialog && !isProvisioning) {
            CustomDialog(
                isSuccess = false,
                title = errorDialogMessage.ifEmpty {
                    stringResource(R.string.pin_setup_incorrect_pin_title)
                },
                buttonText = stringResource(R.string.dialog_button_back_default),
                onDismiss = {
                    showErrorDialog = false
                    errorDialogMessage = ""
                    pin = ""
                    confirmedPin = null
                    showError = false
                }
            )
        }
    }
}

/**
 * Provisioning overlay with loading indicator and status text.
 */
@Composable
private fun ProvisioningOverlay(provisioningStep: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp
            )

            Text(
                text = provisioningStep,
                fontSize = 20.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}