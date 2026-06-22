/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.security;

import core.api.ApiConfig;
import core.logging.Logger;
import models.entity.User;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Timer;

/**
 *
 * @author Sanod
 */
public class UserSession {

    // singleton instance
    private static UserSession instance;

    // session data
    private User currentUser;
    private LocalDateTime loginTime;
    private Set<String> permissions;

    // jwt tokens
    private String accessToken;
    private String refreshToken;

    // inactivity tracking
    private long lastActivityTime;
    private Timer inactivityTimer;
    private static final int TIMER_CHECK_INTERVAL_MS = 30_000; // 30 seconds

    // private constructor for singleton
    private UserSession() {
        this.permissions = new HashSet<>();
    }

    // get singleton instance
    public static UserSession getInstance() {
        if (instance == null) {
            synchronized (UserSession.class) {
                if (instance == null) {
                    instance = new UserSession();
                }
            }
        }
        return instance;
    }

    // init session with user, permissions, and tokens
    public void initialize(User user, Set<String> userPermissions,
                           String accessToken, String refreshToken) {
        this.currentUser = user;
        this.loginTime = LocalDateTime.now();
        this.permissions.clear();
        if (userPermissions != null) {
            this.permissions.addAll(userPermissions);
        }
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.lastActivityTime = System.currentTimeMillis();

        // start inactivity monitor
        // startInactivityTimer(); // disabled inactivity auto-logout
    }

    // init session with user (backward compatibility)
    public void initialize(User user, Set<String> userPermissions) {
        initialize(user, userPermissions, null, null);
    }

    // invalidate session
    public void invalidate() {
        this.currentUser = null;
        this.loginTime = null;
        this.permissions.clear();
        this.accessToken = null;
        this.refreshToken = null;

        stopInactivityTimer();
    }

    // ─── INACTIVITY TIMER ──────────────────────────────────────────────

    private void startInactivityTimer() {
        stopInactivityTimer();

        inactivityTimer = new Timer(TIMER_CHECK_INTERVAL_MS, e -> {
            if (isExpired()) {
                Logger.log("UserSession", "Session expired due to inactivity ("
                        + ApiConfig.SESSION_TIMEOUT_MINUTES + " minutes)");
                stopInactivityTimer();

                // trigger session expiry on EDT
                javax.swing.SwingUtilities.invokeLater(() -> {
                    LogoutManager.sessionExpired();
                });
            }
        });
        inactivityTimer.setRepeats(true);
        inactivityTimer.start();

        Logger.log("UserSession", "Inactivity timer started ("
                + ApiConfig.SESSION_TIMEOUT_MINUTES + " min timeout)");
    }

    private void stopInactivityTimer() {
        if (inactivityTimer != null && inactivityTimer.isRunning()) {
            inactivityTimer.stop();
            inactivityTimer = null;
        }
    }

    /**
     * Update the last activity time. Called on every API call or user interaction.
     */
    public void updateActivity() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    /**
     * Check if session has expired due to inactivity.
     */
    public boolean isExpired() {
        return false;
    }

    // ─── TOKEN MANAGEMENT ──────────────────────────────────────────────

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    // ─── EXISTING GETTERS ──────────────────────────────────────────────

    // get current user
    public User getCurrentUser() {
        return currentUser;
    }

    // get login time
    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    // check if user has specific permission
    public boolean hasPermission(String permissionCode) {
        if (permissionCode == null || permissionCode.trim().isEmpty()) {
            return false;
        }
        return permissions.contains(permissionCode);
    }

    // check if session is valid
    public boolean isValid() {
        return currentUser != null && loginTime != null;
    }

    // get user id
    public Integer getUserId() {
        return currentUser != null ? currentUser.getUserId() : null;
    }

    // get username
    public String getUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }

    // get user role
    public User.UserRole getUserRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }

    // get all permissions
    public Set<String> getPermissions() {
        return new HashSet<>(permissions);
    }

    // add permission to session
    public void addPermission(String permissionCode) {
        if (permissionCode != null && !permissionCode.trim().isEmpty()) {
            permissions.add(permissionCode);
        }
    }

    // remove permission from session
    public void removePermission(String permissionCode) {
        if (permissionCode != null) {
            permissions.remove(permissionCode);
        }
    }
}
