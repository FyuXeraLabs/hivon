/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database.dao;

import database.DatabaseHelper;
import models.entity.User;
import models.entity.User.UserRole;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import core.logging.Logger;

/**
 *
 * @author Sanod
 */
public class UserDAO {

    public UserDAO() {
        
    }

    // get all users (active and inactive)
    public List<User> getAllUsers() {
        String sql = "SELECT * FROM users ORDER BY username";

        try {
            return DatabaseHelper.executeQueryList(sql, this::mapResultSetToUser);
        } catch (Exception e) {
            Logger.errlog("error getting all users: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // get user by username
    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ? AND is_active = true";

        try {
            List<User> users = DatabaseHelper.executeQueryList(sql, this::mapResultSetToUser, username);
            return users.isEmpty() ? null : users.get(0);
        } catch (Exception e) {
            Logger.errlog("error getting user by username: " + e.getMessage(), e);
            return null;
        }
    }

    // get user by id
    public User getUserById(int userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        try {
            List<User> users = DatabaseHelper.executeQueryList(sql, this::mapResultSetToUser, userId);
            return users.isEmpty() ? null : users.get(0);
        } catch (Exception e) {
            Logger.errlog("error getting user by id: " + e.getMessage(), e);
            return null;
        }
    }

    // get all active users
    public List<User> getAllActiveUsers() {
        String sql = "SELECT * FROM users WHERE is_active = true ORDER BY username";

        try {
            return DatabaseHelper.executeQueryList(sql, this::mapResultSetToUser);
        } catch (Exception e) {
            Logger.errlog("error getting all active users: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // create new user and return generated user id
    public int createUser(User user, String passwordHash) {
        String sql = "INSERT INTO users (username, password_hash, full_name, email, role, warehouse_id, is_active, created_date) VALUES (?, ?, ?, ?, ?, ?, ?, NOW())";

        try {
            return DatabaseHelper.executeInsert(sql,
                    user.getUsername(),
                    passwordHash,
                    user.getFullName(),
                    user.getEmail(),
                    user.getRole().toString(),
                    user.getWarehouseId(),
                    user.isActive());
        } catch (Exception e) {
            Logger.errlog("error creating user: " + e.getMessage(), e);
            return 0;
        }
    }

    // update user
    public boolean updateUser(User user) {
        String sql = "UPDATE users SET full_name = ?, email = ?, role = ?, warehouse_id = ?, is_active = ?, last_modified = NOW() WHERE user_id = ?";

        try {
            int rows = DatabaseHelper.executeUpdate(sql,
                    user.getFullName(),
                    user.getEmail(),
                    user.getRole().toString(),
                    user.getWarehouseId(),
                    user.isActive(),
                    user.getUserId());
            return rows > 0;
        } catch (Exception e) {
            Logger.errlog("error updating user: " + e.getMessage(), e);
            return false;
        }
    }

    // update password
    public boolean updatePassword(int userId, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ?, last_modified = NOW() WHERE user_id = ?";

        try {
            int rows = DatabaseHelper.executeUpdate(sql, passwordHash, userId);
            return rows > 0;
        } catch (Exception e) {
            Logger.errlog("error updating password: " + e.getMessage(), e);
            return false;
        }
    }

    // deactivate user
    public boolean deactivateUser(int userId) {
        String sql = "UPDATE users SET is_active = false, last_modified = NOW() WHERE user_id = ?";

        try {
            int rows = DatabaseHelper.executeUpdate(sql, userId);
            return rows > 0;
        } catch (Exception e) {
            Logger.errlog("error deactivating user: " + e.getMessage(), e);
            return false;
        }
    }

    // delete user
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";

        try {
            int rows = DatabaseHelper.executeUpdate(sql, userId);
            return rows > 0;
        } catch (Exception e) {
            Logger.errlog("error deleting user: " + e.getMessage(), e);
            return false;
        }
    }

    // activate user
    public boolean activateUser(int userId) {
        String sql = "UPDATE users SET is_active = true, last_modified = NOW() WHERE user_id = ?";

        try {
            int rows = DatabaseHelper.executeUpdate(sql, userId);
            return rows > 0;
        } catch (Exception e) {
            Logger.errlog("error activating user: " + e.getMessage(), e);    
            return false;
        }
    }

    // check if username exists
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) as count FROM users WHERE username = ?";

        try {
            Integer count = DatabaseHelper.executeQuery(sql, rs -> {
                if (rs.next()) {
                    return rs.getInt("count");
                }
                return 0;
            }, username);

            return count != null && count > 0;
        } catch (Exception e) {
            Logger.errlog("error checking username exists: " + e.getMessage(), e);
            return false;
        }
    }

    // check if email exists
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) as count FROM users WHERE email = ?";

        try {
            Integer count = DatabaseHelper.executeQuery(sql, rs -> {
                if (rs.next()) {
                    return rs.getInt("count");
                }
                return 0;
            }, email);

            return count != null && count > 0;
        } catch (Exception e) {
            Logger.errlog("error checking email exists: " + e.getMessage(), e);
            return false;
        }
    }

    // update last login timestamp
    public boolean updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login = NOW() WHERE user_id = ?";

        try {
            int rows = DatabaseHelper.executeUpdate(sql, userId);
            return rows > 0;
        } catch (Exception e) {
            Logger.errlog("error updating last login: " + e.getMessage(), e);
            return false;
        }
    }

    // get user permissions
    public List<String> getUserPermissions(int userId) {
        String sql = "SELECT p.permission_code FROM user_permissions up JOIN permissions p ON up.permission_id = p.permission_id WHERE up.user_id = ?";

        try {
            return DatabaseHelper.executeQueryList(sql, rs -> rs.getString("permission_code"), userId);
        } catch (Exception e) {
            Logger.errlog("error getting user permissions: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // map result set to user object
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));

        // convert string role to enum
        String roleStr = rs.getString("role");
        UserRole role = UserRole.valueOf(roleStr);
        user.setRole(role);

        int warehouseId = rs.getInt("warehouse_id");
        if (!rs.wasNull()) {
            user.setWarehouseId(warehouseId);
        } else {
            user.setWarehouseId(null);
        }

        user.setIsActive(rs.getBoolean("is_active"));

        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) {
            user.setLastLogin(lastLogin.toLocalDateTime());
        }

        Timestamp createdDate = rs.getTimestamp("created_date");
        if (createdDate != null) {
            user.setCreatedDate(createdDate.toLocalDateTime());
        }

        Timestamp lastModified = rs.getTimestamp("last_modified");
        if (lastModified != null) {
            user.setLastModified(lastModified.toLocalDateTime());
        }

        return user;
    }
}
