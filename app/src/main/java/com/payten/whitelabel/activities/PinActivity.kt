package com.payten.whitelabel.activities

import android.app.Dialog
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import at.favre.lib.crypto.bcrypt.BCrypt
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.R
import com.payten.whitelabel.adapters.PinAdapter
import com.payten.whitelabel.databinding.ActivityPinBinding
import com.payten.whitelabel.databinding.DialogPinRegistrationBinding
import com.payten.whitelabel.enums.ErrorDescription
import com.payten.whitelabel.enums.PinNumber
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.utils.KeyboardUtil
import com.payten.whitelabel.viewmodel.PinViewModel
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import javax.inject.Inject

@AndroidEntryPoint
class PinActivity : BaseActivity() {
    private val logger = KotlinLogging.logger {}

    var counter = 0;


    companion object {
        const val MAX_PIN_SIZE = 4
        const val RESULT_BACK = "-1"
        const val SPAN_COUNT = 3
        const val ACTION = "action"
        const val ACTION_FORGOTTEN_PIN = "forgottenPin"
        const val ACTION_CHANGE = "actionChange"
        const val ACTION_REGISTER = "actionRegister"
        const val ACTION_CHECK_PIN_CHANGE = "actionCheckPinChange"
        const val ACTION_CHECK_PIN_LOGIN = "actionCheckPinLogin"
        const val ACTION_CHECK_PIN_TRANSACTION = "actionCheckPinTransaction"
    }

    private lateinit var binding: ActivityPinBinding

    private lateinit var pinAdapter: PinAdapter
    private var dataList = mutableListOf<PinNumber>()
    private var firstEntry: Boolean = true
    private var action: String? = ACTION_REGISTER
    private var confirmedPin: Boolean = false
    private lateinit var labelMessage: TextView

    private var firstPin = ""
    private var secondPin = ""
    private var numberAttempts = 3
    private var forgot = false
    private var pinForgottenCheck = false


    @Inject
    lateinit var sharedPreferences: KsPrefs

    lateinit var model: PinViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPinBinding.inflate(layoutInflater)

        val model2: PinViewModel by viewModels()
        model = model2

        counter = 0;

        subscribeOnGetToken()


        setContentView(binding.root)
        applyInsetsToRootWithOutTop(binding.root)

        labelMessage = binding.messageTextView
        labelMessage.text = getString(R.string.label_enter_pin)

        if (intent.hasExtra(ACTION)) {
            action = intent.getStringExtra(ACTION)
        }

        if (action == ACTION_CHANGE) {
            forgot = true
        }

        if (action == ACTION_CHECK_PIN_LOGIN) {
            binding.labelRegistration.visibility = View.INVISIBLE
            binding.logoContainer.visibility = View.VISIBLE
            binding.labelForgot.visibility = View.VISIBLE
            //binding.labelForgot.setPaintFlags(binding.labelForgot.getPaintFlags() or Paint.UNDERLINE_TEXT_FLAG)
            binding.labelForgot.setOnClickListener {
                pinForgottenCheck = true
                startActivityForgottenPin()

            }
        } else {
            binding.labelForgot.visibility = View.INVISIBLE
        }

        model.healthCheck()

        model.healthCheck.observe(this, {
            if (it) {
                binding.healthCheckLed.setBackgroundResource(R.drawable.circle_green)
            } else {
                binding.healthCheckLed.setBackgroundResource(R.drawable.circle_red)


            }
        })

        binding.logoContainer.setOnClickListener{
            counter++

            if (counter == 5) {
                counter = 0

                model.logError(
                    model.createErrorLog(
                        sharedPreferences.pull(SharedPreferencesKeys.USER_TID),
                        sharedPreferences.pull(SharedPreferencesKeys.REGISTRATION_USER_ID),
                        ErrorDescription.systemLogs.name,
                        "logIN",
                        this
                    )
                )
            }
        }

        model.logsSendSuccess.observe(this, Observer {
            showDialog("", it)
        })

        model.logsSendFailed.observe(this, Observer {
            showDialog(it, false)
        })

        if (action == ACTION_CHECK_PIN_CHANGE) {
        }

        numberAttempts = sharedPreferences.pull(SharedPreferencesKeys.PIN_COUNT, 3)

