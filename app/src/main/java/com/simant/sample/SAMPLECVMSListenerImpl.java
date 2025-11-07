package com.simant.sample;


import com.icmp10.cvms.api.CVMEDlgFragmentConfigurator;
import com.icmp10.cvms.api.CVMSListener;
import com.icmp10.cvms.codes.opCvms.CvmsResult;

public class SAMPLECVMSListenerImpl implements CVMSListener
{
    private CVMSListener mCVMSListener;

    public void setListener(CVMSListener mCVMSListener)
    {
        this.mCVMSListener = mCVMSListener;
    }

    @Override
    public void onOnlineResponse(CvmsResult cvmsResponseData) {
        if (mCVMSListener != null)
            mCVMSListener.onOnlineResponse(cvmsResponseData);
    }

    @Override
    public void onCVMEEntered(int pdc){
        if (mCVMSListener != null)
            mCVMSListener.onCVMEEntered(pdc);
    }

    @Override
    public void onCVMETimeout(){
        if (mCVMSListener != null)
            mCVMSListener.onCVMETimeout();
    }

    @Override
    public void onCVMECancelled(){
        if (mCVMSListener != null)
        mCVMSListener.onCVMECancelled();
    }

    @Override
    public CVMEDlgFragmentConfigurator getDialogConfiguration() {
        if (mCVMSListener != null)
            return mCVMSListener.getDialogConfiguration();
        return null;
    }
}
