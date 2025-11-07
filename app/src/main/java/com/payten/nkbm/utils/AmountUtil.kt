package com.payten.nkbm.utils

import com.payten.nkbm.config.SupercaseConfig
import com.payten.nkbm.databinding.ActivityAmountBinding
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class AmountUtil {
    companion object {
        fun onButtonClicked(
            value: String,
            RESULT_BACK: String,
            amount: String,
            binding: ActivityAmountBinding
        ): String {
            var mutableAmount = amount
            if (value != RESULT_BACK) {
                if (value.equals("0", true) && binding.enterAmount.text.toString().isEmpty()) {
                    return mutableAmount
                }
                mutableAmount += value
                var formattedAmount = ""
                if (mutableAmount.length == 1) {
                    formattedAmount = "0,0$mutableAmount"

                } else if (mutableAmount.length == 2) {
                    formattedAmount = "0,$mutableAmount"

                } else {

                    val preDotsString = "${mutableAmount.subSequence(0, mutableAmount.length - 2)}"
                    val postDotsString = "${mutableAmount.subSequence(mutableAmount.length - 2, mutableAmount.length)}"
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
                mutableAmount = mutableAmount.dropLast(1)
                if (mutableAmount.isEmpty()) {
                    binding.enterAmount.setText(mutableAmount)
                } else {
                    var formattedAmount = ""
                    if (mutableAmount.length == 1) {
                        formattedAmount = "0,0$mutableAmount"

                    } else if (mutableAmount.length == 2) {
                        formattedAmount = "0,$mutableAmount"

                    } else {

                        val preDotsString = "${mutableAmount.subSequence(0, mutableAmount.length - 2)}"
                        val postDotsString = "${mutableAmount.subSequence(mutableAmount.length - 2, mutableAmount.length)}"
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

            return mutableAmount
        }


        fun formatAmount(amount: String?): String {
            var formattedAmount = ""
            if (amount?.length == 1) {
                formattedAmount = "0,0$amount"
            } else if (amount?.length == 2) {
                formattedAmount = "0,$amount"
            } else {
                var preDotsString = "${amount?.subSequence(0, amount.length - 2)}"
                val postDotsString = "${amount?.subSequence(amount.length - 2, amount.length)}"
                formattedAmount = "${
                    preDotsString.reversed().chunked(3).joinToString(".").reversed()
                },$postDotsString"
            }

            return formattedAmount
        }

        fun setAmount(amount: String): Long {
            return amount.replace(",", "").toLong()
        }

        fun getAmount(amountIn: String): String {
            var formattedAmount = ""

            val symbols = DecimalFormatSymbols(Locale.ENGLISH)
            val df = DecimalFormat("###,###,###.00", symbols)
            df.roundingMode = RoundingMode.DOWN
            val roundoff = df.format(amountIn.toDouble())

            var amount = "${roundoff!!.replace(".", "")}"
            if (amountIn.toDouble() < 0.1) {
                formattedAmount = "0,$amount"
            } else if (amountIn.toDouble() < 1) {
                formattedAmount = "0,$amount"
            } else {
                var preDotsString = "${amount?.subSequence(0, amount.length - 2)}"
                val postDotsString = "${amount?.subSequence(amount.length - 2, amount.length)}"
                preDotsString = preDotsString.replace(",", ".")
                formattedAmount = "$preDotsString,$postDotsString"
            }
            return "${formattedAmount} ${SupercaseConfig.CURRENCY_STRING}"

        }

        fun getAmountNoCurr(amountIn: String): String {
            var formattedAmount = ""

            val symbols = DecimalFormatSymbols(Locale.ENGLISH)
            val df = DecimalFormat("###,###,###.00", symbols)
            df.roundingMode = RoundingMode.DOWN
            val roundoff = df.format(amountIn.toDouble())

            var amount = "${roundoff!!.replace(".", "")}"
            if (amountIn.toDouble() < 0.1) {
                formattedAmount = "0,$amount"
            } else if (amountIn.toDouble() < 1) {
                formattedAmount = "0,$amount"
            } else {
                var preDotsString = "${amount?.subSequence(0, amount.length - 2)}"
                val postDotsString = "${amount?.subSequence(amount.length - 2, amount.length)}"
                preDotsString = preDotsString.replace(",", ".")
                formattedAmount = "$preDotsString,$postDotsString"
            }
            return formattedAmount

        }

        fun getAmountWithOutCurr(amountIn: String): String {
            var formattedAmount = ""

            val symbols = DecimalFormatSymbols(Locale.ENGLISH)
            val df = DecimalFormat("###,###,###.00", symbols)
            df.roundingMode = RoundingMode.DOWN
            val roundoff = df.format(amountIn.toDouble())

            var amount = "${roundoff.replace(".", "")}"
            if (amountIn.toDouble() < 0.1) {
                formattedAmount = "0,$amount"
            } else if (amountIn.toDouble() < 1) {
                formattedAmount = "0,$amount"
            } else {
                var preDotsString = "${amount?.subSequence(0, amount.length - 2)}"
                val postDotsString = "${amount?.subSequence(amount.length - 2, amount.length)}"
                preDotsString = preDotsString.replace(",", ".")
                formattedAmount = "$preDotsString,$postDotsString"
            }
            return "${formattedAmount}"

        }

        fun dialogAmount(amount: String): String {

            var amount = "${String.format("%.2f", amount.toFloat())}"
                .replace(".", "")
            amount = amount.replace(",", "")

            var preDotsString = "${amount?.subSequence(0, amount.length - 2)}"
            val postDotsString = "${amount?.subSequence(amount.length - 2, amount.length)}"
            amount = "${
                preDotsString.reversed()
                    .chunked(3)
                    .joinToString(".")
                    .reversed()
            },$postDotsString"
            return "$amount ${SupercaseConfig.CURRENCY_STRING}"
        }

        fun adapterAmount(amountIn: String): String {
            var formattedAmount = ""

            val symbols = DecimalFormatSymbols(Locale.ENGLISH)
            val df = DecimalFormat("###,###,###.00", symbols)
            df.roundingMode = RoundingMode.DOWN
            val roundoff = df.format(amountIn.toDouble())

            var amount = "${roundoff.replace(".", "")}"
            if (amountIn.toDouble() < 0.1) {
                formattedAmount = "0,$amount"
            } else if (amountIn.toDouble() < 1) {
                formattedAmount = "0,$amount"
            } else {
                var preDotsString = "${amount?.subSequence(0, amount.length - 2)}"
                val postDotsString = "${amount?.subSequence(amount.length - 2, amount.length)}"
                preDotsString = preDotsString.replace(",", ".")
                formattedAmount = "$preDotsString,$postDotsString"
            }
            return "${formattedAmount} ${SupercaseConfig.CURRENCY_STRING}"

        }

        fun voidAmount(packageName: Boolean, amount: String?): Pair<Long, String> {
            if (packageName) {

                return Pair(amount!!.toLong(), formatAmount(amount.toLong()))

            } else {
                var sdkAmount = Math.round(amount!!.toDouble() * 100)

                return Pair(sdkAmount, formatAmount(sdkAmount))
            }


        }

        fun stringPretty(amount: String): String{
            return amount

        }

        private fun formatAmount(sdkAmount: Long): String {
            var formattedAmount = ""
            formattedAmount = sdkAmount.toString()
            if (formattedAmount.length == 1) {
                formattedAmount = "0,0$formattedAmount"
            } else if (formattedAmount.length == 2)
                formattedAmount = "0,$formattedAmount"
            else {
                var preDotsString = "${formattedAmount?.subSequence(0, formattedAmount.length - 2)}"
                val postDotsString = "${
                    formattedAmount?.subSequence(
                        formattedAmount.length - 2,
                        formattedAmount.length
                    )
                }"
                formattedAmount = "${
                    preDotsString.reversed()
                        .chunked(3)
                        .joinToString(".")
                        .reversed()
                },$postDotsString"
            }
            return formattedAmount
        }

    }
}