package com.simant.sample;


import com.icmp10.mtms.api.MTMSListener;
import com.icmp10.mtms.codes.opGetTransaction.GetTransactionResult;
import com.icmp10.mtms.codes.opTransact.TransactResult;

public class SAMPLEMTMSListenerImpl implements MTMSListener
{
    private MTMSListener mMTMSListener;

    public void onOnlineResponse(TransactResult transactResult)
    {
        if (mMTMSListener != null)
            mMTMSListener.onOnlineResponse(transactResult);
    }

    public void onOnlineResponse(GetTransactionResult getTransactionResult)
    {
        if (mMTMSListener != null)
            mMTMSListener.onOnlineResponse(getTransactionResult);
    }

    public void setListener(MTMSListener mMTMSListener)
    {
        this.mMTMSListener = mMTMSListener;
    }
}
