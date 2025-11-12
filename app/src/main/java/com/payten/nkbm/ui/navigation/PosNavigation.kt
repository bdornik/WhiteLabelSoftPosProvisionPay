package com.payten.nkbm.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.payten.nkbm.config.SupercaseConfig
import com.payten.nkbm.ui.screens.FirstPage
import com.payten.nkbm.ui.screens.PdfViewerScreen
import com.payten.nkbm.ui.screens.RegistrationPage
import com.payten.nkbm.ui.screens.SplashScreen

/**
 * Main navigation component for the Payten POS application.
 *
 * Manages the navigation graph and screen transitions via Navigation Compose.
 *
 * Current implementation handles the following flow: FirstPage->Login/Register.
 *
 * To be expanded further.
 * */
@Composable
fun PosNavigation() {
    //Manages navigation state
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "splash" // Initial screen of the app
    ) {
        composable("splash") {
        SplashScreen(
            onNavigateToNext = {
                // Navigate to landing and remove splash from back stack
                navController.navigate("landing") {
                    popUpTo("splash") { inclusive = true }
                }
            }
        )
    }

        composable("landing") {
            Log.d("Navigation", "Landing screen composable")
            FirstPage(
                onNavigateToLogin = {
                    Log.d("Navigation", "Login button clicked")
                    //Navigates to PIN insertion if the user is registered on this device
                    //This might be moved in the future, if we want to skip the landing page when registered
                    navController.navigate("login")
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
                    Log.d("Navigation", "Back button clicked")
                    navController.popBackStack()
                },
                onNavigateNext = {
                    // TODO: Navigate to PIN setup after registration
                },
                onViewTermsClick = {
                    //Navigates to the PDF viewer if the user clicks the T&C link
                    navController.navigate("pdf_terms")
                }
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
        //Other screens
    }
}