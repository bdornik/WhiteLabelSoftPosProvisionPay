package com.payten.nkbm.utils

import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.payten.nkbm.dto.Message
import com.sacbpp.api.SACBTPApplication
import com.sacbpp.api.SACBTPLogRecord
import com.sacbpp.api.SACBTPModuleConfigurator
import com.simant.MainApplication
import com.simcore.api.SoftPOSSDK

class SDKUtility {
    companion object {
        fun getModulesLogs(): String {
            var logs = SACBTPModuleConfigurator.getInstance().getModulesLogs()
            var allLogs = ""

            logs.forEach { log ->

                allLogs += logToString(log)
            }

            return allLogs
        }

        fun getModulesLogsMessage(): List<Message> {
            var logs = SACBTPModuleConfigurator.getInstance().getModulesLogs()
            val returnLogs = ArrayList<Message>()

            logs.forEach { log ->
                var logUnit = Message()
                logUnit.message = logToString(log)
                returnLogs.add(logUnit)
            }

            return returnLogs
        }


        fun logToString(log: SACBTPLogRecord): String {
            val sb = StringBuilder()

            sb.append(
                "type: " + log.type + ",timestamp: " + log.timestamp + ",user: " + log.user + ",result: " + log.result + ",originator: " + log.originator
                        + ",affected: " + log.affected + ",mpaid: " + log.mpaid + ",message: " + log.message + ",attestation: " + log.attestation
            )

            return sb.toString()
        }

        fun getTR(): String? {
            val value = SACBTPModuleConfigurator.getInstance().getTR()
            if (value !=null)
                return "null"
            else
                return value
        }

        fun checkSDKReleaseMode(): Boolean {
            return SACBTPModuleConfigurator.getInstance().isReleaseMode()
        }

        fun checkSDKVerified(): Boolean {
            return SACBTPModuleConfigurator.getInstance().isVerified()
        }

        fun checkSDKValidated(): Boolean {
            return SACBTPModuleConfigurator.getInstance().isValidated()
        }

        fun getLibraryVersions(): Array<String> {
            return SoftPOSSDK.getInstance().getLibraryVersions()
        }

        fun checkSDKStatus(): Boolean {
            if (MainApplication.getSDKStatus() != 0) {
                return false
            }
            return true
        }

        fun checkRootedDevice(): Boolean {
            var i = 0
            val rb = SACBTPModuleConfigurator.getInstance().modulesStatus
            i++ //JNI
            if (rb[i] == 1)//rootDetected
                return true
            return false
        }

        fun checkDebuggingDevice(): Boolean {
            var i = 0
            val rb = SACBTPModuleConfigurator.getInstance().modulesStatus
            i++ //JNI
            i++ //rootDetected
            i++ //rootCountPositive
            i++ //rootCountNegative
            if (rb[i] == 1) //debugDetected
                return true
            return false
        }

        fun checkEmulatorDevice(): Boolean {
            var i = 0
            val rb = SACBTPModuleConfigurator.getInstance().modulesStatus
            i++ //JNI
            i++ //rootDetected
            i++ //rootCountPositive
            i++ //rootCountNegative
            i++ //debugDetected
            i++ //debugCountPositive
            i++ //debugCountNegative
            if (rb[i] == 1) //emulatorDetected
                return true
            return false
        }

        fun checkHookDevice(): Boolean {
            var i = 0
            val rb = SACBTPModuleConfigurator.getInstance().modulesStatus
            i++ //JNI
            i++ //rootDetected
            i++ //rootCountPositive
            i++ //rootCountNegative
            i++ //debugDetected
            i++ //debugCountPositive
            i++ //debugCountNegative
            i++ //emulatorDetected
            i++ //emulatorCountPositive
            i++ //emulatorCountNegative
            if (rb[i] == 1) //hookDetected
                return true
            return false
        }

        fun showSDKInfo(context: Context) {
            val alertDialogBuilder = AlertDialog.Builder(context)
            alertDialogBuilder.setTitle("SDK Info\n")
            val textview = TextView(context)
            textview.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL)
            textview.movementMethod = ScrollingMovementMethod.getInstance()
            textview.text = SDKUtility.logSecurityStatus(context)
            alertDialogBuilder.setView(textview)
            alertDialogBuilder.setNegativeButton(
                "Cancel",
                DialogInterface.OnClickListener { dialog, id -> dialog.cancel() })
            val alert = alertDialogBuilder.create()
            alert.show()
        }

