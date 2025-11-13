package com.payten.whitelabel.ui.screens

import android.app.Application
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import at.favre.lib.crypto.bcrypt.BCrypt
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.config.SupercaseConfig
import com.payten.whitelabel.dto.keys.GetKeysRequestDto
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.components.CustomDialog
import com.payten.whitelabel.ui.components.CustomTextField
import com.payten.whitelabel.ui.components.TermsAndConditionsBox
import com.payten.whitelabel.ui.theme.AppTheme
import com.payten.whitelabel.ui.theme.MyriadPro
import com.payten.whitelabel.viewmodel.RegistrationViewModel
import com.simant.MainApplication
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Registration screen for new users.
 *
 * Allows users to register by entering the User ID and the Activation code.
 *
 * User must accept terms and conditions otherwise the button for proceeding will be disabled.
 *
 * @param onNavigateBack Callback invoked when the back button is clicked.
 * @param onNavigateNext Callback invoked when the registration is successful.
 * @param onViewTermsClick Callback invoked when the T&C link is clicked.
 * */
@Composable
fun RegistrationPage(
    sharedPreferences: KsPrefs,
    onNavigateBack: () -> Unit,
    onNavigateNext: () -> Unit,
    onViewTermsClick: () -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel()
) {
    val TAG = "RegistrationPage"
    val context = LocalContext.current
    val application = context.applicationContext as Application

    var userId by remember { mutableStateOf(sharedPreferences.pull(SharedPreferencesKeys.REGISTRATION_USER_ID, ""))}
    var activationCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false)}
    var showErrorDialog by remember { mutableStateOf(false)}
    var errorMessage by remember { mutableStateOf("") }

    Log.d(TAG, "RegistrationPage composed")

    val activationSuccess by viewModel.paytenActivationSuccessfull.observeAsState()
    val activationFailed by viewModel.paytenActivationFailed.observeAsState()
    val tokenSuccess by viewModel.paytenGenerateTokenSuccessfull.observeAsState()
    val tokenFailed by viewModel.paytenGenerateTokenFailed.observeAsState()
    val hostKeysSuccess by viewModel.paytenGetHostKeys.observeAsState()
    val sdkSuccess by viewModel.sdkRegisterSuccess.observeAsState()
    val sdkFailed by viewModel.sdkRegisterFailed.observeAsState()
    val healthCheckStatus by viewModel.healthCheck.observeAsState(false)

    // We are performing the health check on first composition
    LaunchedEffect(Unit) {
        Log.d(TAG, "Health check started")
        viewModel.healthCheck()
    }

    // Save User ID to SharedPreferences when it changes
    LaunchedEffect(userId) {
        sharedPreferences.push(SharedPreferencesKeys.REGISTRATION_USER_ID, userId)
        Log.d(TAG, "User ID saved: $userId")
    }

    // Handle activation success
    LaunchedEffect(activationSuccess) {
        activationSuccess?.let {
            Log.d(TAG, "Activation success: $it")
            if (it) {
                // We need to update the parameters
                (application as? MainApplication)?.updateParameters()

                // Hash activation code
                val encryptedActId = BCrypt
                    .withDefaults()
                    .hashToString(12, activationCode.toCharArray())
                sharedPreferences.push(SharedPreferencesKeys.HASHED_ACT_ID, encryptedActId)
                sharedPreferences.push(SharedPreferencesKeys.IS_REGISTERED, true)

                // Generate token
                val userIdFromPrefs = sharedPreferences.pull(SharedPreferencesKeys.USER_ID, "")
                val tidFromPrefs = sharedPreferences.pull(SharedPreferencesKeys.USER_TID, "")
                Log.d(TAG, "Generating token: userId=$userIdFromPrefs, tid=$tidFromPrefs")
                viewModel.generateToken(userIdFromPrefs, tidFromPrefs)
            }
        }
    }

    // Handle token generation success
    LaunchedEffect(tokenSuccess) {
        tokenSuccess?.let {
            Log.d(TAG, "Token generation success: $it")
            if (it) {
                val tid = sharedPreferences.pull(SharedPreferencesKeys.USER_TID, "")
                Log.d(TAG, "Getting host keys for TID: $tid")
                viewModel.getHostKeys(GetKeysRequestDto(tid))
            }
        }
    }

    // Handle host keys success
    LaunchedEffect(hostKeysSuccess) {
        hostKeysSuccess?.let { success ->
            Log.d(TAG, "Host keys success: $success")
            if (success) {
                val regUserId =
                    sharedPreferences.pull(SharedPreferencesKeys.REGISTRATION_USER_ID, "")
                Log.d(TAG, "Initializing MTA: userId=$regUserId, code=$activationCode")
                viewModel.initializeMta(
                    application as MainApplication,
                    regUserId,
                    activationCode,
                    context
                )
            } else {
                isLoading = false
                errorMessage = "Unsuccessful key retrieval"
                showErrorDialog = true
            }
        }
    }

    // Handle SDK registration success
    LaunchedEffect(sdkSuccess) {
        sdkSuccess?.let {
            Log.d(TAG, "SDK registration success: $it")
            if (it) {
                isLoading = false
                sharedPreferences.push(SharedPreferencesKeys.IS_LOGGED_IN, true)

                // Set End of Day date
                val currentDateTime = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
                sharedPreferences.push(SharedPreferencesKeys.END_OF_DAY_DATE, currentDateTime.format(formatter))

                // Navigate to PIN Setup
                Log.d(TAG, "Navigating to PIN Setup")
                onNavigateNext()
            }
        }
    }

    // Handle activation failure
    LaunchedEffect(activationFailed) {
        activationFailed?.let {
            Log.e(TAG, "Activation failed: $it")
            if (it) {
                isLoading = false
                errorMessage = "Unsuccessful registration"
                showErrorDialog = true
            }
        }
    }

    // Handle token failure
    LaunchedEffect(tokenFailed) {
        tokenFailed?.let {
            Log.e(TAG, "Token generation failed: $it")
            if (it) {
                isLoading = false
                errorMessage = "Unsuccessful token generation"
                showErrorDialog = true
            }
        }
    }

    // Handle SDK failure
    LaunchedEffect(sdkFailed) {
        sdkFailed?.let { error ->
            isLoading = false
            errorMessage = "SDK registration unsuccessful: $error"
            showErrorDialog = true
        }
    }
    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                /*BackButton(
                    onClick = onNavigateBack
                )*/

                Spacer(modifier = Modifier.height((96).dp))

                Text(
                    text = "REGISTRACIJA",
                    fontSize = 20.sp,
                    fontFamily = MyriadPro,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    letterSpacing = TextUnit(4f, TextUnitType.Sp)
                )
            }

            Spacer(modifier = Modifier.size(48.dp))

            RegistrationForm(
                userId = userId,
                onUserIdChange = {
                    Log.d(TAG, "User ID changed: $it")
                    userId = it },
                activationCode = activationCode,
                onActivationCodeChange = {
                    Log.d(TAG, "Activation code changed: ${it.length} chars")
                    activationCode = it },
                termsAccepted = termsAccepted,
                onTermsAcceptedChange = {
                    Log.d(TAG, "Terms accepted: $it")
                    termsAccepted = it },
                isLoading = isLoading,
                onContinue = {
                    Log.d(TAG, "=== CONTINUE CLICKED ===")
                    Log.d(TAG, "userId: $userId")
                    Log.d(TAG, "activationCode length: ${activationCode.length}")
                    Log.d(TAG, "termsAccepted: $termsAccepted")
                    Log.d(TAG, "isLoading: $isLoading")
                    isLoading = true

                    // Check for dummy login
                    if (userId == SupercaseConfig.dummyUsId && activationCode == SupercaseConfig.dummyActCode) {
                        Log.d(TAG, "DUMMY LOGIN detected")
                        // Dummy login - skip API calls
                        val encryptedActId = BCrypt
                            .withDefaults()
                            .hashToString(12, activationCode.toCharArray())
                        sharedPreferences.push(SharedPreferencesKeys.IS_LOGGED_IN, true)
                        sharedPreferences.push(SharedPreferencesKeys.HASHED_ACT_ID, encryptedActId)
                        sharedPreferences.push(SharedPreferencesKeys.IS_REGISTERED, true)
                        sharedPreferences.push(SharedPreferencesKeys.DUMMY, true)
                        sharedPreferences.push(SharedPreferencesKeys.MERCHANT_NAME, "Google google")
                        sharedPreferences.push(SharedPreferencesKeys.MERCHANT_ADDRESS, "Google 123")
                        sharedPreferences.push(SharedPreferencesKeys.MERCHANT_PLACE_NAME, "Google")
                        isLoading = false
                        Log.d(TAG, "Navigating to next (dummy)")
                        onNavigateNext()
                    } else {
                        Log.d(TAG, "REAL REGISTRATION - calling activate()")
                        // Real registration - start API flow
                        viewModel.activate(
                            userId,
                            activationCode,
                            "Intesa.Android",
                            context,
                            context.getSharedPreferences("SOFTPOS_PARAMETERS_MDI", android.content.Context.MODE_PRIVATE)
                        )
                    }
                },
                onViewTermsClick = onViewTermsClick
            )
        }
        if (showErrorDialog) {
            CustomDialog(
                isSuccess = false,
                title = errorMessage,
                buttonText = "NAZAD",
                onDismiss = {
                    showErrorDialog = false
                }
            )
        }

    }
}

