package com.payten.whitelabel.dto
import android.content.Context

object BluetoothPreferencesManager {
    private const val PREFERENCES_NAME = "BluetoothPreferences"
    private const val KEY_BLUETOOTH_ADDRESS = "bluetoothAddress"

    fun saveBluetoothAddress(context: Context, bluetoothAddress: String?) {
        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        if (bluetoothAddress != null) {
            editor.putString(KEY_BLUETOOTH_ADDRESS, bluetoothAddress)
        } else {
            editor.remove(KEY_BLUETOOTH_ADDRESS)
        }

        editor.apply()
    }

    fun getBluetoothAddress(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_BLUETOOTH_ADDRESS, null)
    }
}