package com.payten.whitelabel.utils

import android.content.Context
import android.graphics.Typeface
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.payten.whitelabel.dto.Message
import com.sacbpp.api.SACBTPApplication
import com.sacbpp.api.SACBTPLogRecord
import com.sacbpp.api.SACBTPModuleConfigurator
import com.simant.MainApplication
import com.simcore.api.SoftPOSSDK
import java.util.Locale

/**
 * Utility class for payment SDK integration, diagnostics, and security monitoring.
 *
 * This class provides comprehensive utilities for interfacing with the SoftPOS payment SDK,
 * including security status checks, transaction record retrieval, logging capabilities, and
 * runtime environment validation. It serves as the primary bridge between the app and the
 * proprietary payment SDK modules.
 *
 * ## SDK Security Monitoring:
 *
 * The payment SDK includes sophisticated security mechanisms to detect compromised devices:
 *
 * ### Device Security Checks:
 * - **Root Detection** (`checkRootedDevice()`) - Detects if device has root access
 * - **Debugging Detection** (`checkDebuggingDevice()`) - Identifies attached debuggers
 * - **Emulator Detection** (`checkEmulatorDevice()`) - Detects virtual devices
 * - **Hook Detection** (`checkHookDevice()`) - Identifies runtime hooks/instrumentation
 *
 * Each check returns `true` if the security threat is detected. The SDK may refuse to
 * process payments on compromised devices depending on configuration.
 *
 * ### SDK Validation Status:
 * - **Release Mode** (`checkSDKReleaseMode()`) - Production vs development SDK build
 * - **Verified** (`checkSDKVerified()`) - SDK binary integrity verified
 * - **Validated** (`checkSDKValidated()`) - SDK configuration validated
 * - **SDK Status** (`checkSDKStatus()`) - Overall SDK readiness (must be 0)
 *
 * ## Logging and Diagnostics:
 *
 * ### Module Logs:
 * The SDK maintains detailed internal logs across multiple modules:
 * - `getModulesLogs()` - Returns all SDK module logs as concatenated string
 * - `getModulesLogsMessage()` - Returns logs as List<Message> for backend transmission
 *
 * Each log record (`SACBTPLogRecord`) contains:
 * - Type, timestamp, user identifier
 * - Result code, originator, affected component
 * - MPA ID, message text, attestation data
 *
 * ### Transaction Records:
 * - `getTR()` - Retrieves transaction record from SDK module configurator
 * - Used in error logging to capture transaction context
 * - **Note**: Current implementation has inverted null check (bug)
 *
 * ### Security Status Report:
 * `logSecurityStatus(context)` generates comprehensive diagnostic report including:
 *
 * #### JNI Status:
 * - JNI interface health (critical - app terminates if failed)
 *
 * #### Security Flags (R/D/E/H):
 * - **R**: Root detection (True/False + positive/negative counts)
 * - **D**: Debug detection (True/False + positive/negative counts)
 * - **E**: Emulator detection (True/False + positive/negative counts)
 * - **H**: Hook detection (True/False)
 *
 * #### SDK Information:
 * - SDK mode (Production/Development)
 * - SDK verified/validated status
 * - SDK version and expiry date
 * - SDK readiness status
 * - RNS ID (Remote Notification Service)
 *
 * #### Device Information:
 * - App version code and name
 * - Android OS version
 * - Network connectivity status
 * - NFC support and enabled status
 *
 * #### Library Versions:
 * - `getLibraryVersions()` - Returns array of SDK library versions
 * - Includes simcore-lib, visa-sensory-branding, sonic-sdk versions
 *
 * ## Usage in Error Logging:
 *
 * ViewModels use SDKUtility when creating error logs for backend transmission:
 * ```kotlin
 * val errorLog = ErrorLog(
 *     // ... other fields ...
 *     status = SDKUtility.logSecurityStatus(context),
 *     tr = SDKUtility.getTR(),
 *     logs = SDKUtility.getModulesLogsMessage()
 * )
 * ```
 *
 * This ensures error reports include complete SDK diagnostic state for debugging.
 *
 * ## SDK Information Dialog:
 *
 * `showSDKInfo(context)` displays an AlertDialog with the security status report,
 * useful for:
 * - Developer diagnostics during testing
 * - Support troubleshooting
 * - Security audit verification
 *
 * ## Critical Dependencies:
 *
 * This utility interfaces with proprietary SDK classes:
 * - `SACBTPModuleConfigurator` - SDK module configuration and status
 * - `SACBTPApplication` - SDK application instance
 * - `SoftPOSSDK` - Core SoftPOS SDK interface
 * - `MainApplication` - App-level SDK accessor
 *
 * ## Security Considerations:
 *
 * The SDK security checks are critical for PCI compliance:
 * - Rooted devices may be rejected for payment processing
 * - Debugging/emulator detection prevents development-time exploitation
 * - Hook detection prevents runtime code injection attacks
 * - JNI integrity ensures native code hasn't been tampered with
 *
 * Consult payment SDK documentation for security requirements and certification.
 *
 * @see RegistrationViewModel for SDK initialization usage
 * @see PosViewModel for error logging usage
 * @see HeadlessPaymentActivity for payment processing usage
 * @see ErrorLog for error logging DTO structure
 */
