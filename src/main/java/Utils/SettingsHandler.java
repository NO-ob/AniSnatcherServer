package Utils;

import java.io.*;

public final class SettingsHandler {
    public static String transmissionIP = "",transmissionPort = "",serverPort = "6969",settingsPath,
            databaseDirectory = System.getProperty("user.home") + "/.AniSnatcher/server/",
            logDirectory = System.getProperty("user.home") + "/.AniSnatcher/server/logs/";
    public static boolean enableLogging = true;
    private static File settingsFile;
    private static SettingsHandler settingsHandlerInstance = null;

    /**
     * Creates and returns a settingsHandler instance
     */
    public static SettingsHandler Inst(){
        if (settingsHandlerInstance == null) {
            settingsHandlerInstance = new SettingsHandler();
            if (!settingsHandlerInstance.settingsFileExists()){
                settingsHandlerInstance.makeFile();
                settingsHandlerInstance.writeSettings();
            }
            settingsHandlerInstance.loadSettings();
        }
        return settingsHandlerInstance;
    }

    /**
     * Load settings from a text file
     */
    private void loadSettings() {
        String input;
        try {
            BufferedReader br = new BufferedReader(new FileReader(System.getProperty("user.home") + "/.AniSnatcher/server/settings.conf"));
            System.out.println("reading settings");
            try {
                while ((input = br.readLine()) != null) {
                    //Splits line and then switches on the option name
                    if (input.split(" = ").length > 1) {
                        switch (input.split(" = ")[0]) {
                            case ("Server Port"):
                                serverPort = input.split(" = ")[1];
                                break;
                            case ("Transmission IP"):
                                transmissionIP = input.split(" = ")[1];
                                break;
                            case ("Transmission Port"):
                                transmissionPort = input.split(" = ")[1];
                                break;
                            case("Database Directory"):
                                databaseDirectory = input.split(" = ")[1];
                                break;
                            case("Log Directory"):
                                logDirectory = input.split(" = ")[1];
                                break;
                            case("Enable Logging"):
                                if (!input.split(" = ")[1].isEmpty()){
                                    enableLogging = Boolean.valueOf(input.split(" = ")[1]);
                                }
                                break;

                        }
                    }
                }
            } catch (IOException | ArrayIndexOutOfBoundsException e) {
                System.out.println("Exception when loading settings" + e);
            }
        } catch (FileNotFoundException e) {
            System.out.println("Settings File not found");
        }
        if (serverPort.isEmpty()){
            serverPort = "6969";
        }
        if(transmissionPort.isEmpty()){
            System.out.println("Transmission Port is empty please add it to the config file at ~/.AniSnatcher/server/settings.conf");
            System.exit(0);
        }
        if(transmissionIP.isEmpty()){
            System.out.println("Transmission IP is empty please add it to the config file at ~/.AniSnatcher/server/settings.conf");
            System.exit(0);
        }
        if(databaseDirectory.isEmpty()){
            databaseDirectory = System.getProperty("user.home") + "/.AniSnatcher/server";
        } else if (!databaseDirectory.endsWith("/")){
            databaseDirectory += "/";
        }
        if(logDirectory.isEmpty()){
            logDirectory = System.getProperty("user.home") + "/.AniSnatcher/server/logs";
        } else if (!logDirectory.endsWith("/")){
            logDirectory += "/";
        }
    }
    private Boolean settingsFileExists(){
        File settingsFile = new File(System.getProperty("user.home") + "/.AniSnatcher/server/settings.conf");
        if (settingsFile.exists() && !settingsFile.isDirectory()){
            return true;
        } else {
            return false;
        }
    }
    /**
     * Write settings to a text file
     */
    private void writeSettings(){
        try {
            FileWriter fw = new FileWriter(settingsFile);
            fw.write(System.lineSeparator());
            fw.write("Server Port" + " = " + serverPort);
            fw.write(System.lineSeparator());
            fw.write("Transmission IP" + " = " + transmissionIP);
            fw.write(System.lineSeparator());
            fw.write("Transmission Port" + " = " + transmissionPort);
            fw.write(System.lineSeparator());
            fw.write("Database Directory" + " = " + databaseDirectory);
            fw.write(System.lineSeparator());
            fw.write("Log Directory" + " = " + logDirectory);
            fw.write(System.lineSeparator());
            fw.write("Enable Logging" + " = " + enableLogging);
            fw.write(System.lineSeparator());
            fw.flush();
            fw.close();
        } catch(IOException e) {
            System.out.println("Failed to write settings " + e);
        }
    }
    /**
     * Creates a new settings file in the users home directory
     */
    private void makeFile(){
        try {
            settingsPath = System.getProperty("user.home") + "/.AniSnatcher/server/";
            new File(settingsPath).mkdirs();
            settingsFile = new File(settingsPath +"settings.conf");
            if (settingsFile.createNewFile()) {
                System.out.println("File created: " + settingsFile.getName());
            } else {
                System.out.println("Settings File already exists.");
            }
        } catch (IOException e){
            System.out.println("SettingsHandler makeFile exception" + e);
        }
    }
}

