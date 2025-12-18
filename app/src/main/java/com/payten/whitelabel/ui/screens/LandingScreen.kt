package com.payten.whitelabel.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.payten.whitelabel.R
import com.payten.whitelabel.ui.components.CustomDialog
import com.payten.whitelabel.ui.theme.AppTheme
import com.payten.whitelabel.ui.theme.MyriadPro
import com.payten.whitelabel.viewmodel.LandingViewModel

private const val TAG = "LandingScreen"

/**
 * Main landing/dashboard screen displayed after successful login.
 *
 * This is the primary screen users see after PIN authentication. It serves as the
 * central hub for initiating transactions and accessing app features.
 *
 * ## Screen Components:
 *
 * ### Visual Elements:
 * - Gradient background with Payten branding
 * - Payten logo with shadow effect
 * - Welcome message with merchant name
 * - Large "New Transaction" button (primary action)
 * - Menu icon in top-right corner
 *
 * ### Terminal Status Monitoring:
 * - Automatically checks terminal status on screen entry via `getTerminalStatus()` API
 * - Monitors for reactivation requirements from backend
 * - Displays dialog if terminal needs reactivation (status="A" or advice="FORCE_REACTIVATION")
 * - Blocks transactions until reactivation is completed
 *
 * ## Lifecycle Behavior:
 *
 * ### On Screen Entry (LaunchedEffect):
 * 1. Calls `viewModel.getTerminalStatus()` to check terminal health
 * 2. Observes `reactivationNeeded` LiveData from ViewModel
 * 3. Shows reactivation dialog if backend requires terminal reactivation
 *
 * ### Reactivation Dialog:
 * - **Title**: "Terminal Reactivation Required"
 * - **Message**: Explains why reactivation is needed
 * - **Action**: "Activate" button navigating to ReactivationScreen
 * - **No Dismiss**: Dialog cannot be dismissed - user must complete reactivation
 *
 * ## Navigation Routes:
 *
 * ### From Landing Screen:
 * - **New Transaction** → `amount_entry` - Start payment flow
 * - **Menu** → `menu` - Access settings, transactions, end-of-day, sign out
 * - **Reactivation Required** → `reactivation` - Terminal reactivation flow
 *
 * ### To Landing Screen (from navigation graph):
 * - After PIN login (returning user)
 * - After registration + PIN setup (new user)
 * - After completing reactivation
 * - After transaction completion (on back navigation)
 *
 * ## State Management:
 *
 * ### ViewModel (LandingViewModel):
 * - `getTerminalStatus()` - Fetches terminal health from backend API
 * - `reactivation: LiveData<Boolean>` - Observed for reactivation requirement
 *
 * ### Local State:
 * - `showReactivationDialog` - Controls reactivation dialog visibility
 *
 * ## UI Layout:
 * ```
 * [Gradient Background]
 *   [Menu Icon - Top Right]
 *   [Centered Content]
 *     - Payten Logo
 *     - Welcome Message ("Welcome, {merchantName}!")
 *     - "New Transaction" Button
 * ```
 *
 * @param onNavigateToTransaction Callback when "New Transaction" clicked - navigates to amount_entry
 * @param onNavigateToMenu Callback when menu icon clicked - navigates to menu screen
 * @param onRequireReactivation Callback when reactivation required - navigates to reactivation screen
 * @param viewModel LandingViewModel (Hilt injected) - handles terminal status checks
 *
 * @see LandingViewModel for terminal status logic
 * @see ReactivationScreen for reactivation flow
 * @see AmountEntryScreen for transaction flow entry
 * @see MenuScreen for menu navigation
 */
@Composable
fun LandingScreen(
    onNavigateToTransaction: () -> Unit = {},
    onNavigateToMenu: () -> Unit = {},
    onRequireReactivation: () -> Unit = {},
    viewModel: LandingViewModel = hiltViewModel()
) {
    val reactivationNeeded by viewModel.reactivation.observeAsState()
    var showReactivationDialog by remember { mutableStateOf(false) }

    Log.d(TAG, "LandingScreen composed, reactivationNeeded=$reactivationNeeded")

    LaunchedEffect(Unit) {
        Log.d(TAG, "Calling getTerminalStatus()")
        viewModel.getTerminalStatus()
    }

    LaunchedEffect(reactivationNeeded) {
        Log.d(TAG, "reactivationNeeded changed to: $reactivationNeeded")
        if (reactivationNeeded == true) {
            Log.d(TAG, "Showing reactivation dialog")
            showReactivationDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
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

        // Reactivation dialog (matches old LandingActivity showDialog for REACTIVATION)
        if (showReactivationDialog) {
            CustomDialog(
                isSuccess = true,
                title = stringResource(R.string.reactivation_message),
                buttonText = stringResource(R.string.reactivation_button),
                onDismiss = {
                    Log.d(TAG, "Reactivation dialog dismissed - starting reactivation flow")
                    showReactivationDialog = false
                    onRequireReactivation()
                }
            )
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
            text = stringResource(R.string.landing_welcome),
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
            text = stringResource(R.string.landing_welcome_description),
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
            text = stringResource(R.string.landing_new_transaction_button),
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
