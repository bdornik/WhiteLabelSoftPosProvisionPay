package com.simant.softpos.impl;

import static com.simant.utils.FileUtils.createDirectory;

import com.simant.MainApplication;
import com.simant.utils.FileUtils;
import com.simcore.api.interfaces.TransactionLogger;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class SampleTransactionLogger implements TransactionLogger {

    private static File mLogFile;

    public SampleTransactionLogger() {
        String filePath = MainApplication.getInstance().getWorkingDirectory() + File.separator + "LOG" + File.separator + "SOFTPOS.LOG";

        mLogFile = new File(filePath);
        try {
            if (!mLogFile.exists()) {
                String rootPath = MainApplication.getInstance().getWorkingDirectory();
                createDirectory(rootPath);
                String directoryPath = rootPath + File.separator + "LOG";
                createDirectory(directoryPath);
                mLogFile.createNewFile();
            }
            FileUtils.createLogWriter(mLogFile);
            String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
            FileUtils.startLoggingToFile("SOFTPOS started ", formattedDate);
        } catch (IOException e) {
            //Log.e("TPL", e.toString());
        }
    }

    @Override
    public void logd(String logData) {
        //Log.d("", logData);
        FileUtils.logDataToFile("DEBUG", logData);
    }

    @Override
    public void logi(String logData) {
        //Log.i("", logData);
        FileUtils.logDataToFile("INFO", logData);
    }

    @Override
    public void logw(String logData) {
        //Log.w("", logData);
        FileUtils.logDataToFile("WARNING", logData);
    }

    @Override
    public void loge(String logData) {
        //Log.e("", logData);
        FileUtils.logDataToFile("ERROR", logData);
    }

    public void log(String header, String logData) {
        //Log.d(header, logData);
        FileUtils.logDataToFile(header, logData);
    }

    public void closeFileWriter() {
        FileUtils.closeFileWriter();
    }

    public void resetLogWriter() {
        String filePath = MainApplication.getInstance().getWorkingDirectory() + File.separator + "LOG" + File.separator + "SOFTPOS.LOG";
        mLogFile = new File(filePath);
        FileUtils.resetLogWriter(mLogFile);
    }

}