        @JvmStatic
        fun logSecurityStatus(context: Context): String {
            val packageManager = context.packageManager
            val packageName = context.packageName
            val packageInfo = packageManager.getPackageInfo(packageName, 0)

            val versionCode: Long = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            }
            val versionName: String = packageInfo.versionName ?: "unknown"

            var i = 0
            val rb = SACBTPModuleConfigurator.getInstance().modulesStatus
            val sb = StringBuilder()
            sb.append("\n")
            sb.append(
                "JNI " + (if (rb[i++] == 0) "OK" else "[TERMINATED:0x" + Integer.toHexString(
                    rb[0]
                ).toUpperCase() + "]") + "\n"
            )
            sb.append("R : " + (if (rb[i++] == 1) "[T]" else "[F]") + " P [" + rb[i++] + "] N [" + rb[i++] + "]\n")
            sb.append("D : " + (if (rb[i++] == 1) "[T]" else "[F]") + " P [" + rb[i++] + "] N [" + rb[i++] + "]\n")
            sb.append("E : " + (if (rb[i++] == 1) "[T]" else "[F]") + " P [" + rb[i++] + "] N [" + rb[i++] + "]\n")
            sb.append("H : " + (if (rb[i++] == 1) "[T]" else "[F]") + "\n")
            sb.append("C : [" + rb[i++] + ":" + rb[i++] + ":" + rb[i++] + "]\n")
            sb.append("V : " + (if (rb[i++] == 1) "[ OK]" else "[ERR]") + "\n")
            val modulesStatusReason = SACBTPModuleConfigurator.getInstance().modulesStatusReason
            if (modulesStatusReason != null) sb.append("\nModulesStatusReason\n$modulesStatusReason")
            //int adbstatus = Settings.Secure.getInt(activity.getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED , 0);
            //sb.append("ADB : " + adbstatus);

            sb.append("\n")
            sb.append("SDK Mode       : " + (if (SACBTPModuleConfigurator.getInstance().isReleaseMode) "Production" else "Development") + "\n")
            sb.append("SDK Verified   : " + (if (SACBTPModuleConfigurator.getInstance().isVerified) "Yes" else "No") + "\n")
            sb.append("SDK Validated  : " + (if (SACBTPModuleConfigurator.getInstance().isValidated) "Yes" else "No") + "\n")
            sb.append("SDK Status     : " + MainApplication.getSDKStatus() + "\n")
            sb.append("SDK Expiry Date:" + SACBTPModuleConfigurator.getInstance().expiryDate.toString() + "\n")
            sb.append("SDK Version    :" + SACBTPApplication.getiSDKVersion() + "\n")
            sb.append("APP Version    : $versionCode\n")
            sb.append("APP Version Name    : $versionName\n")
            sb.append("SDK Is Ready    :" + MainApplication.getInstance().getConfigurationInterface().isReady() + "\n")//false
            try {
                sb.append("RnsId    :" + MainApplication.getSACBTPApplication().gcM_ID + "\n")
            }catch (exc : Exception){
                sb.append("RnsId    : ERROR -> ${exc.message} " + "\n")
            }
            sb.append("SupportedOSVersion: " + DeviceUtility.getAndroidVersion() + "\n")
            sb.append("Network Connection: " + DeviceUtility.haveNetworkConnection(context) + "\n")
            sb.append("Nfc Support        : " + HardwareUtility.checkNfcSupport(context) + "\n")
            sb.append("Nfc Enabled        : " + HardwareUtility.checkNfcEnabled(context) + "\n")
            sb.append("SDK Release Mode   : " + checkSDKReleaseMode() + "\n")
            sb.append("SDK Verified       : " + checkSDKVerified() + "\n")
            sb.append("SDK Validated      : " + checkSDKValidated() + "\n")
            sb.append("Rooted    Device   : " + checkRootedDevice() + "\n")
            sb.append("Debugging Device   : " + checkDebuggingDevice() + "\n")
            sb.append("Emulator  Device   : " + checkEmulatorDevice() + "\n")
            sb.append("Hook      Device   : " + checkHookDevice() + "\n")
            return sb.toString()
        }

    }
}
