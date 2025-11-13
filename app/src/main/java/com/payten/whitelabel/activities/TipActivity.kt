package com.payten.whitelabel.activities

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.cioccarellia.ksprefs.KsPrefs
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.payten.whitelabel.R
import com.payten.whitelabel.adapters.PinAdapter
import com.payten.whitelabel.config.SupercaseConfig
import com.payten.whitelabel.databinding.ActivityTipBinding
import com.payten.whitelabel.databinding.DialogTipBinding
import com.payten.whitelabel.databinding.DialogTransactionCompletedBinding
import com.payten.whitelabel.enums.PinNumber
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.utils.AmountUtil
import com.payten.whitelabel.utils.KeyboardUtil
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import javax.inject.Inject

@AndroidEntryPoint
class TipActivity : BaseActivity() {
    private val logger = KotlinLogging.logger {}

    private lateinit var binding: ActivityTipBinding

    private lateinit var pinAdapter: PinAdapter
    private var dataList = mutableListOf<PinNumber>()

    private var amount = ""
    private var totalAmount = ""
    private var tipPercentage = 0.00
    private var tip = ""
    private var formattedAmount = ""

    private lateinit var buttons: List<Button>

    @Inject
    lateinit var sharedPreferences: KsPrefs

    private var selectedButton = 0

    var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                amount = ""
                formattedAmount = ""
                binding.amountValue.setText(amount)
            }
        }


    private val someActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            // Handle result here
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTipBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsToRoot(binding.root)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.statusBarColor = ContextCompat.getColor(this, R.color.backgroundAmount)

        binding.removeCustomAmount.isSelected = true
        selectedButton = binding.removeCustomAmount.id

        pinAdapter = PinAdapter(applicationContext)


        amount = intent.getStringExtra("Amount").toString()

