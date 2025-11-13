package com.payten.whitelabel.utils

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build


class DeviceUtility {

    companion object{
        fun getAndroidVersion(): String? {
            val release = Build.VERSION.RELEASE
            val sdkVersion = Build.VERSION.SDK_INT
            return "Android SDK: $sdkVersion ($release)"
        }


        fun haveNetworkConnection(context: Context): Boolean {
            var haveConnectedWifi = false
            var haveConnectedMobile = false
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
            val netInfo = cm!!.allNetworkInfo
            for (ni in netInfo) {
                if (ni.typeName.equals(
                        "WIFI",
                        ignoreCase = true
                    )
                ) if (ni.isConnected) haveConnectedWifi = true
                if (ni.typeName.equals(
                        "MOBILE",
                        ignoreCase = true
                    )
                ) if (ni.isConnected) haveConnectedMobile = true
            }
            return haveConnectedWifi || haveConnectedMobile
        }

    }

}