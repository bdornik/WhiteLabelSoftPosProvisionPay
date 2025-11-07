package com.simant.sample;

import android.util.Log;

import com.cioccarellia.ksprefs.KsPrefs;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.sacbpp.remotemanagement.RNSService;
import com.simant.MainApplication;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SAMPLEFirebaseInstanceIdService
        extends FirebaseMessagingService
        implements RNSService {

    private static final String TAG = "FirebaseInstanceId";
    private static String token_id = null;

    @Inject
    KsPrefs sharedPreferences;

    public static void regenerate(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(new OnSuccessListener<String>() {
            @Override
            public void onSuccess(String s) {
//                    sharedPreferences.push(SharedPreferencesKeys.Companion.getFIREBASE_TOKEN(), token_id, CommitStrategy.COMMIT);

                token_id = s;
            }
        });
    }

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        token_id = s;
        //Log.d(TAG, "Token: " + token_id);
//        sharedPreferences.push(SharedPreferencesKeys.Companion.getFIREBASE_TOKEN(), token_id, CommitStrategy.COMMIT);
        MainApplication.getSACBTPApplication().setGCM_ID(token_id);
        //MainApplication.getSACBTPApplication().goOnlineForSyncRNSID();
    }

    @Override
    public void registerApplication() {

    }

    @Override
    public String getRegistrationId() {
        if (token_id == null)
        {
//            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
//            if (!task.isSuccessful) {
//                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
//                return@OnCompleteListener
//            }
//
//            // Get new FCM registration token
//            val token = task.result
//        })
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(new OnSuccessListener<String>() {
                @Override
                public void onSuccess(String s) {
//                    sharedPreferences.push(SharedPreferencesKeys.Companion.getFIREBASE_TOKEN(), token_id, CommitStrategy.COMMIT);
                    //Log.i("FIRE_BASE",s);
                    token_id = s;
                }
            });
//            FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
//                @Override
//                public void onSuccess(InstanceIdResult instanceIdResult) {
//                    token_id = instanceIdResult.getToken();
//                    //Log.e(TAG, "getRegistrationId: " + token_id);
//
//                }
//            });
        }
        return token_id;
//        return sharedPreferences.pull(SharedPreferencesKeys.Companion.getFIREBASE_TOKEN(), "");
    }
}