package com.payten.nkbm.activities

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.cioccarellia.ksprefs.KsPrefs
import com.github.barteksc.pdfviewer.PDFView
import com.payten.nkbm.config.SupercaseConfig
import com.payten.nkbm.databinding.ActivityTermsConditionsBinding
import com.payten.nkbm.viewmodel.TermsConditionsViewModel
import com.trello.rxlifecycle4.components.support.RxAppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import androidx.lifecycle.Observer
import com.payten.nkbm.persistance.SharedPreferencesKeys
import javax.inject.Inject


@AndroidEntryPoint
class TermsConditionsActivity : BaseActivity() {
    private val logger = KotlinLogging.logger {}

    private lateinit var binding : ActivityTermsConditionsBinding
    private lateinit var pdfView: PDFView
    private val viewModel: TermsConditionsViewModel by viewModels()

    @Inject
    lateinit var sharedPreferences: KsPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTermsConditionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsToRoot(binding.root)


        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        binding.back.setOnClickListener{
            finish()
        }

        viewModel.downloadPdf(SupercaseConfig.termsConditionURL)

        // Observe the LiveData from ViewModel
        viewModel.pdfFileLiveData.observe(this, Observer { file ->
            file?.let {
                // Load the file into PDFView
                binding.pdfView.fromFile(it).load()
            } ?: run {
                // Handle download failure
                // You might show a Toast or some error message here
            }
        })
    }



    override fun onResume() {
        super.onResume()
    }
}