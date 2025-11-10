package com.payten.nkbm.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.payten.nkbm.ui.navigation.PosNavigation
import com.payten.nkbm.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint


/**
 * Main Activity for the Payten POS application.
 *
 * This is the single entry point for the entre domain.
 *
 * Uses Jetpack Compose for UI and Navigate Compose for navigation.
 * Annotated with @AndroidEntryPoint to allow Hilt dependency injection.
 * */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("DEBUG", "MainActivity started!")

        setContent {
            AppTheme {
                PosNavigation()
            }
        }
    }
}