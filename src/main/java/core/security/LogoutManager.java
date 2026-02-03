/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.security;

import javax.swing.*;
import java.awt.*;
import core.logging.Logger;
import ui.LoginFrame;

/**
 * centralized logout
 *
 * @author Sanod
 */
public class LogoutManager {

    public static void logout() {
        String username = UserSession.getInstance().getUsername();
        
        // confirm logout
        int confirm = JOptionPane.showConfirmDialog(null, "Are you sure you want to logout?        \n", "Confirm Logout", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            // invalidate the session
            UserSession.getInstance().invalidate();
            Logger.log(username, "User session invalidated");

            closeAllWindows();
            showLoginFrame();

            Logger.log(username, "User logged out successfully");
            
        } catch (Exception e) {
            Logger.errlog("Error during logout: " + e.getMessage(), e);
            JOptionPane.showMessageDialog(null, "An error occurred during logout. Please restart the application.", "Logout Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // close all open windows
    private static void closeAllWindows() {
        // get all windows
        Window[] windows = Window.getWindows();

        for (Window window : windows) {
            if (window.isVisible() && !isLoginFrame(window)) {
                window.dispose();
            }
        }
    }

    // check if window is a login frame
    private static boolean isLoginFrame(Window window) {
        return window.getClass().getSimpleName().equals("LoginFrame");
    }

    // show the login frame
    private static void showLoginFrame() {
        SwingUtilities.invokeLater(() -> {
            try {
                
                ui.LoginFrame loginFrame = new ui.LoginFrame();
                loginFrame.setVisible(true);
                loginFrame.setLocationRelativeTo(null);

            } catch (Exception e) {
                Logger.errlog("Error showing login frame: " + e.getMessage(), e);
            }
        });
    }

    // logout without confirmation
    public static void forceLogout() {
        String username = UserSession.getInstance().getUsername();
        
        UserSession.getInstance().invalidate();
        closeAllWindows();
        showLoginFrame();
        Logger.log(username, "Force logout completed!");
    }
}
