package com.payten.nkbm.utils

import android.content.Context
import android.icu.text.DateFormat.HourCycle
import android.util.Log
import com.payten.nkbm.R
import mu.withLoggingContext
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeFormatterBuilder
import java.util.*

class DateUtil {
    companion object {

        private const val TAG = "DateUtil"
        var formatter: DateTimeFormatter? = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("dd-MM-yyyy HH:mm")
            .toFormatter(Locale.ENGLISH)

        val formatter2: DateTimeFormatter = DateTimeFormatterBuilder()
            // Case insensitive parsing
            .parseCaseInsensitive()
            // Date and time format pattern
            .appendPattern("EEE MMM dd HH:mm:ss zzz yyyy")
            // Set locale to English
            .toFormatter(Locale.ENGLISH)
            // Ensure parsing is in UTC time zone
            .withZone(ZoneOffset.UTC)

        fun localDateFromString(dateTime: String?, traffic: Boolean?): LocalDateTime? {
            if (dateTime == null) {
                return null
            }
            try {
                if (traffic == true) {
                    var time = LocalDateTime.parse(dateTime, formatter)
                    return time
                } else {
                    var timed = convertToUTC(dateTime)
//                var time = LocalDateTime.parse(timed, formatter2)
                    var time = LocalDateTime.parse(timed, formatter)

                    return time
                }
            } catch (exception: Exception) {
                return null
            }
        }

        fun convertToUTC(dateTimeString: String): String? {
            return try {
                // Define the input and output date time format
                val inputFormat =
                    DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss 'GMT'xxx yyyy", Locale.ENGLISH)
                val outputFormat =
                    DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss 'GMT'xxx yyyy", Locale.ENGLISH)

                // Parse the input string to ZonedDateTime
                val zonedDateTime = ZonedDateTime.parse(dateTimeString, inputFormat)

                // Convert to UTC by adjusting the time zone
                val utcZonedDateTime = zonedDateTime.withZoneSameInstant(ZoneOffset.UTC)

                // Format the adjusted ZonedDateTime to the desired output format
                utcZonedDateTime.format(outputFormat)
            } catch (e: Exception) {
                null
            }
        }

        fun getMonthNameLong(number: Int, context: Context): String {
            if (number == 1) {
                return context.resources.getString(R.string.month_1_long)
            } else if (number == 2) {
                return context.resources.getString(R.string.month_2_long)
            } else if (number == 3) {
                return context.resources.getString(R.string.month_3_long)
            } else if (number == 4) {
                return context.resources.getString(R.string.month_4_long)
            } else if (number == 5) {
                return context.resources.getString(R.string.month_5_long)
            } else if (number == 6) {
                return context.resources.getString(R.string.month_6_long)
            } else if (number == 7) {
                return context.resources.getString(R.string.month_7_long)
            } else if (number == 8) {
                return context.resources.getString(R.string.month_8_long)
            } else if (number == 9) {
                return context.resources.getString(R.string.month_9_long)
            } else if (number == 10) {
                return context.resources.getString(R.string.month_10_long)
            } else if (number == 11) {
                return context.resources.getString(R.string.month_11_long)
            } else if (number == 12) {
                return context.resources.getString(R.string.month_12_long)
            }

            return ""
        }
    }
}