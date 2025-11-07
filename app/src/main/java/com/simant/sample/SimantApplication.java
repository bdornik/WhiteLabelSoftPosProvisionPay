package com.simant.sample;

import android.app.Application;
import android.content.Context;

import com.sacbpp.api.SACBTPApplication;
import com.sacbpp.api.SAMTAApplicationBase;
import com.sacbpp.core.bytes.ByteArray;
import com.sacbpp.ui.InitializationListener;
import com.sacbpp.ui.UIListener;
import com.sacbpp.utils.SACBPPRemoteListener;

/**
 * The type Sample application.
 */
public abstract class SimantApplication extends Application {
    private static SAMTAApplicationBase samtaApplicationBase = new SAMTAApplicationBase();

    /**
     * Create sacbtp application.
     *
     * @param context        the context
     * @param mpadiversifier the mpadiversifier
     * @param hostdiversifier the hostdiversifier
     */
    public void createSACBTPApplication(final Context context, String mpadiversifier, String hostdiversifier) {
        samtaApplicationBase.createSACBTPApplication(context, mpadiversifier, hostdiversifier);
    }
    public void createSACBTPApplication(final Context context, String mpadiversifier) {
        createSACBTPApplication(context, mpadiversifier, null);
    }

    /**
     * Sets sacbtp application.
     *
     * @param application the application
     */
    public static void setSACBTPApplication(SACBTPApplication application) {
        SAMTAApplicationBase.setSACBTPApplication(application);
    }

    /**
     * Gets sacbtp application.
     *
     * @return the sacbtp application
     */
    public static SACBTPApplication getSACBTPApplication() {
        return SAMTAApplicationBase.getSACBTPApplication();
    }

    /**
     * Sets samta application.
     *
     * @param application the application
     * @param mcc         the mcc
     * @param acc         the acc
     */
    public void setSAMTAApplication(SimantApplication application, Class<?> mcc, Class<?> acc) {
        samtaApplicationBase.setSAMTAApplicationBase(application, mcc, acc);
    }

    /**
     * Sets samta application.
     *
     * @param application the application
     * @param mcc         the mcc
     */
    public void setSAMTAApplication(SimantApplication application, Class<?> mcc) {
        setSAMTAApplication(application, mcc, null);
    }

    /**
     * Gets samta application.
     *
     * @return the samta application
     */
    public static SAMTAApplicationBase getSAMTAApplication() {
        return samtaApplicationBase;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        samtaApplicationBase.onCreate();
    }

    @Override
    public void onTerminate() {
        samtaApplicationBase.onTerminate();
        super.onTerminate();
    }

    /**
     * Open icmp remote session.
     *
     * @param rnsMessage     the rns message
     * @param remoteListener the remote listener
     */
    public void openICMPRemoteSession(ByteArray rnsMessage, SACBPPRemoteListener remoteListener) {
        getSACBTPApplication().openICMPRemoteSession(rnsMessage, remoteListener);
    }

    /**
     * Initialize mpa.
     *
     * @param initListener the init listener
     */
    public void initializeMTA(final InitializationListener initListener) {
        getSACBTPApplication().initializeMPA(initListener);
    }

    /**
     * Register sampaui listener.
     */
    public void registerSAMTAUIListener() {
        getSACBTPApplication().registerUIListener();
    }

    /**
     * Register sampaui listener.
     *
     * @param listener the listener
     */
    public void registerSAMTAUIListener(UIListener listener) {
        getSACBTPApplication().registerUIListener(listener);
    }

    /**
     * Un register sampaui listener.
     */
    public void unRegisterSAMTAUIListener() {
        getSACBTPApplication().unRegisterUIListener();
    }

    public static void setSDKStatus(int SDKStatusn) { SAMTAApplicationBase.setSDKStatus(SDKStatusn);}

    public static int getSDKStatus() {
        return SAMTAApplicationBase.getSDKStatus();
    }

    public static String getMPAID() {
        return samtaApplicationBase.getMPAID();
    }

    public abstract void publishDetail(String tag, int process);
}
