package com.simant;

import static com.payten.whitelabel.config.SupercaseConfig.CMS_URL;
import static com.payten.whitelabel.config.SupercaseConfig.TMS_URL;
import static com.payten.whitelabel.config.SupercaseConfig.CVMS_URL;
import static com.payten.whitelabel.config.SupercaseConfig.IDENTIFIER;
import static com.sacbpp.remotemanagement.SACBPPNotificationManager.END_OF_REMOTE_PROCESS;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.multidex.MultiDex;
import androidx.work.Configuration;

import com.icmp10.icmp.api.ParameterProvider;
import com.jakewharton.threetenabp.AndroidThreeTen;
import com.payten.whitelabel.R;
import com.payten.whitelabel.activities.MainActivity;
import com.payten.whitelabel.activities.SplashActivity;
import com.payten.whitelabel.config.SupercaseConfig;
import com.sacbpp.api.SAMPAActivation;
import com.sacbpp.api.SAMPAKeyStore;
import com.sacbpp.api.configuration.SAMPARNSConfiguration;
import com.sacbpp.core.bytes.ByteArray;
import com.sacbpp.core.bytes.ByteArrayFactory;
import com.sacbpp.core.device.ApplicationInfo;
import com.sacbpp.core.device.MobileDeviceInfo;
import com.sacbpp.remotemanagement.CMSConfiguration;
import com.sacbpp.remotemanagement.SACBPPNotificationManager;
import com.simant.sample.GenericFirebaseInstanceIdService;
import com.simant.sample.SimantApplication;
import com.simant.sample.SAMPLECVMSListenerImpl;
import com.simant.sample.SAMPLEMTMSListenerImpl;
import com.simant.softpos.impl.SampleDataProvider;
import com.simant.softpos.impl.SamplePaymentData;
import com.simant.softpos.impl.SampleTransactionLogger;
import com.simant.utils.AppEvent;
import com.simant.utils.AppEventBus;
import com.simcore.api.SoftPOSSDK;
import com.simcore.api.interfaces.ConfigurationInterface;
import com.simcore.api.providers.CardCommunicationProvider;
import com.simcore.api.providers.LoyaltyObserver;
import com.simcore.api.providers.OutcomeObserver;

