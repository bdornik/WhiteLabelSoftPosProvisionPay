package com.simant.softpos.api;

import android.util.Log;

import com.icmp10.mtms.codes.opGetTransactions.GetTransactionsInputData;
import com.icmp10.mtms.codes.opGetTransactions.GetTransactionsResult;
import com.simant.MainApplication;
import com.simcore.api.SoftPOSSDK;
import com.simcore.api.interfaces.PaymentData;
import com.simcore.api.interfaces.TransactionResultListener;

public class TransactionApi
{
    public static boolean isInTransaction() {
        return inTransaction;
    }

    public static void setInTransaction(boolean inTransaction) {
        TransactionApi.inTransaction = inTransaction;
        Log.i("API", "Set in Transaction " + inTransaction);
    }

    private static boolean inTransaction = false;

    private static TransactionResultListener transactionResultListener;
    public static void setTransactionListener(final TransactionResultListener itransactionResultListener) {
        transactionResultListener = itransactionResultListener;
    }
    public static void doTransaction(final TransactionResultListener itransactionResultListener, final PaymentData paymentData) {
        setTransactionListener(itransactionResultListener);
        if (transactionResultListener == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                String TAG = "doTransaction";
                try {
                    if (isInTransaction() == false)
                    {
                        setInTransaction(true);
                        transactionResultListener.onTransactionIdle();

                        if (MainApplication.getInstance().getConfigurationInterface().isReady())
                        {
                            Log.i(TAG, "Started");
                            transactionResultListener.onTransactionReadyToRead();
                            int ret = MainApplication.getInstance().getConfigurationInterface().getTransactionProcessor().doTransaction(paymentData);
                            if ((ret == SoftPOSSDK.SIMCORE_TRUE) && (SoftPOSSDK.getTransactionId() != null))
                            {

                                int recordID = Integer.parseInt(SoftPOSSDK.getTransactionId());
                                if (recordID <= 0)
                                {
                                    transactionResultListener.onTransactionEnded(String.format("Host Response Error Description=%s ErrorCode=%s:", SoftPOSSDK.getErrorDescription(), SoftPOSSDK.getErrorCode()));
                                    setInTransaction(false);
                                    return;
                                }

                                Thread.sleep(1000);
                                int cnt = 0;
                                while (cnt < 6) {
                                    cnt++;
                                    int ctret = MainApplication.getInstance().getConfigurationInterface().getTransactionProcessor().checkTransaction(paymentData.getTransactionType(), true);
                                    Log.i(TAG, "checkTransaction " + cnt + " " + ctret);
                                    if (ctret == SoftPOSSDK.SIMCORE_TRUE) {
                                        cnt += 1000;
                                    }
                                    else if (ctret == SoftPOSSDK.SIMCORE_PCPOC) {
                                        cnt += 2000;
                                    }
                                    else if (ctret == SoftPOSSDK.SIMCORE_CAMPAIGN) {
                                        cnt += 3000;
                                    }
                                    else {
                                        Thread.sleep(2000);
                                    }
                                }
                                if (cnt == 6) {
                                    Log.i(TAG, "checkTransaction Timeout");
                                    transactionResultListener.onTransactionEnded("Timeout");
                                }
                            }
                            else {
                                Log.i(TAG, "checkTransaction No Need");
                                transactionResultListener.onTransactionDeclined();
                            }
                        }
                        else
                        {
                            Log.i(TAG, "Parameters not Ready Yet");
                            transactionResultListener.onTransactionNotStarted("Parameters not Ready Yet");
                        }
                        setInTransaction(false);
                    }
                    else
                    {
                        Log.i(TAG, "Already In Transaction");
                        transactionResultListener.onTransactionNotStarted("Already In Transaction");
                    }
                }
                catch (Exception er) {
                    Log.e(TAG, "Exception : " + er.toString());
                    transactionResultListener.onTransactionDeclined();
                    setInTransaction(false);
                }
            }
        }
        ).start();
    }

    private static void doReversalVoidRefund(final TransactionResultListener transactionResultListener, final String transactionType, final String transactionId) {
        if (transactionResultListener == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                String TAG = "doReversalVoidRefund";
                try {
                    if (isInTransaction() == false)
                    {
                        setInTransaction(true);
                        if (MainApplication.getInstance().getConfigurationInterface().isReady())
                        {
                            //Log.i(TAG, "doTransaction");
                            int ret = MainApplication.getInstance().getConfigurationInterface().getTransactionProcessor().doTransaction(transactionType, transactionId);
                            if ((ret == SoftPOSSDK.SIMCORE_TRUE) && (SoftPOSSDK.getTransactionId() != null))
                            {
                                int recordID = Integer.parseInt(SoftPOSSDK.getTransactionId());
                                if (recordID <= 0)
                                {
                                    transactionResultListener.onTransactionEnded(String.format("Host Response ErrorDescrition=%s ErrorCode=%s:", SoftPOSSDK.getErrorDescription(), SoftPOSSDK.getErrorCode()));
                                    setInTransaction(false);
                                    return;
                                }

                                Thread.sleep(1000);
                                int cnt = 0;
                                while (cnt < 6) {
                                    cnt++;
                                    int ctret = MainApplication.getInstance().getConfigurationInterface().getTransactionProcessor().checkTransaction(transactionType, true);
                                    //Log.i(TAG, "checkTransaction " + cnt + " " + ctret);
                                    if (ctret == SoftPOSSDK.SIMCORE_TRUE) {
                                        cnt += 1000;
                                    }
                                    else if (ctret == SoftPOSSDK.SIMCORE_PCPOC) {
                                        cnt += 2000;
                                    }
                                    else {
                                        Thread.sleep(2000);
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
                        setInTransaction(false);
                    }
                    else
                    {
                        transactionResultListener.onTransactionNotStarted("Already In Transaction");
                    }
                }
                catch (Exception er) {
                    //Log.e(TAG, "Exception : " + er.getMessage()); ane.printStackTrace();
                    transactionResultListener.onTransactionDeclined();
                    setInTransaction(false);
                }
            }
        }
        ).start();
    }

    public static void doTransactionReversal(final TransactionResultListener transactionResultListener, final String transactionId) {
        doReversalVoidRefund(transactionResultListener, PaymentData.TransactionType.REVERSAL.getInternalType(), transactionId);
    }

    public static void doTransactionVoid(final TransactionResultListener transactionResultListener, final String transactionId) {
        doReversalVoidRefund(transactionResultListener, PaymentData.TransactionType.VOID.getInternalType(), transactionId);
    }

    public static void doTransactionRefund(final TransactionResultListener transactionResultListener, final String transactionId) {
        doReversalVoidRefund(transactionResultListener, PaymentData.TransactionType.REFUND.getInternalType(), transactionId);
    }

    public static void doTransactionRetry(final TransactionResultListener transactionResultListener, final String transactionId) {
        doReversalVoidRefund(transactionResultListener, PaymentData.TransactionType.RETRY.getInternalType(), transactionId);
    }

    public static GetTransactionsResult doGetTransactions(GetTransactionsInputData getTransactionsData) {
        return MainApplication.getInstance().getConfigurationInterface().getTransactionProcessor().getTransactions(getTransactionsData);
    }

    public static void testMCL311() {
        MainApplication.getInstance().getConfigurationInterface().getTransactionProcessor().testMCL311();
    }
}