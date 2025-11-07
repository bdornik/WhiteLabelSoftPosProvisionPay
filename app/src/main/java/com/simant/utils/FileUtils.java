package com.simant.utils;

import static android.content.ContentValues.TAG;

import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


public class FileUtils {

    private static BufferedWriter mLogWriter;

    public static ArrayList<String> loadFilesFromDir(String dir,
                                                     final String fileType)
            throws FileNotFoundException {
        File mPath = new File(dir);
        String[] list;
        if (mPath.exists()) {
            FilenameFilter filter = new FilenameFilter() {
                @Override
                public boolean accept(File dir, String filename) {
                    return filename.endsWith(fileType);
                }
            };
            list = mPath.list(filter);
        } else {
            throw new FileNotFoundException();
        }
        if (list == null)
            throw new FileNotFoundException();
        return new ArrayList<>(Arrays.asList(list));
    }

    /*
    private static int rok = 1;
    public static void writeRandoms(String filePath) {
        if (rok != 0) return;
        ByteArray rseed = ByteArrayFactory.getInstance().fromHexString("BAEF898D39B33947F445F8B499B09643C4EFFC163DE95062B150BC6A166C3EA9");
        try {
            SACBPPCryptoService.getInstance().seedRandom(rseed);
            for (int k = 0; k < 1; k++) {
                String fileName = "rnd128-0" + Integer.toString(k+1);
                File file = new File(filePath, fileName);
                DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
                for (int i = 0; i < 1024; i++) {
                    for (int j = 0; j < 1024; j++) {
                        ByteArray rba = SACBPPCryptoService.getInstance().generateRandom(128);
                        dos.write(rba.getBytes());
                        rok++;
                    }
                }
                dos.close();
            }
        } catch (Exception ane) {
            if (1==1) System.out.println("E-EOC " + ane.getMessage()); ane.printStackTrace();
        }
    }
    */

    public static String readDataFromLocalStorage(String filePath, String fileName)
            throws IOException {
        //writeRandoms(filePath);
        //Get the text file
        File file = new File(filePath, fileName);

        //Read text from file
        StringBuilder text = new StringBuilder();

        BufferedReader br = new BufferedReader(new FileReader(file));
        String line;

        while ((line = br.readLine()) != null) {
            text.append(line + "\n");
        }
        br.close();
        return new String(text);
    }

    public static void createDirectory(String rootPath) {
        if (isExternalStorageWritable()) {
            final File mRootFolder = new File(rootPath);
            if (!mRootFolder.exists() && !mRootFolder.mkdirs()) {
                Log.e(TAG, "createDirectory: Unable to create the root folder");
            }
        } else {
            Log.e(TAG, "createDirectory: SD Card not available for read and write");
        }

    }

    private static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static void logDataToFile(String dataToBeLogged) {
        try {
            if (mLogWriter == null) return;
            mLogWriter.append(dataToBeLogged);
            mLogWriter.newLine();
            mLogWriter.flush();

        } catch (IOException e) {
            Log.e(TAG, "IOException was encountered: ", e.getCause());
            try {
                mLogWriter.close();
            } catch (IOException er) {
                Log.e(TAG, "IOException was encountered while closing Log writer: ", er.getCause());
            }
        }
    }

    public static void logDataToFile(String headerToBeLogged, String dataToBeLogged) {
        Log.d("LOGD2F", headerToBeLogged + " : "+ dataToBeLogged);
        logDataToFile(headerToBeLogged + " : "+ dataToBeLogged);
    }

    public static void closeFileWriter() {
        if (mLogWriter == null) return;
        try {
            Log.d("", "closeFileWriter: ");
            mLogWriter.close();
        } catch (IOException e) {
            Log.e(TAG, "IOException was thrown while closing File Writer: ", e.getCause());
        }
    }

    public static void createLogWriter(File mLogFile) {
        try {
            mLogWriter = new BufferedWriter(new FileWriter(mLogFile, false));
        } catch (IOException e) {
            Log.e(TAG, "IOException was encounterd while creating File writer: ", e.getCause());
        }
    }

    public static void resetLogWriter(File mLogFile) {
        closeFileWriter();
        createLogWriter(mLogFile);
    }

    public static void startLoggingToFile(String typeOfData, String dataToBeLogged) {
        try {
            if (mLogWriter == null) return;
            mLogWriter.write(typeOfData + " : " + dataToBeLogged);
            mLogWriter.newLine();
            mLogWriter.flush();
        } catch (IOException e) {
            Log.e(TAG, "IOException encountered while logging to file: ", e.getCause());
        }
    }

    public enum LogFileType {
        OUTnMSG("OUTnMSG.LOG")
        ,SOFTPOS("SOFTPOS.LOG")
        ,EMVTERM("EMVTERM.LOG")
        //,OUTCOME("TPOUTCOME.LOG")
        //,DISPLAYUI("TPDISPLAYUI.LOG")
        ,RAISOUT("TPOUTCOMERAIS.LOG")
        ;

        private String mLogFileName;

        LogFileType(String logName) {
            mLogFileName = logName;
        }

        public String getLogFileName() {
            return mLogFileName;
        }

    }
}
