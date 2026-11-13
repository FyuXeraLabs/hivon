/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.fyuxera.hivon;

import javax.swing.UIManager;
import ui.LoginFrame;
import core.logging.Logger;
import core.api.ApiClient;
import core.api.ApiConfig;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import core.config.SettingsManager;
import core.workers.BackgroundTask;

/**
 *
 * @author Sanod D. Mendis
 */
public class Hivon {

    private static boolean isInternetAvailable() {
        try {
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("google.com", 80), 3000);
                return true;
            }
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean internetCheckPassed = false;
    private static boolean healthCheckPassed = false;

    public static void main(String[] args) {
        // Load config
        SettingsManager.init();

        try {
            UIManager.setLookAndFeel(SettingsManager.getTheme());
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatIntelliJLaf");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // Initialize system tray
        core.workers.SystemTray.init();

        /*
        // Check internet connection with retry support
        while (!internetCheckPassed) {
            createInternetCheckTask().executeWithRetry();
        }
        */

        // Check API health with retry support
        while (!healthCheckPassed) {
            createHealthCheckTask().executeWithRetry();
        }
                
        LoginFrame login = new LoginFrame();
        login.setVisible(true);
    }

    private static BackgroundTask createInternetCheckTask() {
        return new BackgroundTask((java.awt.Component) null, "Checking Internet") {
            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Checking internet connection...");
                if (!isInternetAvailable()) {
                    throw new Exception("No internet connection detected. Please check your network connection!");
                }
                return true;
            }

            @Override
            protected void onSuccess() {
                internetCheckPassed = true;
                Logger.log("System", "Internet connection check passed.");
            }

            @Override
            protected void onFailure(Exception e) {
                // handled by retry/cancel on progress dialog
            }

            @Override
            protected void onCancel() {
                System.exit(1);
            }

            @Override
            protected BackgroundTask createRetryTask() {
                return createInternetCheckTask();
            }
        };
    }

    private static BackgroundTask createHealthCheckTask() {
        return new BackgroundTask((java.awt.Component) null, "API Health Check") {
            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Checking API health...");
                JsonObject healthResponse;
                try {
                    healthResponse = ApiClient.getInstance().checkHealth();
                } catch (Exception e) {
                    Logger.errlog("API health check request failed: " + e.getMessage(), e);
                    throw new Exception("Unable to connect to application server. Please verify your connection!");
                }
                boolean success = healthResponse != null && healthResponse.has("success") && healthResponse.get("success").getAsBoolean();
                
                if (success) {
                    Logger.log("System", "API health check passed: " + healthResponse.toString());
                    return true;
                } else {
                    Logger.errlog("API health check failed: " + (healthResponse != null ? healthResponse.toString() : "null"), null);
                    throw new Exception("Application server is undergoing maintenance or experiencing issues!");
                }
            }

            @Override
            protected void onSuccess() {
                healthCheckPassed = true;
            }

            @Override
            protected void onFailure(Exception e) {
                // handled by retry/cancel on progress dialog
            }

            @Override
            protected void onCancel() {
                System.exit(1);
            }

            @Override
            protected BackgroundTask createRetryTask() {
                return createHealthCheckTask();
            }
        };
    }
}
