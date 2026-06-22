/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.security;

import core.api.dao.PermissionDAO;
import java.util.HashSet;
import java.util.Set;

import core.logging.Logger;

/**
 * Permission management via REST API.
 * Replaces direct PermissionDAO access with API calls.
 *
 * @author Sanod
 */
public class PermissionManager {

    // load user permissions from API
    public Set<String> loadUserPermissions(Integer userId) {
        Set<String> permissions = new HashSet<>();

        if (userId == null) {
            return permissions;
        }

        try {
            permissions = PermissionDAO.getInstance().getUserPermissions(userId);
        } catch (Exception e) {
            Logger.errlog("error loading permissions for user id " + userId, e);
        }

        return permissions;
    }

    // check if user has specific permission
    public boolean hasPermission(Integer userId, String permissionCode) {
        if (userId == null || permissionCode == null) {
            return false;
        }

        Set<String> permissions = loadUserPermissions(userId);
        return permissions.contains(permissionCode);
    }

    // get all available permission codes in system
    public Set<String> getAllSystemPermissions() {
        Set<String> allPermissions = new HashSet<>();

        try {
            allPermissions = PermissionDAO.getInstance().getAllPermissions();
        } catch (Exception e) {
            Logger.errlog("error loading system permissions", e);
        }

        return allPermissions;
    }

    // assign permission to user
    public boolean assignPermission(Integer userId, String permissionCode) {
        try {
            return PermissionDAO.getInstance().assignPermission(userId, permissionCode);
        } catch (Exception e) {
            Logger.errlog("error assigning permission to user id " + userId, e);
            return false;
        }
    }

    // remove permission from user
    public boolean removePermission(Integer userId, String permissionCode) {
        try {
            return PermissionDAO.getInstance().removePermission(userId, permissionCode);
        } catch (Exception e) {
            Logger.errlog("error removing permission from user id " + userId, e);
            return false;
        }
    }

    // validate permission code exists in system
    public boolean isValidPermission(String permissionCode) {
        Set<String> allPermissions = getAllSystemPermissions();
        return allPermissions.contains(permissionCode);
    }
}
