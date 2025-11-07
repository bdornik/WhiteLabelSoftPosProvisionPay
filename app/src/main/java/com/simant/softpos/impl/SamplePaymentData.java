package com.simant.softpos.impl;

import com.simcore.api.interfaces.PaymentData;

public class SamplePaymentData implements PaymentData
{
    private String transactionType =  PaymentData.TransactionType.GOODS.getInternalType();
    private long amountTransaction = 10000;
    private long amountOther = 0;
    private String currencyCode = "0978";
    private String currencyExponent = "02";
    private String merchantCustomData = "SimAnt::MCL311:Test";
    private String merchantAdditionalData = "None";
    private String transactionId;

    @Override
    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    @Override
    public long getAmountTransaction() {
        return amountTransaction;
    }

    public void setAmountTransaction(long amountTransaction) {
        this.amountTransaction = amountTransaction;
    }

    @Override
    public long getAmountOther() {
        return amountOther;
    }

    public void setAmountOther(long amountOther) {
        this.amountOther = amountOther;
    }

    @Override
    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    @Override
    public String getCurrencyExponent() {
        return currencyExponent;
    }

    public void setCurrencyExponent(String currencyExponent) {
        this.currencyExponent = currencyExponent;
    }

    @Override
    public String getMerchantCustomData() {
        return merchantCustomData;
    }

    public void setMerchantCustomData(String merchantCustomData) {
        this.merchantCustomData = merchantCustomData;
    }

    @Override
    public String getMerchantAdditionalData() {
        return merchantAdditionalData;
    }

    @Override
    public String getTerminalSpecificData() {
        return null;
    }

    @Override
    public String getGeolocationData() {
        return null;
    }

    @Override
    public String getInstanceData() {
        return null;
    }

    @Override
    public String getUserData() {
        return null;
    }

    public void setMerchantAdditionalData(String merchantAdditionalData) {
        this.merchantAdditionalData = merchantAdditionalData;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    @Override
    public String getTransactionEntryData()
    {
        // Transaction Type + Amount Transaction + Amount Others + Currency Code + Exponent
        // 1 + 6 + 6 + 2 + 1
        //String ret = String.format("%.2s%012.0lf%012.0lf%.4s%.2s" , transactionType, amountTransaction, amountOther, currencyCode, currencyExponent);
        String ret = String.format("%.2s%012.0lf%012.0lf%.4s%.2s" ,
                TransactionType.descriptionToEnum(transactionType).getCLCInternalType(),
                amountTransaction, amountOther, currencyCode, currencyExponent);
        return ret;
        //return transactionType + "000000001250" + "000000000000" + currencyCode + currencyExponent;
        //return "00000000001250000000000000094902";
    }

    @Override
    public String toString() {
        return "SamplePaymentData{" +
                "transactionType='" + transactionType + '\'' +
                ", amountTransaction=" + amountTransaction +
                ", amountOther=" + amountOther +
                ", currencyCode='" + currencyCode + '\'' +
                ", currencyExponent='" + currencyExponent + '\'' +
                ", merchantCustomData='" + merchantCustomData + '\'' +
                ", merchantAdditionalData='" + merchantAdditionalData + '\'' +
                ", transactionId='" + transactionId + '\'' +
                '}';
    }
}
