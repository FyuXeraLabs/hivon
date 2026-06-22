/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.security;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import core.api.ApiClient;
import core.logging.Logger;
import ui.LoginFrame;

/**
 * centralized logout with frame state preservation
 *
 * @author Sanod
 */
public class LogoutManager {

    // saved frame states survive session invalidation (static)
    private static Map<String, Map<String, String>> savedFrameStates = new HashMap<>();
    private static boolean wasSessionExpired = false;

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
            ApiClient.getInstance().clearTokens();
            Logger.log(username, "User session invalidated");

            clearSavedState();
            closeAllWindows();
            showLoginFrame();

            Logger.log(username, "User logged out successfully");

        } catch (Exception e) {
            Logger.errlog("Error during logout: " + e.getMessage(), e);
            JOptionPane.showMessageDialog(null, "An error occurred during logout. Please restart the application.", "Logout Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Called by inactivity timer when session expires.
     * Captures frame state before closing windows.
     */
    public static void sessionExpired() {
        String username = UserSession.getInstance().getUsername();

        try {
            Logger.log(username, "Session expired due to inactivity");

            // capture state from all visible frames BEFORE closing
            captureAllFrameStates();
            wasSessionExpired = true;

            // invalidate session and clear tokens
            UserSession.getInstance().invalidate();
            ApiClient.getInstance().clearTokens();

            // close all windows
            closeAllWindows();

            // show message and login frame
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                        "Your session has expired due to inactivity.\nPlease login again to continue.",
                        "Session Expired",
                        JOptionPane.WARNING_MESSAGE);
                showLoginFrame();
            });

            Logger.log(username, "Session expiry handled - state captured for restoration");

        } catch (Exception e) {
            Logger.errlog("Error during session expiry: " + e.getMessage(), e);
            // fallback: force logout
            closeAllWindows();
            showLoginFrame();
        }
    }

    /**
     * Capture the state (text fields, combos, checkboxes) of all visible JFrames.
     */
    private static void captureAllFrameStates() {
        savedFrameStates.clear();

        Window[] windows = Window.getWindows();
        for (Window window : windows) {
            if (window.isVisible() && window instanceof JFrame && !isLoginFrame(window)) {
                String frameKey = window.getClass().getName();
                Map<String, String> state = captureFrameState((JFrame) window);
                if (!state.isEmpty()) {
                    savedFrameStates.put(frameKey, state);
                    Logger.log("LogoutManager", "Captured state for: " + frameKey
                            + " (" + state.size() + " fields)");
                }
            }
        }
    }

    /**
     * Capture all field values from a JFrame.
     */
    private static Map<String, String> captureFrameState(JFrame frame) {
        Map<String, String> state = new HashMap<>();
        captureComponentStates(frame.getContentPane(), state, "");
        return state;
    }

    /**
     * Recursively capture component states.
     */
    private static void captureComponentStates(Container container,
                                                Map<String, String> state,
                                                String prefix) {
        for (Component comp : container.getComponents()) {
            String name = comp.getName();
            if (name == null || name.isEmpty()) {
                // use field variable name if available via reflection
                name = getFieldName(container, comp);
            }

            String key = prefix.isEmpty() ? (name != null ? name : "") : prefix + "." + (name != null ? name : "");

            if (comp instanceof JTextField && !(comp instanceof JPasswordField)) {
                JTextField tf = (JTextField) comp;
                if (key != null && !key.isEmpty()) {
                    state.put("tf:" + key, tf.getText());
                    state.put("tf:" + key + ":editable", String.valueOf(tf.isEditable()));
                }
            } else if (comp instanceof JComboBox) {
                JComboBox<?> cb = (JComboBox<?>) comp;
                if (key != null && !key.isEmpty()) {
                    state.put("cb:" + key, String.valueOf(cb.getSelectedIndex()));
                }
            } else if (comp instanceof JCheckBox) {
                JCheckBox chk = (JCheckBox) comp;
                if (key != null && !key.isEmpty()) {
                    state.put("chk:" + key, String.valueOf(chk.isSelected()));
                }
            } else if (comp instanceof Container) {
                captureComponentStates((Container) comp, state, key);
            }
        }
    }

    /**
     * Try to find the field name for a component via reflection.
     */
    private static String getFieldName(Container parent, Component comp) {
        try {
            // check the top-level frame for field names
            Container root = parent;
            while (root.getParent() != null) {
                root = root.getParent();
            }

            for (java.lang.reflect.Field field : root.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    if (field.get(root) == comp) {
                        return field.getName();
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // ─── STATE ACCESS METHODS ──────────────────────────────────────────

    /**
     * Check if there is saved state from a session timeout.
     */
    public static boolean hasSavedState() {
        return wasSessionExpired && !savedFrameStates.isEmpty();
    }

    /**
     * Get the saved state for a specific frame class.
     */
    public static Map<String, String> getSavedFrameState(String frameClassName) {
        return savedFrameStates.get(frameClassName);
    }

    /**
     * Get all saved frame class names (to know which frames to re-open).
     */
    public static java.util.Set<String> getSavedFrameClassNames() {
        return savedFrameStates.keySet();
    }

    /**
     * Clear all saved state after restoration.
     */
    public static void clearSavedState() {
        savedFrameStates.clear();
        wasSessionExpired = false;
    }

    /**
     * Check if session was expired (vs manual logout).
     */
    public static boolean wasExpired() {
        return wasSessionExpired;
    }

    // ─── EXISTING METHODS ──────────────────────────────────────────────

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

        // capture state before force logout
        captureAllFrameStates();
        wasSessionExpired = true;

        UserSession.getInstance().invalidate();
        ApiClient.getInstance().clearTokens();
        closeAllWindows();
        showLoginFrame();
        Logger.log(username, "Force logout completed!");
    }
}
