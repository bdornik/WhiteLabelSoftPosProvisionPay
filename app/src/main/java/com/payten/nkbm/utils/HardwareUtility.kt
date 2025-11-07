package com.payten.nkbm.utils

import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.NfcManager


class HardwareUtility {
    companion object {
        fun checkNfcSupport(context: Context): Boolean {
            val manager: NfcManager = context.getSystemService(Context.NFC_SERVICE) as NfcManager
            val adapter: NfcAdapter = manager.getDefaultAdapter()
            if (adapter != null && adapter.isEnabled) {
                return true
            } else {
                return false
            }
        }

        fun checkNfcEnabled(context: Context): Boolean{
            val manager = context.getSystemService(Context.NFC_SERVICE) as NfcManager
            val adapter = manager.defaultAdapter
            if (adapter != null && adapter.isEnabled) {

               return true
            }else{
                //NFC is not enabled.Need to enable by the user.
                return false
            }
        }
    }
}