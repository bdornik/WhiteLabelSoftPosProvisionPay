package com.payten.nkbm.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Observer
import at.favre.lib.crypto.bcrypt.BCrypt
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.nkbm.databinding.ActivityMainBinding
import com.payten.nkbm.persistance.SharedPreferencesKeys
import com.payten.nkbm.viewmodel.SampleViewModel
import com.trello.rxlifecycle4.components.support.RxAppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : RxAppCompatActivity() {
    private val logger = KotlinLogging.logger {}

    private lateinit var binding : ActivityMainBinding

    @Inject
    lateinit var sharedPreferences: KsPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val encryptedPint = BCrypt.withDefaults().hashToString(12, binding.pinCode.text.toString().toCharArray())
        sharedPreferences.push(SharedPreferencesKeys.PIN, encryptedPint)

        binding.pinCode.addTextChangedListener {binding.hello.setText(it.toString())}

        val model: SampleViewModel by viewModels()
        model.getValues().observe(this, Observer { values ->
            logger.info { "Loaded values" }
        })

        binding.back.setOnClickListener{
            finish()
        }

    }
}