package com.payten.whitelabel.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payten.whitelabel.ui.theme.AppTheme
import com.payten.whitelabel.ui.theme.MyriadPro
import com.payten.whitelabel.R

/**
 * Landing screen after successful login.
 *
 * Shows welcome message and button to start a new transaction.
 * Includes menu icon for navigation to user menu.
 *
 * @param onNavigateToTransaction Callback when the button is clicked.
 * @param onNavigateToMenu Callback when menu icon is clicked.
 */
@Composable
fun LandingScreen(
    onNavigateToTransaction: () -> Unit = {},
    onNavigateToMenu: () -> Unit = {}
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.gradient_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Image(
            painter = painterResource(id = R.drawable.corner_lines),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (-120).dp, y = (-20).dp)
                .size(width = 350.dp, height = 450.dp),
            contentScale = ContentScale.Fit
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Header(onMenuClick = onNavigateToMenu)

            Spacer(modifier = Modifier.weight(1f))

            WelcomeContent()

            Spacer(modifier = Modifier.weight(1f))

            TransactionButton(onClick = onNavigateToTransaction)

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
private fun Header(
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 48.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.payten),
            contentDescription = null,
            modifier = Modifier
                .height(28.dp)
                .align(Alignment.Center),
            contentScale = ContentScale.FillHeight
        )

        Image(
            painter = painterResource(id = R.drawable.menu_image),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.CenterEnd)
                .clickable(onClick = onMenuClick)
        )
    }
}

@Composable
private fun WelcomeContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Dobrodošli",
            fontSize = 24.sp,
            fontFamily = MyriadPro,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            style = TextStyle(
                shadow = Shadow(
                    color = Color(0x40000000),
                    offset = Offset(0f, 4f),
                    blurRadius = 24f
                )
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "u Payten Soft POS aplikaciju, koja vam pruža brz, siguran i jednostavan način prihvatanja platnih kartica i Flik instant plaćanja.",
            fontSize = 16.sp,
            fontFamily = MyriadPro,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun TransactionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text(
            text = "NOVA TRANSAKCIJA",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MyriadPro,
            letterSpacing = TextUnit(1f, TextUnitType.Sp)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LandingScreenPreview() {
    AppTheme {
        LandingScreen()
    }
}