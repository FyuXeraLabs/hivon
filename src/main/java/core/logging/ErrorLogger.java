/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.logging;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.logging.*;

/**
 *
 * @author Sanod
 */
public class ErrorLogger {
    private static final String LOG_DIR = "errlog";
    private static final Logger LOGGER = Logger.getLogger("ErrorLogger");

    static {
        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.SEVERE);
    }

    public static void logError(String message, Throwable t) {
        try {
            Files.createDirectories(Paths.get(LOG_DIR));

            String fileName = LocalDate.now() + ".log";
            Path logPath = Paths.get(LOG_DIR, fileName);

            if (!isHandlerPresent(logPath.toString())) {
                FileHandler handler = new FileHandler(logPath.toString(), true);
                handler.setFormatter(new TimestampFormatter());
                handler.setLevel(Level.SEVERE);
                LOGGER.addHandler(handler);
            }

            LOGGER.log(Level.SEVERE, message, t);

        } catch (IOException e) {
            System.err.println("Failed to write error log: " + e.getMessage());
        }
    }

    private static boolean isHandlerPresent(String path) {
        for (Handler h : LOGGER.getHandlers()) {
            if (h instanceof FileHandler &&
                h.toString().contains(path)) {
                return true;
            }
        }
        return false;
    }
}
