/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.logging;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.logging.*;
import java.io.FileOutputStream;

/**
 *
 * @author Sanod
 */
public class Logger {
    private static final String LOG_DIR = "logs";
    private static final String ERROR_LOG_DIR = "errlog";
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger("AppLogger");
    private static final java.util.logging.Logger ERROR_LOGGER = java.util.logging.Logger.getLogger("ErrorLogger");
    
    private static String currentLogFile = null;
    private static String currentErrorLogFile = null;

    static {
        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.ALL);
        
        ERROR_LOGGER.setUseParentHandlers(false);
        ERROR_LOGGER.setLevel(Level.SEVERE);
    }

    public static void log(String username, String message) {
        log(username, Level.INFO, message, null);
    }

    public static void log(String username, Level level, String message) {
        log(username, level, message, null);
    }

    public static void log(String username, Level level, String message, Throwable t) {
        try {
            Files.createDirectories(Paths.get(LOG_DIR));

            String fileName = LocalDate.now() + ".log";
            Path logPath = Paths.get(LOG_DIR, fileName);
            String logPathStr = logPath.toString();

            if (!logPathStr.equals(currentLogFile)) {
                for (Handler h : LOGGER.getHandlers()) {
                    h.close();
                    LOGGER.removeHandler(h);
                }
                
                StreamHandler handler = new StreamHandler(new FileOutputStream(logPath.toFile(), true), new UsernameFormatter()) {
                    @Override
                    public synchronized void publish(LogRecord record) {
                        super.publish(record);
                        flush();
                    }
                };
                handler.setLevel(Level.ALL);
                LOGGER.addHandler(handler);
                currentLogFile = logPathStr;
            }

            String logMessage = "[" + username + "] " + message;
            LOGGER.log(level, logMessage, t);

        } catch (IOException e) {
            System.err.println("Failed to write log: " + e.getMessage());
        }
    }

    public static void errlog(String message, Throwable t) {
        try {
            Files.createDirectories(Paths.get(ERROR_LOG_DIR));

            String fileName = LocalDate.now() + ".log";
            Path logPath = Paths.get(ERROR_LOG_DIR, fileName);
            String logPathStr = logPath.toString();

            if (!logPathStr.equals(currentErrorLogFile)) {
                for (Handler h : ERROR_LOGGER.getHandlers()) {
                    h.close();
                    ERROR_LOGGER.removeHandler(h);
                }
                
                StreamHandler handler = new StreamHandler(new FileOutputStream(logPath.toFile(), true), new TimestampFormatter()) {
                    @Override
                    public synchronized void publish(LogRecord record) {
                        super.publish(record);
                        flush();
                    }
                };
                handler.setLevel(Level.SEVERE);
                ERROR_LOGGER.addHandler(handler);
                currentErrorLogFile = logPathStr;
            }

            ERROR_LOGGER.log(Level.SEVERE, message, t);

        } catch (IOException e) {
            System.err.println("Failed to write error log: " + e.getMessage());
        }
    }
}