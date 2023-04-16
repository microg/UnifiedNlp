package org.microg.nlp.backend.dejavu;

import android.util.Log;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import android.location.Location;
import org.microg.nlp.api.LocationBackend;

public class LogToFile {

    private static final String TAG = LogToFile.class.getName();
    
    private static final String TIME_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
    
    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat(TIME_DATE_PATTERN);
    
    public static String logFilePathname;
    public static boolean logToFileEnabled;
    public static int logFileHoursOfLasting;
    private static Calendar logFileAtTheEndOfLive;
        
    public static void appendLog(String tag, String text) {
        appendLog(tag, text, null);
    }

    public static void appendLogWithLocationBackend(String tag, String text, LocationBackend locationBackend) {
      if (locationBackend == null) {
        appendLog(tag, text + "null", null);
      } else {
        appendLog(tag, text + locationBackend.toString(), null);
      }
    }
    
    public static void appendLogWithLocation(String tag, String text, Location location) {
      if (location == null) {
        appendLog(tag, text + "null", null);
      } else {
        appendLog(tag, text + location.toString(), null);
      }
    }

    public static void appendLog(String tag, String text, Throwable throwable) {
            
            logToFileEnabled = false;
            logFilePathname = "/sdcard/Download/logs/log-dejavu.txt";
            logFileHoursOfLasting = 96;

        if (!logToFileEnabled || (logFilePathname == null)) {
            //Log.e(TAG, "logging not allowed " + logToFileEnabled + " " + logFilePathname);
            return;
        }
        
        File logFile = new File(logFilePathname);
        
        Date now = new Date();        
        try {
            if (logFile.exists()) {                
                if (logFileAtTheEndOfLive == null) {
                    boolean succeeded = initFileLogging(logFile);
                    if (!succeeded) {
                        createNewLogFile(logFile, now);
                    }
                } else if(Calendar.getInstance().after(logFileAtTheEndOfLive)) {
                    logFile.delete();
                    createNewLogFile(logFile, now);
                }
            } else {
               createNewLogFile(logFile, now);
            }
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(DATE_FORMATTER.format(now));
            buf.append(" ");
            buf.append(tag);
            buf.append(" - ");
            buf.append(text);
            if (throwable != null) {
                buf.append(" - ");
                buf.append(throwable.getMessage());
                for (StackTraceElement ste: throwable.getStackTrace()) {
                    buf.newLine();
                    buf.append(ste.toString());
                }
            }
            buf.newLine();
            buf.close();
        } catch (IOException e) {
           Log.e(TAG, e.getMessage());
        }
    }
    
    private static boolean initFileLogging(File logFile) {
        char[] logFileDateCreatedBytes = new char[TIME_DATE_PATTERN.length()];
        Date logFileDateCreated;
        FileReader logFileReader = null;
        try {
            logFileReader = new FileReader(logFile);
            logFileReader.read(logFileDateCreatedBytes);
            logFileDateCreated = DATE_FORMATTER.parse(new String(logFileDateCreatedBytes));
        } catch (Exception e) {
            return false;
        } finally {
            if (logFileReader != null) {
                try {
                    logFileReader.close();
                } catch (IOException ex) {
                    
                }
            }
        }
        initEndOfLive(logFileDateCreated);
        return true;
    }
    
    private static void initEndOfLive(Date logFileDateCreated) {
        logFileAtTheEndOfLive = Calendar.getInstance();
        logFileAtTheEndOfLive.setTime(logFileDateCreated);
        logFileAtTheEndOfLive.add(Calendar.HOUR_OF_DAY, logFileHoursOfLasting);
    }
    
     private static void createNewLogFile(File logFile, Date dateOfCreation) throws IOException {
        logFile.createNewFile();
        initEndOfLive(dateOfCreation);
    }
}
