/*
 * Copyright (c) 2016.
 */
package com.simant.m.b;

import android.content.Context;

import com.payten.nkbm.config.SupercaseConfig;
import com.sacbpp.api.SAMPAjne;
import com.sacbpp.core.bytes.ByteArray;

/**
 * The type E.
 */
public class e extends SAMPAjne {
    /**
     * The constant INSTANCE.
     */
    protected static SAMPAjne INSTANCE = new e();

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static SAMPAjne getInstance() {
        return INSTANCE;
    }

    /**
     * Sets instance.
     *
     * @param ni the ni
     */
    public static void setInstance(SAMPAjne ni) { INSTANCE = ni; }

    /**
     * Gets instance i.
     *
     * @return the instance i
     */
    public SAMPAjne getInstanceI() {
        return getInstance();
    }

    /**
     * Sets instance i.
     *
     * @param ni the ni
     */
    public void setInstanceI(SAMPAjne ni) {
        setInstance(ni);
    }

    /**
     * A.
     *
     * @param context the context
     * @throws Exception the exception
     */
    public void a(Context context) throws Exception {
        aI(context, SupercaseConfig.ActivationCode1);
    }

    /**
     * Np byte [ ].
     *
     * @param key_wbc  the key wbc
     * @param data_key the data key
     * @param pmode    the pmode
     * @return the byte [ ]
     */
    protected byte[] np(ByteArray key_wbc, ByteArray data_key, int pmode) {
        return p(key_wbc, data_key, pmode);
    }

    private native byte[] p(ByteArray k_w, ByteArray d_k, int pmode);
}