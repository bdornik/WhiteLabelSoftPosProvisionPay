package com.payten.nkbm.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.payten.nkbm.R
import com.payten.nkbm.ui.theme.AppTheme
import com.payten.nkbm.ui.theme.MyriadPro
/**
 * Initial screen of the Payten POS application.
 *
 * Contains Payten and SoftPOS logos, motto and a button for proceeding to registration.
 *
 * @param onNavigateToLogin Callback invoked when user navigates to login (will probably be removed).
 * @param onNavigateToRegister Callback invoked when user navigates to registration.
 * */
@Composable
fun LandingPage(onNavigateToLogin: () -> Unit, onNavigateToRegister: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize()
    ){
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.onBackground)
        )
        Image(
            painter = painterResource(id = R.drawable.vortex),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(500.dp). align(Alignment.BottomCenter),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(
                modifier = Modifier.height(90.dp)
            )

            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onPrimary)){append("Soft")}
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary)){append("POS")}
                },
                fontSize = 24.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )

            Spacer(
                modifier = Modifier.height(82.dp)
            )

            Column(
                modifier = Modifier.size(210.dp, 92.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.payten),
                    contentDescription = null,
                    modifier = Modifier.size(210.dp, 64.dp),
                    contentScale = ContentScale.FillWidth
                )
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = painterResource(id = R.drawable.below_payten),
                    contentDescription = null,
                    modifier = Modifier.width(174.dp),
                    contentScale = ContentScale.FillWidth
                )
            }

            Spacer(modifier = Modifier.height(125.dp))

            Column(modifier = Modifier.width(297.dp).align(Alignment.CenterHorizontally))
            {
                Column(modifier = Modifier.height(89.dp)) {
                    Text(
                        text= "Brzo. Bezbedno. Bilo kada.",
                        color = MaterialTheme.colorScheme.onTertiary,
                        fontSize = 24.sp,
                        fontFamily = MyriadPro,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center)

                    Spacer (modifier = Modifier.height(22.dp))
                    Text(
                        text = "Jednostavan način prihvatanja plaćanja karticama i Flik računima.",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 16.sp,
                        fontFamily = MyriadPro,
                        textAlign = TextAlign.Center)
                }

                Spacer(modifier = Modifier.height(96.dp))

                Button(

                    onClick = {
                        Log.d("LandingScreen", "Register button clicked")
                        onNavigateToRegister()
                              },
                    modifier = Modifier.size(297.dp, 51.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "POSTANITE KORISNIK",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
@Preview
@Composable
fun LandPreview()
{
    AppTheme{
        LandingPage(onNavigateToLogin = {}, onNavigateToRegister = {})
    }

}