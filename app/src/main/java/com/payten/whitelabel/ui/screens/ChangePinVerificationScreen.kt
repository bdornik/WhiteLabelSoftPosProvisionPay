package com.payten.whitelabel.ui.screens

import android.app.Activity
import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.os.Build.VERSION_CODES.TIRAMISU
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.payten.whitelabel.R
import com.payten.whitelabel.dto.OtpCheckDto
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.utils.SmsBroadcastReceiver
import com.payten.whitelabel.ui.components.BackButton
import com.payten.whitelabel.ui.components.CustomDialog
import com.payten.whitelabel.ui.components.CustomTextField
import com.payten.whitelabel.ui.theme.MyriadPro
import com.payten.whitelabel.viewmodel.ForgotPinModel

/**
 * Screen for verifying user identity before PIN change.
 *
 * User enters credentials, clicks "Send SMS", SMS is auto-read and verified.
 *
 * @param sharedPreferences SharedPreferences instance
 * @param onNavigateBack Callback when back button is clicked
 * @param onVerificationSuccess Callback when OTP is verified (navigate to PIN change)
 * @param viewModel ViewModel for SMS and OTP handling
 */
@RequiresApi(Build.VERSION_CODES.O, TIRAMISU)
@Composable
fun ChangePinVerificationScreen(
    sharedPreferences: KsPrefs,
    onNavigateBack: () -> Unit = {},
    onVerificationSuccess: () -> Unit = {},
    viewModel: ForgotPinModel = hiltViewModel()
) {
    val TAG = "ChangePinVerification"
    val context = LocalContext.current

    var userId by remember {
        mutableStateOf(sharedPreferences.pull(SharedPreferencesKeys.USER_ID, ""))
    }
    var activationCode by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var smsListenerStarted by remember { mutableStateOf(false) }

    val smsConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
            Log.d(TAG, "SMS received: $message")

            val otpRegex = "\\b\\d{6}\\b".toRegex()
            val extractedOtp = otpRegex.find(message ?: "")?.value

            if (extractedOtp != null) {
                Log.d(TAG, "Extracted OTP: $extractedOtp")
                activationCode = extractedOtp

                viewModel.otpCheck(
                    OtpCheckDto(
                        userId = userId,
                        activationCode = extractedOtp
                    )
                )
            } else {
                Log.e(TAG, "Could not extract OTP from SMS")
                isLoading = false
                errorMessage = context.getString(R.string.wrong_credentials)
                showErrorDialog = true
            }
        } else {
            Log.d(TAG, "SMS consent cancelled by user")
            isLoading = false
            errorMessage = context.getString(R.string.wrong_credentials)
            showErrorDialog = true
        }
    }

    LaunchedEffect(smsListenerStarted) {
        if (smsListenerStarted) {
            Log.d(TAG, "Starting SMS User Consent API")
            try {
                val client = SmsRetriever.getClient(context)
                client.startSmsUserConsent(null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SMS retriever", e)
            }
        }
    }

    DisposableEffect(smsListenerStarted) {
        if (!smsListenerStarted) {
            return@DisposableEffect onDispose {}
        }

        val smsReceiver = SmsBroadcastReceiver { intent ->
            Log.d(TAG, "SMS broadcast received")
            smsConsentLauncher.launch(intent)
        }

        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        context.registerReceiver(smsReceiver, intentFilter, Context.RECEIVER_EXPORTED)

        onDispose {
            Log.d(TAG, "Unregistering SMS receiver")
            try {
                context.unregisterReceiver(smsReceiver)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver", e)
            }
        }
    }

    val otpResponse by viewModel.otpResponse.observeAsState()

    LaunchedEffect(otpResponse) {
        otpResponse?.let { success ->
            Log.d(TAG, "SMS send response: $success")

            if (success) {
                Log.d(TAG, "SMS sent successfully, waiting for incoming SMS")
                smsListenerStarted = true
            } else {
                isLoading = false
                errorMessage = context.getString(R.string.wrong_credentials)
                showErrorDialog = true
            }
        }
    }

    val otpCheckResponse by viewModel.otpCheckResponse.observeAsState()

    LaunchedEffect(otpCheckResponse) {
        otpCheckResponse?.let { statusCode ->
            Log.d(TAG, "OTP check response: $statusCode")
            isLoading = false

            if (statusCode.equals("00", ignoreCase = true)) {
                sharedPreferences.push(SharedPreferencesKeys.APP_BLOCKED, false)
                sharedPreferences.push(SharedPreferencesKeys.PIN_COUNT, 3)
                Log.d(TAG, "OTP verified successfully, navigating to PIN change")
                onVerificationSuccess()
            } else {
                errorMessage = context.getString(R.string.wrong_credentials)
                showErrorDialog = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            ChangePinVerificationHeader(
                onNavigateBack = onNavigateBack
            )

            Spacer(modifier = Modifier.height(48.dp))

            ChangePinVerificationForm(
                userId = userId,
                onUserIdChange = { userId = it },
                activationCode = activationCode,
                onActivationCodeChange = { activationCode = it },
                isLoading = isLoading,
                onSendSms = {
                    Log.d(TAG, "=== VERIFICATION DEBUG START ===")
                    Log.d(TAG, "Entered userId: '$userId'")
                    Log.d(TAG, "Entered activationCode: '${activationCode}'")

                    val isDummy = sharedPreferences.pull(SharedPreferencesKeys.DUMMY, false)

                    if (isDummy) {
                        Log.d(TAG, "Dummy login detected - skipping verification")
                        onVerificationSuccess()
                    } else {
                        val storedUserId = sharedPreferences.pull(SharedPreferencesKeys.USER_ID, "")
                        val storedRegUserId = sharedPreferences.pull(SharedPreferencesKeys.REGISTRATION_USER_ID, "")
                        val hashedActCode = sharedPreferences.pull(SharedPreferencesKeys.HASHED_ACT_ID, "")

                        val actualUserId = storedUserId.ifEmpty { storedRegUserId }

                        Log.d(TAG, "Stored USER_ID: '$storedUserId'")
                        Log.d(TAG, "Stored REGISTRATION_USER_ID: '$storedRegUserId'")
                        Log.d(TAG, "Using userId: '$actualUserId'")

                        val isUserIdCorrect = userId == actualUserId

                        val isActCodeCorrect = if (hashedActCode.isEmpty()) {
                            Log.e(TAG, "HASHED_ACT_ID is empty!")
                            false
                        } else {
                            try {
                                val result = BCrypt.verifyer().verify(activationCode.toCharArray(), hashedActCode)
                                Log.d(TAG, "BCrypt result: ${result.verified}")
                                result.verified
                            } catch (e: Exception) {
                                Log.e(TAG, "BCrypt exception", e)
                                false
                            }
                        }

                        Log.d(TAG, "User ID match: $isUserIdCorrect")
                        Log.d(TAG, "Act Code match: $isActCodeCorrect")
                        Log.d(TAG, "=== VERIFICATION DEBUG END ===")

                        if (isUserIdCorrect && isActCodeCorrect) {
                            Log.d(TAG, "Credentials verified, sending SMS")
                            isLoading = true
                            viewModel.sendSms()
                        } else {
                            Log.e(TAG, "Credentials verification failed")
                            errorMessage = context.getString(R.string.wrong_credentials)
                            showErrorDialog = true
                        }
                    }
                }
            )
        }

        if (showErrorDialog) {
            CustomDialog(
                isSuccess = false,
                title = errorMessage,
                buttonText = stringResource(R.string.dialog_button_back_default),
                onDismiss = {
                    showErrorDialog = false
                }
            )
        }
    }
}

/**
 * Header with back button and title.
 */
@Composable
private fun ChangePinVerificationHeader(
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BackButton(onClick = onNavigateBack)

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = stringResource(R.string.change_pin_verification_title),
            fontSize = 20.sp,
            fontFamily = MyriadPro,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            letterSpacing = TextUnit(4f, TextUnitType.Sp)
        )

        Spacer(modifier = Modifier.width(40.dp))
    }
}

/**
 * Form with User ID and Activation Code fields.
 */
@Composable
private fun ChangePinVerificationForm(
    userId: String,
    onUserIdChange: (String) -> Unit,
    activationCode: String,
    onActivationCodeChange: (String) -> Unit,
    isLoading: Boolean,
    onSendSms: () -> Unit
) {
    val isButtonEnabled = userId.isNotEmpty() && activationCode.isNotEmpty() && !isLoading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CustomTextField(
            label = stringResource(R.string.change_pin_user_id_label),
            placeholder = stringResource(R.string.change_pin_user_id_hint),
            value = userId,
            onValueChange = onUserIdChange
        )

        CustomTextField(
            label = stringResource(R.string.change_pin_activation_code_label),
            placeholder = stringResource(R.string.change_pin_activation_code_hint),
            value = activationCode,
            onValueChange = onActivationCodeChange
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = stringResource(R.string.sms_disclaimer),
            fontSize = 14.sp,
            fontFamily = MyriadPro,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Button(
                onClick = onSendSms,
                enabled = isButtonEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.sms_send_code),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}