package com.payten.whitelabel.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import at.favre.lib.crypto.bcrypt.BCrypt
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.config.SupercaseConfig
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.screens.FirstPage
import com.payten.whitelabel.ui.screens.LandingScreen
import com.payten.whitelabel.ui.screens.MenuScreen
import com.payten.whitelabel.ui.screens.PdfViewerScreen
import com.payten.whitelabel.ui.screens.PinLoginScreen
import com.payten.whitelabel.ui.screens.PinSetupScreen
import com.payten.whitelabel.ui.screens.RegistrationPage
import com.payten.whitelabel.ui.screens.SettingsScreen
import com.payten.whitelabel.ui.screens.SplashScreen

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
                    //Navigates to registration if the user isnt registered
                    navController.navigate("registration")
                }
            )
        }

        composable("registration") {
            Log.d("Navigation", "Registration screen composable")
            RegistrationPage(
                onNavigateBack = {
                    // This is currently unused since we removed the back button
                    Log.d("Navigation", "Back button clicked")
                    navController.popBackStack()
                },
                onNavigateNext = {
                    // Navigates to the PIN setup screen
                    navController.navigate("pin_setup")
                },
                onViewTermsClick = {
                    // Navigates to the PDF viewer if the user clicks the T&C link
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

                    // Navigates to the landing page after a successful registration process
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
                    // Navigates to the landing page after a successful login
                     navController.navigate("landing") {
                        popUpTo("pin_login") { inclusive = true }
                    }
                },
                onForgotPin = {
                    // TODO: Navigate to Forgot PIN flow
                    // navController.navigate("forgot_pin")
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
                    navController.navigate("transaction")  // TODO: Implement TransactionScreen
                },
                onNavigateToMenu = {
                    // Navigates to the Menu screen from the top-right buton on the landing page.
                    navController.navigate("menu")
                }
            )
        }
        composable("menu"){
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
                    navController.navigate("settings")
                },
                onEndOfDayClick = {
                    navController.navigate("end_of_day")
                },
                onSignOutClick = {
                    navController.navigate("pin_login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable("settings"){
            SettingsScreen(
                sharedPreferences = sharedPreferences,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProfileClick = {
                    navController.navigate("profile")
                },
                onChangePinClick = {
                    navController.navigate("pin_setup")
                },
                onTermsClick = {
                    navController.navigate("pdf_terms")
                }
            )
        }
        //Other screens
    }
}