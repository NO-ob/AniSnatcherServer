package Utils;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
/**
 * <h1>Logger</h1>
 * A singleton class which is used for logging, it will print log output to the terminal and also to a file
 */
public final class Logger {
    private static Logger loggerInstance;
    private static FileWriter fileWriter;
    private static File logFile;
    private static String logPath;
    private boolean logEnabled = SettingsHandler.Inst().enableLogging;
    private Logger(){
    }
    /**
     * Creates and returns a logger instance
     */
    public static Logger Inst(){
        if (loggerInstance == null) {
            loggerInstance = new Logger();
            loggerInstance.makeFile();
        }
        return loggerInstance;
    }
    /**
     * Creates a new log file in the users home directory
     */
    private void makeFile(){
        try {
            //logPath = System.getProperty("user.home") + "/.AniSnatcher/logs/";
            //logPath = "/mnt/Eucli/.AniSnatcher/logs/";
            logPath = SettingsHandler.Inst().logDirectory;
            new File(logPath).mkdirs();
            if (logEnabled){
                logFile = new File(logPath + getDateTime() + ".log");
                if (logFile.createNewFile()) {
                    System.out.println("File created: " + logFile.getName());
                } else {
                    System.out.println("File already exists.");
                }
                fileWriter = new FileWriter(logFile,true);
            }
        } catch (IOException e){
            System.out.println("Logger makeFile exception" + e);
        }
        Logger.Inst().log("Logger","MakeFile","Log File Created");
    }
    /**
     * Gets the current date/time and returns it as a string
     * @return String - The date/time string
     */
    private String getDateTime(){
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        return dtf.format(now);
    }
    /**
     * Gets the current date/time and returns it as a string
     */
    // Log to logfile and also print to console

    /**
     * The log function will create a log string and then print it to the log file and the terminal
     * @param className - The name of the class calling the log function
     * @param funcName - The name of the function calling the log function
     * @param logStr - The message to be logged
     */
    public void log(String className, String funcName, String logStr){
        String dateTime = getDateTime();
        String logString = "[" + dateTime +"]" + className + "::" + funcName + " - " + logStr;
        System.out.println(logString);
        if (logEnabled){
            // red log "\u001B[31m [SEVERE]\u001B[0m" +
            Thread fileWriteThread = new Thread(() -> {
                try {
                fileWriter = new FileWriter(logFile,true);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                PrintWriter printWriter = new PrintWriter(bufferedWriter);
                printWriter.println(logString);
                printWriter.close();
                bufferedWriter.close();
                fileWriter.close();
                } catch (IOException e){
                    System.out.println("Exception thrown in logger" + e);
                }
            });
            fileWriteThread.start();
        }
    }
}
