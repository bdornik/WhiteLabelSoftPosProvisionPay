package com.payten.whitelabel.activities

import android.os.Bundle
import android.view.View
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.databinding.ActivityProfiledetailsBinding
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import javax.inject.Inject

@AndroidEntryPoint
class ProfileDetailsActivity : BaseActivity() {
    private val logger = KotlinLogging.logger {}

    private lateinit var binding : ActivityProfiledetailsBinding

    @Inject
    lateinit var sharedPreferences: KsPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfiledetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsToRoot(binding.root)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        binding.userIdValue.setText(sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME, ""))
        binding.cityValue.setText(sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_PLACE_NAME, ""))
        binding.addressValue.setText(sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_ADDRESS, ""))
        binding.terminalValue.setText(sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_TERMINAL_ID, ""))
        binding.terminalValue.setText(sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_TERMINAL_ID, ""))

        binding.back.setOnClickListener {
            finish()
        }

    }

    override fun onResume() {
        super.onResume()
//        handleDarkLightMode()
    }

}