package com.payten.nkbm.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.payten.nkbm.ui.screens.LandingPage

/**
 * Main navigation component for the Payten POS application.
 *
 * Manages the navigation graph and screen transitions via Navigation Compose.
 *
 * Current implementation handles the following flow: Landing->Login/Register.
 *
 * To be expanded further.
 * */
@Composable
fun PosNavigation() {
    //Manages navigation state
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "landing" // Initial screen of the app
    ) {
        composable("landing") {
            LandingPage(
                onNavigateToLogin = {
                    //Navigates to PIN insertion if the user is registered on this device
                    //This might be moved in the future, if we want to skip the landing page when registered
                    navController.navigate("login")
                },
                onNavigateToRegister = {
                    //Navigates to registration if the user isnt registered
                    navController.navigate("register")
                }
            )
        }
        //Other screens
    }
}