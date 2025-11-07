package com.payten.nkbm.activities

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.nkbm.R
import com.payten.nkbm.config.SupercaseConfig
import com.payten.nkbm.databinding.ActivityPaymenttypeBinding
import com.payten.nkbm.persistance.SharedPreferencesKeys
import com.payten.nkbm.utils.AmountUtil
import com.trello.rxlifecycle4.components.support.RxAppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import javax.inject.Inject


@AndroidEntryPoint
class PaymentTypeActivity : BaseActivity() {
    private val logger = KotlinLogging.logger {}

    private lateinit var binding : ActivityPaymenttypeBinding

    @Inject
    lateinit var sharedPreferences: KsPrefs

    var selectedType = 0

    var resultLauncher = registerForActivityResult(StartActivityForResult()) { result ->
        logger.info { "RESULT CODE: ${result.resultCode}" }
        if (result.resultCode == Activity.RESULT_OK) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPaymenttypeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsToRoot(binding.root)

        val amount = intent.getStringExtra("Amount")

        var formattedAmount = AmountUtil.formatAmount(amount)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.statusBarColor = ContextCompat.getColor(this, R.color.backgroundAmount)

        binding.amountValue.text = formattedAmount
        binding.currency.text = SupercaseConfig.CURRENCY_STRING

        binding.back.setOnClickListener{
            finish()
        }

        binding.proceed.setOnClickListener {
            var intent = Intent(this, PosActivity::class.java)

            if(sharedPreferences.pull(SharedPreferencesKeys.TIPS, false))
                intent = Intent(this, TipActivity::class.java)

            if (selectedType == 1){
                intent = Intent(this, IpsActivity::class.java)
            }

            intent.putExtra("Amount",amount)
            startActivity(intent)
            finish()
        }

        binding.cardPaymentOption.isSelected = true
        ViewCompat.setBackgroundTintList(binding.flikPaymentOption, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)))

        binding.flikPaymentOption.setOnClickListener {
            this.selectedType = 1
            binding.cardPaymentOption.isSelected = false
            binding.flikPaymentOption.isSelected = true
            ViewCompat.setBackgroundTintList(binding.cardPaymentOption, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)))
            //ViewCompat.setBackgroundTintList(binding.flikPaymentOption, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green_background)))
            ViewCompat.setBackgroundTintList(binding.flikPaymentOption, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)))
            binding.buttonCardIcon.setImageResource(R.drawable.ok_icon_type_disable)
            binding.buttonFlikIcon.setImageResource(R.drawable.ok_icon_type)

        }


        binding.cardPaymentOption.setOnClickListener {
            this.selectedType = 0
            binding.cardPaymentOption.isSelected = true
            binding.flikPaymentOption.isSelected = false
            ViewCompat.setBackgroundTintList(binding.flikPaymentOption, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)))
            //ViewCompat.setBackgroundTintList(binding.cardPaymentOption, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.green_background)))
            ViewCompat.setBackgroundTintList(binding.cardPaymentOption, ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)))
            binding.buttonFlikIcon.setImageResource(R.drawable.ok_icon_type_disable)
            binding.buttonCardIcon.setImageResource(R.drawable.ok_icon_type)

        }

        if (!sharedPreferences.pull(SharedPreferencesKeys.IPS_EXISTS,false)){
            binding.flikPaymentOption.isEnabled = false
            binding.flikPaymentOption.alpha = 0.5f
        }

    }

    override fun onResume() {
        super.onResume()
        handleDarkLightMode()
    }

    private fun handleDarkLightMode() {
        if(sharedPreferences.pull(SharedPreferencesKeys.IS_DARK_MODE, false)){
            binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.globalBlack))
            binding.labelMethod.setTextColor(ContextCompat.getColor(this, R.color.white))
            DrawableCompat.setTint(
                DrawableCompat.wrap(binding.back.drawable),
                ContextCompat.getColor(this, R.color.colorPrimary)
            )
        }else{
            binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
            binding.labelMethod.setTextColor(ContextCompat.getColor(this, R.color.black))
            DrawableCompat.setTint(
                DrawableCompat.wrap(binding.back.drawable),
                ContextCompat.getColor(this, R.color.colorPrimary)
            )
        }
    }
}

