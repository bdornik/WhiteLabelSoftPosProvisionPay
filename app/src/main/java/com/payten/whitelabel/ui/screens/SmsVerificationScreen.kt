package com.payten.whitelabel.ui.screens

import android.app.Activity
import android.content.Context
import android.content.IntentFilter
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cioccarellia.ksprefs.KsPrefs
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.payten.whitelabel.R
import com.payten.whitelabel.dto.OtpCheckDto
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.SmsBroadcastReceiver
import com.payten.whitelabel.ui.components.BackButton
import com.payten.whitelabel.ui.components.CustomDialog
import com.payten.whitelabel.ui.theme.MyriadPro
import com.payten.whitelabel.viewmodel.ForgotPinModel

/**
 * SMS verification screen with automatic SMS reading and verification.
 *
 * Flow:
 * 1. User clicks "Send SMS"
 * 2. SMS is sent
 * 3. Wait for SMS (show loading)
 * 4. Auto-read SMS code
 * 5. Auto-verify OTP
 * 6. Navigate to PIN change
 *
 * @param sharedPreferences SharedPreferences instance
 * @param onNavigateBack Callback when back button is clicked
 * @param onVerificationComplete Callback when OTP is verified (navigate to PIN change)
 * @param viewModel ViewModel for SMS and OTP handling
 */
@Composable
fun SmsVerificationScreen(
    sharedPreferences: KsPrefs,
    onNavigateBack: () -> Unit = {},
    onVerificationComplete: () -> Unit = {},
    viewModel: ForgotPinModel = hiltViewModel()
) {
    val TAG = "SmsVerificationScreen"
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var smsListenerStarted by remember { mutableStateOf(false) }

    val userId = sharedPreferences.pull(SharedPreferencesKeys.USER_ID, "")

    val smsConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // Extract SMS message
            val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
            Log.d(TAG, "SMS received: $message")

            // Extract OTP code (6-digit)
            val otpRegex = "\\b\\d{6}\\b".toRegex()
            val extractedOtp = otpRegex.find(message ?: "")?.value

            if (extractedOtp != null) {
                Log.d(TAG, "Extracted OTP: $extractedOtp")
                loadingMessage = context.getString(R.string.verifying_code)

                viewModel.otpCheck(
                    OtpCheckDto(
                        userId = userId,
                        activationCode = extractedOtp
                    )
                )
            } else {
                Log.e(TAG, "Could not extract OTP from SMS")
                isLoading = false
                errorMessage = context.getString(R.string.sms_code_not_found)
                showErrorDialog = true
            }
        } else {
            Log.d(TAG, "SMS consent cancelled by user")
            isLoading = false
            errorMessage = context.getString(R.string.sms_consent_denied)
            showErrorDialog = true
        }
    }

    // Start SMS retrieval when screen loads
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

    // Listen for SMS
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

    // Observe SMS sending response
    val otpResponse by viewModel.otpResponse.observeAsState()

    LaunchedEffect(otpResponse) {
        otpResponse?.let { success ->
            Log.d(TAG, "SMS send response: $success")

            if (success) {
                Log.d(TAG, "SMS sent successfully, waiting for incoming SMS")
                loadingMessage = context.getString(R.string.waiting_for_sms)
                smsListenerStarted = true
            } else {
                // Failed to send SMS
                isLoading = false
                errorMessage = context.getString(R.string.sms_send_failed)
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
                onVerificationComplete()
            } else {
                errorMessage = context.getString(R.string.wrong_otp_code)
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
            SmsVerificationHeader(
                onNavigateBack = onNavigateBack
            )

            Spacer(modifier = Modifier.height(48.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .statusBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF001F3F),
                                    Color(0xFF0051A8)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.sms),
                        contentDescription = null,
                        modifier = Modifier.size(140.dp)
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                if (isLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = loadingMessage,
                            fontSize = 16.sp,
                            fontFamily = MyriadPro,
                            fontWeight = FontWeight.Normal,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            Log.d(TAG, "Send SMS button clicked")
                            isLoading = true
                            loadingMessage = context.getString(R.string.sending_sms)
                            viewModel.sendSms()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.sms_send_code),
                            fontSize = 18.sp,
                            fontFamily = MyriadPro,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (!isLoading) {
                    Text(
                        text = stringResource(R.string.sms_info_text),
                        fontSize = 14.sp,
                        fontFamily = MyriadPro,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(100.dp))
            }
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
private fun SmsVerificationHeader(
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