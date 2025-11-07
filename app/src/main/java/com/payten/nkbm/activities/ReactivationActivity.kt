package com.payten.nkbm.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import at.favre.lib.crypto.bcrypt.BCrypt
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.nkbm.R
import com.payten.nkbm.databinding.ActivityReactivationBinding
import com.payten.nkbm.dto.keys.GetKeysRequestDto
import com.payten.nkbm.persistance.SharedPreferencesKeys
import com.payten.nkbm.viewmodel.RegistrationViewModel
import com.simant.MainApplication
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ReactivationActivity : BaseActivity() {
    private val TAG = "ReactivationActivity"

    @Inject
    lateinit var sharedPreferences: KsPrefs

    private lateinit var binding: ActivityReactivationBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reactivation)

        binding = ActivityReactivationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsToRoot(binding.root)

        val model: RegistrationViewModel by viewModels()

        binding.spinner.visibility =View.VISIBLE
        //binding.pinOverlay.visibility = View.VISIBLE
        binding.visualIndicatorLed1.setBackgroundResource(R.drawable.circle_primary)

        model.reactivation(sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_TERMINAL_ID))


        model.reactivationSuccess.observe(this, {
            if (it){
                binding.visualIndicatorLed2.setBackgroundResource(R.drawable.circle_primary)
                model.activate(
                    sharedPreferences.pull(
                        SharedPreferencesKeys.REGISTRATION_USER_ID,
                        ""
                    ), sharedPreferences.pull(
                        SharedPreferencesKeys.USER_ACTIVATION_CODE,
                        ""
                    ), "Intesa.Android", this, getSharedPreferences("SOFTPOS_PARAMETERS_MDI",
                        MODE_PRIVATE
                    )
                )
            }else{
                //error
            }
        })


        model.paytenActivationSuccessfull.observe(this, {
            binding.visualIndicatorLed3.setBackgroundResource(R.drawable.circle_primary)

            val act = sharedPreferences.pull(SharedPreferencesKeys.USER_ACTIVATION_CODE,"")
            Log.i(TAG, "ACT: " + act)
            val encryptedActId = BCrypt.withDefaults()
                .hashToString(12, act.toCharArray())
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
            binding.visualIndicatorLed4.setBackgroundResource(R.drawable.circle_primary)
            if (it){
                (application as MainApplication).updateParameters()
                model.initializeMta(
                    application as MainApplication,
                    sharedPreferences.pull(SharedPreferencesKeys.REGISTRATION_USER_ID, ""),
                    sharedPreferences.pull(SharedPreferencesKeys.USER_ACTIVATION_CODE),
                    this
                )
            }
//            else{
//                binding.proceed.isEnabled = true
//                binding.spinner.visibility = View.GONE
//
//                val dialog = Dialog(this)
//                val dialogBinding: DialogPinRegistrationBinding =
//                    DialogPinRegistrationBinding.inflate(layoutInflater)
//
//                handleDialogDarkMode(dialogBinding)
//                dialogBinding.icon.setImageResource(R.drawable.icon_warning)
//                dialogBinding.warningLabel.text =
//                    "${this.getString(R.string.label_registration_failed)}"
//                dialogBinding.btn.text = this.getString(R.string.button_registration_back)
//
//                dialogBinding.btn.setOnClickListener {
//                    dialog.dismiss()
//                }
//                dialog.setContentView(dialogBinding.root)
//                dialog.show()
//            }
        })



        model.sdkRegisterSuccess.observe(this, {
            binding.visualIndicatorLed5.setBackgroundResource(R.drawable.circle_primary)
            binding.spinner.visibility = View.GONE
            sharedPreferences.push(SharedPreferencesKeys.IS_LOGGED_IN, true)
            val intent = Intent(this, PinActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()

//            model.activate(sharedPreferences.pull(SharedPreferencesKeys.REGISTRATION_USER_ID, ""), binding.pinCode.text.toString(), "Intesa.Android")
        })



    }
}