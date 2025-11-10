package com.payten.nkbm.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.payten.nkbm.ui.navigation.PosNavigation
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

        setContent {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PosNavigation()
                }

        }
    }
}