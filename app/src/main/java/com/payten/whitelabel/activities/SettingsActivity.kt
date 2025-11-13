package com.payten.whitelabel.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.R
import com.payten.whitelabel.config.SupercaseConfig
import com.payten.whitelabel.databinding.ActivitySettingsBinding
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : BaseActivity() {
    private val logger = KotlinLogging.logger {}

    private lateinit var binding : ActivitySettingsBinding

    @Inject
    lateinit var sharedPreferences: KsPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsToRoot(binding.root)

        val packageManager = this.packageManager
        val packageName = this.packageName
        val packageInfo = packageManager.getPackageInfo(packageName, 0)

        val versionCode: Long = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
        val versionName: String = packageInfo.versionName ?: "unknown"


        binding.profile.setOnClickListener {
            val intent = Intent(this, ProfileDetailsActivity::class.java)
            startActivity(intent)
        }

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

//        binding.labelContacts.setOnClickListener {
//            if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
//                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CALL_PHONE),
//                    1)
//            } else {
//                val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", SupercaseConfig.CONTACT_PHONE.replace(",","", true).replace(" ", "", true), null))
//                startActivity(intent)
//            }
//        }

        binding.version.text = "${versionName} (${versionCode})"

        binding.labelContactNumber.text = SupercaseConfig.CONTACT_NUMBER

        binding.changePin.setOnClickListener {
            val intent = Intent(this, PinActivity::class.java)
            intent.putExtra(PinActivity.ACTION, PinActivity.ACTION_CHECK_PIN_CHANGE)
            startActivity(intent)
        }

        binding.labelTermsConditions.setOnClickListener {
            val intent = Intent(this, TermsConditionsActivity::class.java)
            startActivity(intent)
        }

        binding.changeTheme.setOnClickListener {
            val intent = Intent(this, ThemeActivity::class.java)
            startActivity(intent)
        }

        binding.back.setOnClickListener {
            finish()
        }

        val languages = arrayListOf<String>()

        languages.add(getString(R.string.language_slo))
        languages.add(getString(R.string.language_en))

        val arrayAdapter: ArrayAdapter<String> = ArrayAdapter(this, R.layout.spinner_item, languages).also {
            // Specify the layout to use when the list of choices appears
            it.setDropDownViewResource(R.layout.spinner_item_dropdown)
            // Apply the adapter to the spinner
            binding.languageSpinner.adapter = it

        }

        binding.languageSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    adapterView: AdapterView<*>?,
                    view: View?,
                    i: Int,
                    l: Long
                ) {
                    if(sharedPreferences.pull(SharedPreferencesKeys.LANGUAGE, 0) != i) {
                        sharedPreferences.push(SharedPreferencesKeys.LANGUAGE, i)
                            if (i == 0) {
                                AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.forLanguageTags(
                                        "sl"
                                    )
                                )
                            } else if (i == 1) {
                                AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.forLanguageTags(
                                        "en"
                                    )
                                )
                            }
                        }
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) {}
            }
        binding.languageSpinner.setSelection(sharedPreferences.pull(SharedPreferencesKeys.LANGUAGE, 0))
    }

    override fun onResume() {
        super.onResume()
    }
}