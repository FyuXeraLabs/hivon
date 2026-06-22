/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.fyuxera.hivon;

import javax.swing.UIManager;
import javax.swing.JOptionPane;
import ui.LoginFrame;
import core.logging.Logger;
import core.api.ApiClient;
import core.api.ApiConfig;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;

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

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatIntelliJLaf");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // check internet conn
        if (!isInternetAvailable()) {
            Logger.errlog("Startup Check Failed: No internet connection detected.", null);
            JOptionPane.showMessageDialog(null,
                    "No internet connection detected. Please check your network connection and try again!",
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        //check API health
        try {
            JsonObject healthResponse = ApiClient.getInstance().checkHealth();
            boolean success = healthResponse.has("success") && healthResponse.get("success").getAsBoolean();
            String message = healthResponse.has("message") ? healthResponse.get("message").getAsString() : "No message provided";
            
            if (success) {
                Logger.log("System", "API health check passed: " + healthResponse.toString());
            } else {
                Logger.errlog("API health check failed: " + healthResponse.toString(), null);
                JOptionPane.showMessageDialog(null,
                        "The application server is experiencing issues or undergoing maintenance. Please try again later!",
                        "System Health Error",
                        JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        } catch (Exception e) {
            Logger.errlog("API health check request failed: " + e.getMessage(), e);
            JOptionPane.showMessageDialog(null,
                    "Unable to connect to the application server. Please verify your connection or try again later!",
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
                
        LoginFrame login = new LoginFrame();
        login.setVisible(true);
    }
}
