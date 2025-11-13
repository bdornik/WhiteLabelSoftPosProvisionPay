package com.payten.whitelabel.activities

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.R
import com.payten.whitelabel.databinding.ActivityFilterBinding
import com.payten.whitelabel.enums.TransactionSortType
import com.payten.whitelabel.enums.TransactionSource
import com.payten.whitelabel.enums.TransactionStatusFilterType
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import dagger.hilt.android.AndroidEntryPoint
import mu.KotlinLogging
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class FilterActivity : BaseActivity() {
    private val logger = KotlinLogging.logger {}

    private lateinit var binding: ActivityFilterBinding
    private lateinit var calendar: Calendar
    private lateinit var datePicker: DatePickerDialog.OnDateSetListener
    private lateinit var timePicker: TimePickerDialog.OnTimeSetListener

    @Inject
    lateinit var sharedPreferences: KsPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFilterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsetsToRoot(binding.root)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)

        loadFilters()

        changeVisibility()

        hideSoftKeyboard()

        setLanguage()

        if (!sharedPreferences.pull(SharedPreferencesKeys.IPS_EXISTS,false)){
            binding.labelTransactionType.visibility = View.GONE
            binding.layoutTransactionType.visibility = View.GONE
            binding.arrowType.visibility = View.GONE
        }


        binding.iconClose.setOnClickListener {
            finish()
        }

        binding.back.setOnClickListener {
            finish()
        }

        binding.applyFilter.setOnClickListener {
            saveFilters()
            finish()
        }
        binding.clearFilter.setOnClickListener {
            sharedPreferences.remove(SharedPreferencesKeys.FILTER_TYPE)
            sharedPreferences.remove(SharedPreferencesKeys.FILTER_STATUS)
            sharedPreferences.remove(SharedPreferencesKeys.FILTER_SORT)
            sharedPreferences.remove(SharedPreferencesKeys.DATE_FROM)
            sharedPreferences.remove(SharedPreferencesKeys.DATE_TO)
            finish()
        }

        calendar = Calendar.getInstance()

        binding.editFrom.setOnClickListener {
            calendarFrom()
            var dp = DatePickerDialog(
                this, datePicker, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )



            if (AppCompatDelegate.getApplicationLocales().toLanguageTags().equals("sl"))
                dp.setButton(DatePickerDialog.BUTTON_POSITIVE, "VREDU", dp);

            dp.show()
            dp.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY)
            if (AppCompatDelegate.getApplicationLocales().equals("sl"))
                dp.getButton(DatePickerDialog.BUTTON_POSITIVE).setText("VREDU")


        }

        binding.editTo.setOnClickListener {
            calendarTo()
            var dp = DatePickerDialog(
                this, datePicker, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            if (AppCompatDelegate.getApplicationLocales().toLanguageTags().equals("sl"))
                dp.setButton(DatePickerDialog.BUTTON_POSITIVE, "VREDU", dp);
            dp.show()
            dp.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY)
        }

        binding.fromContainer.setOnClickListener {
            calendarFrom()
            var dp = DatePickerDialog(
                this, datePicker, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            if (AppCompatDelegate.getApplicationLocales().toLanguageTags().equals("sl"))
                dp.setButton(DatePickerDialog.BUTTON_POSITIVE, "VREDU", dp);
            dp.show()
            dp.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY)


        }

        binding.toContainer.setOnClickListener {
            calendarTo()
            var dp = DatePickerDialog(
                this, datePicker, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            if (AppCompatDelegate.getApplicationLocales().toLanguageTags().equals("sl"))
                dp.setButton(DatePickerDialog.BUTTON_POSITIVE, "VREDU", dp);
            dp.show()
            dp.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY)
        }

    }

    private fun saveFilters() {
        var selectedBtnType = binding.typeGroup.checkedRadioButtonId
        var selectedBtnStatus = binding.statusGroup.checkedRadioButtonId
        var selectedBtnSort = binding.sortGroup.checkedRadioButtonId
        val periodFrom = binding.editFrom.text.toString()
        val periodTo = binding.editTo.text.toString()

        if (selectedBtnType == R.id.radioBtnPos) {
            selectedBtnType = TransactionSource.POS.ordinal
        } else {
            selectedBtnType = TransactionSource.IPS.ordinal
        }

        if (selectedBtnStatus == R.id.radioBtnAll) {
            selectedBtnStatus = TransactionStatusFilterType.ALL.ordinal
        } else if (selectedBtnStatus == R.id.radioBtnAccepted) {
            selectedBtnStatus = TransactionStatusFilterType.ACCEPTED.ordinal
        } else if (selectedBtnStatus == R.id.radioBtnRejected) {
            selectedBtnStatus = TransactionStatusFilterType.REJECTED.ordinal
        } else if (selectedBtnStatus == R.id.radioBtnCanceled) {
            selectedBtnStatus = TransactionStatusFilterType.VOID.ordinal
        }

        if (selectedBtnSort == R.id.radioBtnDateAsc) {
            selectedBtnSort = TransactionSortType.DateAsc.ordinal
        } else if (selectedBtnSort == R.id.radioBtnDateDsc) {
            selectedBtnSort = TransactionSortType.DateDesc.ordinal
        } else if (selectedBtnSort == R.id.radioBtnAmountAsc) {
            selectedBtnSort = TransactionSortType.AmountAsc.ordinal
        } else if (selectedBtnSort == R.id.radioBtnAmountDsc) {
            selectedBtnSort = TransactionSortType.AmountDesc.ordinal
        }

        sharedPreferences.push(SharedPreferencesKeys.FILTER_TYPE, selectedBtnType)
        sharedPreferences.push(SharedPreferencesKeys.FILTER_STATUS, selectedBtnStatus)
        sharedPreferences.push(SharedPreferencesKeys.FILTER_SORT, selectedBtnSort)
        sharedPreferences.push(SharedPreferencesKeys.DATE_FROM, periodFrom)
        sharedPreferences.push(SharedPreferencesKeys.DATE_TO, periodTo)
    }

    private fun loadFilters() {
        val selectedBtnType =
            sharedPreferences.pull(SharedPreferencesKeys.FILTER_TYPE, TransactionSource.POS.ordinal)
        val selectedBtnStatus = sharedPreferences.pull(
            SharedPreferencesKeys.FILTER_STATUS,
            TransactionStatusFilterType.ALL.ordinal
        )
        val selectedBtnSort = sharedPreferences.pull(
            SharedPreferencesKeys.FILTER_SORT,
            TransactionSortType.DateDesc.ordinal
        )
        val periodFrom = sharedPreferences.pull(SharedPreferencesKeys.DATE_FROM, "")
        val periodTo = sharedPreferences.pull(SharedPreferencesKeys.DATE_TO, "")

        if (selectedBtnType == TransactionSource.POS.ordinal) {
            val btn: RadioButton = findViewById(R.id.radioBtnPos)
            btn.isChecked = true
        } else {
            val btn: RadioButton = findViewById(R.id.radioBtnIps)
            btn.isChecked = true
        }

        if (selectedBtnStatus == TransactionStatusFilterType.ALL.ordinal) {
            val btn: RadioButton = findViewById(R.id.radioBtnAll)
            btn.isChecked = true
        } else if (selectedBtnStatus == TransactionStatusFilterType.ACCEPTED.ordinal) {
            val btn: RadioButton = findViewById(R.id.radioBtnAccepted)
            btn.isChecked = true
        } else if (selectedBtnStatus == TransactionStatusFilterType.REJECTED.ordinal) {
            val btn: RadioButton = findViewById(R.id.radioBtnRejected)
            btn.isChecked = true
        } else if (selectedBtnStatus == TransactionStatusFilterType.VOID.ordinal) {
            val btn: RadioButton = findViewById(R.id.radioBtnCanceled)
            btn.isChecked = true
        }

        if (selectedBtnSort == TransactionSortType.DateAsc.ordinal) {
            val btn: RadioButton = findViewById(R.id.radioBtnDateAsc)
            btn.isChecked = true
        } else if (selectedBtnSort == TransactionSortType.DateDesc.ordinal) {
            val btn: RadioButton = findViewById(R.id.radioBtnDateDsc)
            btn.isChecked = true
        } else if (selectedBtnSort == TransactionSortType.AmountAsc.ordinal) {
            val btn: RadioButton = findViewById(R.id.radioBtnAmountAsc)
            btn.isChecked = true
        } else if (selectedBtnSort == TransactionSortType.AmountDesc.ordinal) {
            val btn: RadioButton = findViewById(R.id.radioBtnAmountDsc)
            btn.isChecked = true
        }

        binding.editFrom.setText(periodFrom)
        binding.editTo.setText(periodTo)

    }

    private fun clearFilter() {
        binding.typeGroup.clearCheck()
        binding.statusGroup.clearCheck()
        binding.sortGroup.clearCheck()
        binding.editFrom.setText("")
        binding.editTo.setText("")
    }

    private fun changeVisibility() {
        binding.arrowDate.setOnClickListener {
            binding.layoutDate.toggleVisibility()
            it.animate().rotation(it.rotation - ARROW_ROTATION).start()
        }

        binding.arrowType.setOnClickListener {
            binding.layoutTransactionType.toggleVisibility()
            it.animate().rotation(it.rotation - ARROW_ROTATION).start()
        }

        binding.arrowStatus.setOnClickListener {
            binding.layoutTransactionStatus.toggleVisibility()
            it.animate().rotation(it.rotation - ARROW_ROTATION).start()
        }

        binding.arrowSort.setOnClickListener {
            binding.layoutTransactionSort.toggleVisibility()
            it.animate().rotation(it.rotation - ARROW_ROTATION).start()
        }
    }

    private fun calendarFrom() {
        val sdf = SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH)
        datePicker = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            binding.editFrom.setText(sdf.format(calendar.time))
            var monthString = "${month}"
            if (month < 10) {
                monthString = "0${month}"
            }

            var dayString = "${dayOfMonth}"
            if (dayOfMonth < 10) {
                dayString = "0${dayOfMonth}"
            }
            val dateFrom = "$dayString-$monthString-$year"
            sharedPreferences.push(SharedPreferencesKeys.DATE_FROM, dateFrom)

            timePicker = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
                var date = sharedPreferences.pull(SharedPreferencesKeys.DATE_FROM, "")
                sharedPreferences.push(SharedPreferencesKeys.DATE_FROM, "${date} $hour:$minute")
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                binding.editFrom.setText(sdf.format(calendar.time))
            }

            val timePickerDialog = TimePickerDialog(
                this@FilterActivity, timePicker, 0, 0,
                true
            )
            if (AppCompatDelegate.getApplicationLocales().toLanguageTags().equals("sl"))
                timePickerDialog.setButton(
                    DatePickerDialog.BUTTON_POSITIVE,
                    "VREDU",
                    timePickerDialog
                );
            timePickerDialog.show()
            timePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY)
        }
    }

    private fun calendarTo() {
        val sdf = SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH)
        datePicker = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            binding.editTo.setText(sdf.format(calendar.time))

            var monthString = "${month}"
            if (month < 10) {
                monthString = "0${month}"
            }

            var dayString = "${dayOfMonth}"
            if (dayOfMonth < 10) {
                dayString = "0${dayOfMonth}"
            }
            val dateTo = "$dayString-$monthString-$year"
            sharedPreferences.push(SharedPreferencesKeys.DATE_TO, dateTo)

            timePicker = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
                var date = sharedPreferences.pull(SharedPreferencesKeys.DATE_TO, "")
                sharedPreferences.push(SharedPreferencesKeys.DATE_TO, "${date} $hour:$minute")
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                binding.editTo.setText(sdf.format(calendar.time))
            }

            val timePickerDialog = TimePickerDialog(
                this@FilterActivity, timePicker, 0, 0,
                true
            )
            if (AppCompatDelegate.getApplicationLocales().toLanguageTags().equals("sl"))
                timePickerDialog.setButton(
                    DatePickerDialog.BUTTON_POSITIVE,
                    "VREDU",
                    timePickerDialog
                );
            timePickerDialog.show()
            timePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY)
        }
    }

    private fun View.toggleVisibility() {
        visibility = if (visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun hideSoftKeyboard() {
        binding.editFrom.inputType = InputType.TYPE_NULL
        binding.editTo.inputType = InputType.TYPE_NULL
    }

    private fun setLanguage() {
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags().equals("sl")) {
            val languageToLoad = "sl" // your language
            val locale = Locale(languageToLoad)
            Locale.setDefault(locale)
        } else {
            val languageToLoad = "en" // your language
            val locale = Locale(languageToLoad)
            Locale.setDefault(locale)
        }
    }

    companion object {
        const val ARROW_ROTATION = 180
        const val NO_SELECTED_BUTTON = -1
        const val RADIO_BUTTON_FALLBACK = "-1"
        const val DATE_FORMAT = "dd-MM-yyyy HH:mm"
    }
}