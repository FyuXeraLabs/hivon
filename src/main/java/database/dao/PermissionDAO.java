/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package database.dao;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

import core.logging.Logger;
import database.DBConnection;

/**
 *
 * @author Sanod
 */
public class PermissionDAO {

    // get all permission codes from database
    public Set<String> getAllPermissionCodes() {
        Set<String> permissions = new HashSet<>();

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT permission_code FROM permissions ORDER BY permission_code";
            PreparedStatement stmt = conn.prepareStatement(sql);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                permissions.add(rs.getString("permission_code"));
            }
        } catch (SQLException e) {
            Logger.errlog("error getting all permission codes: " + e.getMessage(), e);
        }

        return permissions;
    }

    // get permissions by userId
    public Set<String> getPermissionsByUserId(Integer userId) {
        Set<String> permissions = new HashSet<>();

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT p.permission_code FROM user_permissions up JOIN permissions p ON up.permission_id = p.permission_id WHERE up.user_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                permissions.add(rs.getString("permission_code"));
            }
        } catch (SQLException e) {
            Logger.errlog("error getting permissions for user id " + userId + ": " + e.getMessage(), e);
        }

        return permissions;
    }

    // get permission id by permission code
    private Integer getPermissionIdByCode(Connection conn, String permissionCode) throws SQLException {
        String sql = "SELECT permission_id FROM permissions WHERE permission_code = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, permissionCode);

        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getInt("permission_id");
        }
        return null;
    }

    // assign permission to user
    public boolean assignPermissionToUser(Integer userId, String permissionCode) {
        try (Connection conn = DBConnection.getConnection()) {
            // first get the permission_id from permission_code
            Integer permissionId = getPermissionIdByCode(conn, permissionCode);

            if (permissionId == null) {
                Logger.errlog("invalid permission code: " + permissionCode, new IllegalArgumentException("Invalid permission code"));
                return false;
            }

            String sql = "INSERT INTO user_permissions (user_id, permission_id) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, permissionId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Logger.errlog("error assigning permission to user: " + e.getMessage(), e);
            return false;
        }
    }

    // remove permission from user
    public boolean removePermissionFromUser(Integer userId, String permissionCode) {
        try (Connection conn = DBConnection.getConnection()) {
            // first get the permission_id from permission_code
            Integer permissionId = getPermissionIdByCode(conn, permissionCode);

            if (permissionId == null) {
                Logger.errlog("invalid permission code: " + permissionCode, new IllegalArgumentException("Invalid permission code"));
                return false;
            }

            String sql = "DELETE FROM user_permissions WHERE user_id = ? AND permission_id = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, permissionId);

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            Logger.errlog("error removing permission from user: " + e.getMessage(), e);
            return false;
        }
    }
}