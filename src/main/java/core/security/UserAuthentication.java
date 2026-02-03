/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.security;

import database.dao.UserDAO;
import models.entity.User;
import core.logging.Logger;
import org.mindrot.jbcrypt.BCrypt;

/**
 *
 * @author Sanod
 */
public class UserAuthentication {

    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 30;

    public UserAuthentication() {
        
    }

    // auth user with username and password
    public User authenticate(String username, String password) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {

            Logger.errlog("authentication failed: username or password empty", new IllegalArgumentException("Username or password is empty"));
            return null;
        }

        UserDAO userDAO = new UserDAO();

        // get user from database
        User user = userDAO.getUserByUsername(username);
        if (user == null) {
            logFailedAttempt(username);
            Logger.errlog("authentication failed: user not found - " + username, new IllegalArgumentException("User not found"));
            return null;
        }

        // check if user is active
        if (!user.isActive()) {
            Logger.errlog("authentication failed: inactive user - " + username, new IllegalStateException("User account is inactive"));
            return null;
        }

        // verify password
        if (verifyPassword(password, user.getPasswordHash())) {
            // update last login timestamp
            userDAO.updateLastLogin(user.getUserId());
            Logger.log(username, "Authentication Successful!");
            return user;
        } else {
            logFailedAttempt(username);
            Logger.errlog("authentication failed for user: " + username, new IllegalArgumentException("Invalid password"));
            return null;
        }
    }

    // verify password against stored hash
    private boolean verifyPassword(String password, String hash) {
        try {
            return BCrypt.checkpw(password, hash);
        } catch (Exception e) {
            Logger.errlog("error verifying password: " + e.getMessage(), e);
            return false;
        }
    }

    // hash password using bcrypt
    public String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    // check if username exists
    public boolean usernameExists(String username) {
        UserDAO userDAO = new UserDAO();
        return userDAO.usernameExists(username);
    }

    // reset user password
    public boolean resetPassword(String username, String newPassword) {
        UserDAO userDAO = new UserDAO();
        User user = userDAO.getUserByUsername(username);
        if (user == null) {
            return false;
        }

        String hashedPassword = hashPassword(newPassword);
        return userDAO.updatePassword(user.getUserId(), hashedPassword);
    }

    // validate password meets complexity req
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

    public void lockAccount(String username) {
        UserDAO userDAO = new UserDAO();
        User user = userDAO.getUserByUsername(username);

        if (user != null) {
            // set user as inactive
            user.setIsActive(false);
            userDAO.updateUser(user);
            Logger.log(username, "Account locked due to multiple failed login attempts.");
        }
    }

    // log failed attempt
    private void logFailedAttempt(String username) {
        Logger.errlog("failed login attempt for user: " + username, new IllegalArgumentException("Invalid login attempt"));
    }
}
