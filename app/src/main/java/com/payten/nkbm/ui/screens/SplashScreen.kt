package com.payten.nkbm.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import com.payten.nkbm.R
import com.payten.nkbm.ui.theme.MyriadPro

/**
 * Splash screen is displayed when the app launches.
 *
 * This screen performs initial app setup and determines the next destination:
 * - If user is not registered → navigate to LandingPage
 * - If user is registered but not logged in navigate to Login
 *
 * Currently simplified to navigate to Landing after a delay.
 * Full logic will be implemented with ViewModel in future iterations.
 *
 * @param onNavigateToNext Callback invoked after splash delay completes
 */
@Preview
@Composable
fun SplashScreen(
    onNavigateToNext: () -> Unit = {}
) {
    // Trigger navigation after delay
    LaunchedEffect(Unit) {
        // Simulate splash delay for now
        delay(2000)

        // TODO: Add logic to determine next destination:
        // - Check if user is registered
        // - Check if user is logged in
        // - Navigate accordingly

        onNavigateToNext()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.payten),
                contentDescription = null,
                 modifier = Modifier.size(300.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color.White)){append("Soft")}
                    withStyle(style = SpanStyle(color = Color.Red)){append("POS")}
                },
                fontSize = 24.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.Bold,
                color = Color.White)

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
