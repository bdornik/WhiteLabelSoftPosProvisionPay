package com.payten.whitelabel.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.ui.navigation.PosNavigation
import com.payten.whitelabel.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.payten.whitelabel.persistance.SharedPreferencesKeys


/**
 * Main Activity for the Payten POS application.
 *
 * It is an AppCompatActivity that has extended functions, primarily locale management.
 *
 * This is the single entry point for the entire domain.
 *
 * Uses Jetpack Compose for UI and Navigate Compose for navigation.
 * Annotated with @AndroidEntryPoint to allow Hilt dependency injection.
 * */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var sharedPreferences: KsPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val languageIndex = sharedPreferences.pull(SharedPreferencesKeys.LANGUAGE, 2)
        val localeTag = when (languageIndex) {
            0 -> "sl"
            1 -> "en"
            2 -> "sr"
            else -> "sr"
        }

        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(localeTag)
        )

        Log.d("DEBUG", "MainActivity started!")

        enableEdgeToEdge()

        setContent {
            AppTheme {
                PosNavigation(sharedPreferences = sharedPreferences)
            }
        }
    }
}