package com.payten.whitelabel.ui.navigation

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cioccarellia.ksprefs.KsPrefs
import androidx.core.app.ShareCompat
import com.payten.whitelabel.R
import com.payten.whitelabel.activities.HeadlessPaymentActivity
import com.payten.whitelabel.config.SupercaseConfig
import com.payten.whitelabel.dto.TransactionDetailsDto
import com.payten.whitelabel.dto.slip.Slip
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.screens.AmountEntryScreen
import com.payten.whitelabel.ui.screens.CardProcessingScreen
import com.payten.whitelabel.ui.screens.ChangePinVerificationScreen
import com.payten.whitelabel.ui.screens.CustomTipEntryScreen
import com.payten.whitelabel.ui.screens.EndOfDayScreen
import com.payten.whitelabel.ui.screens.FilterScreen
import com.payten.whitelabel.ui.screens.FirstPage
import com.payten.whitelabel.ui.screens.IpsQRScreen
import com.payten.whitelabel.ui.screens.LandingScreen
import com.payten.whitelabel.ui.screens.MenuScreen
import com.payten.whitelabel.ui.screens.PaymentMethod
import com.payten.whitelabel.ui.screens.PaymentMethodScreen
import com.payten.whitelabel.ui.screens.PdfViewerScreen
import com.payten.whitelabel.ui.screens.PinLoginScreen
import com.payten.whitelabel.ui.screens.PinSetupScreen
import com.payten.whitelabel.ui.screens.ProfileScreen
import com.payten.whitelabel.ui.screens.ReactivationScreen
import com.payten.whitelabel.ui.screens.RegistrationPage
import com.payten.whitelabel.ui.screens.SettingsScreen
import com.payten.whitelabel.ui.screens.SmsVerificationScreen
import com.payten.whitelabel.ui.screens.SplashScreen
import com.payten.whitelabel.ui.screens.TipSelectionScreen
import com.payten.whitelabel.ui.screens.TransactionScreen
import com.payten.whitelabel.ui.screens.TransactionsListScreen
import com.payten.whitelabel.ui.states.PaymentUiBridge
import com.payten.whitelabel.utils.AmountUtil.Companion.formatAmount

