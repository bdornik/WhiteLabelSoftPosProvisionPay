package com.payten.whitelabel.activities

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Observer
import at.favre.lib.crypto.bcrypt.BCrypt
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.R
import com.payten.whitelabel.config.SupercaseConfig
import com.payten.whitelabel.databinding.ActivityRegistrationBinding
import com.payten.whitelabel.databinding.DialogPinRegistrationBinding
import com.payten.whitelabel.dto.keys.GetKeysRequestDto
import com.payten.whitelabel.enums.ErrorDescription
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.utils.Utility
import com.payten.whitelabel.viewmodel.RegistrationViewModel
import com.simant.MainApplication
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class RegistrationActivity : BaseActivity() {
    private val logger = KotlinLogging.logger {}

    private lateinit var binding: ActivityRegistrationBinding

    @Inject
    lateinit var sharedPreferences: KsPrefs

    var counter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsToRoot(binding.root)

        val model: RegistrationViewModel by viewModels()
        val isRegistered = sharedPreferences.pull(SharedPreferencesKeys.IS_REGISTERED, false)
        counter = 0
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR


        if (isRegistered) {
            binding.labelRegistration.text = getString(R.string.label_login)
            binding.proceed.text = getString(R.string.label_login)
        }

        binding.back.setOnClickListener {
            finish()
        }

        binding.userIdValue.doAfterTextChanged { text ->
            run {
                sharedPreferences.push(SharedPreferencesKeys.REGISTRATION_USER_ID, text.toString())
            }
        }

        model.healthCheck()

        model.paytenActivationSuccessfull.observe(this, {
            (application as MainApplication).updateParameters()
            val encryptedActId = BCrypt.withDefaults()
                .hashToString(12, binding.pinCode.text.toString().toCharArray())
            sharedPreferences.push(SharedPreferencesKeys.HASHED_ACT_ID, encryptedActId)

            sharedPreferences.push(SharedPreferencesKeys.IS_REGISTERED, true)
            model.generateToken(
                sharedPreferences.pull(SharedPreferencesKeys.USER_ID, ""),
                sharedPreferences.pull(SharedPreferencesKeys.USER_TID)
            )
        })

        model.paytenGenerateTokenSuccessfull.observe(this, {
            model.getHostKeys(GetKeysRequestDto(sharedPreferences.pull(SharedPreferencesKeys.USER_TID)))
        })

        model.paytenGetHostKeys.observe(this, {

            if (it) {
                model.initializeMta(
                    application as MainApplication,
                    sharedPreferences.pull(SharedPreferencesKeys.REGISTRATION_USER_ID, ""),
                    binding.pinCode.text.toString(),
                    this
                )
            } else {
                binding.proceed.isEnabled = true
                binding.spinner.visibility = View.GONE

                val dialog = Dialog(this)
                val dialogBinding: DialogPinRegistrationBinding =
                    DialogPinRegistrationBinding.inflate(layoutInflater)

                handleDialogDarkMode(dialogBinding)
                dialogBinding.icon.setImageResource(R.drawable.icon_warning)
                dialogBinding.warningLabel.text =
                    "${this.getString(R.string.label_registration_failed)}"
                dialogBinding.btn.text = this.getString(R.string.button_registration_back)

                dialogBinding.btn.setOnClickListener {
                    dialog.dismiss()
                }
                dialog.setContentView(dialogBinding.root)
                dialog.show()
            }
        })



        model.healthCheck.observe(this, {
            if (it) {
                binding.healthCheckLed.setBackgroundResource(R.drawable.circle_green)
            } else {
                binding.healthCheckLed.setBackgroundResource(R.drawable.circle_red)


            }
        })

        model.paytenActivationFailed.observe(this, {
            binding.proceed.isEnabled = true
            binding.spinner.visibility = View.GONE

            val dialog = Dialog(this)
            val dialogBinding: DialogPinRegistrationBinding =
                DialogPinRegistrationBinding.inflate(layoutInflater)

            handleDialogDarkMode(dialogBinding)
            dialogBinding.icon.setImageResource(R.drawable.icon_warning)
            dialogBinding.warningLabel.text =
                "${this.getString(R.string.label_registration_failed)}"
            dialogBinding.btn.text = this.getString(R.string.button_registration_back)

            dialogBinding.btn.setOnClickListener {
                dialog.dismiss()
            }
            dialog.setContentView(dialogBinding.root)
            dialog.show()
        })

        model.paytenGenerateTokenFailed.observe(this, {
            binding.proceed.isEnabled = true
            binding.spinner.visibility = View.GONE
        })

        binding.proceed.setOnClickListener {
            binding.proceed.isEnabled = false
            binding.spinner.visibility = View.VISIBLE
            if (binding.userIdValue.text.toString() == SupercaseConfig.dummyUsId && binding.pinCode.text.toString() == SupercaseConfig.dummyActCode) {
                dummyLogin()
            } else {
                if (isRegistered) {
                    model.generateToken(
                        sharedPreferences.pull(
                            SharedPreferencesKeys.REGISTRATION_USER_ID,
                            ""
                        ), binding.pinCode.text.toString()
                    )
                } else {
                    model.activate(
                        sharedPreferences.pull(
                            SharedPreferencesKeys.REGISTRATION_USER_ID,
                            ""
                        ),
                        binding.pinCode.text.toString(),
                        "Intesa.Android",
                        this,
                        getSharedPreferences(
                            "SOFTPOS_PARAMETERS_MDI",
                            MODE_PRIVATE
                        )
                    )
                }
            }
        }

        binding.labelRegistration.setOnClickListener {
            counter++

            if (counter == 5) {
                counter = 0

                model.logError(
                    model.createErrorLog(
                        sharedPreferences.pull(SharedPreferencesKeys.REGISTRATION_USER_ID),
                        ErrorDescription.systemLogs.name,
                        "systemLogs",
                        this
                    ), true
                )
            }
        }
        model.logsSendSuccess.observe(this, Observer {
            if (it.dialog)
                showDialog("", it.send)
        })

        model.logsSendFailed.observe(this, Observer {
            if (it.dialog)
                showDialog(it.message, it.send)
        })

        model.sdkRegisterFailed.observe(this, {
            binding.proceed.isEnabled = true
            binding.spinner.visibility = View.GONE
            logger.error { "Registration failed: $it" }

            val dialog = Dialog(this)
            val dialogBinding: DialogPinRegistrationBinding =
                DialogPinRegistrationBinding.inflate(layoutInflater)

            handleDialogDarkMode(dialogBinding)
            dialogBinding.icon.setImageResource(R.drawable.icon_warning)
            //dialogBinding.warningLabel.text = "${this.getString(R.string.label_registration_failed)}: $it"
            dialogBinding.warningLabel.text =
                "${this.getString(R.string.label_registration_failed)}"
            dialogBinding.btn.text = this.getString(R.string.button_registration_back)

            dialogBinding.btn.setOnClickListener {
                dialog.dismiss()
            }
            dialog.setContentView(dialogBinding.root)
            dialog.show()
        })

        model.sdkRegisterSuccess.observe(this, {
            binding.spinner.visibility = View.GONE
            sharedPreferences.push(SharedPreferencesKeys.IS_LOGGED_IN, true)

            val newEndOfDay = getCurrentDateTime()
            sharedPreferences.push(SharedPreferencesKeys.END_OF_DAY_DATE, newEndOfDay)

            val intent = Intent(this, PinActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()

//            model.activate(sharedPreferences.pull(SharedPreferencesKeys.REGISTRATION_USER_ID, ""), binding.pinCode.text.toString(), "Intesa.Android")
        })

        model.sdkRegisterFailed.observe(this, {
            sharedPreferences.push(SharedPreferencesKeys.IS_REGISTERED, false) //probati
            model.logError(
                model.createErrorLog(
                    sharedPreferences.pull(SharedPreferencesKeys.REGISTRATION_USER_ID),
                    ErrorDescription.registerOnSDK.name,
                    it,
                    this
                ), false
            )
            binding.proceed.isEnabled = true
            binding.spinner.visibility = View.GONE
            logger.error { "Initialization failed" }
        })

        binding.appScanQrCodeBtn.setOnClickListener {
            val intent = Intent(this, ScanQRActivity::class.java)
            startActivity(intent)
        }

        binding.pinCode.addTextChangedListener(afterTextChanged = {
            handleButtonEnabled()
            if (it!!.isNotEmpty()) {
                binding.warningMessageCode.visibility = View.INVISIBLE
            } else {
                binding.warningMessageCode.visibility = View.VISIBLE
            }
        })

        val imm: InputMethodManager = getSystemService(
            Context.INPUT_METHOD_SERVICE
        ) as InputMethodManager
        if (isRegistered) {
            imm.showSoftInput(binding.pinCode, 0)
        }

        handleButtonEnabled()

        binding.checkboxLink.setPaintFlags(binding.checkboxLink.getPaintFlags() or Paint.UNDERLINE_TEXT_FLAG)

        binding.checkbox.setOnCheckedChangeListener { compoundButton, b -> handleButtonEnabled() }

        binding.checkboxLink.setOnClickListener {
            val intent = Intent(this, TermsConditionsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun dummyLogin() {
        logger.info { "dummyLogin" }
        binding.spinner.visibility = View.GONE
        val encryptedActId =
            BCrypt.withDefaults().hashToString(12, binding.pinCode.text.toString().toCharArray())
        sharedPreferences.push(SharedPreferencesKeys.IS_LOGGED_IN, true)
        sharedPreferences.push(SharedPreferencesKeys.HASHED_ACT_ID, encryptedActId)
        sharedPreferences.push(SharedPreferencesKeys.IS_REGISTERED, true)
        sharedPreferences.push(SharedPreferencesKeys.DUMMY, true)
        val intent = Intent(this, PinActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        handleDarkLightMode()

        binding.userIdValue.setText(
            sharedPreferences.pull(
                SharedPreferencesKeys.REGISTRATION_USER_ID,
                ""
            )
        )

        Utility.checkLanguage(sharedPreferences.pull(SharedPreferencesKeys.LANGUAGE, 0))
//        if(sharedPreferences.pull(SharedPreferencesKeys.REGISTRATION_USER_ID, "").isEmpty()){
//            binding.appActivateQrSuccess.visibility = View.INVISIBLE
//        } else {
//            binding.appActivateQrSuccess.visibility = View.VISIBLE
//        }
    }

    private fun handleDarkLightMode() {
        if (sharedPreferences.pull(SharedPreferencesKeys.IS_DARK_MODE, false)) {
            binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.globalBlack))
            binding.labelRegistration.setTextColor(ContextCompat.getColor(this, R.color.white))
            DrawableCompat.setTint(
                DrawableCompat.wrap(binding.back.drawable),
                ContextCompat.getColor(this, R.color.white)
            )
            binding.pinCode.setTextColor(ContextCompat.getColor(this, R.color.white))
        } else {
            binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
            binding.labelRegistration.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.bigLabelBlack
                )
            )
            DrawableCompat.setTint(
                DrawableCompat.wrap(binding.back.drawable),
                ContextCompat.getColor(this, R.color.bigLabelBlack)
            )
            binding.pinCode.setTextColor(ContextCompat.getColor(this, R.color.bigLabelBlack))
        }
    }

    fun handleButtonEnabled() {
        if (sharedPreferences.pull(SharedPreferencesKeys.REGISTRATION_USER_ID, "")
                .isEmpty() || binding.pinCode.text.toString()
                .isEmpty() || !binding.checkbox.isChecked
        ) {
            binding.proceed.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.gray);
            binding.proceed.isEnabled = false
        } else {
            binding.proceed.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.buttonGreen);
            binding.proceed.isEnabled = true
        }
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

        DrawableCompat.setTint(
            DrawableCompat.wrap(binding.back.drawable),
            ContextCompat.getColor(this, R.color.colorPrimary)
        )
    }

    fun showDialog(message: String, success: Boolean) {

        val dialog = Dialog(this)
        val dialogBinding: DialogPinRegistrationBinding =
            DialogPinRegistrationBinding.inflate(layoutInflater)

        handleDialogDarkMode(dialogBinding)
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
    }

    private fun getCurrentDateTime(): String {
        var localDateFrom = LocalDateTime.now()
        var tF = DateTimeFormatter.ofPattern("HH:mm:ss")
        var dF = DateTimeFormatter.ofPattern("yyyy-MM-dd")


        return "${localDateFrom.format(dF)}T${localDateFrom.format(tF)}"
    }



}