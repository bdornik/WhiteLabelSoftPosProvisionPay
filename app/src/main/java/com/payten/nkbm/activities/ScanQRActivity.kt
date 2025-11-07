package com.payten.nkbm.activities

import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.cioccarellia.ksprefs.KsPrefs
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager
import com.payten.nkbm.R
import com.payten.nkbm.databinding.ActivityScanQrBinding
import com.payten.nkbm.persistance.SharedPreferencesKeys
import com.trello.rxlifecycle4.components.support.RxAppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import javax.inject.Inject

@AndroidEntryPoint
class ScanQRActivity : RxAppCompatActivity() {
    private val logger = KotlinLogging.logger {}

    private lateinit var binding : ActivityScanQrBinding
    private lateinit var capture : CaptureManager

    @Inject
    lateinit var sharedPreferences: KsPrefs

    private var loaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScanQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.back.setOnClickListener{
            finish()
        }

        capture = CaptureManager(this, binding.scanView)
        capture.initializeFromIntent(intent, savedInstanceState)
        capture.decode()

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val callback = object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                if(!loaded) {
                    loaded = true
                    logger.info { "Scanned: ${result.text}" }
                    sharedPreferences.push(SharedPreferencesKeys.REGISTRATION_USER_ID, result.text)
                    finish()
                }
            }
        }

        binding.scanView.decodeContinuous(callback)
    }

    override fun onResume() {
        super.onResume()
        capture.onResume()
    }

    override fun onPause() {
        super.onPause()
        capture.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capture.onSaveInstanceState(outState)
    }
}