package com.payten.whitelabel.activities

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.R
import com.payten.whitelabel.databinding.ActivityBillBinding
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.trello.rxlifecycle4.components.support.RxAppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import javax.inject.Inject

@AndroidEntryPoint
class BillActivity : RxAppCompatActivity() {
    private val logger = KotlinLogging.logger {}

    private lateinit var binding : ActivityBillBinding

    @Inject
    lateinit var sharedPreferences: KsPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityBillBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.back.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        handleDarkLightMode()
    }

    private fun handleDarkLightMode() {
        if(sharedPreferences.pull(SharedPreferencesKeys.IS_DARK_MODE, false)){
            binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.globalBlack))
            binding.layoutDetails.setBackgroundColor(ContextCompat.getColor(this, R.color.globalBlackDialog))
            binding.labelBill.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.labelName.setTextColor(ContextCompat.getColor(this, R.color.suffixDarkMode))
            binding.name.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.labelCard.setTextColor(ContextCompat.getColor(this, R.color.suffixDarkMode))
            binding.card.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.labelCode.setTextColor(ContextCompat.getColor(this, R.color.suffixDarkMode))
            binding.code.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.labelE2E.setTextColor(ContextCompat.getColor(this, R.color.suffixDarkMode))
            binding.e2e.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.labelStatus.setTextColor(ContextCompat.getColor(this, R.color.suffixDarkMode))
            binding.status.setTextColor(ContextCompat.getColor(this, R.color.globalGreen))
            binding.btnResendBill.setBackgroundColor(ContextCompat.getColor(this, R.color.globalBlackDialog))
            binding.btnCancelTransaction.setBackgroundColor(ContextCompat.getColor(this, R.color.globalBlackDialog))

            DrawableCompat.setTint(
                DrawableCompat.wrap(binding.back.drawable),
                ContextCompat.getColor(this, R.color.white)
            )
        }else{
            binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
            binding.layoutDetails.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
            binding.labelBill.setTextColor(ContextCompat.getColor(this, R.color.bigLabelBlack))
            binding.labelName.setTextColor(ContextCompat.getColor(this, R.color.suffixDarkMode))
            binding.name.setTextColor(ContextCompat.getColor(this, R.color.bigLabelBlack))
            binding.labelCard.setTextColor(ContextCompat.getColor(this, R.color.suffix))
            binding.card.setTextColor(ContextCompat.getColor(this, R.color.bigLabelBlack))
            binding.labelCode.setTextColor(ContextCompat.getColor(this, R.color.suffix))
            binding.code.setTextColor(ContextCompat.getColor(this, R.color.bigLabelBlack))
            binding.labelE2E.setTextColor(ContextCompat.getColor(this, R.color.suffix))
            binding.e2e.setTextColor(ContextCompat.getColor(this, R.color.bigLabelBlack))
            binding.labelStatus.setTextColor(ContextCompat.getColor(this, R.color.suffix))
            binding.status.setTextColor(ContextCompat.getColor(this, R.color.globalGreen))
            binding.btnResendBill.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
            binding.btnCancelTransaction.setBackgroundColor(ContextCompat.getColor(this, R.color.white))

            DrawableCompat.setTint(
                DrawableCompat.wrap(binding.back.drawable),
                ContextCompat.getColor(this, R.color.bigLabelBlack)
            )
        }
    }
}