/**
 * Main navigation component for the Payten POS application.
 *
 * Manages the navigation graph and screen transitions via Navigation Compose.
 *
 * Current implementation handles the following flows:
 * -Splash->FirstPage->Register->PinSetup->Landing->Menu
 * -Splash->PinLogin-------------------------^
 *
 * To be expanded further.
 * */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun PosNavigation(sharedPreferences: KsPrefs) {
    //Manages navigation state
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash" // Initial screen of the app
    ) {
        composable("splash") {
            SplashScreen(
                sharedPreferences = sharedPreferences,
                onNavigateToNext = { destination ->
                    navController.navigate(destination) {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("first") {
            Log.d("Navigation", "First screen composable")
            FirstPage(
                onNavigateToLogin = {
                    Log.d("Navigation", "Login button clicked")
                    //Navigates to PIN insertion if the user is registered on this device
                    //This might be moved in the future, if we want to skip the landing page when registered
                    navController.navigate("pin_login")
                },
                onNavigateToRegister = {
                    Log.d("Navigation", "Register button clicked - navigating...")
                    //Navigates to registration if the user isn't registered.
                    navController.navigate("registration")
                }
            )
        }

        composable("registration") {
            Log.d("Navigation", "Registration screen composable")
            RegistrationPage(
                onNavigateBack = {
                    // This is currently unused since we removed the back button.
                    Log.d("Navigation", "Back button clicked")
                    navController.popBackStack()
                },
                onNavigateNext = {
                    // Navigates to the PIN setup screen.
                    navController.navigate("pin_setup")
                },
                onViewTermsClick = {
                    // Navigates to the PDF viewer if the user clicks the T&C link.
                    navController.navigate("pdf_terms")
                },
                sharedPreferences = sharedPreferences
            )
        }
        composable("pdf_terms") {
            PdfViewerScreen(
                pdfUrl = SupercaseConfig.termsConditionURL,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable("pin_setup") {
            PinSetupScreen(
                sharedPreferences = sharedPreferences,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPinSetupComplete = {
                    // Navigates to the landing page after a successful registration process.
                    navController.navigate("landing") {
                        popUpTo("landing") { inclusive = true }
                    }
                }
            )
        }
        composable("pin_login") {
            PinLoginScreen(
                sharedPreferences = sharedPreferences,
                onLoginSuccess = {
                    // Navigates to the landing page after a successful login.
                    navController.navigate("landing") {
                        popUpTo("pin_login") { inclusive = true }
                    }
                },
                onForgotPin = {
                    // Navigates to the PIN verification screen to choose a new PIN.
                    navController.navigate("change_pin_verification")
                },
                onLoginFailed = {
                    // App blocked - reset to splash
                    navController.navigate("splash") {
                        popUpTo(0)
                    }
                }
            )
        }
        composable("landing") {
            LandingScreen(
                onNavigateToTransaction = {
                    // Navigates to the amount entry screen.
                    navController.navigate("amount_entry")
                },
                onNavigateToMenu = {
                    // Navigates to the Menu screen from the top-right button on the landing page.
                    navController.navigate("menu")
                },
                onRequireReactivation = {
                    // Terminal requires reactivation - navigate to reactivation screen
                    // Clear back stack
                    Log.d("Navigation", "Navigating to reactivation screen")
                    navController.navigate("reactivation") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("reactivation") {
            ReactivationScreen(
                sharedPreferences = sharedPreferences,
                onReactivationComplete = {
                    // Navigate to PIN setup to set new PIN
                    // Clear back stack
                    Log.d("Navigation", "Reactivation complete - navigating to pin_setup")
                    navController.navigate("pin_setup") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("menu") {
            val merchantName = sharedPreferences.pull(
                SharedPreferencesKeys.MERCHANT_NAME,
                ""
            )

            val merchantAddress = sharedPreferences.pull(
                SharedPreferencesKeys.MERCHANT_ADDRESS,
                ""
            )

            val merchantPlaceName = sharedPreferences.pull(
                SharedPreferencesKeys.MERCHANT_PLACE_NAME,
                ""
            )

            val fullAddress = if (merchantAddress.isNotBlank() && merchantPlaceName.isNotBlank()) {
                "$merchantAddress, $merchantPlaceName"
            } else merchantAddress.ifBlank {
                merchantPlaceName
            }

            MenuScreen(
                merchantName = merchantName,
                merchantAddress = fullAddress,
                onClose = {
                    navController.popBackStack()
                },
                onTrafficClick = {
                    navController.navigate("traffic")
                },
                onSettingsClick = {
                    // Navigates to the settings.
                    navController.navigate("settings")
                },
                onEndOfDayClick = {
                    navController.navigate("end_of_day")
                },
                onSignOutClick = {
                    // Navigates to login after the user signs out.
                    navController.navigate("pin_login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("end_of_day"){
            EndOfDayScreen(
                sharedPreferences = sharedPreferences,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                sharedPreferences = sharedPreferences,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProfileClick = {
                    // Navigates to the profile details screen.
                    navController.navigate("profile")
                },
                onChangePinClick = {
                    // Navigates to the PIN verification screen.
                    navController.navigate("change_pin_verification")
                },
                onTermsClick = {
                    // Opens the T&C PDF file.
                    navController.navigate("pdf_terms")
                }
            )
        }
        composable("profile") {
            ProfileScreen(
                sharedPreferences = sharedPreferences,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable("change_pin_verification") {
            ChangePinVerificationScreen(
                sharedPreferences = sharedPreferences,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onVerificationSuccess = {
                    // Navigates to SMS activation code screen.
                    navController.navigate("pin_setup") {
                        popUpTo("change_pin_verification") { inclusive = true }
                    }
                }
            )
        }
        composable("send_sms") {
            SmsVerificationScreen(
                sharedPreferences = sharedPreferences,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onVerificationComplete = {
                    // Navigates to PIN setup screen to change the PIN.
                    navController.navigate("pin_setup")
                }
            )
        }
        composable("amount_entry") {
            AmountEntryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onContinue = { amountInPare ->
                    // Navigate to payment method choice with the desired amount.
                    navController.navigate("payment_method/$amountInPare")
                }
            )
        }
        composable(
            "payment_method/{amountInPare}",
            listOf(navArgument("amountInPare") { type = NavType.LongType })
        ) { backStackEntry ->
            val amountInPare = backStackEntry.arguments?.getLong("amountInPare") ?: 0L

            PaymentMethodScreen(
                amountInPare = amountInPare,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onContinue = { method ->
                    // Navigate to tip selection screen.
                    navController.navigate("tip_selection/$amountInPare/${method.name}")
                }
            )
        }
        composable(
            "tip_selection/{amountInPare}/{paymentMethod}",
            listOf(
                navArgument("amountInPare") { type = NavType.LongType },
                navArgument("paymentMethod") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val amountInPare = backStackEntry.arguments?.getLong("amountInPare") ?: 0L
            val paymentMethodStr = backStackEntry.arguments?.getString("paymentMethod") ?: "CARD"
            val paymentMethod = PaymentMethod.valueOf(paymentMethodStr)

            val savedStateHandle = backStackEntry.savedStateHandle
            val customTipResult = savedStateHandle.getLiveData<Long>("custom_tip_result").observeAsState()

            TipSelectionScreen(
                amountInPare = amountInPare,
                paymentMethod = paymentMethod,
                externalCustomTip = customTipResult.value,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onCustomTipClick = {
                    // Navigate to custom tip screen.
                    navController.navigate("custom_tip")
                },
                onContinueCard = { tipAmount ->
                    navController.navigate("card_tap/$amountInPare/$tipAmount")
                },
                onContinueIps = { totalAmount ->
                    navController.navigate("ips_qr/$totalAmount")
                },
                onTransactionComplete = { transactionData ->
                    navController.navigate("transaction_result") {
                        popUpTo("landing") { inclusive = false }
                    }
                    navController.currentBackStackEntry
                        ?.savedStateHandle
                        ?.set("transaction_data", transactionData)
                }
            )
        }
        composable("custom_tip") {
            CustomTipEntryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onContinue = { tipAmount ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("custom_tip_result", tipAmount)
                    navController.popBackStack()
                }
            )
        }
        composable(
            "ips_qr/{totalAmount}",
            listOf(navArgument("totalAmount") { type = NavType.LongType })
        ) { backStackEntry ->
            val totalAmount = backStackEntry.arguments?.getLong("totalAmount") ?: 0L

            IpsQRScreen(
                amount = totalAmount.toString(),
                sharedPreferences = sharedPreferences,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onTransactionComplete = { isSuccess, statusCode, message, e2eRef ->
                    // Create transaction data
                    val transactionData = TransactionDetailsDto(
                        aid = "",
                        applicationLabel = "",
                        authorizationCode = message,
                        bankName = "",
                        cardNumber = "",
                        dateTime = org.threeten.bp.LocalDateTime.now().toString(),
                        merchantId = sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_MERCHANT_ID, ""),
                        merchantName = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME, ""),
                        message = message,
                        operationName = "Prodaja",
                        response = statusCode,
                        rrn = e2eRef,
                        code = "",
                        status = if (isSuccess) "A" else "F",
                        terminalId = sharedPreferences.pull(SharedPreferencesKeys.IPS_SERVICE_TERMINAL_ID, ""),
                        amount = (totalAmount / 100.0).toString(),
                        isIps = true,
                        sdkStatus = null,
                        billStatus = null,
                        color = -1,
                        recordId = "",
                        listName = "",
                        tipAmount = "0.0"
                    )

                    // Save to landing and navigate to result
                    navController.getBackStackEntry("landing")
                        .savedStateHandle["transaction_data"] = transactionData

                    navController.navigate("transaction_result") {
                        popUpTo("landing") { inclusive = false }
                    }
                }
            )
        }
        composable(
            "card_tap/{amountInPare}/{tipAmount}",
            listOf(
                navArgument("amountInPare") {type = NavType.LongType},
                navArgument("tipAmount") { type = NavType.LongType}
            )
        ){ backStackEntry ->
            val amountInPare = backStackEntry.arguments?.getLong("amountInPare") ?: 0L
            val tipAmount = backStackEntry.arguments?.getLong("tipAmount") ?: 0L
            val context = LocalContext.current

            val transactionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val transactionData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        result.data?.getSerializableExtra("transaction_data", TransactionDetailsDto::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        result.data?.getSerializableExtra("transaction_data") as? TransactionDetailsDto
                    }

                    if (transactionData != null) {
                        // Save data to Landing.
                        navController.getBackStackEntry("landing")
                            .savedStateHandle["transaction_data"] = transactionData

                        // Navigate to the transaction result.
                        navController.navigate("transaction_result") {
                            popUpTo("landing") { inclusive = false }
                        }
                    }
                } else {
                    navController.popBackStack()
                }
            }

            LaunchedEffect(Unit) {
                val totalAmount = amountInPare + tipAmount
                val intent = Intent(context, HeadlessPaymentActivity::class.java).apply {
                    putExtra("Amount", amountInPare.toString())
                    putExtra("Tip", tipAmount.toString())
                    putExtra("TotalAmount", formatAmount(totalAmount))
                }
                transactionLauncher.launch(intent)
            }

            // Reset processing state when navigating away from this screen
            DisposableEffect(Unit) {
                onDispose {
                    PaymentUiBridge.reset()
                }
            }

            CardProcessingScreen(
                amountInPare = amountInPare,
                tipAmount = tipAmount,
                onNavigateBack = {
                    // Cancel transaction and go back
                    Log.d("Navigation", "Card processing cancelled by user")
                    PaymentUiBridge.reset()
                    navController.popBackStack()
                }
            )
        }
        composable("transaction_result") {
            val transactionData = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<TransactionDetailsDto>("transaction_data")
            val context = LocalContext.current
            val activity = context as? Activity

            if (transactionData != null) {
                Log.d("Navigation", "Showing transaction result for: ${transactionData.response}")

                TransactionScreen(
                    transactionData = transactionData,
                    onNavigateHome = {
                        Log.d("Navigation", "Navigating back from transaction result")
                        // Navigate back to previous screen (landing or traffic)
                        navController.popBackStack()
                    },
                    onShare = {
                        Log.d("Navigation", "Share clicked")
                        activity?.let { shareTransaction(it, transactionData) }
                    },
                    onPrint = {
                        Log.d("Navigation", "Print clicked")
                        activity?.let { printTransaction(it, transactionData) }
                    }
                )
            } else {
                Log.e("Navigation", "No transaction data found - returning to landing")
                LaunchedEffect(Unit) {
                    navController.navigate("landing") {
                        popUpTo("landing") { inclusive = false }
                    }
                }
            }
        }
        composable("traffic"){
            TransactionsListScreen(
                sharedPreferences = sharedPreferences,
                navController = navController,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onTransactionDetailsClick = { transaction ->
                    Log.d("Navigation", "Transaction details clicked: ${transaction.recordId}")

                    // Format dateTime properly - convert LocalDateTime to ISO string format
                    val formattedDateTime = transaction.transactionDate?.let { localDateTime ->
                        try {
                            val formatter = org.threeten.bp.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
                            localDateTime.format(formatter)
                        } catch (_: Exception) {
                            localDateTime.toString()
                        }
                    } ?: ""

                    // Convert TransactionDto to TransactionDetailsDto
                    val transactionData = TransactionDetailsDto(
                        aid = transaction.applicationId,
                        applicationLabel = transaction.applicationLabel ?: "",
                        authorizationCode = transaction.authorizationCode ?: "",
                        bankName = "",
                        cardNumber = transaction.maskedPAN ?: "",
                        dateTime = formattedDateTime,
                        merchantId = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_MERCHANT_ID, ""),
                        merchantName = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME, ""),
                        message = transaction.screenMessage ?: "",
                        operationName = if (transaction.operationName.isNullOrEmpty()) "Prodaja" else transaction.operationName!!,
                        response = transaction.responseCode ?: "",
                        rrn = transaction.creaditTransferIdentificator ?: "",
                        code = transaction.recordId,
                        status = transaction.statusCode ?: "",
                        terminalId = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_TERMINAL_ID, ""),
                        amount = transaction.amount,
                        isIps = transaction.isIps ?: false,
                        sdkStatus = transaction.status,
                        billStatus = null,
                        color = -1,
                        recordId = transaction.recordId,
                        listName = "",
                        tipAmount = transaction.tipAmount
                    )

                    Log.d("Navigation", "Transaction data created: merchantId=${transactionData.merchantId}, operationName=${transactionData.operationName}, dateTime=${transactionData.dateTime}")

                    // Navigate and pass data via navigation arguments
                    navController.currentBackStackEntry?.savedStateHandle?.set("transaction_data", transactionData)
                    navController.navigate("transaction_details_from_list")
                },
                onFilterClick = {
                    // Navigate to filter screen.
                    navController.navigate("filter")
                }
            )
        }
        composable("filter"){
            FilterScreen(
                sharedPreferences = sharedPreferences,
                onApplyFilter = {
                    // Set flag to trigger filter refresh in TransactionsListScreen
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("filter_applied", true)
                    navController.popBackStack()
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable("transaction_details_from_list") {
            val transactionData = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<TransactionDetailsDto>("transaction_data")
            val context = LocalContext.current
            val activity = context as? Activity

            if (transactionData != null) {
                Log.d("Navigation", "Showing transaction details from list: ${transactionData.recordId}")

                TransactionScreen(
                    transactionData = transactionData,
                    onNavigateHome = {
                        Log.d("Navigation", "Navigating back to traffic list")
                        // Explicitly pop back to traffic route
                        navController.popBackStack("traffic", inclusive = false)
                    },
                    onShare = {
                        Log.d("Navigation", "Share clicked")
                        activity?.let { shareTransaction(it, transactionData) }
                    },
                    onPrint = {
                        Log.d("Navigation", "Print clicked")
                        activity?.let { printTransaction(it, transactionData) }
                    }
                )
            } else {
                Log.e("Navigation", "No transaction data found - returning to traffic")
                LaunchedEffect(Unit) {
                    navController.popBackStack("traffic", inclusive = false)
                }
            }
        }
        //Other screens
    }
}

/**
 * Print transaction receipt via Bluetooth printer
 */
@SuppressLint("DefaultLocale")
private fun printTransaction(
    activity: Activity,
    transactionData: TransactionDetailsDto
) {
    Log.d("Navigation", "printTransaction called for transaction: ${transactionData.recordId}")
    try {
        // Determine card type
        val cardType = when {
            transactionData.applicationLabel?.contains("visa", ignoreCase = true) == true -> "VISA"
            transactionData.applicationLabel?.contains("master", ignoreCase = true) == true -> "MASTERCARD"
            else -> ""
        }

        // Format dateTime
        val dateTimeFormatter = org.threeten.bp.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
        val parsedDateTime = try {
            org.threeten.bp.LocalDateTime.parse(transactionData.dateTime)
        } catch (_: Exception) {
            org.threeten.bp.LocalDateTime.now()
        }
        val formattedDate = parsedDateTime.format(dateTimeFormatter)

        // Format amount
        val formattedAmount = try {
            val amount = transactionData.amount.toDouble()
            val tip = if (transactionData.tipAmount != "0.0" && transactionData.tipAmount.isNotEmpty()) {
                transactionData.tipAmount.toDouble()
            } else {
                0.0
            }
            val total = amount + tip
            String.format("%.2f ${SupercaseConfig.CURRENCY_STRING}", total)
        } catch (_: Exception) {
            "${transactionData.amount} ${SupercaseConfig.CURRENCY_STRING}"
        }

        // Determine status text
        val statusText = when {
            transactionData.response.equals("00", ignoreCase = true) ->
                activity.resources.getString(R.string.transaction_status_accepted_label).uppercase()
            transactionData.response.equals("06", ignoreCase = true) && transactionData.isIps ->
                activity.resources.getString(R.string.transaction_status_canceled).uppercase()
            else ->
                activity.resources.getString(R.string.transaction_status_reversed_message).uppercase()
        }

        // Build print text with special formatting codes
        val printText = if (transactionData.isIps) {
            // IPS transaction format
            """
                [L]
                [C]================================
                [L]
                [C]${activity.resources.getString(R.string.transaction_receipt_amount)}: $formattedAmount
                [L]
                [L]${activity.resources.getString(R.string.share_date)} $formattedDate
                [L]${activity.resources.getString(R.string.label_transaction_status)} $statusText
                [L]${activity.resources.getString(R.string.label_transaction_e2e_ips)} ${transactionData.rrn}
                [L]${activity.resources.getString(R.string.label_transaction_merchant_id)} ${transactionData.merchantId}
                [L]${activity.resources.getString(R.string.label_transaction_terminal_id)} ${transactionData.terminalId}
                [L]${activity.resources.getString(R.string.transaction_receipt_merchant)} ${transactionData.merchantName}
                [L]${activity.resources.getString(R.string.transaction_receipt_operation)} ${transactionData.operationName}
                [L]${activity.resources.getString(R.string.transaction_receipt_message)} ${transactionData.message}
                [L]
                [C]================================
            """.trimIndent()
        } else {
            // Card transaction format
            """
                [L]
                [C]================================
                [L]
                [C]${activity.resources.getString(R.string.transaction_receipt_amount)}: $formattedAmount
                [L]
                [L]${activity.resources.getString(R.string.share_date)} $formattedDate
                [L]${activity.resources.getString(R.string.label_transaction_status)} $statusText
                [L]${activity.resources.getString(R.string.label_transaction_merchant_id)} ${transactionData.merchantId}
                [L]${activity.resources.getString(R.string.label_transaction_terminal_id)} ${transactionData.terminalId}
                [L]${activity.resources.getString(R.string.transaction_receipt_merchant)} ${transactionData.merchantName}
                [L]${activity.resources.getString(R.string.transaction_receipt_card_number)} ${transactionData.cardNumber}
                [L]${activity.resources.getString(R.string.transaction_receipt_auth_code)} ${transactionData.authorizationCode}
                [L]${activity.resources.getString(R.string.transaction_receipt_operation)} ${transactionData.operationName}
                [L]${activity.resources.getString(R.string.transaction_receipt_response)} ${transactionData.response}
                [L]${activity.resources.getString(R.string.transaction_receipt_message)} ${transactionData.message}
                [L]
                [C]<b>$cardType</b>
                [L]
                [C]================================
            """.trimIndent()
        }

        // Get selected printer device from preferences (if any)
        val selectedDevice: com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection? = null

        Log.d("Navigation", "Print text prepared, length=${printText.length}, calling PrintUtil...")
        // Print via Bluetooth
        com.payten.whitelabel.utils.printer.PrintUtil.printBluetooth(
            activity,
            selectedDevice,
            printText
        )

        Log.d("Navigation", "Print initiated successfully")
    } catch (e: Exception) {
        Log.e("Navigation", "Error printing transaction: ${e.message}", e)
    }
}

/**
 * Share transaction receipt via Android share sheet
 */
@SuppressLint("DefaultLocale")
private fun shareTransaction(
    activity: Activity,
    transactionData: TransactionDetailsDto
) {
    try {
        // Format dateTime
        val dateTimeFormatter = org.threeten.bp.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val parsedDateTime = try {
            org.threeten.bp.LocalDateTime.parse(transactionData.dateTime)
        } catch (_: Exception) {
            org.threeten.bp.LocalDateTime.now()
        }
        val formattedDate = parsedDateTime.format(dateTimeFormatter)

        // Format amount
        val formattedAmount = try {
            val amount = transactionData.amount.toDouble()
            val tip = if (transactionData.tipAmount != "0.0" && transactionData.tipAmount.isNotEmpty()) {
                transactionData.tipAmount.toDouble()
            } else {
                0.0
            }
            val total = amount + tip
            String.format("%.2f", total)
        } catch (_: Exception) {
            transactionData.amount
        }

        // Format base amount
        val formattedBaseAmount = try {
            val amount = transactionData.amount.toDouble()
            String.format("%.2f", amount)
        } catch (_: Exception) {
            transactionData.amount
        }

        // Format tip amount
        val formattedTipAmount = try {
            val tip = if (transactionData.tipAmount != "0.0" && transactionData.tipAmount.isNotEmpty()) {
                transactionData.tipAmount.toDouble()
            } else {
                0.0
            }
            String.format("%.2f", tip)
        } catch (_: Exception) {
            "0.00"
        }

        // Determine status text
        val statusText = when {
            transactionData.response.equals("00", ignoreCase = true) ->
                activity.resources.getString(R.string.transaction_status_accepted_label).uppercase()
            transactionData.response.equals("06", ignoreCase = true) && transactionData.isIps ->
                activity.resources.getString(R.string.transaction_status_canceled).uppercase()
            else ->
                activity.resources.getString(R.string.transaction_status_reversed_message).uppercase()
        }

        val shareText = if (transactionData.isIps) {
            // IPS transaction format
            """
                ${activity.resources.getString(R.string.transaction_receipt_amount)}: $formattedAmount ${SupercaseConfig.CURRENCY_STRING}
                ${activity.resources.getString(R.string.share_date)}: $formattedDate
                ${activity.resources.getString(R.string.label_transaction_status)} $statusText
                ${activity.resources.getString(R.string.label_transaction_e2e_ips)} ${transactionData.rrn}
                ${activity.resources.getString(R.string.label_transaction_merchant_id)} ${transactionData.merchantId}
                ${activity.resources.getString(R.string.label_transaction_terminal_id)} ${transactionData.terminalId}
                ${activity.resources.getString(R.string.transaction_receipt_merchant)} ${transactionData.merchantName}
                ${activity.resources.getString(R.string.transaction_receipt_operation)} ${transactionData.operationName}
                ${activity.resources.getString(R.string.transaction_receipt_message)} ${transactionData.message}
            """.trimIndent()
        } else {
            // Card transaction format using Slip class
            val cardType = when {
                transactionData.applicationLabel?.contains("visa", ignoreCase = true) == true -> "VISA"
                transactionData.applicationLabel?.contains("master", ignoreCase = true) == true -> "MASTERCARD"
                else -> ""
            }

            val slip = Slip(
                statusText,
                "$formattedAmount ${SupercaseConfig.CURRENCY_STRING}",
                "$formattedBaseAmount ${SupercaseConfig.CURRENCY_STRING}",
                "$formattedTipAmount ${SupercaseConfig.CURRENCY_STRING}",
                formattedDate,
                transactionData.merchantId,
                transactionData.terminalId,
                transactionData.merchantName,
                transactionData.cardNumber!!,
                transactionData.authorizationCode!!,
                transactionData.operationName,
                transactionData.response!!,
                transactionData.message!!,
                "", // installment
                "", // uniqueId
                cardType,
                activity.resources
            )

            slip.toString()
        }

        // Share via Android share sheet
        ShareCompat.IntentBuilder(activity)
            .setText(shareText)
            .setType("text/plain")
            .setChooserTitle(activity.resources.getString(R.string.share_title))
            .startChooser()

        Log.d("Navigation", "Share initiated successfully")
    } catch (e: Exception) {
        Log.e("Navigation", "Error sharing transaction: ${e.message}", e)
    }
}