package com.payten.whitelabel.utils;

import com.cioccarellia.ksprefs.KsPrefs;
import com.payten.whitelabel.persistance.SharedPreferencesKeys;
import com.sacbpp.api.SACBTPLogRecord;
import com.sacbpp.api.SACBTPModuleConfigurator;
import com.simant.MainApplication;
import com.simcore.api.SoftPOSSDK;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class DebugSdk {

    public static String getSDKDetailedInfo(KsPrefs sharedPreferences)
    {

        StringBuilder sb = new StringBuilder();
        String[] lv = SoftPOSSDK.getInstance().getLibraryVersions();
//        sb.append("AppId: " + BuildConfig.APPLICATION_ID + "\n");
//        sb.append("Version code: " +BuildConfig.VERSION_CODE + "\n");
//        sb.append("Version name: " +BuildConfig.VERSION_NAME + "\n");
        sb.append("POS Status: " + sharedPreferences.pull(SharedPreferencesKeys.Companion.getPOS_STATUS(), "unknown") + "\n");
        sb.append("IPS Status: " + sharedPreferences.pull(SharedPreferencesKeys.Companion.getIPS_EXISTS(), "unknown") + "\n");
        sb.append("POS merchant id: " + sharedPreferences.pull(SharedPreferencesKeys.Companion.getPOS_SERVICE_MERCHANT_ID(), "unknown") + "\n");
        sb.append("POS terminal id: " + sharedPreferences.pull(SharedPreferencesKeys.Companion.getPOS_SERVICE_TERMINAL_ID(), "unknown") + "\n");
        sb.append("IPS merchant id: " + sharedPreferences.pull(SharedPreferencesKeys.Companion.getIPS_SERVICE_MERCHANT_ID(), "unknown") + "\n");
        sb.append("IPS terminal id: " + sharedPreferences.pull(SharedPreferencesKeys.Companion.getIPS_SERVICE_TERMINAL_ID(), "unknown") + "\n");
        sb.append(lv[0] + "\n");
        sb.append(lv[2] + "\n");
        sb.append(lv[4] + "\n");
        sb.append(SoftPOSSDK.getInstance().getLibraryInformation());

        try {
            sb.append("\n");
            String mpaId = MainApplication.getMPAID();
            sb.append("\nMpaId: ");
            if(mpaId != null && mpaId.length() > 0 )
                sb.append(mpaId);

            sb.append("\nDtId: ");
            String currentDtId = MainApplication.getInstance().getParameterProvider().getCurrentDtId();
            if(currentDtId != null && currentDtId.length() > 0 )
                sb.append(currentDtId);

            sb.append("\nRnsId: ");
            String rnsMpaId = MainApplication.getSACBTPApplication().getGCM_ID();
            if(rnsMpaId != null && rnsMpaId.length() > 0)
                sb.append(rnsMpaId);
        }
        catch (Exception ex) {
            sb.append("\n EXC : " + ex.toString());
        }

        try {
            sb.append("\nPath: ");
            sb.append(SoftPOSSDK.getApplicationPath());
            List<SACBTPLogRecord> logs = SACBTPModuleConfigurator.getInstance().getModulesLogs();
            if (logs != null) {
                sb.append("\nLogs: ");
                for (int i = logs.size() - 1; i >= 0; i--) {
                    sb.append("\n" +
                            new SimpleDateFormat("yyyyMMdd HHmmss SSS").format(new Date(logs.get(i).getTimestamp()))
                            + " "
                            + logs.get(i).getMessage()
                            + " "
                            + logs.get(i).getAttestation()
                    );
                }
            }
        }
        catch (Exception ex) {
            sb.append("\n EXC : " + ex.toString());
        }
        sb.append("\n");
        sb.append("\n");
        return sb.toString();
    }
}