class SDKUtility {
    companion object {
        fun getModulesLogs(): String {
            val logs = SACBTPModuleConfigurator.getInstance().modulesLogs
            var allLogs = ""

            logs.forEach { log ->

                allLogs += logToString(log)
            }

            return allLogs
        }

        fun getModulesLogsMessage(): List<Message> {
            val logs = SACBTPModuleConfigurator.getInstance().modulesLogs
            val returnLogs = ArrayList<Message>()

            logs.forEach { log ->
                val logUnit = Message()
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

        fun getTR(): String {
            val value = SACBTPModuleConfigurator.getInstance().tr
            return value ?: "null"
        }

        fun checkSDKReleaseMode(): Boolean {
            return SACBTPModuleConfigurator.getInstance().isReleaseMode
        }

        fun checkSDKVerified(): Boolean {
            return SACBTPModuleConfigurator.getInstance().isVerified
        }

        fun checkSDKValidated(): Boolean {
            return SACBTPModuleConfigurator.getInstance().isValidated
        }

        fun getLibraryVersions(): Array<String> {
            return SoftPOSSDK.getInstance().libraryVersions
        }

        fun checkSDKStatus(): Boolean {
            return MainApplication.getSDKStatus() == 0
        }

        fun checkRootedDevice(): Boolean {
            var i = 0
            val rb = SACBTPModuleConfigurator.getInstance().modulesStatus
            i++ //JNI
            return rb[i] == 1//rootDetected
        }

        fun checkDebuggingDevice(): Boolean {
            var i = 0
            val rb = SACBTPModuleConfigurator.getInstance().modulesStatus
            i++ //JNI
            i++ //rootDetected
            i++ //rootCountPositive
            i++ //rootCountNegative
            return rb[i] == 1 //debugDetected
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
            return rb[i] == 1 //emulatorDetected
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
            return rb[i] == 1 //hookDetected
        }

        fun showSDKInfo(context: Context) {
            val alertDialogBuilder = AlertDialog.Builder(context)
            alertDialogBuilder.setTitle("SDK Info\n")
            val textview = TextView(context)
            textview.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL)
            textview.movementMethod = ScrollingMovementMethod.getInstance()
            textview.text = logSecurityStatus(context)
            alertDialogBuilder.setView(textview)
            alertDialogBuilder.setNegativeButton(
                "Cancel"
            ) { dialog, _ -> dialog.cancel() }
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
                            ).uppercase(Locale.ROOT) + "]") + "\n"
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
            sb.append("SDK Is Ready    :" + MainApplication.getInstance().configurationInterface.isReady + "\n")//false
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
