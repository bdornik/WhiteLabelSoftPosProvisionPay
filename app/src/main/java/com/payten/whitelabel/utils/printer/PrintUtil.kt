package com.payten.whitelabel.utils.printer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothDevice
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dantsu.escposprinter.connection.DeviceConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.payten.whitelabel.R
import com.payten.whitelabel.async.AsyncBluetoothEscPosPrint
import com.payten.whitelabel.async.AsyncEscPosPrint
import com.payten.whitelabel.async.AsyncEscPosPrinter
import com.payten.whitelabel.dto.BluetoothPreferencesManager
import com.payten.whitelabel.dto.PrintingText

object PrintUtil {

    val PERMISSION_BLUETOOTH = 1
    val PERMISSION_BLUETOOTH_ADMIN = 2
    val PERMISSION_BLUETOOTH_CONNECT = 3
    val PERMISSION_BLUETOOTH_SCAN = 4

    fun checkBluetoothPermissions(
        activity: Activity,
        onBluetoothPermissionsGranted: OnBluetoothPermissionsGranted
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.BLUETOOTH
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf<String>(Manifest.permission.BLUETOOTH),
                PERMISSION_BLUETOOTH
            )
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.BLUETOOTH_ADMIN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf<String>(Manifest.permission.BLUETOOTH_ADMIN),
                PERMISSION_BLUETOOTH_ADMIN
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf<String>(Manifest.permission.BLUETOOTH_CONNECT),
                PERMISSION_BLUETOOTH_CONNECT
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf<String>(Manifest.permission.BLUETOOTH_SCAN),
                PERMISSION_BLUETOOTH_SCAN
            )
        } else {
            onBluetoothPermissionsGranted.onPermissionsGranted()
        }
    }

    fun printBluetooth(activity: Activity, selectedDevice: BluetoothConnection?, shareText: String) {
        checkBluetoothPermissions(activity) {
            AsyncBluetoothEscPosPrint(
                activity,
                object : AsyncEscPosPrint.OnPrintFinished() {
                    override fun onError(
                        asyncEscPosPrinter: AsyncEscPosPrinter?,
                        codeException: Int
                    ) {
                        Log.i(
                            "Async.OnPrintFinished",
                            "AsyncEscPosPrint.OnPrintFinished : An error occurred !: $codeException"
                        )
                    }

                    override fun onSuccess(asyncEscPosPrinter: AsyncEscPosPrinter?) {
                        asyncEscPosPrinter?.printerConnection
                        Log.i(
                            "Async.OnPrintFinished",
                            "AsyncEscPosPrint.OnPrintFinished : Print is finished !"
                        )
                    }
                }
            )
                .execute(getAsyncEscPosPrinter(selectedDevice, shareText))
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun getAsyncEscPosPrinter(printerConnection: DeviceConnection?, shareText: String): AsyncEscPosPrinter? {
        val printer = AsyncEscPosPrinter(printerConnection, 100, 32f, 32)
        val stampaniTekst = PrintingText(shareText)
        stampaniTekst.update()
        return printer.addTextToPrint(stampaniTekst.getTekst())
    }

    fun interface OnBluetoothPermissionsGranted {
        fun onPermissionsGranted()
    }

    @SuppressLint("MissingPermission")
    fun browseBluetoothDevice(
        activity: Activity,
        selectedDevice: BluetoothConnection?
    ): BluetoothConnection? {
        var returnValue: BluetoothConnection? = null
        checkBluetoothPermissions(activity) {

            val bluetoothDevicesList =
                BluetoothPrintersConnections().list
            if (bluetoothDevicesList != null) {
                val items =
                    arrayOfNulls<String>(bluetoothDevicesList.size + 1)
                items[0] = "Default printer"
                var i = 0
                for (device in bluetoothDevicesList) {
                    items[++i] = device.device.name
                }
                val alertDialog =
                    AlertDialog.Builder(activity)

                alertDialog.setTitle("Bluetooth printer selection")
                alertDialog.setItems(
                    items
                ) { dialogInterface: DialogInterface?, i1: Int ->
                    val index = i1 - 1

                    if (index == -1) {

                    } else {
                        returnValue = bluetoothDevicesList[index]
                        BluetoothPreferencesManager.saveBluetoothAddress(
                            activity,
                            selectedDevice?.device?.address.toString()
                        )
                    }

                    activity.findViewById<ImageView>(R.id.print).performClick()
                }
                val alert = alertDialog.create()
                alert.setCanceledOnTouchOutside(true)
                alert.show()

            }
        }
        return returnValue
    }

    interface OnBluetoothDeviceSelected {
        fun onDeviceSelected(device: BluetoothDevice?)
    }

}