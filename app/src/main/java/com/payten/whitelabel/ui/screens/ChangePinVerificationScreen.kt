package com.payten.whitelabel.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import at.favre.lib.crypto.bcrypt.BCrypt
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.R
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.components.BackButton
import com.payten.whitelabel.ui.components.CustomDialog
import com.payten.whitelabel.ui.components.CustomTextField
import com.payten.whitelabel.ui.theme.MyriadPro

/**
 * Screen for verifying user identity before PIN change.
 *
 * Verifies User ID and Activation Code LOCALLY from SharedPreferences.
 * NO API calls - just local credential verification.
 *
 * @param sharedPreferences SharedPreferences instance
 * @param onNavigateBack Callback when back button is clicked
 * @param onVerificationSuccess Callback when credentials are verified (navigate to SMS screen)
 */
@Composable
fun ChangePinVerificationScreen(
    sharedPreferences: KsPrefs,
    onNavigateBack: () -> Unit = {},
    onVerificationSuccess: () -> Unit = {}
) {
    val TAG = "ChangePinVerificationScreen"
    val context = LocalContext.current

    var userId by remember {
        mutableStateOf(sharedPreferences.pull(SharedPreferencesKeys.USER_ID, ""))
    }
    var activationCode by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

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
                onContinue = {
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
                            Log.d(TAG, "SUCCESS - Navigating")
                            onVerificationSuccess()
                        } else {
                            Log.e(TAG, "FAILED - Error dialog")
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
    onContinue: () -> Unit
) {
    val isButtonEnabled = userId.isNotEmpty() && activationCode.isNotEmpty()

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

        Button(
            onClick = onContinue,
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
                text = stringResource(R.string.change_pin_continue),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}