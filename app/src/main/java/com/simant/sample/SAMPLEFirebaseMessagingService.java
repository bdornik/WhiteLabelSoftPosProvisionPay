package com.simant.sample;

import android.content.Intent;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.sacbpp.api.gcm.SAMTAGCMProcessor;
import com.sacbpp.remotemanagement.SACBPPNotificationManager;
import com.simant.MainApplication;

import java.util.logging.Logger;

public class SAMPLEFirebaseMessagingService
        extends FirebaseMessagingService
{
    private Class<?> paymentMessageClass = null;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (MainApplication.getSDKStatus() != 0) return;
        if (SAMTAGCMProcessor.getSACBTPApplication() == null) return;
        String key = remoteMessage.getData().keySet().toArray()[0].toString();

        Log.d("Backend PusH Message", key);
        if (key != null) {
            if (key.contains(SACBPPNotificationManager.SAMSG_NOTIFICATION_TAG))
            {
                if (paymentMessageClass != null) {
                    try {
                        Intent pmintent = new Intent(getApplicationContext(), paymentMessageClass);
                        pmintent.setAction(SACBPPNotificationManager.SAMSG_NOTIFICATION_TAG);
                        pmintent.putExtra("remoteData", remoteMessage.getData().get(SACBPPNotificationManager.SAMSG_NOTIFICATION_TAG));
                        pmintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        getApplicationContext().startActivity(pmintent);
                    }
                    catch (Exception ane) {}
                }
            }
            else if (key.contains(SACBPPNotificationManager.ICMP_NOTIFICATION_TAG))
                SAMTAGCMProcessor.processMessage(SACBPPNotificationManager.ICMP_NOTIFICATION_TAG, remoteMessage.getData().get(SACBPPNotificationManager.ICMP_NOTIFICATION_TAG));
            else if (key.contains(SACBPPNotificationManager.SCBP_NOTIFICATION_TAG))
                SAMTAGCMProcessor.processMessage(SACBPPNotificationManager.SCBP_NOTIFICATION_TAG, remoteMessage.getData().get(SACBPPNotificationManager.SCBP_NOTIFICATION_TAG));
        }
    }
}