//        binding.enterAmount.setText(AmountUtil.formatAmount(amount))
//        binding.totalAmount.setText("${AmountUtil.formatAmount(amount)}  ${SupercaseConfig.CURRENCY_STRING}")
        binding.amountValue.setText("${AmountUtil.formatAmount(amount)}")


        binding.amountValue.inputType = InputType.TYPE_NULL

        binding.addCustomAmountButton.setOnClickListener {
            // showTipDialog()

            val intent = Intent(this, CustomAmountActivity::class.java)
            //startActivity(intent)
            startActivityForResult(intent, 2)

        }

        KeyboardUtil.populateKeyboardDataList(dataList)


        binding.proceed.setOnClickListener {
            if (sharedPreferences.pull(SharedPreferencesKeys.DUMMY, false)) {
                Toast.makeText(this, "Dummy User", Toast.LENGTH_LONG).show()

                return@setOnClickListener

            }
            val intent = Intent(this, PosActivity::class.java)
            if (amount != null) {
                if (amount.isEmpty()) {
                    openDialog()
                } else {
                    val formattedAmount = "$amount"
                    logger.info { "Sending amount: $amount" }
                    intent.putExtra("Amount", formattedAmount)
                    if (tip != "" && tip != "0.00") {
                        intent.putExtra("Tip", tip)
                    }
                    intent.putExtra("TotalAmount", totalAmount)


                    resultLauncher.launch(intent)
                    finish()
                }
            }
        }

        binding.back.setOnClickListener {
            finish()
        }

        binding.removeCustomAmount.setOnClickListener {
            binding.amountValue.setText("${AmountUtil.formatAmount(amount)}")

            val buttonName =
                binding.removeCustomAmount.resources.getResourceEntryName(binding.removeCustomAmount.id)
            val checkIconId = resources.getIdentifier("${buttonName}Icon", "id", packageName)
            val checkIcon = findViewById<FloatingActionButton>(checkIconId)

            checkIcon.visibility = View.VISIBLE
            binding.removeCustomAmount.isSelected = true

            val selectedButton = findViewById<Button>(selectedButton)
            if (selectedButton != null && binding.removeCustomAmount.id != this.selectedButton) {
                selectedButton.isSelected = false

                val buttonNamePrevious =
                    selectedButton.resources.getResourceEntryName(selectedButton.id)
                val checkIconIdPrevious =
                    resources.getIdentifier("${buttonNamePrevious}Icon", "id", packageName)
                val checkIconPrevious = findViewById<FloatingActionButton>(checkIconIdPrevious)

                checkIconPrevious.visibility = View.GONE
            }

            tip = ""
            this.selectedButton = binding.removeCustomAmount.id
        }

        //videti ovo
        binding.currency.text = SupercaseConfig.CURRENCY_STRING


        val buttons = listOf(
            binding.button1,
            binding.button2,
            binding.button3,
            binding.button4
        )
        buttons.forEach { button ->
            button.setOnClickListener { increaseAmountByButton(button) }
        }
    }

    private fun showCustomTipDialog() {
        // Create an EditText for user input
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Enter custom tip"
            gravity = Gravity.CENTER
        }

        // Build an AlertDialog
        AlertDialog.Builder(this)
            .setTitle("Add Custom Tip")
            .setView(input)
            .setPositiveButton("Add") { dialog, _ ->
                val customTip = input.text.toString().toDoubleOrNull()
                if (customTip != null) {
                    addCustomTip(customTip)
                } else {
                    Toast.makeText(
                        this,
                        "Invalid input. Please enter a valid number.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun addCustomTip(customTip: Double) {
        //val currentAmount = binding.totalAmount.text.toString()

        val currentAmount = binding.amountValue.text.toString()
            .replace("[^\\d.]".toRegex(), "")
            .toDoubleOrNull() ?: 0.0

        val updatedAmount = currentAmount + customTip
        binding.amountValue.text = String.format("%.2f", updatedAmount)
    }


    private fun increaseAmountByButton(button: Button) {


        // button.setBackgroundResource(R.drawable.bg_tip_selected)
        button.isSelected = true


        val buttonName = button.resources.getResourceEntryName(button.id)
        val checkIconId = resources.getIdentifier("${buttonName}Icon", "id", packageName)
        val checkIcon = findViewById<FloatingActionButton>(checkIconId)

        checkIcon.visibility = View.VISIBLE

        val selectedButton = findViewById<Button>(selectedButton)

        if (selectedButton != null && button.id != this.selectedButton) {
            selectedButton.isSelected = false

            val buttonNamePrevious =
                selectedButton.resources.getResourceEntryName(selectedButton.id)
            val checkIconIdPrevious =
                resources.getIdentifier("${buttonNamePrevious}Icon", "id", packageName)
            val checkIconPrevious = findViewById<FloatingActionButton>(checkIconIdPrevious)

            checkIcon.visibility = View.VISIBLE
            checkIconPrevious.visibility = View.GONE
        }

        this.selectedButton = button.id






        tipPercentage = button.text.toString().removeSuffix("%").toDouble()

        println("OLD Amount: $amount")
        val numericAmount = formatInputToDecimal(amount)


        val updatedAmount = numericAmount + (numericAmount * tipPercentage / 100)
        println("updatedAmount: $updatedAmount")


        totalAmount = String.format("%.2f", updatedAmount).replace(".", "")
            .replace(",", "") // Format back to string with 2 decimal places

        tip = String.format("%.2f", updatedAmount - numericAmount).replace(".", "").replace(",", "")

        println("New Tip: $tip")
        println("New Amount: $totalAmount")

        binding.amountValue.setText("${AmountUtil.formatAmount(totalAmount)}")
        println("New Amount: $totalAmount") // Replace with your display logic (e.g., update a TextView)


    }

    private fun formatInputToDecimal(input: String): Double {
        // Divide the input by 100 to convert it to decimal format
        val numericValue = input.toDoubleOrNull() ?: 0.0
        return numericValue / 100
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
    }

    override fun onResume() {
        super.onResume()
        handleDarkLightMode()
    }

    private fun handleDarkLightMode() {
        if (sharedPreferences.pull(SharedPreferencesKeys.IS_DARK_MODE, false)) {
            binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.globalBlack))
            DrawableCompat.setTint(
                DrawableCompat.wrap(binding.back.drawable),
                ContextCompat.getColor(this, R.color.colorPrimary)
            )
            binding.amountValue.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.amountValue.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.globalBlackDialog
                )
            )
//            ContextCompat.getColorStateList(this, R.color.suffixDarkMode)?.let {
//                binding.enterAmountLayout.setSuffixTextColor(
//                    it
//                )
//            }
        } else {
            binding.root.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
            DrawableCompat.setTint(
                DrawableCompat.wrap(binding.back.drawable),
                ContextCompat.getColor(this, R.color.colorPrimary)
            )
            binding.amountValue.setTextColor(ContextCompat.getColor(this, R.color.bigLabelBlack))
            binding.amountValue.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    R.color.amountBackground
                )
            )
