package com.payten.nkbm.dto;

import java.io.Serializable;

public class QR implements Serializable {

    private String userId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    private String identificationCode;
    private String version;
    private String charSet;
    private String recepientAccNumber;
    private String recepientNameAndPlace;
    private String amountAndCurrency;
    private String payerAccNumber;
    private String payerNameAndPlace;
    private String paymentCode;
    private String mcc;
    private String oneTimePaymentPassword;
    private String recepientReferenceNumber;
    private String transactionReference;
    private String paymentPurpose;
    private String recepientCallNumberReference;
    private String payerReference;
    private String merchantName;
    private String merchantAddress;
    private String creditTransferIdentificator;

    public String getCreditTransferIdentificator() {
        return creditTransferIdentificator;
    }

    public void setCreditTransferIdentificator(String creditTransferIdentificator) {
        this.creditTransferIdentificator = creditTransferIdentificator;
    }

    public String getMerchantAddress() {
        return merchantAddress;
    }

    public void setMerchantAddress(String merchantAddress) {
        this.merchantAddress = merchantAddress;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getPayerReference() {
        return payerReference;
    }

    public void setPayerReference(String payerReference) {
        this.payerReference = payerReference;
    }

    public String getRecepientCallNumberReference() {
        return recepientCallNumberReference;
    }

    public void setRecepientCallNumberReference(String recepientCallNumberReference) {
        this.recepientCallNumberReference = recepientCallNumberReference;
    }

    public String getPaymentPurpose() {
        return paymentPurpose;
    }

    public void setPaymentPurpose(String paymentPurpose) {
        this.paymentPurpose = paymentPurpose;
    }

    public String getIdentificationCode() {
        return identificationCode;
    }

    public void setIdentificationCode(String identificationCode) {
        this.identificationCode = identificationCode;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCharSet() {
        return charSet;
    }

    public void setCharSet(String charSet) {
        this.charSet = charSet;
    }

    public String getRecepientAccNumber() {
        return recepientAccNumber;
    }

    public void setRecepientAccNumber(String recepientAccNumber) {
        this.recepientAccNumber = recepientAccNumber;
    }

    public String getRecepientNameAndPlace() {
        return recepientNameAndPlace;
    }

    public void setRecepientNameAndPlace(String recepientNameAndPlace) {
        this.recepientNameAndPlace = recepientNameAndPlace;
    }

    public String getAmountAndCurrency() {
        return amountAndCurrency;
    }

    public void setAmountAndCurrency(String amountAndCurrency) {
        this.amountAndCurrency = amountAndCurrency;
    }

    public String getAccNumber() {
        return payerAccNumber;
    }

    public void setAccNumber(String payerAccNumber) {
        this.payerAccNumber = payerAccNumber;
    }

    public String getNameAndPlace() {
        return payerNameAndPlace;
    }

    public void setNameAndPlace(String payerNameAndPlace) {
        this.payerNameAndPlace = payerNameAndPlace;
    }

    public String getPaymentCode() {
        return paymentCode;
    }

    public void setPaymentCode(String paymentCode) {
        this.paymentCode = paymentCode;
    }

    public String getMcc() {
        return mcc;
    }

    public void setMcc(String mcc) {
        this.mcc = mcc;
    }

    public String getOneTimePaymentPassword() {
        return oneTimePaymentPassword;
    }

    public void setOneTimePaymentPassword(String oneTimePaymentPassword) {
        this.oneTimePaymentPassword = oneTimePaymentPassword;
    }



    public String getRecepientReferenceNumber() {
        return recepientReferenceNumber;
    }

    public void setRecepientReferenceNumber(String recepientReferenceNumber) {
        this.recepientReferenceNumber = recepientReferenceNumber;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getPayerAccNumber() {
        return payerAccNumber;
    }

    public String getPayerNameAndPlace() {
        return payerNameAndPlace;
    }

    @Override
    public String toString() {
        return "QR{" +
                "userId='" + userId + '\'' +
                ", identificationCode='" + identificationCode + '\'' +
                ", version='" + version + '\'' +
                ", charSet='" + charSet + '\'' +
                ", recepientAccNumber='" + recepientAccNumber + '\'' +
                ", recepientNameAndPlace='" + recepientNameAndPlace + '\'' +
                ", amountAndCurrency='" + amountAndCurrency + '\'' +
                ", payerAccNumber='" + payerAccNumber + '\'' +
                ", payerNameAndPlace='" + payerNameAndPlace + '\'' +
                ", paymentCode='" + paymentCode + '\'' +
                ", mcc='" + mcc + '\'' +
                ", oneTimePaymentPassword='" + oneTimePaymentPassword + '\'' +
                ", recepientReferenceNumber='" + recepientReferenceNumber + '\'' +
                ", transactionReference='" + transactionReference + '\'' +
                ", paymentPurpose='" + paymentPurpose + '\'' +
                ", recepientCallNumberReference='" + recepientCallNumberReference + '\'' +
                ", payerReference='" + payerReference + '\'' +
                '}';
    }


}
