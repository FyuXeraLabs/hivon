/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package core.config;

import core.logging.Logger;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;

/**
 *
 * @author Sanod D. Mendis
 */
public class SettingsManager {

    private static final String APP_NAME = "Hivon";
    private static final String FILE_NAME = "settings.properties";
    private static Path settingsFilePath;

    private static final String DEFAULT_THEME = "com.formdev.flatlaf.FlatIntelliJLaf";

    private static boolean soundEnabled = true;
    private static int soundVolume = 100;
    private static String theme = DEFAULT_THEME;

    public static void init() {
        try {
            Path dirPath = getSettingsDirectory();
            Files.createDirectories(dirPath);

            settingsFilePath = dirPath.resolve(FILE_NAME);

            if (Files.exists(settingsFilePath)) {
                load();
            } else {
                resetToDefaults();
            }

        } catch (Exception e) {
            Logger.errlog("Settings init failed: " + e.getMessage(), e);
        }
    }

    // returns settings dir according to os
    private static Path getSettingsDirectory() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isEmpty()) {
                return Paths.get(appData, APP_NAME);
            }
        } else if (os.contains("mac")) {
            return Paths.get(System.getProperty("user.home"),
                    "Library", "Application Support", APP_NAME);
        } else {
            String xdg = System.getenv("XDG_CONFIG_HOME");
            if (xdg != null && !xdg.isEmpty()) {
                return Paths.get(xdg, APP_NAME);
            }
            return Paths.get(System.getProperty("user.home"),
                    ".config", APP_NAME);
        }

        return Paths.get(System.getProperty("user.home"), APP_NAME);
    }

    public static void resetToDefaults() {
        soundEnabled = true;
        soundVolume = 100;
        theme = DEFAULT_THEME;
        save();
    }

    private static synchronized void load() {
        Properties props = new Properties();

        try (InputStream in = Files.newInputStream(settingsFilePath)) {
            props.load(in);

            soundEnabled = Boolean.parseBoolean(props.getProperty("sound.enabled", "true"));

            soundVolume = Integer.parseInt(props.getProperty("sound.volume", "100"));
            if (soundVolume < 0) soundVolume = 0;
            if (soundVolume > 100) soundVolume = 100;

            theme = props.getProperty("theme", DEFAULT_THEME);

        } catch (Exception e) {
            Logger.errlog("Settings load failed: " + e.getMessage(), e);
            resetToDefaults();
        }
    }

    private static synchronized void save() {
        Properties props = new Properties();

        props.setProperty("sound.enabled", String.valueOf(soundEnabled));
        props.setProperty("sound.volume", String.valueOf(soundVolume));
        props.setProperty("theme", theme);

        try (OutputStream out = Files.newOutputStream(settingsFilePath)) {
            props.store(out, "Hivon Settings");
        } catch (Exception e) {
            Logger.errlog("Settings save failed: " + e.getMessage(), e);
        }
    }

    public static boolean isSoundEnabled() {
        return soundEnabled;
    }

    public static void setSoundEnabled(boolean enabled) {
        if (soundEnabled != enabled) {
            soundEnabled = enabled;
            save();
        }
    }

    public static int getSoundVolume() {
        return soundVolume;
    }

    public static void setSoundVolume(int volume) {
        if (volume < 0) volume = 0;
        if (volume > 100) volume = 100;

        if (soundVolume != volume) {
            soundVolume = volume;
            save();
        }
    }

    public static String getTheme() {
        return theme;
    }

    public static void setTheme(String themeClass) {
        if (themeClass != null && !theme.equals(themeClass)) {
            theme = themeClass;
            save();
        }
    }

    public static Path getSettingsFilePath() {
        return settingsFilePath;
    }
}
