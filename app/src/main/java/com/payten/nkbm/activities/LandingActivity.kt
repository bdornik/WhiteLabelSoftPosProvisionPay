package com.payten.nkbm.activities

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.nkbm.R
import com.payten.nkbm.databinding.ActivityLandingBinding
import com.payten.nkbm.databinding.DialogPinRegistrationBinding
import com.payten.nkbm.enums.ErrorDescription
import com.payten.nkbm.enums.Operation
import com.payten.nkbm.persistance.SharedPreferencesKeys
import com.payten.nkbm.utils.DebugSdk
import com.payten.nkbm.viewmodel.LandingViewModel
import com.sacbpp.api.SACBTPModuleConfigurator
import com.simant.MainApplication
import com.trello.rxlifecycle4.components.support.RxAppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import javax.inject.Inject

@AndroidEntryPoint
class LandingActivity : BaseActivity() {
    private val logger = KotlinLogging.logger {}

    private lateinit var binding: ActivityLandingBinding

    var counter = 0;

    @Inject
    lateinit var sharedPreferences: KsPrefs

    lateinit var model: LandingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val model2: LandingViewModel by viewModels()
        model = model2
        binding = ActivityLandingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsToRoot(binding.root)

        counter = 0;

        if (intent.hasExtra("REGISTRATION"))
            model.logError(
                model.createErrorLog(
                    sharedPreferences.pull(SharedPreferencesKeys.USER_TID),
                    sharedPreferences.pull(SharedPreferencesKeys.REGISTRATION_USER_ID),
                    ErrorDescription.systemLogs.name,
                    "REGISTER: TID:" + sharedPreferences.pull(SharedPreferencesKeys.USER_TID),
                    this
                ),true
            )


        model.getTerminalStatus()



        binding.btnNewTransaction.setOnClickListener {
            val intent = Intent(this, AmountActivity::class.java)
            startActivity(intent)
        }

        binding.menuButton.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.systemLogsButton.setOnClickListener {
            counter++

            if (counter == 5) {
                counter = 0

                model.logError(
                    model.createErrorLog(
                        sharedPreferences.pull(SharedPreferencesKeys.USER_TID),
                        sharedPreferences.pull(SharedPreferencesKeys.REGISTRATION_USER_ID),
                        ErrorDescription.systemLogs.name,
                        "systemLogs",
                        this
                    ),true
                )
            }
        }




        checkSecurityStatus()

        if (!sharedPreferences.pull(SharedPreferencesKeys.DUMMY, false)) {
            try {
                logger.info { "goOnlineForUpdate" }
                MainApplication.getSACBTPApplication().goOnlineForUpdate()
            } catch (e: Exception) {

                Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show()
                logger.error { e }
                model.logError(
                    model.createErrorLog(
                        sharedPreferences.pull(SharedPreferencesKeys.USER_TID),
                        sharedPreferences.pull(SharedPreferencesKeys.REGISTRATION_USER_ID),
                        ErrorDescription.systemLogs.name,
                        "systemLogs",
                        this
                    ),true
                )

                //finish()
            }
        }

