package com.payten.whitelabel.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payten.whitelabel.ui.components.CustomDialog
import com.payten.whitelabel.ui.components.NumericKeypad
import com.payten.whitelabel.ui.components.PinIndicators
import com.payten.whitelabel.ui.theme.AppTheme
import com.payten.whitelabel.ui.theme.MyriadPro
import com.payten.whitelabel.R

/**
 * PIN Setup screen for creating a new PIN.
 *
 * User enters a 4-digit PIN using the numeric keypad.
 *
 * @param onNavigateBack Callback when back button is clicked.
 * @param onPinSetupComplete Callback when PIN is successfully set up (PIN entered and confirmed).
 */
@Composable
fun PinSetupScreen(
    onNavigateBack: () -> Unit = {},
    onPinSetupComplete: (String) -> Unit = {}
) {
    var pin by remember { mutableStateOf("") }
    var confirmedPin by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }

    // Check the validity of the PIN
    LaunchedEffect(pin.length) {
        if (pin.length == 4 && confirmedPin == null) {
            confirmedPin = pin
            pin = ""
        } else if (confirmedPin != null && pin.length == 4) {
            if (pin == confirmedPin) {
                showSuccessDialog = true
                kotlinx.coroutines.delay(300)
                onPinSetupComplete(pin)
            } else {
                showErrorDialog = true
                kotlinx.coroutines.delay(1000)
                pin = ""
                confirmedPin = null
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

        if (showSuccessDialog) {
            CustomDialog(
                isSuccess = true,
                title = stringResource(R.string.pin_setup_success_dialog_title),
                buttonText = stringResource(R.string.dialog_button_ok_default),
                onDismiss = {
                    showSuccessDialog = false
                    onPinSetupComplete(confirmedPin ?: "")
                }
            )
        }

        if (showErrorDialog) {
            CustomDialog(
                isSuccess = false,
                title = stringResource(R.string.pin_setup_incorrect_pin_title),
                buttonText = stringResource(R.string.dialog_button_back_default),
                onDismiss = {
                    showErrorDialog = false
                    pin = ""
                    confirmedPin = null
                    showError = false
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PinSetupScreenPreview() {
    AppTheme {
        PinSetupScreen()
    }
}