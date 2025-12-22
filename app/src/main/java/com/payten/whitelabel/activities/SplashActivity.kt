package com.payten.whitelabel.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import at.favre.lib.crypto.bcrypt.BCrypt
import com.cioccarellia.ksprefs.KsPrefs
import com.fatboyindustrial.gsonjavatime.Converters
import com.google.gson.GsonBuilder
import com.payten.whitelabel.dto.AppToAppRequestDto
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.navigation.PosNavigation
import com.payten.whitelabel.ui.theme.AppTheme
import com.simant.MainApplication
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import javax.inject.Inject

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
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var sharedPreferences: KsPrefs

    private val logger = KotlinLogging.logger {}

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Check if this is an app-to-app request
        if (handleAppToAppRequest()) {
            // App-to-app request handled, don't show normal UI
            return
        }

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

        MainApplication.getInstance().createActivationCodes()

        enableEdgeToEdge()

        setContent {
            AppTheme {
                PosNavigation(sharedPreferences = sharedPreferences)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        handleAppToAppRequest()
    }

    /**
     * Handles app-to-app payment/void requests.
     * Returns true if this was an app-to-app request, false otherwise.
     */
    private fun handleAppToAppRequest(): Boolean {
        val requestJson = intent.getStringExtra("REQUEST_JSON_STRING")

        if (requestJson.isNullOrEmpty()) {
            // Not an app-to-app request
            return false
        }

        logger.info { "App-to-app request detected" }

        try {
            val gson = Converters.registerLocalDateTime(GsonBuilder()).create()
            val dto = gson.fromJson(requestJson, AppToAppRequestDto::class.java)

            logger.info { "App-to-app DTO: ${dto.request}" }

            if (dto?.request?.pin == null || dto.request.amount == null ||
                dto.request.packageName == null || dto.request.pin.isEmpty() ||
                dto.request.amount.isEmpty() || dto.request.packageName.isEmpty()) {

                Toast.makeText(this, "Invalid app-to-app request", Toast.LENGTH_LONG).show()
                finish()
                return true
            }

            // Verify PIN
            val savedPin = sharedPreferences.pull(SharedPreferencesKeys.PIN, "")
            val pinVerifyResult = BCrypt.verifyer().verify(dto.request.pin.toCharArray(), savedPin)
            if (!pinVerifyResult.verified) {
                Toast.makeText(this, "Invalid PIN", Toast.LENGTH_LONG).show()
                finish()
                return true
            }

            // Route based on transaction type
            val amount = dto.request.amount
            val packageName = dto.request.packageName
            val uniqueId = dto.request.merchantUniqueID

            when {
                dto.request.transactionType?.equals("IPS", ignoreCase = true) == true -> {
                    // IPS payment
                    val ipsExists = sharedPreferences.pull(SharedPreferencesKeys.IPS_EXISTS, false)
                    if (ipsExists) {
                        logger.info { "Launching IpsActivity for app-to-app IPS" }
                        val ipsIntent = Intent(this, HeadlessIpsActivity::class.java).apply {
                            putExtra("Amount", amount)
                            putExtra("providedPackageName", packageName)
                            putExtra("uniqueId", uniqueId)
                            addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                        }
                        startActivity(ipsIntent)
                        finish()
                    } else {
                        Toast.makeText(this, "IPS ne postoji na terminalu", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
                dto.request.transactionClass?.equals("void", ignoreCase = true) == true -> {
                    // Void transaction
                    logger.info { "Launching HeadlessVoidActivity for app-to-app void" }
                    val voidIntent = Intent(this, HeadlessVoidActivity::class.java).apply {
                        putExtra("Amount", amount)
                        putExtra("providedPackageName", packageName)
                        putExtra("authorizationCode", dto.request.authorizationCode)
                        putExtra("uniqueId", uniqueId)
                        addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    }
                    startActivity(voidIntent)
                    finish()
                }
                else -> {
                    // Payment transaction
                    logger.info { "Launching HeadlessPaymentActivity for app-to-app payment" }
                    val paymentIntent = Intent(this, HeadlessPaymentActivity::class.java).apply {
                        putExtra("Amount", amount)
                        putExtra("providedPackageName", packageName)
                        putExtra("uniqueId", uniqueId)
                        addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    }
                    startActivity(paymentIntent)
                    finish()
                }
            }

            return true

        } catch (e: Exception) {
            logger.error(e) { "Error parsing app-to-app request" }
            Toast.makeText(this, "Error processing app-to-app request", Toast.LENGTH_LONG).show()
            finish()
            return true
        }
    }
}