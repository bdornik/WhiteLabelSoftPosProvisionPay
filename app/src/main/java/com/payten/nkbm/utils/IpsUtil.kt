package com.payten.nkbm.utils

import com.cioccarellia.ksprefs.KsPrefs
import com.payten.nkbm.dto.QRDto
import mu.KotlinLogging
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class IpsUtil {
    companion object {
        private val logger = KotlinLogging.logger {}

        enum class Tags {
            K, V, C, R, N, I, O, P, SF, S, M, JS, RK, RO, RL, RP
        }

        val VALUE_DELIMITER = ":"
        val LINE_DELIMITER = "|"
        val MAX_ACC_CAR_NO = 18

        fun getDate(): String {
            val formatter = SimpleDateFormat("MMddHHmmss", Locale.getDefault())
            return formatter.format(Date())
        }

        fun generateQrCodeContent(qr: QRDto): String {


            val stringBuilder = StringBuilder()
            stringBuilder
                .append("001")
                .append(LINE_DELIMITER)
                .append("POTASI20")
                .append(LINE_DELIMITER)
                .append(qr.terminalIdentification)
                .append(LINE_DELIMITER)
                .append(qr.merchantIdentification)
                .append(LINE_DELIMITER)
                .append(qr.payerAccNumber)
                .append(LINE_DELIMITER)
                .append(qr.amountAndCurrency.replace(",", ""))
                .append(LINE_DELIMITER)
                .append(qr.creditTransferIdentificator)

                .append(LINE_DELIMITER)
                .append(padleft(qr.stan, 6, '0'))
                .append(LINE_DELIMITER)
                .append(qr.date)
                .append(LINE_DELIMITER)
                .append(qr.mcc)
            return stringBuilder.toString()
        }

        fun createPaymentIdentificatorReference(tid: String, counter: Int, date: String): String {
            logger.info { "Creatin paymentIdentificator tid: $tid counter: $counter" }
            var paymentIdentificationReference = ""
            try {
                paymentIdentificationReference =
                   "POTASI20" + tid + date + padleft(counter.toString(), 6, '0')
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            return paymentIdentificationReference
        }


        fun getJulianDate(d: Date?): String? {
            val day: String =
                formatDate(d, "DDD", TimeZone.getDefault())!!
            val year: String =
                formatDate(d, "yy", TimeZone.getDefault())!!
            return year + day
        }

        fun formatDate(d: Date?, pattern: String?, timeZone: TimeZone?): String? {
            val df = DateFormat.getDateTimeInstance() as SimpleDateFormat
            df.timeZone = timeZone
            df.applyPattern(pattern)
            return df.format(d)
        }

        fun padleft(s: String, len: Int, c: Char): String? {
            var s = s
            s = s.trim { it <= ' ' }
            if (s.length > len) {
                return s.substring(s.length - 6)
            }
            val d = StringBuilder(len)
            var fill = len - s.length
            while (fill-- > 0) d.append(c)
            d.append(s)
            return d.toString()
        }

    }
}