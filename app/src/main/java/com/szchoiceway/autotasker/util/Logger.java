package com.szchoiceway.autotasker.util;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RequiresApi(api = Build.VERSION_CODES.O)
public class Logger {
    private static final String FILE_DIR_PATH = ("sdcard" + File.separator + "autotasker");
    private static final String FILE_NAME_LOG = (FILE_DIR_PATH + File.separator + "autotasker_logs.txt");
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    public static final String NO_LOG = "There is no log to show.";
    private final static String TAG = Logger.class.getName();
    private static File logFile;

    public static void log(String tag, String log) {
        try {
            FileWriter fileWriter = new FileWriter(getLogFile(), true);
            LocalDateTime now = LocalDateTime.now();
            fileWriter.write(dtf.format(now) + " | " + tag + " : " + log  + "\n");
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String readLogs() {
        File file = new File(FILE_NAME_LOG);
        if (!file.exists()) {
            return null;
        }
        StringBuilder logText = new StringBuilder();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                logText.append(line);
                logText.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            Logger.log(TAG, e.getMessage());
        }
        boolean noLog = logText.length() == 0;
        return noLog ? Logger.NO_LOG : logText.toString();
    }

    public static boolean clearLogs() {
        try (PrintWriter writer = new PrintWriter(getLogFile())) {
            writer.print("");
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static File getLogFile() {
        if (logFile != null) {
            return logFile;
        }
        File dir = new File(FILE_DIR_PATH);
        if (!dir.exists()) dir.mkdirs();
        File file = new File(FILE_NAME_LOG);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                Logger.log(TAG, e.getMessage());
            }
        }
        logFile = file;
        return logFile;
    }
}
