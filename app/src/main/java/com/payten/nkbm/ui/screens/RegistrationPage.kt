package com.payten.nkbm.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.payten.nkbm.ui.components.BackButton
import com.payten.nkbm.ui.components.CustomTextField
import com.payten.nkbm.ui.theme.MyriadPro
/**
 * Registration screen for new users.
 *
 * Allows users to register by entering the User ID and the Activation code.
 *
 * User must accept terms and conditions otherwise the button for proceeding will be disabled.
 *
 * @param onNavigateBack Callback invoked when the back button is clicked.
 * @param onNavigateNext Callback invoked when the registration is successful
 * */
@Composable
fun RegistrationPage(onNavigateBack: () -> Unit, onNavigateNext: () -> Unit) {

    var userId by remember { mutableStateOf("") }
    var activationCode by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(Color(0xffeff2fa))) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.offset(x = 32.dp, y = 62.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(
                    onClick = onNavigateBack
                )

                Spacer(modifier = Modifier.size((43).dp))

                Text(
                    text = "REGISTRACIJA",
                    fontSize = 16.sp,
                    fontFamily = MyriadPro,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(y = 9.dp),
                    textAlign = TextAlign.Center,
                    letterSpacing = TextUnit(4f, TextUnitType.Sp)
                )
            }

            Spacer(modifier = Modifier.size(100.dp))

            RegistrationForm()
        }

    }
}

/***
 * Registration form containing input fields and T&C box.
 */
@Composable
fun RegistrationForm(modifier: Modifier = Modifier) {
    var isChecked by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CustomTextField(
            label = "ID Korisnika",
            placeholder = "Unesite svoj korisnički ID",
        )

        CustomTextField(
            label = "Aktivacioni kod",
            placeholder = "Unesite aktivacioni kod"
        )

        Spacer(modifier = modifier.size(150.dp))

        TermsAndConditionsBox(
            isChecked = isChecked,
            onCheckedChange = { isChecked = it }
        )

        Spacer(modifier = Modifier.size(10.dp))


        Button(
            onClick = {  },//TODO
            enabled = isChecked,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Red,
                disabledContainerColor = Color.Red.copy(alpha = 0.4f),
                contentColor = Color.White,
                disabledContentColor = Color.White.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = "NASTAVI",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
/**
 * T&C acceptance box with a checkbox.
 * */
@Composable
fun TermsAndConditionsBox(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x65d7dee2), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                modifier = Modifier
                    .background(Color.White)
                    .size(16.dp, 16.dp),
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color.Red,
                    uncheckedColor = Color.White
                )
            )

            Text(
                text = "Slažem se sa opštim uslovima i odredbama za iznajmljivanje POS opreme, " +
                        "prihvatanje platnih kartica i Flik instant plaćanja.",
                fontSize = 14.sp,
                fontFamily = MyriadPro,
                color = Color.DarkGray,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}
@Preview
@Composable
fun RegPreview()
{
    RegistrationPage(onNavigateBack = {}, onNavigateNext = {})
}