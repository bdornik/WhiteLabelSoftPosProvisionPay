package com.payten.nkbm.activities

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.nkbm.R
import com.payten.nkbm.databinding.ActivityForgotPinBinding
import com.payten.nkbm.databinding.DialogPinRegistrationBinding
import com.payten.nkbm.dto.OtpCheckDto
import com.payten.nkbm.persistance.SharedPreferencesKeys
import com.payten.nkbm.utils.Utility
import com.payten.nkbm.viewmodel.ForgotPinModel
import com.trello.rxlifecycle4.components.support.RxAppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import org.slf4j.helpers.Util
import javax.inject.Inject

@AndroidEntryPoint
class ForgotPinActivity : BaseActivity() {
    private val logger = KotlinLogging.logger {}

    private lateinit var binding: ActivityForgotPinBinding

    @Inject
    lateinit var sharedPreferences: KsPrefs

    lateinit var model: ForgotPinModel

    var mode = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityForgotPinBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsToRoot(binding.root)

        val model2: ForgotPinModel by viewModels()
        model = model2

        binding.back.setOnClickListener {
            finish()
        }

        binding.button.setOnClickListener {
            binding.spinner.visibility = View.VISIBLE
            model.sendSms()
        }

        binding.buttonSendOtp.setOnClickListener {
            model.otpCheck(
                OtpCheckDto(
                    binding.userId.text.toString(),
                    binding.pin.text.toString()
                )
            )
        }

        model.otpResponse.observe(this, {
            binding.spinner.visibility = View.INVISIBLE
            if (it) {
                mode = 1
                binding.pinLabel.visibility = View.VISIBLE
                binding.pin.visibility = View.VISIBLE
                binding.userIdLabel.visibility = View.VISIBLE
                binding.userId.visibility = View.VISIBLE
                binding.button.visibility = View.GONE
                binding.buttonSendOtp.visibility = View.VISIBLE
                binding.button.text = getString(R.string.next)
                binding.userId.requestFocus()
            } else {

            }
        })

        model.refreshData()

        model.otpCheckResponse.observe(this, {
            binding.spinner.visibility = View.INVISIBLE
            if (it.equals("00", true)) {
                val intent = Intent(applicationContext, PinActivity::class.java)
                intent.putExtra(PinActivity.ACTION, PinActivity.ACTION_CHANGE)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                sharedPreferences.push(SharedPreferencesKeys.APP_BLOCKED, false)
                sharedPreferences.push(SharedPreferencesKeys.PIN_COUNT, 3)
                startActivity(intent)
            } else {
                val dialog = Dialog(this)
                val dialogBinding: DialogPinRegistrationBinding =
                    DialogPinRegistrationBinding.inflate(layoutInflater)

                dialogBinding.icon.setImageResource(R.drawable.icon_warning)
                dialogBinding.warningLabel.text =
                    "${this.getString(R.string.wrong_activation_code)}"
                dialogBinding.btn.text = this.getString(R.string.button_registration_back)

                dialogBinding.btn.setOnClickListener {
                    dialog.dismiss()
                }
                dialog.setContentView(dialogBinding.root)
                dialog.show()

                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        Utility.checkLanguage(sharedPreferences.pull(SharedPreferencesKeys.LANGUAGE, 0))
    }
}