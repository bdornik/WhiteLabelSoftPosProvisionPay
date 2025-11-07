package com.simant.softpos.providers;

import com.simant.utils.FileUtils.LogFileType;

import java.util.ArrayList;
import java.util.List;

public interface DataProvider {

    void resetTransactionLogs();

    String getTransactionLogs(final List<LogFileType> logFileType);

    List<String> getCurrencyList();

    List<String> getTransactionTypeList();

    ArrayList<String> getMCL311Cases();

    ArrayList<String> getMCL311Profiles();

    String getMCL311CaseProfile(String caseName);

    ArrayList<String> getMCL311TransactionData(String caseName);
}
