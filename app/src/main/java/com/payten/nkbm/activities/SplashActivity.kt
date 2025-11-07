package com.payten.nkbm.activities

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Observer
import at.favre.lib.crypto.bcrypt.BCrypt
import com.cioccarellia.ksprefs.KsPrefs
import com.fatboyindustrial.gsonjavatime.Converters
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.gson.GsonBuilder
import com.payten.nkbm.R
import com.payten.nkbm.config.SupercaseConfig
import com.payten.nkbm.databinding.ActivitySplashBinding
import com.payten.nkbm.dto.AppToAppRequestDto
import com.payten.nkbm.persistance.SharedPreferencesKeys
import com.payten.nkbm.viewmodel.SampleViewModel
import com.payten.nkbm.viewmodel.SplashViewModel
import com.simant.MainApplication
import com.trello.rxlifecycle4.components.support.RxAppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import java.util.*
import javax.inject.Inject


@AndroidEntryPoint
class SplashActivity : RxAppCompatActivity() {
    private val logger = KotlinLogging.logger {}

    private lateinit var binding: ActivitySplashBinding

    @Inject
    lateinit var sharedPreferences: KsPrefs

    private var appUpdateManager: AppUpdateManager? = null
    private val UPDATE_REQUEST_CODE = 123
    private val updateType: Int = AppUpdateType.IMMEDIATE

    private val TAG = "SplashActivity"

    var providePackageName = ""


    lateinit var splashModel: SplashViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val model2: SplashViewModel by viewModels()
        splashModel = model2