//            ContextCompat.getColorStateList(this, R.color.suffixDarkMode)?.let {
//                binding.enterAmountLayout.setSuffixTextColor(
//                    it
//                )
//            }
        }
    }


    private fun showTipDialog() {
        val dialog = Dialog(this)
        val bindingDialog: DialogTipBinding = DialogTipBinding.inflate(layoutInflater)

        var enteredTip = ""

        bindingDialog.btn.setOnClickListener {
            if (!enteredTip.isEmpty()) {
                totalAmount = (amount.toInt() + enteredTip.toInt()).toString()
                tip = enteredTip.toInt().toString()

//                binding.totalAmount.setText("${AmountUtil.formatAmount(totalAmount)}  ${SupercaseConfig.CURRENCY_STRING}")
                binding.amountValue.setText("${AmountUtil.formatAmount(totalAmount)}")


                val buttonName =
                    binding.addCustomAmountButton.resources.getResourceEntryName(binding.addCustomAmountButton.id)
                val checkIconId = resources.getIdentifier("${buttonName}Icon", "id", packageName)
                val checkIcon = findViewById<FloatingActionButton>(checkIconId)

                checkIcon.visibility = View.VISIBLE

                val selectedButton = findViewById<Button>(selectedButton)
                if (selectedButton != null) {
                    selectedButton.isSelected = false

                    val buttonNamePrevious =
                        selectedButton.resources.getResourceEntryName(selectedButton.id)
                    val checkIconIdPrevious =
                        resources.getIdentifier("${buttonNamePrevious}Icon", "id", packageName)
                    val checkIconPrevious = findViewById<FloatingActionButton>(checkIconIdPrevious)

                    checkIconPrevious.visibility = View.GONE
                }

                this.selectedButton = binding.addCustomAmountButton.id

            }



            getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            )

            dialog.dismiss()
        }
        bindingDialog.btnCancel.setOnClickListener {
            getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            )
            dialog.dismiss()
        }

        val text = "${getString(R.string.label_enter_tip)}"
        bindingDialog.enterTipLayout.suffixText = SupercaseConfig.CURRENCY_STRING
        bindingDialog.label.text = text

        bindingDialog.enterTip.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val input = s.toString().replace("[^\\d]".toRegex(), "")
                    .replace(".", "") // Remove non-numeric characters
                if (input.isNotEmpty()) {

                    logger.info { "input:" + input }
                    logger.info { "output: " + formatToDecimal(input) }

                    enteredTip = input.toInt().toString()

                    bindingDialog.enterTip.removeTextChangedListener(this)
                    bindingDialog.enterTip.setText(formatToDecimal(input))
                    bindingDialog.enterTip.setSelection(formatToDecimal(input).length)
                    bindingDialog.enterTip.addTextChangedListener(this)
                } else {
                    enteredTip = ""
                    logger.info { "input: " + "0.00" }
                }


            }
        })

        dialog.setContentView(bindingDialog.root)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        dialog.show()
        bindingDialog.enterTip.requestFocus()

        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY)
    }

    fun formatToDecimal(input: String): String {
        var inputToFormat = input.toInt().toString() //deleting leading 0

        // Remove any non-numeric characters
        val numericInput = inputToFormat.replace("[^\\d]".toRegex(), "")

        // Handle empty input case
        if (numericInput.isEmpty()) return "0.00"

        // Convert to the desired format
        return if (numericInput.length > 2) {
            val integerPart = numericInput.substring(0, numericInput.length - 2)
            val decimalPart = numericInput.substring(numericInput.length - 2)
            "$integerPart.$decimalPart"
        } else {
            val paddedInput = numericInput.padStart(2, '0')
            "0.${paddedInput}"
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2 && resultCode == RESULT_OK) {

            if (data != null && data.getStringExtra("Tips") != null) {

                try {
                    if (!data.getStringExtra("Tips")!!.isEmpty()) {
                        val enteredTip = data.getStringExtra("Tips")
                        totalAmount = (amount.toInt() + enteredTip!!.toInt()).toString()
                        tip = enteredTip.toInt().toString()
                        binding.amountValue.setText("${AmountUtil.formatAmount(totalAmount)}")


                        val buttonName =
                            binding.addCustomAmountButton.resources.getResourceEntryName(binding.addCustomAmountButton.id)
                        val checkIconId =
                            resources.getIdentifier("${buttonName}Icon", "id", packageName)
                        val checkIcon = findViewById<FloatingActionButton>(checkIconId)

                        checkIcon.visibility = View.VISIBLE

                        val selectedButton = findViewById<Button>(selectedButton)
                        if (selectedButton != null && binding.addCustomAmountButton.id != this.selectedButton) {
                            selectedButton.isSelected = false

                            val buttonNamePrevious =
                                selectedButton.resources.getResourceEntryName(selectedButton.id)
                            val checkIconIdPrevious = resources.getIdentifier(
                                "${buttonNamePrevious}Icon",
                                "id",
                                packageName
                            )
                            val checkIconPrevious =
                                findViewById<FloatingActionButton>(checkIconIdPrevious)

                            checkIconPrevious.visibility = View.GONE
                        }

                        binding.addCustomAmountButton.isSelected = true
                        this.selectedButton = binding.addCustomAmountButton.id

                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

}