        firstEntry = true

        binding.back.setOnClickListener {
            finish()
        }

        binding.recyclerView.layoutManager = GridLayoutManager(applicationContext, SPAN_COUNT)

        pinAdapter = PinAdapter(applicationContext)
        binding.recyclerView.adapter = pinAdapter

        binding.recyclerView.addItemDecoration(
            MarginItemDecoration(resources.getDimensionPixelSize(R.dimen.pin_button_margin))
        )

        KeyboardUtil.populateKeyboardDataList(dataList)

        pinAdapter.setDataList(dataList)

        setLabel()

        pinAdapter.setOnItemClickListener(object : PinAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val value = KeyboardUtil.pinNumberValue(dataList[position])
                onPinButtonClicked(value)
            }
        })
    }

    override fun onResume() {
        super.onResume()
//        handleDarkLightMode()
//        pinDialog(true)
    }

    private fun getCurrentPin(): String {
        if (firstEntry) {
            return firstPin
        } else {
            return secondPin
        }
    }

    private fun onPinButtonClicked(value: String) {
        var currentPin = getCurrentPin().length

        if (value != RESULT_BACK) {
            if (firstEntry) {
                firstPin += value
            } else {
                secondPin += value
            }
            currentPin += 1
            reloadPinView()
        } else {
            if (firstEntry) {
                firstPin = firstPin.dropLast(1)
            } else {
                secondPin = secondPin.dropLast(1)
            }
            currentPin -= 1
            reloadPinView()
        }

        if (currentPin == MAX_PIN_SIZE) {
            onPinEntered()
        }
    }

    private fun onPinEntered() {
        when (this.action) {
            ACTION_REGISTER -> {
                register()
            }
            ACTION_CHANGE -> {
                changePin()
            }
            ACTION_CHECK_PIN_CHANGE -> {
                checkPin()
            }
            ACTION_CHECK_PIN_LOGIN -> {
                login()
            }
            ACTION_CHECK_PIN_TRANSACTION -> {
                checkPin()
            }
        }
    }

    private fun login() {
        var currentPin = getCurrentPin()

        val encryptedPin = sharedPreferences.pull(
            SharedPreferencesKeys.PIN,
            ""
        )

        val blockedApp = sharedPreferences.pull(
            SharedPreferencesKeys.APP_BLOCKED,
            false
        )

        if (!blockedApp) {
            val result: BCrypt.Result =
                BCrypt.verifyer().verify(currentPin.toCharArray(), encryptedPin)

            val result2: BCrypt.Result =
                BCrypt.verifyer().verify(firstPin.toCharArray(), encryptedPin)

            if (result.verified || result2.verified) {
                startActivityLanding()
            } else {
                if (numberAttempts == 1) {
                    sharedPreferences.push(SharedPreferencesKeys.APP_BLOCKED, true)
                    pinDialog(
                        true,
                        this.getString(R.string.label_change_pin_wrong_pin_max_attempts)
                    )
                } else {
                    //smanjiti broj pokusaja
                    numberAttempts--
                    sharedPreferences.push(SharedPreferencesKeys.PIN_COUNT, numberAttempts)
                    pinDialog(true, null)
                }
            }
        } else {
            pinDialog(true, this.getString(R.string.label_change_pin_wrong_pin_max_attempts))
        }
    }

    private fun checkIsPinValid(): Boolean {
        when (action) {
            ACTION_REGISTER -> {
                if (firstEntry) {
                    return true
                } else {
                    return firstPin.equals(secondPin, true)
                }
            }

            ACTION_CHECK_PIN_TRANSACTION, ACTION_CHECK_PIN_LOGIN, ACTION_CHECK_PIN_CHANGE -> {
                val result: BCrypt.Result =
                    BCrypt.verifyer().verify(
                        getCurrentPin().toCharArray(),
                        sharedPreferences.pull(SharedPreferencesKeys.PIN, "")
                    )

                return result.verified
            }

            ACTION_CHANGE -> {
                if (firstEntry) {
                    return true
                } else {
                    return firstPin.equals(secondPin, true)
                }
            }
        }

        return false
    }

    private fun checkPin() {
        if (checkIsPinValid()) {
            when (action) {
                ACTION_CHECK_PIN_LOGIN -> {
                    finish()
                }
                ACTION_CHECK_PIN_CHANGE -> {
                    action = ACTION_CHANGE
                    firstEntry()
                    setLabel()
                    clearUI()
                }
                ACTION_CHECK_PIN_TRANSACTION -> {
                    finish()
                }
            }
        } else {
            pinDialog(true, null)
        }

    }

    private fun register() {
        if (firstEntry) {
            firstEntry = false

            setLabel()
            clearUI()
        } else {
            if (checkIsPinValid()) {
                sharedPreferences.push(SharedPreferencesKeys.REGISTERED, true)

                val encryptedPint =
                    BCrypt.withDefaults().hashToString(12, getCurrentPin().toCharArray())
                sharedPreferences.push(SharedPreferencesKeys.PIN, encryptedPint)
//                sharedPreferences.push(SharedPreferencesKeys.PIN, getCurrentPin())
                pinDialog(false, null)
            } else {
                pinDialog(true, null)
            }
        }
    }

    private fun changePin() {
        if (firstEntry) {
            labelMessage.text = this.getString(R.string.label_change_pin)
            firstEntry = false

            setLabel()
            clearUI()
        } else {
            if (firstPin.equals(secondPin, true)) {
                val encryptedPint = BCrypt.withDefaults().hashToString(12, firstPin.toCharArray())
                sharedPreferences.push(SharedPreferencesKeys.PIN, encryptedPint)
                pinDialog(false, null)
            } else {
                pinDialog(true, null)
            }
        }
    }

    private fun firstEntry() {
        firstPin = ""
        secondPin = ""
        firstEntry = true
        setLabel()
        reloadPinView()
    }

    private fun clearUI() {
        reloadPinView()
    }

    private fun setLabel() {
        when (action) {
            ACTION_CHECK_PIN_LOGIN -> {
                labelMessage.text = this.getString(R.string.label_login_enter_pin)
                binding.back.visibility = View.GONE
                binding.labelRegistration.visibility = View.GONE
            }
            ACTION_CHANGE -> {
                if (firstEntry) {
                    labelMessage.text = getString(R.string.label_new_pin_first)
                } else {
                    labelMessage.text = getString(R.string.label_new_pin_repeat)
                }
                binding.labelRegistration.text = this.getString(R.string.label_change_pin)
            }
            ACTION_CHECK_PIN_CHANGE -> {
                binding.labelRegistration.visibility = View.GONE
                binding.back.setImageResource(R.drawable.logo_header)
                labelMessage.text = this.getString(R.string.label_pin_sign_in)
            }

            ACTION_CHECK_PIN_TRANSACTION -> {
                binding.labelRegistration.visibility = View.GONE
                binding.back.setImageResource(R.drawable.logo_header)
                labelMessage.text = this.getString(R.string.label_pin_sign_in)
            }

            ACTION_REGISTER -> {
                if (firstEntry) {
                    labelMessage.text = getString(R.string.label_enter_pin_registration)
                } else {
                    labelMessage.text = getString(R.string.label_new_pin_repeat)
                }
            }
        }
    }

    private fun subscribeOnGetToken() {
        val dialog = Dialog(this)
        val dialogBinding: DialogPinRegistrationBinding =
            DialogPinRegistrationBinding.inflate(layoutInflater)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        model.getTokenSuccessfull.observe(this, Observer { res ->
            if (res) {
                if (pinForgottenCheck) {
                    val intent = Intent(applicationContext, ForgotPinActivity::class.java)
                    pinForgottenCheck = false
                    startActivity(intent)
                } else {
                    if (this.action.equals(ACTION_REGISTER))
                        intent.putExtra("REGISTRATION",true)
                    val intent = Intent(applicationContext, LandingActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    sharedPreferences.push(SharedPreferencesKeys.PIN_COUNT, 3)
                    startActivity(intent)
                    finish()
                }
            } else {
                handleDialogDarkMode(dialogBinding)
                dialogBinding.icon.setImageResource(R.drawable.icon_warning)
                dialogBinding.warningLabel.text = this.getString(R.string.error)
                dialogBinding.btn.text = this.getString(R.string.button_registration_back)

                dialogBinding.btn.setOnClickListener {
                    firstEntry()
                    dialog.dismiss()
                }
                dialog.setContentView(dialogBinding.root)
                dialog.show()
                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
        })
    }



    private fun startActivityLanding() {
        model.refreshData(sharedPreferences.pull(SharedPreferencesKeys.DUMMY,false))
    }

    private fun startActivityForgottenPin() {
        model.refreshData(sharedPreferences.pull(SharedPreferencesKeys.DUMMY,false))
    }

    private fun reloadPinView() {
        val circle1 = binding.circle1
        val circle2 = binding.circle2
        val circle3 = binding.circle3
        val circle4 = binding.circle4

        var pin = getCurrentPin()

        when (pin.length) {
            0 -> {
                circle1.setImageResource(R.drawable.circle_grey)
                circle2.setImageResource(R.drawable.circle_grey)
                circle3.setImageResource(R.drawable.circle_grey)
                circle4.setImageResource(R.drawable.circle_grey)
            }
            1 -> {
                circle1.setImageResource(R.drawable.circle_green)
                circle2.setImageResource(R.drawable.circle_grey)
                circle3.setImageResource(R.drawable.circle_grey)
                circle4.setImageResource(R.drawable.circle_grey)
            }
            2 -> {
                circle1.setImageResource(R.drawable.circle_green)
                circle2.setImageResource(R.drawable.circle_green)
                circle3.setImageResource(R.drawable.circle_grey)
                circle4.setImageResource(R.drawable.circle_grey)
            }
            3 -> {
                circle1.setImageResource(R.drawable.circle_green)
                circle2.setImageResource(R.drawable.circle_green)
                circle3.setImageResource(R.drawable.circle_green)
                circle4.setImageResource(R.drawable.circle_grey)
            }
            4 -> {
                circle1.setImageResource(R.drawable.circle_green)
                circle2.setImageResource(R.drawable.circle_green)
                circle3.setImageResource(R.drawable.circle_green)
                circle4.setImageResource(R.drawable.circle_green)
            }
        }
    }

    private fun pinDialog(failed: Boolean, message: String?) {
        val dialog = Dialog(this)
        val dialogBinding: DialogPinRegistrationBinding =
            DialogPinRegistrationBinding.inflate(layoutInflater)

        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        when (this.action) {
            ACTION_CHECK_PIN_CHANGE -> {
                if (failed) {
                    handleDialogDarkMode(dialogBinding)
                    dialogBinding.icon.setImageResource(R.drawable.icon_warning)
                    dialogBinding.warningLabel.text =
                        this.getString(R.string.label_change_pin_wrong_pin_no_attempts)
                    dialogBinding.btn.text = this.getString(R.string.button_registration_back)

                    dialogBinding.btn.setOnClickListener {
                        firstEntry()
                        dialog.dismiss()
                    }
                    dialog.setContentView(dialogBinding.root)
                    dialog.show()
                } else {
                    handleDialogDarkMode(dialogBinding)
                    dialogBinding.icon.setImageResource(R.drawable.icon_success)
                    dialogBinding.warningLabel.text = this.getString(R.string.label_change_success)
                    dialogBinding.btn.text = this.getString(R.string.button_registration_success)

                    dialogBinding.btn.setOnClickListener {
                        if (forgot) {
                            startActivityLanding()
                        }
                        finish()
                        dialog.dismiss()
                    }
                    dialog.setContentView(dialogBinding.root)
                    dialog.show()
                }
            }
            ACTION_CHECK_PIN_LOGIN, ACTION_CHECK_PIN_TRANSACTION -> {
                if (failed) {
                    handleDialogDarkMode(dialogBinding)
                    dialogBinding.icon.setImageResource(R.drawable.icon_warning)
                    dialogBinding.warningLabel.text =
                        "${this.getString(R.string.label_change_pin_wrong_pin_)} ${numberAttempts}"
                    dialogBinding.btn.text = this.getString(R.string.button_try_again)

                    if (message != null) {
                        dialogBinding.warningLabel.text = message
                        dialogBinding.btn.text = resources.getString(R.string.label_pin_not_entered_button)
                        //dialogBinding.btn.visibility = View.INVISIBLE
                    }

                    dialogBinding.btn.setOnClickListener {
                        if (sharedPreferences.pull(SharedPreferencesKeys.PIN_COUNT, 3) == 0) {
                            sharedPreferences.push(SharedPreferencesKeys.IS_BLACKLISTED, true)

                            val intent = Intent(applicationContext, SplashActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            firstEntry()
                            dialog.dismiss()
                        }
                    }
                    dialog.setContentView(dialogBinding.root)
                    dialog.show()
                } else {
                    finish()
                }
            }
            ACTION_CHANGE -> {
                if (failed) {
                    handleDialogDarkMode(dialogBinding)
                    dialogBinding.icon.setImageResource(R.drawable.icon_warning)
                    dialogBinding.warningLabel.text =
                        this.getString(R.string.label_registration_wrong_pin)
                    dialogBinding.btn.text = this.getString(R.string.button_registration_back)

                    dialogBinding.btn.setOnClickListener {
                        if (sharedPreferences.pull(SharedPreferencesKeys.PIN_COUNT, 3) == 0) {
                            sharedPreferences.push(SharedPreferencesKeys.IS_BLACKLISTED, true)

                            val intent = Intent(applicationContext, SplashActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        } else {
                            firstEntry()
                            dialog.dismiss()
                        }
                    }
                    dialog.setContentView(dialogBinding.root)
                    dialog.show()
                } else {
                    handleDialogDarkMode(dialogBinding)
                    dialogBinding.icon.setImageResource(R.drawable.icon_success)
                    dialogBinding.warningLabel.text = this.getString(R.string.label_change_success)
                    dialogBinding.btn.text = this.getString(R.string.button_registration_success)

                    dialogBinding.btn.setOnClickListener {
                        //if (forgot) {
                        startActivityLanding()
                        dialog.dismiss()
                        //}
                    }
                    dialog.setContentView(dialogBinding.root)
                    dialog.show()
                }
            }
            ACTION_REGISTER -> {
                if (failed) {
                    handleDialogDarkMode(dialogBinding)
                    dialogBinding.icon.setImageResource(R.drawable.icon_warning)
                    dialogBinding.warningLabel.text =
                        this.getString(R.string.label_registration_wrong_pin)
                    dialogBinding.btn.text = this.getString(R.string.button_registration_back)

                    dialogBinding.btn.setOnClickListener {
                        firstEntry()
                        dialog.dismiss()
                    }
                    dialog.setContentView(dialogBinding.root)
                    dialog.show()
                } else {
                    handleDialogDarkMode(dialogBinding)
                    dialogBinding.icon.setImageResource(R.drawable.icon_success)
                    dialogBinding.warningLabel.text =
                        this.getString(R.string.label_registration_success)
                    dialogBinding.btn.text = this.getString(R.string.button_registration_success)

                    dialogBinding.btn.setOnClickListener {
                        val intent = Intent(this, ProvisionActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        startActivity(intent)
                        //startActivityLanding()
                        finish()
                        dialog.dismiss()
                    }
                    dialog.setContentView(dialogBinding.root)
                    dialog.show()
                }
            }
        }
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun handleDialogDarkMode(dialogBinding: DialogPinRegistrationBinding) {
        if (sharedPreferences.pull(SharedPreferencesKeys.IS_DARK_MODE, false)) {
//            dialogBinding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.globalBlackDialog))
            dialogBinding.warningLabel.setTextColor(ContextCompat.getColor(this, R.color.white))
            dialogBinding.btn.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.globalBlackDialog
                )
            )
        }
    }

    fun showDialog(message: String, success: Boolean) {

        val dialog = Dialog(this)
        val dialogBinding: DialogPinRegistrationBinding =
            DialogPinRegistrationBinding.inflate(layoutInflater)

        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)

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
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setContentView(dialogBinding.root)
        dialog.show()
    }

    class MarginItemDecoration(private val spaceHeight: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect, view: View,
            parent: RecyclerView, state: RecyclerView.State
        ) {
            with(outRect) {
                top = spaceHeight
                left = spaceHeight
                right = spaceHeight
                bottom = spaceHeight
            }
        }
    }
}