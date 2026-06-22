/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.security;

import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.api.dao.UserDAO;
import models.entity.User;
import core.logging.Logger;

/**
 * Authentication via REST API.
 * Replaces direct database access with API calls.
 *
 * @author Sanod
 */
public class UserAuthentication {

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 30;

    public UserAuthentication() {

    }

    // auth user with username and password via REST API
    public User authenticate(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            Logger.errlog("authentication failed: username or password empty", new IllegalArgumentException("Username or password is empty"));
            return null;
        }

        try {
            ApiClient api = ApiClient.getInstance();
            JsonObject response = api.login(username, password);

            // extract user from response
            JsonObject data = response.getAsJsonObject("data");
            JsonObject userJson = data.getAsJsonObject("user");

            User user = UserDAO.getInstance().jsonToUser(userJson);

            Logger.log(username, "Authentication Successful!");
            return user;

        } catch (Exception e) {
            logFailedAttempt(username);
            Logger.errlog("authentication failed for user: " + username + " - " + e.getMessage(), e);
            return null;
        }
    }

    // lock account via API (deactivate user)
    public void lockAccount(String username) {
        try {
            // we don't have userId here from username easily via API
            // the lock is handled server-side if needed
            Logger.log(username, "Account lock requested due to multiple failed login attempts.");
        } catch (Exception e) {
            Logger.errlog("Error locking account: " + e.getMessage(), e);
        }
    }

    // validate password meets complexity req (client-side validation)
    public boolean validatePasswordComplexity(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpper = true;
            }
            if (Character.isLowerCase(c)) {
                hasLower = true;
            }
            if (Character.isDigit(c)) {
                hasDigit = true;
            }
            if (!Character.isLetterOrDigit(c)) {
                hasSpecial = true;
            }
        }

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    // log failed attempt
    private void logFailedAttempt(String username) {
        Logger.errlog("failed login attempt for user: " + username, new IllegalArgumentException("Invalid login attempt"));
    }
}
