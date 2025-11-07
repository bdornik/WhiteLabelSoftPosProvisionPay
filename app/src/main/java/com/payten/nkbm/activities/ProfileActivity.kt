package com.payten.nkbm.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.nkbm.R
import com.payten.nkbm.databinding.ActivityProfileBinding
import com.payten.nkbm.persistance.SharedPreferencesKeys
import com.trello.rxlifecycle4.components.support.RxAppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import javax.inject.Inject


@AndroidEntryPoint
class ProfileActivity : BaseActivity() {
    private val logger = KotlinLogging.logger {}

    private lateinit var binding : ActivityProfileBinding

    @Inject
    lateinit var sharedPreferences: KsPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsToRoot(binding.root)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        binding.closeButton.setOnClickListener {
            finish()
        }

        binding.labelDm.setText(sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME, ""))
        binding.labelAddress.setText("${sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_ADDRESS, "")}, ${sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_PLACE_NAME, "")}")

        binding.signOut.setOnClickListener{
            val intent = Intent(applicationContext, PinActivity::class.java)
            intent.putExtra(PinActivity.ACTION, PinActivity.ACTION_CHECK_PIN_LOGIN)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            sharedPreferences.push(SharedPreferencesKeys.IS_LOGGED_IN, false)
            this@ProfileActivity.finishAffinity()
            startActivity(intent)
        }

        binding.layoutTraffic.setOnClickListener {
            val intent = Intent(this, TrafficActivity::class.java)
            startActivity(intent)
        }

        binding.layoutSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        binding.layoutEndOfDay.setOnClickListener {
            val intent = Intent(this, EndOfDayActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
    }
}