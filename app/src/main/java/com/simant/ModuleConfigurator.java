/*
 * Copyright (c) 2017.
 */

package com.simant;

import android.content.Context;

import com.payten.nkbm.config.SupercaseConfig;
import com.sacbpp.api.SACBPPModuleListener;
import com.sacbpp.api.SACBTPModuleConfigurator;
import com.simant.m.b.e;
import com.simcore.api.SimCoreModuleConfigurator;

import java.util.Date;

/**
 * The type Module configurator.
 */
public class ModuleConfigurator {
    private SACBTPModuleConfigurator sacbppModuleConfigurator;
    private SimCoreModuleConfigurator simcoreModuleConfigurator;

    /**
     * Instantiates a new Module configurator.
     *
     * @param ctx the ctx
     */
    public ModuleConfigurator(Context ctx) {
        SACBTPModuleConfigurator.init(ctx, new SACBPPModuleListener() {
            @Override
            public void onSuccess() {
                //SAMPAApplication.setSDKStatus(0);
                System.out.println("Listener Succeded");
            }

            @Override
            public void onError(int i) {
                MainApplication.setSDKStatus(i);
                System.out.println("Listener with Error " + i);
            }
        });
        sacbppModuleConfigurator = SACBTPModuleConfigurator.getInstance();
        SimCoreModuleConfigurator.init(ctx);
        simcoreModuleConfigurator = SimCoreModuleConfigurator.getInstance();
    }

    /**
     * Configure modules int.
     *
     * @param context the context
     * @return the int
     */
    public int configureModules(Context context) {
        if (sacbppModuleConfigurator.configureModules() != true) {
            if (1 == 1) System.out.println("Unable to Configure Modules");
            if (1 == 1)
                System.out.println("Modules Expiry Date " + sacbppModuleConfigurator.getExpiryDate().toString());
        }
        return verifyModules(context);
    }

    /**
     * Verify modules int.
     *
     * @param context the context
     * @return the int
     */
    public int verifyModules(Context context) {
        int lcnt = 0;
        lcnt = sacbppModuleConfigurator.verifyModules();
        if (lcnt != 0) {
            if (1 == 1) System.out.println("CSE:i.INSTANCE is null");
        }
        if (lcnt == 0) {
            if (e.getInstance() != null) {
                e.getInstance().v();
                if (e.getInstance() != null) {
                    try {
                        e.getInstance().a(context);
                    } catch (Exception ane) {
                        if (1 == 1) System.out.println("CSE:" + ane.getMessage());
                        lcnt++;
                    }
                } else {
                    if (1 == 1) System.out.println("CSE:e.INSTANCE is null");
                    lcnt++;
                }
            }
        }
        int ecnt = sacbppModuleConfigurator.validateModules();
        if ((lcnt == 0) && (ecnt != 0)) if (e.getInstance() != null) e.getInstance().t();
        int slcnt = simcoreModuleConfigurator.verifyModules();
        int iecnt = simcoreModuleConfigurator.validateModules();
        if ((slcnt != 0) || (iecnt != 0)) if (e.getInstance() != null) e.getInstance().t();
        sacbppModuleConfigurator.setprm((byte) 0x02, "26371B705BA71B891D9058BDD93ED9B2124D1AE15AD4D94F59EF9955DD0A48BB9C0D5BFF");
        //libjniPdfium.so
        sacbppModuleConfigurator.setprm((byte) 0x02, "535C9B73DAA718CA5A29D8BA5AA7D7435951DB599B65DDD51AB5883A5E4EDA3D");

        //libmodpdfium.so
        sacbppModuleConfigurator.setprm((byte) 0x02, "F4FBD8F3DBA41948DBB759FED9111EC1D8D31B5899A75C165B7408F85D8D9A7F");

        //libmodft2.so
        sacbppModuleConfigurator.setprm((byte) 0x02, "0E02D9321B6758C8DB765AFC5B505ADA1E930D898A389C4DDA7E");

        //libmodpng.so
        sacbppModuleConfigurator.setprm((byte) 0x02, "34389B319A27994AD9751AFF58109D021839DBDE0B3B5D0F983F");

        return ecnt + lcnt + slcnt + iecnt;
    }

    /**
     * Is release mode boolean.
     *
     * @return the boolean
     */
    public boolean isReleaseMode() {
        return sacbppModuleConfigurator.isReleaseMode();
    }

    /**
     * Gets expiry date.
     *
     * @return the expiry date
     */
    public Date getExpiryDate() {
        return sacbppModuleConfigurator.getExpiryDate();
    }

    /**
     * Is verified boolean.
     *
     * @return the boolean
     */
    public boolean isVerified() {
        return sacbppModuleConfigurator.isVerified();
    }

    /**
     * Is validated boolean.
     *
     * @return the boolean
     */
    public boolean isValidated() {
        return sacbppModuleConfigurator.isValidated();
    }

    public void setUserToken(String userToken) {
        sacbppModuleConfigurator.setUserToken(userToken);
    }

    public void setMPAToken(String applicationToken) {
        sacbppModuleConfigurator.setMPAToken(applicationToken);
    }

    public void getModulesStatus() {
        sacbppModuleConfigurator.getModulesStatus();
    }
}
