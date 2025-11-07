package com.simant.sample;

import com.simant.GlobalParams;

public class GenericFirebaseInstanceIdService extends SAMPLEFirebaseInstanceIdService {
    @Override
    public String getRegistrationId() {
        String tid = super.getRegistrationId();
        if (GlobalParams.isNoRNS) if (tid == null) tid = "HPSPOS"; //CASTECH
        return tid;
    }
}