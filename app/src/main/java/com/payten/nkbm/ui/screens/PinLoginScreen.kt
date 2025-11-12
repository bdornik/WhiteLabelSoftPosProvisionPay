package com.payten.nkbm.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.favre.lib.crypto.bcrypt.BCrypt
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.nkbm.persistance.SharedPreferencesKeys
import com.payten.nkbm.ui.components.CustomDialog
import com.payten.nkbm.ui.theme.MyriadPro
import kotlinx.coroutines.delay
import com.payten.nkbm.ui.components.NumericKeypad
import com.payten.nkbm.ui.components.PinIndicators

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
                    errorMessage = "Aplikacija je blokirana. Kontaktirajte podršku."
                    showErrorDialog = true
                } else {
                    errorMessage = "Netačan PIN. Preostali pokušaji: $attemptsRemaining"
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
            Spacer(modifier = Modifier.height(120.dp))

            Spacer(modifier = Modifier.height(60.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = if (isAppBlocked) {
                        "Aplikacija je blokirana"
                    } else {
                        "Unesite PIN"
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

                Spacer(modifier = Modifier.height(24.dp))

                if (!isAppBlocked) {
                    Text(
                        text = "Zaboravili ste PIN?",
                        fontSize = 14.sp,
                        fontFamily = MyriadPro,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { onForgotPin() }
                    )
                }

                Spacer(modifier = Modifier.height(80.dp))

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

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
        if (showErrorDialog) {
            CustomDialog(
                isSuccess = false,
                title = errorDialogMessage,
                buttonText = if (attemptsRemaining == 0) "U REDU" else "NAZAD",
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