        val lang = sharedPreferences.pull(SharedPreferencesKeys.LANGUAGE, 0);
        logger.info { "Lang: " + lang }
        if (lang == 1) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(
                    "en"
                )
            )
        } else {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(
                    "sl"
                )
            )
            sharedPreferences.push(SharedPreferencesKeys.LANGUAGE, 0)
        }

        appUpdateManager = AppUpdateManagerFactory.create(this)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        logger.info { "PRE POZIVA" }
        MainApplication.getInstance().createActivationCodes();
        logger.info { "POSLE POZIVA" }


        if (SupercaseConfig.PLAYSTORE_CHECK && !verifyPlaystore(applicationContext)) {
            Toast.makeText(
                applicationContext,
                getString(R.string.store_verify),
                Toast.LENGTH_LONG
            ).show()
            finishAffinity()
            return
        }

        if (checkForRoot()) {
            Toast.makeText(
                applicationContext,
                getString(R.string.root_detection),
                Toast.LENGTH_LONG
            ).show()
            finishAffinity()
            return
        }

        val model: SampleViewModel by viewModels()
        model.getValues().observe(this, Observer { values ->
            logger.info { "Loaded values" }
        })

        if (intent.hasExtra("providedPackageName"))
            providePackageName = intent.getStringExtra("providedPackageName").toString()


        callInAppUpdate()



        splashModel.getTokenSuccessfull.observe(this, Observer { res ->
            if (res) {
                val gson = Converters.registerLocalDateTime(GsonBuilder()).create()
                val dto = gson.fromJson(
                    intent.getStringExtra("REQUEST_JSON_STRING"),
                    AppToAppRequestDto::class.java
                )
                logger.info { "Dto: ${dto.request}" }


                if (dto == null || dto.request.pin == null || dto.request.amount == null || dto.request.packageName == null || dto.request.pin.isEmpty() || dto.request.amount.isEmpty() || dto.request.packageName.isEmpty()) {
                    Toast.makeText(
                        applicationContext,
                        "Prosledjeni podaci nisu validni",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val encryptedPin = sharedPreferences.pull(
                        SharedPreferencesKeys.PIN,
                        ""
                    )
                    val result: BCrypt.Result =
                        BCrypt.verifyer().verify(dto.request.pin.toCharArray(), encryptedPin)
                    if (result.verified) {
                        var amount = dto.request.amount
                        if (dto.request.transactionType.equals("IPS")) {
                            if (sharedPreferences.pull(SharedPreferencesKeys.IPS_EXISTS)) {

                                val intent = Intent(this, IpsActivity::class.java)
                                intent.putExtra("Amount", amount)
                                intent.putExtra("providedPackageName", dto.request.packageName)
                                intent.putExtra("uniqueId", dto.request.merchantUniqueID)
                                intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                                startActivity(intent)
                            } else {
                                Toast.makeText(
                                    applicationContext,
                                    "IPS ne postoji na terminalu",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            //
                            if (dto.request.transactionClass != null && dto.request.transactionClass.equals(
                                    "void",
                                    true
                                )
                            ) {
                                // void
                                val intent = Intent(this, VoidActivity::class.java)
                                intent.putExtra("Amount", amount)
                                intent.putExtra("providedPackageName", dto.request.packageName)
                                intent.putExtra("authorizationCode", dto.request.authorizationCode)
                                intent.putExtra("uniqueId", dto.request.merchantUniqueID)
                                intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                                startActivity(intent)
                            } else {
                                val intent = Intent(this, PosActivity::class.java)
                                intent.putExtra("Amount", amount)
                                intent.putExtra("providedPackageName", dto.request.packageName)
                                intent.putExtra("uniqueId", dto.request.merchantUniqueID)
                                intent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                                startActivity(intent)

                            }
                        }


                    } else {
                        Toast.makeText(
                            applicationContext,
                            "Pin nije validan",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } else {
                Toast.makeText(
                    applicationContext,
                    "Terminal nije validan",
                    Toast.LENGTH_LONG
                ).show()
            }

            finish()

        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                finish()
            } else {
                checkApp()
            }
        }
    }

    private fun callInAppUpdate() {
        Log.i(TAG, "Cheking for update!")
        appUpdateManager?.getAppUpdateInfo()?.addOnSuccessListener { appUpdateInfo ->
            Log.i(TAG, "Listener")
            if (appUpdateInfo.updateAvailability() === UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                try {
                    appUpdateManager?.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this,
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: IntentSender.SendIntentException) {
                    checkApp()
                    e.printStackTrace()
                }
            } else {
                Log.i(TAG, "pp is up to date")
                checkApp()
            }
        }?.addOnFailureListener { it: Exception? ->
            Log.e(
                TAG,
                "Failed to check for update:" + it?.message
            )
            checkApp()
        }


    }


    private fun checkApp() {
        if (sharedPreferences.pull(SharedPreferencesKeys.REGISTERED, false)) {
            if (sharedPreferences.pull(SharedPreferencesKeys.IS_LOGGED_IN, false)) {

                if (intent != null && intent.getStringExtra("REQUEST_JSON_STRING") != null) {
                    splashModel.refreshData(false)
                } else {
                    val intent = Intent(applicationContext, PinActivity::class.java)
                    intent.putExtra(PinActivity.ACTION, PinActivity.ACTION_CHECK_PIN_LOGIN)
                    startActivity(intent)
                    finish()
                }
            } else {
                startRegistrationActivity()
            }
        } else {
            startIntroActivity()
        }
    }

    override fun onResume() {
        super.onResume()
        handleDarkLightMode()


        val appUpdateManager = AppUpdateManagerFactory.create(this@SplashActivity)
        appUpdateManager.appUpdateInfo.addOnSuccessListener { it: AppUpdateInfo ->
            if (it.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        it,
                        AppUpdateType.IMMEDIATE,
                        this,
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(TAG, e.message!!)
                }
            }
        }.addOnFailureListener { it: Exception? ->
            Log.e(
                TAG,
                "Failed to check for update:" + it?.message
            )
            if (!providePackageName.isEmpty())
                checkApp()
        }
    }

    private fun handleDarkLightMode() {
        // set dark theme if selected
        if (sharedPreferences.pull(SharedPreferencesKeys.IS_DARK_MODE, false)) {

        }
    }

    private fun startRegistrationActivity() {
        startActivity(Intent(this, RegistrationActivity::class.java))
        finish()
    }

    private fun startIntroActivity() {
        startActivity(Intent(this, IntroActivity::class.java))
        finish()
    }

    private fun checkForRoot(): Boolean {
        return false
//        val rootBeer = RootBeer(applicationContext)
//        if (rootBeer.isRooted) {
//            //we found indication of root
//            return true;
//        } else {
//            //we didn't find indication of root, check magisk
//            return checkForMagisk()
//        }
    }

//    private fun checkForMagisk(): Boolean {
//        val pm = packageManager
//        @SuppressLint("QueryPermissionsNeeded") val installedPackages = pm.getInstalledPackages(0)
//
//        for (i in installedPackages.indices) {
//            val info = installedPackages[i]
//            val appInfo: ApplicationInfo = info.applicationInfo
//            val nativeLibraryDir: String = appInfo.nativeLibraryDir
//            val packageName: String = appInfo.packageName
//            val f = File("$nativeLibraryDir/libstub.so")
//            if (f.exists()) {
//                return true
//            }
//        }
//
//        return false
//    }

    fun verifyPlaystore(context: Context): Boolean {
        // A list with valid installers package name
        val validInstallers: List<String> =
            ArrayList(Arrays.asList("com.android.vending", "com.google.android.feedback"))

        // The package name of the app that has installed your app
        val installer: String? =
            context.getPackageManager().getInstallerPackageName(context.getPackageName())

        // true if your app has been downloaded from Play Store
        return installer != null && validInstallers.contains(installer)
    }
}