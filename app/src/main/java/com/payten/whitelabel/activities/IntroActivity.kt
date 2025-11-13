package com.payten.whitelabel.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.R
import com.payten.whitelabel.databinding.ActivityIntroBinding
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.trello.rxlifecycle4.components.support.RxAppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import javax.inject.Inject

@AndroidEntryPoint
class IntroActivity : RxAppCompatActivity() {
    private val logger = KotlinLogging.logger {}

    private lateinit var binding : ActivityIntroBinding

    @Inject
    lateinit var sharedPreferences: KsPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setNextActivity()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    }

    private fun setNextActivity() {
        val button = binding.buttonSignIn
        var reg = sharedPreferences.pull(SharedPreferencesKeys.IS_REGISTERED, false)

        if (reg) {
            button.text = getString(R.string.btn_intro_login)

            button.setOnClickListener {
                val intent = Intent(this, PinActivity::class.java)
                startActivity(intent)
            }
        } else {
            button.text = getString(R.string.btn_intro_sign_in)

            button.setOnClickListener {
                val intent = Intent(this, RegistrationActivity::class.java)
                startActivity(intent)
            }
        }
    }
}