package com.payten.nkbm.activities

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.nkbm.R
import com.payten.nkbm.adapters.PinAdapter
import com.payten.nkbm.databinding.ActivityAmountBinding
import com.payten.nkbm.databinding.ActivityCustomAmountBinding
import com.payten.nkbm.databinding.DialogTransactionCompletedBinding
import com.payten.nkbm.enums.PinNumber
import com.payten.nkbm.persistance.SharedPreferencesKeys
import com.payten.nkbm.utils.KeyboardUtil
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import javax.inject.Inject


@AndroidEntryPoint
class CustomAmountActivity : BaseActivity() {
    private val logger = KotlinLogging.logger {}

    private lateinit var binding: ActivityCustomAmountBinding

    private lateinit var pinAdapter: PinAdapter
    private var dataList = mutableListOf<PinNumber>()

    private var amount = ""
    private var formattedAmount = ""

    @Inject
    lateinit var sharedPreferences: KsPrefs

    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                amount = ""
                formattedAmount = ""
                binding.enterAmount.setText(amount)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCustomAmountBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsToRoot(binding.root)

        binding.recyclerView.layoutManager = GridLayoutManager(applicationContext, SPAN_COUNT)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.statusBarColor = ContextCompat.getColor(this, R.color.backgroundAmount)

        pinAdapter = PinAdapter(applicationContext)
        binding.recyclerView.adapter = pinAdapter

        binding.recyclerView.addItemDecoration(
            PinActivity.MarginItemDecoration(resources.getDimensionPixelSize(R.dimen.pin_button_margin))
        )

        binding.enterAmount.inputType = InputType.TYPE_NULL

        KeyboardUtil.populateKeyboardDataList(dataList)

        pinAdapter.setDataList(dataList)

        pinAdapter.setOnItemClickListener(object : PinAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val value = KeyboardUtil.pinNumberValue(dataList[position])
                onButtonClicked(value)
            }
        })


        binding.proceed.setOnClickListener {
            if (sharedPreferences.pull(SharedPreferencesKeys.DUMMY, false)) {
                Toast.makeText(this, "Dummy User", Toast.LENGTH_LONG).show()

                return@setOnClickListener

            }


            //val intent = Intent(this, TipActivity::class.java)

            if (amount.isEmpty()) {
                openDialog()
            } else {
                val formattedAmount = "$amount"
                logger.info { "Sending amount: $amount" }
                intent.putExtra("Tips", formattedAmount)

                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }

        binding.back.setOnClickListener {
            finish()
        }
    }


    private fun onButtonClicked(value: String) {
        if (amount.length == 6 && value != RESULT_BACK)
            return
        // added number
        if (value != RESULT_BACK) {
            if (value.equals("0", true) && binding.enterAmount.text.toString().isEmpty()) {
                return
            }
            amount += value
            var formattedAmount = ""
            if (amount.length == 1) {
                formattedAmount = "0,0$amount"
            } else if (amount.length == 2) {
                formattedAmount = "0,$amount"
            } else {
                val preDotsString = "${amount.subSequence(0, amount.length - 2)}"
                val postDotsString = "${amount.subSequence(amount.length - 2, amount.length)}"
                formattedAmount = "${
                    preDotsString.reversed()
                        .chunked(3)
                        .joinToString(".")
                        .reversed()
                },$postDotsString"
            }
            binding.enterAmount.setText(formattedAmount)
        }
        // removed number
        else {
            amount = amount.dropLast(1)
            if (amount.isEmpty()) {
                binding.enterAmount.setText(amount)
            } else {
                var formattedAmount = ""
                if (amount.length == 1) {
                    formattedAmount = "0,0$amount"
                } else if (amount.length == 2) {
                    formattedAmount = "0,$amount"
                } else {
                    val preDotsString = "${amount.subSequence(0, amount.length - 2)}"
                    val postDotsString = "${amount.subSequence(amount.length - 2, amount.length)}"
                    formattedAmount = "${
                        preDotsString.reversed()
                            .chunked(3)
                            .joinToString(".")
                            .reversed()
                    },$postDotsString"
                }
                binding.enterAmount.setText(formattedAmount)
            }
        }
    }

    private fun setAmount() {
        val oldAmount = intent.getStringExtra("Amount")

        if (oldAmount != null) {
            amount = oldAmount.dropLast(3)
            val formattedAmount = "$amount,00"
            binding.enterAmount.setText(formattedAmount)
        }
    }

    private fun openDialog() {
        val dialog = Dialog(this)
        val dBinding: DialogTransactionCompletedBinding =
            DialogTransactionCompletedBinding.inflate(layoutInflater)

        dBinding.icon.setImageResource(R.drawable.icon_warning)
        dBinding.warningLabel.text = this.getString(R.string.message_must_enter_amount)
        dBinding.btn.text = this.getString(R.string.btn_must_enter_amount)

        dBinding.btn.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(dBinding.root)
        dialog.show()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onResume() {
        super.onResume()
    }

    companion object {
        const val RESULT_BACK = "-1"
        const val SPAN_COUNT = 3
    }
}