        val s = DebugSdk.getSDKDetailedInfo(sharedPreferences)
        logger.info { "PORUKA: $s" }
    }

    override fun onResume() {
        super.onResume()
        subscribe()
        model.refreshData(sharedPreferences.pull(SharedPreferencesKeys.DUMMY, false))
    }

    private fun checkSecurityStatus() {
        try {
            val rb = SACBTPModuleConfigurator.getInstance().modulesStatus
            if (rb[0] == 1 || rb[1] == 1 || rb[7] == 1) {
                //tempered device
                Toast.makeText(
                    this@LandingActivity.applicationContext,
                    this@LandingActivity.applicationContext.getString(R.string.label_root_device),
                    Toast.LENGTH_LONG
                ).show()
                this@LandingActivity.finishAffinity()
            }

            if (rb[10] == 1) {
                Toast.makeText(
                    this@LandingActivity.applicationContext,
                    this@LandingActivity.applicationContext.getString(R.string.hooks_detected),
                    Toast.LENGTH_LONG
                ).show()
                this@LandingActivity.finishAffinity()
            }
        } catch (exc: Exception) {
            Toast.makeText(this, R.string.error, Toast.LENGTH_LONG).show()
            logger.error { exc }
            model.logError(
                model.createErrorLog(
                    sharedPreferences.pull(SharedPreferencesKeys.USER_TID),
                    sharedPreferences.pull(SharedPreferencesKeys.REGISTRATION_USER_ID),
                    ErrorDescription.systemLogs.name,
                    "systemLogs",
                    this
                ),true
            )

        }

    }

    fun subscribe() {
        model.getDetailsSuccessfull.observe(this, Observer { res ->
            if (!res) {
                val intent = Intent(applicationContext, PinActivity::class.java)
                intent.putExtra(PinActivity.ACTION, PinActivity.ACTION_CHECK_PIN_LOGIN)
                startActivity(intent)
                finish()
            }
        })
        model.getDetailsProvision.observe(this, Observer {
            if (it) {
                binding.warningIcon.visibility = View.GONE
            } else {
                binding.warningIcon.visibility = View.VISIBLE
            }
        })

        model.logsSendSuccess.observe(this, Observer {
            showDialog("", it, Operation.LOGS.name)
        })

        model.logsSendFailed.observe(this, Observer {
            showDialog(it, false, Operation.LOGS.name)
        })

        model.reactivation.observe(this, {
            logger.info { "Reactivaction: $it" }
            if (it) {
                showDialog(
                    getString(R.string.reactivation_message),
                    true,
                    Operation.REACTIVATION.name
                )
            }
        })
    }


    fun showDialog(message: String, success: Boolean, operation: String) {

        val dialog = Dialog(this)
        val dialogBinding: DialogPinRegistrationBinding =
            DialogPinRegistrationBinding.inflate(layoutInflater)

        dialog.setCanceledOnTouchOutside(false)

        handleDialogDarkMode(dialogBinding)

        if (operation.equals(Operation.LOGS.toString())) {
            if (success) {
                dialogBinding.icon.setImageResource(R.drawable.icon_success)
                dialogBinding.warningLabel.text = getString(R.string.logs_successfully_send)
            } else {
                dialogBinding.icon.setImageResource(R.drawable.icon_warning)
                dialogBinding.warningLabel.text = getString(R.string.logs_unsuccessfully_send)
            }

            dialogBinding.btn.text = this.getString(R.string.button_registration_back)

            dialogBinding.btn.setOnClickListener {
                dialog.dismiss()
            }
            dialog.setContentView(dialogBinding.root)
            dialog.show()
        } else if (operation.equals(Operation.REACTIVATION.toString())) {

            dialogBinding.icon.setImageResource(R.drawable.icon_success)
            dialogBinding.warningLabel.text = message
            dialog.setCancelable(false)

            dialogBinding.btn.text = getString(R.string.reactivation_button)

            dialogBinding.btn.setOnClickListener {
                reactivation()
                dialog.dismiss()
            }
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.setContentView(dialogBinding.root)
            dialog.show()
        }


    }

    private fun reactivation() {
        model.logError(
            model.createErrorLog(
                sharedPreferences.pull(SharedPreferencesKeys.USER_TID),
                sharedPreferences.pull(SharedPreferencesKeys.REGISTRATION_USER_ID),
                ErrorDescription.systemLogs.name,
                "REACTIVATION",
                this
            ),false
        )


        val intent = Intent(applicationContext, ReactivationActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        sharedPreferences.push(SharedPreferencesKeys.PIN_COUNT, 3)
        startActivity(intent)

        finish()


    }

    private fun handleDialogDarkMode(dialogBinding: DialogPinRegistrationBinding) {
        if (sharedPreferences.pull(SharedPreferencesKeys.IS_DARK_MODE, false)) {
            dialogBinding.root.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.globalBlackDialog
                )
            )
            dialogBinding.warningLabel.setTextColor(ContextCompat.getColor(this, R.color.white))
            dialogBinding.btn.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.globalBlackDialog
                )
            )
        }
    }
}