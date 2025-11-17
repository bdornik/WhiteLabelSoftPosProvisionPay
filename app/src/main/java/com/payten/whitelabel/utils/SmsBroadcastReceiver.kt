package com.payten.whitelabel.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status

class SmsBroadcastReceiver(
    private val onSmsReceived: (Intent) -> Unit
) : BroadcastReceiver() {

    private val TAG = "SmsBroadcastReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == SmsRetriever.SMS_RETRIEVED_ACTION) {
            val extras = intent.extras
            val status = extras?.get(SmsRetriever.EXTRA_STATUS) as? Status

            when (status?.statusCode) {
                CommonStatusCodes.SUCCESS -> {
                    Log.d(TAG, "SMS retrieved successfully")
                    val consentIntent = extras.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)
                    if (consentIntent != null) {
                        onSmsReceived(consentIntent)
                    }
                }
                CommonStatusCodes.TIMEOUT -> {
                    Log.d(TAG, "SMS retrieval timeout")
                }
                else -> {
                    Log.d(TAG, "SMS retrieval failed: ${status?.statusCode}")
                }
            }
        }
    }
}