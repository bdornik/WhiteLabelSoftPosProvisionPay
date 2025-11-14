package com.payten.whitelabel.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.favre.lib.crypto.bcrypt.BCrypt
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.components.CustomDialog
import com.payten.whitelabel.ui.theme.MyriadPro
import kotlinx.coroutines.delay
import com.payten.whitelabel.ui.components.NumericKeypad
import com.payten.whitelabel.ui.components.PinIndicators
import com.payten.whitelabel.R

/**
 * PIN Login screen for returning users.
 *
 * User enters their 4-digit PIN to log in.
 * After 3 failed attempts, the app is blocked.
 *
 * @param sharedPreferences SharedPreferences for PIN verification.
 * @param onLoginSuccess Callback when PIN is correct.
 * @param onForgotPin Callback when "Forgot PIN" is clicked.
 * @param onLoginFailed Callback when max attempts reached and app is blocked.
 */
@Composable
fun PinLoginScreen(
    sharedPreferences: KsPrefs,
    onLoginSuccess: () -> Unit = {},
    onForgotPin: () -> Unit = {},
    onLoginFailed: () -> Unit = {}
) {
    var pin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var attemptsRemaining by remember {
        mutableStateOf(sharedPreferences.pull(SharedPreferencesKeys.PIN_COUNT, 3))
    }
    var showErrorDialog by remember {mutableStateOf(false)}
    var errorDialogMessage by remember { mutableStateOf("") }

    val isAppBlocked = sharedPreferences.pull(SharedPreferencesKeys.APP_BLOCKED, false)
    val appBlockedErrorMessage = stringResource(R.string.pin_login_app_blocked_contact_support)

    val wrongPINErrorMessage = stringResource(R.string.pin_login_incorrect_with_attempts)

    LaunchedEffect(pin.length) {
        if (pin.length == 4) {
            delay(300)

            val storedPin = sharedPreferences.pull(SharedPreferencesKeys.PIN, "")
            val result = BCrypt.verifyer().verify(pin.toCharArray(), storedPin)

            if (result.verified) {
                sharedPreferences.push(SharedPreferencesKeys.PIN_COUNT, 3)
                onLoginSuccess()
            } else {
                attemptsRemaining--
                sharedPreferences.push(SharedPreferencesKeys.PIN_COUNT, attemptsRemaining)

                if (attemptsRemaining == 0) {
                    sharedPreferences.push(SharedPreferencesKeys.APP_BLOCKED, true)
                    errorMessage = appBlockedErrorMessage
                    showErrorDialog = true
                } else {
                    errorMessage = wrongPINErrorMessage + attemptsRemaining
                    showErrorDialog = true
                }

                showError = true
                delay(2000)
                pin = ""
                showError = false
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
                .align(Alignment.TopCenter)
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Image(
                painter = painterResource(id = R.drawable.payten_logo_final_rgb),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(144.dp)
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = if (isAppBlocked) {
                        stringResource(R.string.pin_login_app_is_blocked)
                    } else {
                        stringResource(R.string.pin_login_enter_your_pin)
                    },
                    fontSize = 20.sp,
                    fontFamily = MyriadPro,
                    fontWeight = FontWeight.Bold,
                    color = if (showError) MaterialTheme.colorScheme.error else Color.Black
                )

                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        fontSize = 14.sp,
                        fontFamily = MyriadPro,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                PinIndicators(
                    pinLength = pin.length,
                    isError = showError
                )

                Spacer(modifier = Modifier.height(60.dp))

                if (!isAppBlocked) {
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
                }

                Spacer(modifier = Modifier.height(48.dp))

                if (!isAppBlocked) {
                    Text(
                        text = stringResource(R.string.pin_login_forgot),
                        fontSize = 14.sp,
                        fontFamily = MyriadPro,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { onForgotPin() }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        if (showErrorDialog) {
            CustomDialog(
                isSuccess = false,
                title = errorDialogMessage,
                buttonText =
                    if (attemptsRemaining == 0) stringResource(R.string.dialog_button_ok_default)
                    else stringResource(R.string.dialog_button_back_default),
                onDismiss = {
                    showErrorDialog = false
                    if (attemptsRemaining == 0) {
                        onLoginFailed()
                    }
                }
            )
        }
    }
}