import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Date;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class MainApplication extends SimantApplication implements Configuration.Provider {
    @Inject
    HiltWorkerFactory workerFactory;

    private static final String TAG = "MainApplication";

    private static MainApplication INSTANCE = null;

    public static MainApplication getInstance() {
        return INSTANCE;
    }

    private String CMS_URL_MA = "";
    private String TMS_URL_MA = "";
    private String CVMS_URL_MA = "";
    private String IDENTIFIER_MA = "";

    SoftPOSSDK softPOSLibrary = null;

    private ConfigurationInterface mConfigurationInterface = null;

    private SampleDataProvider mDataProvider;
    private SamplePaymentData mPaymentData = null;
    private SampleTransactionLogger mTransactionProcessLogger;
    private CardCommunicationProvider mCardCommunicationProviderNFC;
    private ParameterProvider mParameterProviderLDE;
    private OutcomeObserver mTransactionOutcomeObserver;
    private LoyaltyObserver mLoyaltyObserver;
    private SAMPLEMTMSListenerImpl mMTMSListener;
    private SAMPLECVMSListenerImpl mCVMSListener;

    public ModuleConfigurator moduleConfigurator;

    private SAMPAKeyStore loadSAMPAKeyStore() {
        SAMPAKeyStore akey = new SAMPAKeyStore(
                getApplicationContext().getResources().openRawResource(SupercaseConfig.KEY),
                "Panda1881".toCharArray(),
                SupercaseConfig.ActivationCode2);
        if (akey == null) System.out.println("KeyStore in Error");
        return akey;
    }

    private SAMPAKeyStore loadSAMPATMSKeyStore() {
        SAMPAKeyStore akey = new SAMPAKeyStore(
                getApplicationContext().getResources().openRawResource(SupercaseConfig.KEY),
                "Panda1881".toCharArray(),
                SupercaseConfig.ActivationCode2);
        if (akey == null) System.out.println("KeyStore in Error");
        return akey;
    }

    private SAMPAKeyStore loadSAMPACVMKeyStore() {
        SAMPAKeyStore akey = new SAMPAKeyStore(
                getApplicationContext().getResources().openRawResource(SupercaseConfig.KEY),
                "Panda1881".toCharArray(),
                SupercaseConfig.ActivationCode2);
        if (akey == null) System.out.println("KeyStore in Error");
        return akey;
    }

    SACBPPNotificationManager softposNotificationManager = null;

    private void createNotificationManager() {
        softposNotificationManager = new SACBPPNotificationManager() {
            @Override
            public void publish(String title, String message) {
                Log.i("NTF", title + "::" + message);
            }

            @Override
            public void publishDetail(String tag, int process) {
                Log.i("NTF", tag + "::" + process);
                switch (process) {
                    case REMOTE_WIPE_RECEIVED:
                        Log.i("NTF", "REMOTE_WIPE_RECEIVED");
                    case TERMINAL_PROFILE_RECEIVED:
                        Log.i("NTF", "TERMINAL_PROFILE_RECEIVED");
                    case RESUME_TERMINAL_RECEIVED:
                        Log.i("NTF", "RESUME_TERMINAL_RECEIVED");
                    case SUSPEND_TERMINAL_RECEIVED:
                        Log.i("NTF", "SUSPEND_TERMINAL_RECEIVED");
                    case DELETE_TERMINAL_RECEIVED:
                        Log.i("NTF", "DELETE_TERMINAL_RECEIVED");
                    case TERMINAL_PRM_RECEIVED:
                        Log.i("NTF", "TERMINAL_PRM_RECEIVED");
                    case TERMINAL_PROFILE_UPDATE_RECEIVED:
                        Log.i("NTF", "TERMINAL_PROFILE_UPDATE_RECEIVED");
                    case END_OF_REMOTE_PROCESS:
                        Log.i("NTF", "END_OF_PROCESS");
                        AppEventBus.INSTANCE.publish(new AppEvent(tag, process));
                        mConfigurationInterface.getParameterProvider().refreshParameters();
                        break;
                }
            }
        };
    }

    ApplicationInfo softposApplicationInfo = null;

    private void createApplicationInfo() {
        softposApplicationInfo = getSACBTPApplication().getApplicationInfo();
        softposApplicationInfo.setStatus("");
        softposApplicationInfo.setVersion(SAMPARNSConfiguration.APP_VERSION);
        softposApplicationInfo.setRFU("");
    }

    MobileDeviceInfo softposMobileDeviceInfo = null;

    private void createMobileDeviceInfo() {
        softposMobileDeviceInfo = getSACBTPApplication().getMobileDeviceInfo();
        String SI = getSharedPreferences("SOFTPOS_PARAMETERS_MDI", Context.MODE_PRIVATE).getString("SI", null);
        if (SI == null) {
            SI = softposMobileDeviceInfo.getImei();
            getSharedPreferences("SOFTPOS_PARAMETERS_MDI", Context.MODE_PRIVATE).edit().putString("SI", SI).apply();
        }
        softposMobileDeviceInfo.setImei(SI);
        softposMobileDeviceInfo.setOsVersion("ANY");
        softposMobileDeviceInfo.setOsUniqueIdentifier("ANY");
        softposMobileDeviceInfo.setOsFirmwarebuild("ANY");
        String SR = getSharedPreferences("SOFTPOS_PARAMETERS_MDI", Context.MODE_PRIVATE).getString("SR", null);
        if (SR == null) {
            SR = softposMobileDeviceInfo.getScreenSize();
            getSharedPreferences("SOFTPOS_PARAMETERS_MDI", Context.MODE_PRIVATE).edit().putString("SR", SR).apply();
        }
        softposMobileDeviceInfo.setScreenSize(SR);
    }

    private boolean useSOFTPOSSDK() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false;
        return true;
    }

    void initializeUrls() {
        String cmsidentifier = null;
        String cmsUrl = null;
        String tmsUrl = null;
        String cvmsUrl = null;

        cmsidentifier = MainApplication.getStringData("CMSIdentifier");
        cmsUrl = MainApplication.getStringData("CMSURL");
        tmsUrl = MainApplication.getStringData("TMSURL");
        cvmsUrl = MainApplication.getStringData("CVMSURL");

        if (cmsidentifier == null || cmsidentifier.length() <= 0)
            cmsidentifier = "mpts-tr-validate";

        if (cmsUrl == null || cmsUrl.length() <= 0)
            cmsUrl = "https://web.sim-ant.com/SOFTPOS_SQL/cbpp/rm";
        if (tmsUrl == null || tmsUrl.length() <= 0)
            tmsUrl = "https://web.sim-ant.com/SOFTPOS_SQL/cbpp/tm";
        if (cvmsUrl == null || cvmsUrl.length() <= 0)
            cvmsUrl = "https://web.sim-ant.com/SOFTPOS_SQL/cbpp/tm";

        if (cmsUrl == null || cmsUrl.length() <= 0) cmsUrl = "https://okayk.sim-ant.com/rm";
        if (tmsUrl == null || tmsUrl.length() <= 0) tmsUrl = "https://okayk.sim-ant.com/cbpp/tm2";
        if (cvmsUrl == null || cvmsUrl.length() <= 0)
            cvmsUrl = "https://okayk.sim-ant.com/cbpp/tm2";

        if (cmsUrl == null || cmsUrl.length() <= 0)
            cmsUrl = "https://web.sim-ant.com/bogwhitelabeloracle/cbpp/rm";
        if (tmsUrl == null || tmsUrl.length() <= 0)
            tmsUrl = "https://web.sim-ant.com/bogwhitelabeloracle/cbpp/tm";
        if (cvmsUrl == null || cvmsUrl.length() <= 0)
            cvmsUrl = "https://web.sim-ant.com/bogwhitelabeloracle/cbpp/tm";

        if (cmsUrl == null || cmsUrl.length() <= 0)
            cmsUrl = "https://web.sim-ant.com/devwhitelabel/SOFTPOS/CBPP/rm";
        if (tmsUrl == null || tmsUrl.length() <= 0)
            tmsUrl = "https://web.sim-ant.com/devwhitelabel/SOFTPOS/cbpp/tm";
        if (tmsUrl == null || tmsUrl.length() <= 0)
            cvmsUrl = "https://web.sim-ant.com/devwhitelabel/SOFTPOS/cbpp/tm";

        if (cmsUrl == null && cmsUrl.length() <= 0)
            cmsUrl = "https://web.sim-ant.com/devSOFTPOS/CBPP/rm";
        if (tmsUrl == null && tmsUrl.length() <= 0)
            tmsUrl = "https://web.sim-ant.com/devSOFTPOS/cbpp/tm";
        if (tmsUrl == null && tmsUrl.length() <= 0)
            cvmsUrl = "https://web.sim-ant.com/devSOFTPOS/cbpp/tm";

        MainApplication.saveStringData("CMSIdentifier", cmsidentifier);
        MainApplication.saveStringData("CMSURL", cmsUrl);
        MainApplication.saveStringData("TMSURL", tmsUrl);
        MainApplication.saveStringData("CVMSURL", cvmsUrl);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    private void populate(String TENANT){
        Log.i(TAG,"populate Tenant: " + !TENANT.isEmpty());
        if (!TENANT.equals("")){
            IDENTIFIER_MA = TENANT;
            CVMS_URL_MA = CVMS_URL.replace(IDENTIFIER,TENANT);
            TMS_URL_MA = TMS_URL.replace(IDENTIFIER,TENANT);
            CMS_URL_MA = CMS_URL.replace(IDENTIFIER,TENANT);
        }else{
            IDENTIFIER_MA = IDENTIFIER;
            CVMS_URL_MA = CVMS_URL;
            TMS_URL_MA = TMS_URL;
            CMS_URL_MA = CMS_URL;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i(TAG, "MainApplication onCreate()");

        String TENANT = getSharedPreferences("SOFTPOS_PARAMETERS_MDI", Context.MODE_PRIVATE).getString("TENANT", "");

        populate(TENANT);

        // check root and store status
        setstctx(getApplicationContext());
        INSTANCE = this;
        preventScreenshot();

        mTransactionProcessLogger = new SampleTransactionLogger();

        setSDKStatus(-1);
        if (useSOFTPOSSDK() == true) {
            moduleConfigurator = new ModuleConfigurator(this);
            setSAMTAApplication(this, SplashActivity.class, MainActivity.class);

            setSDKStatus(moduleConfigurator.configureModules(getApplicationContext()));
            boolean iv = moduleConfigurator.isVerified() & moduleConfigurator.isValidated();
            if (iv) {
                boolean rm = moduleConfigurator.isReleaseMode();
                Date exd = moduleConfigurator.getExpiryDate();
            }

            initializeUrls();
            if (getSDKStatus() == 0) {
                String hostdiversifier = "0000000000000000000000000000000000000000000000000000000000000000";
                hostdiversifier = null;
                String mpadiversifier = "0000000000000000000000000000000000000000000000000000000000000000";
                mpadiversifier = null;
                if (mpadiversifier == null) {
                    SharedPreferences preferences = getSharedPreferences("MPD", Context.MODE_PRIVATE);
                    mpadiversifier = preferences.getString("DMP", "");
                    if ((mpadiversifier == null) || (mpadiversifier.length() < 64)) {
                        byte[] rbytes = new byte[32];
                        new SecureRandom().nextBytes(rbytes);
                        mpadiversifier = ByteArrayFactory.getInstance().getByteArray(rbytes).getHexString();
                        SharedPreferences.Editor edit = preferences.edit();
                        edit.putString("DMP", mpadiversifier);
                        edit.apply();
                    }
                }

                createSACBTPApplication(this, mpadiversifier, hostdiversifier);
                createNotificationManager();
                createApplicationInfo();
                createMobileDeviceInfo();




                getSACBTPApplication().createServices(
                        new CMSConfiguration() {
                            @Override
                            public String issuerIdentifier() {
                                return IDENTIFIER_MA;
                            }

                            @Override
                            public String urlInit() {
                                return CMS_URL_MA;
                            }
                        },
                        softposNotificationManager,
                        softposApplicationInfo,
                        new GenericFirebaseInstanceIdService(),
                        softposMobileDeviceInfo,
                        loadSAMPAKeyStore()
                );
                mMTMSListener = new SAMPLEMTMSListenerImpl();
                getSACBTPApplication().createTmsService(
                        mMTMSListener,
                        () -> TMS_URL_MA,
                        loadSAMPATMSKeyStore()
                );

                mCVMSListener = new SAMPLECVMSListenerImpl();
                getSACBTPApplication().createCvmsService(
                        mCVMSListener,
                        () -> CVMS_URL_MA,
                        loadSAMPACVMKeyStore()
                );
            }


            //}
        }

        initializeUiDataComponents();
        initializeSoftPOSLibrary();

        AndroidThreeTen.init(this);
    }

    public void updateParameters() {

        String tenant = getSharedPreferences("SOFTPOS_PARAMETERS_MDI", Context.MODE_PRIVATE).getString("TENANT", "");
        Log.i(TAG, "UPDATE PARAMTETERS: " + tenant);

        populate(tenant);

        Log.i(TAG,CVMS_URL_MA);
        Log.i(TAG,CMS_URL_MA);
        Log.i(TAG,TMS_URL_MA);
        Log.i(TAG,IDENTIFIER_MA);

        getSACBTPApplication().createServices(
                new CMSConfiguration() {
                    @Override
                    public String issuerIdentifier() {
                        return IDENTIFIER_MA;
                    }

                    @Override
                    public String urlInit() {
                        return CMS_URL_MA;
                    }
                },
                softposNotificationManager,
                softposApplicationInfo,
                new GenericFirebaseInstanceIdService(),
                softposMobileDeviceInfo,
                loadSAMPAKeyStore()
        );
        mMTMSListener = new SAMPLEMTMSListenerImpl();
        getSACBTPApplication().createTmsService(
                mMTMSListener,
                () ->TMS_URL_MA,
                loadSAMPATMSKeyStore()
        );

        mCVMSListener = new SAMPLECVMSListenerImpl();
        getSACBTPApplication().createCvmsService(
                mCVMSListener,
                () -> CVMS_URL_MA,
                loadSAMPACVMKeyStore()
        );

    }


    private void initializeUiDataComponents() {
        mDataProvider = new SampleDataProvider(getApplicationContext());
    }

    public void initializeSoftPOSLibrary() {

        if (softPOSLibrary == null)
            softPOSLibrary = SoftPOSSDK.getInstance(getApplicationContext());
        if (softPOSLibrary == null)
            return;

        mConfigurationInterface = softPOSLibrary.getConfigurationInterface();

        mTransactionOutcomeObserver = OutcomeObserver.getInstance();
        mConfigurationInterface.setOutcomeObserver(mTransactionOutcomeObserver);

        mLoyaltyObserver = LoyaltyObserver.getInstance();
        mConfigurationInterface.setLoyaltyObserver(mLoyaltyObserver);

        mCardCommunicationProviderNFC = CardCommunicationProvider.getInstanceNFC(getApplicationContext());
        mConfigurationInterface.setCardCommunicationProvider(mCardCommunicationProviderNFC);

        mParameterProviderLDE = ParameterProvider.getInstanceLDE(getApplicationContext());
        mConfigurationInterface.setParameterProvider(mParameterProviderLDE);

        mConfigurationInterface.setLogger(mTransactionProcessLogger);
    }

    public SampleDataProvider getDataProvider() {
        return mDataProvider;
    }

    public SamplePaymentData getPaymentData() {
        if (mPaymentData == null) {
            mPaymentData = new SamplePaymentData();
        }
        return mPaymentData;
    }

    public void setPaymentAmount(Long amount) {
        mPaymentData.setAmountTransaction(amount);
    }

    public boolean isMCL311Case() {
        if (mConfigurationInterface == null) return false;
        return false;
    }

    public ConfigurationInterface getConfigurationInterface() {
        return mConfigurationInterface;
    }

    public OutcomeObserver getTransactionOutcomeObserver() {
        return mTransactionOutcomeObserver;
    }

    public LoyaltyObserver getLoyaltyObserver() {
        return mLoyaltyObserver;
    }

    public SAMPLEMTMSListenerImpl getMTMSListener() {
        return mMTMSListener;
    }

    public SAMPLECVMSListenerImpl getCVMSListener() {
        return
                mCVMSListener;
    }

    public SampleTransactionLogger getTransactionProcessLogger() {
        return mTransactionProcessLogger;
    }

    public ParameterProvider getParameterProvider() {
        return mConfigurationInterface.getParameterProvider();
    }

    public CardCommunicationProvider getCardCommunicationProvider() {
        return mConfigurationInterface.getCardCommunicationProvider();
    }

    public void setRealProviders() {
        mConfigurationInterface.setCardCommunicationProvider(mCardCommunicationProviderNFC);
        mConfigurationInterface.setParameterProvider(mParameterProviderLDE);
    }

    public void setMCL311Providers() {
        setRealProviders();
    }

    public void closeFileConnections() {
        mTransactionProcessLogger.closeFileWriter();
    }

    public void resetFileConnections() {
        mTransactionProcessLogger.resetLogWriter();
    }

    private static Context stctx;

    private static void setstctx(Context ctx) {
        stctx = ctx;
    }

    public static void saveStringData(final String key, final String value) {
        stctx.getSharedPreferences("SOFTPOS", Context.MODE_PRIVATE).edit().putString(key, value).apply();
    }

    public static String getStringData(final String key) {
        return stctx.getSharedPreferences("SOFTPOS", Context.MODE_PRIVATE).getString(key, null);
    }

    public static void saveIntData(final String key, final int value) {
        stctx.getSharedPreferences("SOFTPOS", Context.MODE_PRIVATE).edit().putInt(key, value).apply();
    }

    public static int getIntData(final String key) {
        return stctx.getSharedPreferences("SOFTPOS", Context.MODE_PRIVATE).getInt(key, 0);
    }

    private static int prvposition = -1;

    public static void setPrvposition(int prvposition) {
        MainApplication.prvposition = prvposition;
    }

    public static int getPrvposition() {
        return prvposition;
    }

    private static boolean fchecked = false;
    private static boolean isMCL311Mode = true;

    public static boolean getMCL311Mode() {
        if (fchecked == false) {
            File file = new File(getWorkingDirectory() + File.separator + "VISAMODE.ON");
            isMCL311Mode = !file.exists();
            fchecked = true;
        }
        return isMCL311Mode;
    }

    public static String getWorkingDirectory() {
        return MainApplication.getInstance().getApplicationInfo().dataDir + File.separator + "SOFTPOS";
    }

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build();
    }

    private void preventScreenshot() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                if (!SupercaseConfig.SCREENSHOT_ENABLED) {
                    activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
                }
            }

            @Override
            public void onActivityStarted(Activity activity) {
            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
            }
        });
    }

    public void createActivationCodes() {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            ByteArray ActivationCode1 = SAMPAActivation.getInstance().getActivationCode(getApplicationContext());
            System.out.println("PRIMER: ACTIVATION CODE 1 " + ByteArrayFactory.getInstance().getByteArray(md.digest(ActivationCode1.getBytes())).getHexString());
            try {
                InputStream in = getApplicationContext().getResources().openRawResource(R.raw.prod); //Keys for App TLS
                ByteArray ActivationCode2 = SAMPAActivation.getInstance().getActivationCode(in);
                in.close();
                System.out.println("PRIMER: ACTIVATION CODE 2 " + ByteArrayFactory.getInstance().getByteArray(md.digest(ActivationCode2.getBytes())).getHexString());
            } catch (Exception ane) {
            }
        } catch (Exception ane) {
        }
    }


    @Override
    public void publishDetail(String tag, int process) {
        Log.i(TAG, "TAG: " + tag + " ; Proccess: " + process);
        AppEventBus.INSTANCE.publish(new AppEvent(tag, process));
        switch (process) {
//                    case REMOTE_WIPE_RECEIVED :
//                    case TERMINAL_PROFILE_RECEIVED :
//                    case RESUME_TERMINAL_RECEIVED :
//                    case SUSPEND_TERMINAL_RECEIVED :
//                    case DELETE_TERMINAL_RECEIVED :
//                    case TERMINAL_PRM_RECEIVED :
//                    case TERMINAL_PROFILE_UPDATE_RECEIVED :
//                        mConfigurationInterface.getParameterProvider().refreshParameters();
//                        break;
//                }
            case END_OF_REMOTE_PROCESS:
                if (mConfigurationInterface != null)
                    if (mConfigurationInterface.getParameterProvider() != null)
                        mConfigurationInterface.getParameterProvider().refreshParameters();
                break;
        }
    }


}