/***
 * Registration form containing input fields and T&C box.
 */
@Composable
private fun RegistrationForm(
    userId: String,
    onUserIdChange: (String) -> Unit,
    activationCode: String,
    onActivationCodeChange: (String) -> Unit,
    termsAccepted: Boolean,
    onTermsAcceptedChange: (Boolean) -> Unit,
    isLoading: Boolean,
    onContinue: () -> Unit,
    onViewTermsClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val TAG = "RegistrationForm"

    val isButtonEnabled = termsAccepted && userId.isNotEmpty() && activationCode.isNotEmpty() && !isLoading

    Log.d(TAG, "Button enabled: $isButtonEnabled (terms=$termsAccepted, userId=${userId.isNotEmpty()}, code=${activationCode.isNotEmpty()}, loading=$isLoading)")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User ID field
        CustomTextField(
            label = "ID Korisnika",
            placeholder = "Unesite svoj korisnički ID",
            value = userId,
            onValueChange = onUserIdChange
        )

        // Activation code field
        CustomTextField(
            label = "Aktivacioni kod",
            placeholder = "Unesite aktivacioni kod",
            value = activationCode,
            onValueChange = onActivationCodeChange
        )

        Spacer(modifier = Modifier.height(150.dp))

        // T&C checkbox
        TermsAndConditionsBox(
            isChecked = termsAccepted,
            onCheckedChange = onTermsAcceptedChange,
            onViewTermsClick = onViewTermsClick
        )

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = {
                Log.d(TAG, "Button physically clicked!")
                onContinue()
            },
            enabled = termsAccepted && userId.isNotEmpty() && activationCode.isNotEmpty() && !isLoading,
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
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = "NASTAVI",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Registration - Light")
@Composable
fun RegPreview() {
    AppTheme {
        RegistrationPage(
            onNavigateBack = {},
            onNavigateNext = { },
            onViewTermsClick = {},
            sharedPreferences = TODO(),
            viewModel = TODO(),
        )
    }
}