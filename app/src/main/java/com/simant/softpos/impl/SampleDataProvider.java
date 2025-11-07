package com.simant.softpos.impl;

import android.content.Context;

import com.simant.MainApplication;
import com.simant.softpos.providers.DataProvider;
import com.simant.utils.FileUtils;
import com.simant.utils.FileUtils.LogFileType;
import com.simcore.api.interfaces.PaymentData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SampleDataProvider implements DataProvider {

    private final String TAG = "SampleDataProviderImpl";
    private Context ctx = null;
    public SampleDataProvider(Context ctx)
    {
        this.ctx = ctx;
    }

    @Override
    public void resetTransactionLogs()
    {
        String filePath = MainApplication.getInstance().getWorkingDirectory() + File.separator + "LOG" + File.separator;

        try {
            ArrayList<String> transactionLogFilesList = FileUtils.loadFilesFromDir(filePath, ".LOG");
            for (String transactionLogsFileName : transactionLogFilesList) {
                File file = new File(filePath, transactionLogsFileName);
                file.delete();
            }
        } catch (FileNotFoundException e) {
        }
    }

    @Override
    public String getTransactionLogs(List<LogFileType> logFileType) {
        try {
            String filePath = MainApplication.getInstance().getWorkingDirectory() + File.separator + "LOG" + File.separator;
            ArrayList<String> transactionLogFilesList = FileUtils.loadFilesFromDir(filePath, ".LOG");
            //Collections.sort(transactionLogFilesList, Collections.reverseOrder());

            if (transactionLogFilesList == null || transactionLogFilesList.isEmpty()) {
                return "No transaction data found";
            }

            return getAllLogFilesData(transactionLogFilesList, filePath, logFileType);

        } catch (FileNotFoundException e) {
            //Log.e(TAG, "getTransactionLogs: SOFTPOS Directory Not Found");
            return "SOFTPOS directory not exist";
        }
    }

    private String getAllLogFilesData(ArrayList<String> transactionLogFilesList, String filePath,
                                          List<LogFileType> logFileType) {
        String rString = "";
        try {
            List<LogFileType> llogFileType = logFileType;
            if (llogFileType == null) llogFileType = new ArrayList<LogFileType>();
            if (llogFileType.isEmpty())
            {
                llogFileType.add(LogFileType.OUTnMSG);
                llogFileType.add(LogFileType.SOFTPOS);
                //llogFileType.add(LogFileType.EMVTERM);
                //llogFileType.add(LogFileType.OUTCOME);
                //llogFileType.add(LogFileType.DISPLAYUI);
                //llogFileType.add(LogFileType.RAISOUT);
            }
            for (LogFileType alogFileType : llogFileType) {
                for (String transactionLogsFileName : transactionLogFilesList) {
                    if (transactionLogsFileName.equals(alogFileType.getLogFileName()) == true) {
                        rString += "FILE : " + transactionLogsFileName + "----------------------------\n";
                        rString += FileUtils.readDataFromLocalStorage(filePath, transactionLogsFileName);
                        rString += "EOF  : " + transactionLogsFileName + "----------------------------\n";
                    }
                }
            }
        } catch (IOException ex) {
            //Log.e(TAG, "getTransactionLogs: IO Exception encountered " + "while accessing Transaction file");
            rString += "Transaction file not found";
        }
        return rString;
    }

    @Override
    public List<String> getCurrencyList() {
        List<String> currencyList = new ArrayList<>();
        for (PaymentData.CurrencyCode aValue : PaymentData.CurrencyCode.values()) {
            currencyList.add(aValue.getName());
        }
        return currencyList;
    }

    @Override
    public List<String> getTransactionTypeList() {
        List<String> txnTypeList = new ArrayList<>();
        for (PaymentData.TransactionType aValue : PaymentData.TransactionType.values()) {
            txnTypeList.add(aValue.getDescription());
        }
        return txnTypeList;
    }

    private ArrayList<String> readListFromStream(InputStream fin) {
        ArrayList<String> retList = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(fin));
            String rStr;
            while ((rStr = reader.readLine()) != null) {
                retList.add(rStr);
            }
            reader.close();
            fin.close();
        }
        catch (Exception ane) {}
        return retList;
    }

    @Override
    public ArrayList<String> getMCL311Cases() {
        try {
            File file;
            if (MainApplication.getMCL311Mode())
                file = new File(MainApplication.getInstance().getWorkingDirectory() + File.separator + "PRM" +  File.separator + "PAYPASSTESTCASES.TXT");
            else
                file = new File(MainApplication.getInstance().getWorkingDirectory() + File.separator + "PRM" +  File.separator + "PAYWAVETESTCASES.TXT");
            InputStream inputStream = new FileInputStream(file);
            ArrayList<String> fList = readListFromStream(inputStream);
            ArrayList<String> rList = new ArrayList<>();
            if (fList.size() == 0) {
                rList.add("Not Found");
            }
            else {
                rList.add("None of " + fList.size());
                for (String ae : fList) rList.add(ae);
            }
            return rList;
        }
        catch (IOException ane) {
            //Log.d("Not Found", ane.toString());
        }
        return null;
    }

    @Override
    public ArrayList<String> getMCL311Profiles() {
        try {
            File file;
            if (MainApplication.getMCL311Mode())
                file = new File(MainApplication.getInstance().getWorkingDirectory() + File.separator + "PRM" +  File.separator + "PAYPASSPRMLIST.TXT");
            else
                file = new File(MainApplication.getInstance().getWorkingDirectory() + File.separator + "PRM" +  File.separator + "VISAPRMLIST.TXT");
            if (file.exists() == false)
                file = new File(MainApplication.getInstance().getWorkingDirectory() + File.separator + "PRM" +  File.separator + "PRMLIST.TXT");
            InputStream inputStream = new FileInputStream(file);
            ArrayList<String> fList = readListFromStream(inputStream);
            ArrayList<String> rList = new ArrayList<>();
            if (fList.size() == 0) {
                rList.add("Not Found");
            }
            else {
                for (String ae : fList) rList.add(ae.substring(0,20).trim());
            }
            return rList;
        }
        catch (IOException ane) {}
        return null;
    }

    private String[] getMCL311CaseDetails(String caseName) {
        try {
            File file;
            if (MainApplication.getMCL311Mode())
                file = new File(MainApplication.getInstance().getWorkingDirectory() + File.separator + "PRM" +  File.separator + "PAYPASSALLCASES.TXT");
            else
                file = new File(MainApplication.getInstance().getWorkingDirectory() + File.separator + "PRM" +  File.separator + "PAYWAVEALLCASES.TXT");
            InputStream inputStream = new FileInputStream(file);
            ArrayList<String> fList = readListFromStream(inputStream);
            for (String ae : fList) {
                if (!ae.startsWith("/"))
                {
                    String[] ael = ae.split("\t");
                    if (ael != null)
                        if (ael[0].equalsIgnoreCase(caseName))
                            return ael;
                }
            }
        }
        catch (IOException ane) {}
        return null;
    }

    @Override
    public String getMCL311CaseProfile(String caseName) {
        String[] ael = getMCL311CaseDetails(caseName);
        if (ael != null) return ael[1];
        return null;
    }

    @Override
    public ArrayList<String> getMCL311TransactionData(String caseName) {
        ArrayList<String> rttta = new ArrayList<>();
        rttta.add(""); //"00"
        rttta.add(""); //"1500"
        rttta.add("");
        rttta.add("");
        String[] cd = getMCL311CaseDetails(caseName);
        String TRDF = null;
        if (cd != null) TRDF = cd[3];

        if ((TRDF != null) && (TRDF.equalsIgnoreCase("NONE")) == false) {
            try {
                File file = new File(MainApplication.getInstance().getWorkingDirectory() + File.separator + TRDF.replace("\\", "/"));
                InputStream inputStream = new FileInputStream(file);
                ArrayList<String> fList = readListFromStream(inputStream);
                //ArrayList<String> fList = readListFromStream(ctx.getResources().getAssets().open(TRDF.replace("\\", "/")));
                if (fList.size() > 1) {
                    rttta.clear();
                    if (fList.get(0).isEmpty()) rttta.add(""); else rttta.add(fList.get(0)); //"00"
                    if (fList.get(1).isEmpty()) rttta.add(""); else rttta.add(fList.get(1));//"1500"
                    if (fList.get(2).isEmpty()) rttta.add(""); else rttta.add(fList.get(2));
                    if ((fList.size() > 3) && (fList.get(3).isEmpty() == false)) rttta.add(fList.get(3)); else rttta.add("");
                }
            }
            catch (IOException ane) {
                rttta.clear();
                rttta.add(""); //"00"
                rttta.add(""); //"1500"
                rttta.add("");
                rttta.add("");
                rttta.add("");
                rttta.add("NONE");
                return rttta;
            }
        }
        String XMLFile = null;
        if (cd != null) XMLFile = cd[2];
        if (XMLFile != null) if (XMLFile.equalsIgnoreCase("NONE")) XMLFile = null;
        if (XMLFile != null) if (XMLFile.length() > 27) XMLFile = XMLFile.substring(27);
        if (XMLFile != null) rttta.add(XMLFile); else rttta.add("");
        String TRKAT = null;
        if (cd != null) TRKAT = cd[4];
        if (TRKAT != null) rttta.add(TRKAT); else rttta.add("NONE");
        return rttta;
    }
}
