package com.payten.whitelabel.activities

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.R
import com.payten.whitelabel.databinding.ActivityThemeBinding
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import javax.inject.Inject

@AndroidEntryPoint
class ThemeActivity : BaseActivity() {
    private val logger = KotlinLogging.logger {}

    private lateinit var binding : ActivityThemeBinding

    @Inject
    lateinit var sharedPreferences: KsPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsToRoot(binding.root)


        binding.back.setOnClickListener {
            finish()
        }

        binding.lightMode.setOnClickListener {
            sharedPreferences.push(SharedPreferencesKeys.IS_DARK_MODE, false)
            handleDarkLightMode()
        }

        binding.darkMode.setOnClickListener {
            sharedPreferences.push(SharedPreferencesKeys.IS_DARK_MODE, true)
            handleDarkLightMode()
        }

        handleDarkLightMode()
    }
    override fun onResume() {
        super.onResume()
        handleDarkLightMode()
    }

    private fun handleDarkLightMode() {
        if(sharedPreferences.pull(SharedPreferencesKeys.IS_DARK_MODE, false)){
            binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.globalBlack))
            binding.labelChangeTheme.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.labelDay.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.labelNight.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.imageDay.setImageResource(R.drawable.theme_day_unselected)
            binding.imageNight.setImageResource(R.drawable.theme_nigt_selected)

            DrawableCompat.setTint(
                DrawableCompat.wrap(binding.back.drawable),
                ContextCompat.getColor(this, R.color.white)
            )
        }else{
            binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
            binding.labelChangeTheme.setTextColor(ContextCompat.getColor(this, R.color.bigLabelBlack))
            binding.labelDay.setTextColor(ContextCompat.getColor(this, R.color.bigLabelBlack))
            binding.labelNight.setTextColor(ContextCompat.getColor(this, R.color.bigLabelBlack))
            binding.imageDay.setImageResource(R.drawable.theme_day_selected)
            binding.imageNight.setImageResource(R.drawable.theme_night_unselected)

            DrawableCompat.setTint(
                DrawableCompat.wrap(binding.back.drawable),
                ContextCompat.getColor(this, R.color.bigLabelBlack)
            )
        }
    }
}