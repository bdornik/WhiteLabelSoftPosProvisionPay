package com.simant.softpos.api;

import com.simant.MainApplication;
import com.simcore.api.SoftPOSSDK;
import com.simcore.api.interfaces.TransactionResultListener;

public class CVMTransactionApi
{
    public static void doTransactionPCPOC(final TransactionResultListener transactionResultListener, final String transactionType) {
        if (transactionResultListener == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                String TAG = "doTransactionPCPOC";
                try {
                    if (TransactionApi.isInTransaction() == false)
                    {
                        TransactionApi.setInTransaction(true);
                        transactionResultListener.onTransactionProcessing();

                        if (MainApplication.getInstance().getConfigurationInterface().isReady())
                        {
                            //Log.i(TAG, "Started");
                            transactionResultListener.onOnlineRequest();
                            int ret = MainApplication.getInstance().getConfigurationInterface().getCVMProcessor().doTransactionPCPOC();
                            if ((ret == SoftPOSSDK.SIMCORE_TRUE) && (SoftPOSSDK.getTransactionId() != null))
                            {

                                int recordID = Integer.parseInt(SoftPOSSDK.getTransactionId());
                                if (recordID <= 0)
                                {
                                    transactionResultListener.onTransactionEnded(String.format("Host Response Error Description=%s ErrorCode=%s:", SoftPOSSDK.getErrorDescription(), SoftPOSSDK.getErrorCode()));
                                    TransactionApi.setInTransaction(false);
                                    return;
                                }

                                int cnt = 0;
                                while (cnt < 5) {
                                    cnt++;
                                    //int ctret = MainApplication.getInstance().getConfigurationInterface().getTransactionProcessor().checkTransaction(transactionType, false);
                                    int ctret = MainApplication.getInstance().getConfigurationInterface().getTransactionProcessor().checkTransaction(transactionType, true);
                                    //Log.i(TAG, "checkTransaction " + cnt + " " + ctret);
                                    if (ctret == SoftPOSSDK.SIMCORE_TRUE) {
                                        cnt += 1000;
                                    }
                                    else if (ctret == SoftPOSSDK.SIMCORE_PCPOC) {
                                        cnt += 2000;
                                    }
                                    else {
                                        Thread.sleep(1000);
                                    }
                                }

                            }
                            else {
                                //Log.i(TAG, "checkTransaction No Need");
                                transactionResultListener.onTransactionDeclined();
                            }
                        }
                        else
                        {
                            transactionResultListener.onTransactionNotStarted("Parameters not Ready Yet");
                        }
                        TransactionApi.setInTransaction(false);
                    }
                    else
                    {
                        transactionResultListener.onTransactionNotStarted("Already In Transaction");
                    }
                }
                catch (Exception er) {
                    //Log.e(TAG, "Exception : " + er.toString());
                    transactionResultListener.onTransactionDeclined();
                    TransactionApi.setInTransaction(false);
                }
            }
        }
        ).start();
    }
}
