package com.payten.whitelabel.ui.navigation

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import at.favre.lib.crypto.bcrypt.BCrypt
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.activities.HeadlessPaymentActivity
import com.payten.whitelabel.config.SupercaseConfig
import com.payten.whitelabel.dto.TransactionDetailsDto
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.screens.AmountEntryScreen
import com.payten.whitelabel.ui.screens.CardProcessingScreen
import com.payten.whitelabel.ui.screens.ChangePinVerificationScreen
import com.payten.whitelabel.ui.screens.FirstPage
import com.payten.whitelabel.ui.screens.LandingScreen
import com.payten.whitelabel.ui.screens.MenuScreen
import com.payten.whitelabel.ui.screens.PaymentMethod
import com.payten.whitelabel.ui.screens.PaymentMethodScreen
import com.payten.whitelabel.ui.screens.PdfViewerScreen
import com.payten.whitelabel.ui.screens.PinLoginScreen
import com.payten.whitelabel.ui.screens.PinSetupScreen
import com.payten.whitelabel.ui.screens.ProfileScreen
import com.payten.whitelabel.ui.screens.RegistrationPage
import com.payten.whitelabel.ui.screens.SettingsScreen
import com.payten.whitelabel.ui.screens.SmsVerificationScreen
import com.payten.whitelabel.ui.screens.SplashScreen
import com.payten.whitelabel.ui.screens.TipSelectionScreen
import com.payten.whitelabel.ui.screens.TransactionScreen
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
                onNavigateBack = {
                    navController.popBackStack()
                },
                onPinSetupComplete = { pin ->
                    val encryptedPin = BCrypt
                        .withDefaults()
                        .hashToString(12, pin.toCharArray())
                    sharedPreferences.push(SharedPreferencesKeys.PIN, encryptedPin)
                    sharedPreferences.push(SharedPreferencesKeys.REGISTERED, true)

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
                    // Navigates to the Menu screen from the top-right buton on the landing page.
                    navController.navigate("menu")
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
                    // TODO: Implement TrafficScreen
                    navController.navigate("traffic")
                },
                onSettingsClick = {
                    // Navigates to the settings.
                    navController.navigate("settings")
                },
                onEndOfDayClick = {
                    //TODO: Implement EndOfDayScreen
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

            TipSelectionScreen(
                amountInPare = amountInPare,
                paymentMethod = paymentMethod,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onContinueCard = { tipAmount ->
                    navController.navigate("card_tap/$amountInPare/$tipAmount")
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
                        navController.navigate("transaction_result") {
                            popUpTo("landing") { inclusive = false }
                        }
                        navController.currentBackStackEntry
                            ?.savedStateHandle
                            ?.set("transaction_data", transactionData)
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

            CardProcessingScreen(
                amountInPare = amountInPare,
                tipAmount = tipAmount,
                onNavigateBack = {
                    // Cancel transaction and go back
                    Log.d("Navigation", "Card processing cancelled by user")
                    navController.popBackStack()
                }
            )
        }
        composable("transaction_result") {
            val transactionData = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<TransactionDetailsDto>("transaction_data")

            if (transactionData != null) {
                Log.d("Navigation", "Showing transaction result for: ${transactionData.response}")

                TransactionScreen(
                    transactionData = transactionData,
                    onNavigateHome = {
                        Log.d("Navigation", "Navigating back to landing")
                        // Navigate back to landing screen
                        navController.navigate("landing") {
                            popUpTo("landing") { inclusive = false }
                        }
                    },
                    onShare = {
                        // TODO: Implement share functionality
                        Log.d("Navigation", "Share clicked")
                    },
                    onPrint = {
                        // TODO: Implement print functionality
                        Log.d("Navigation", "Print clicked")
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
            //Other screens
        }
    }
}