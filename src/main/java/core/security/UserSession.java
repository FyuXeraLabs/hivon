/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.security;

import models.entity.User;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    // init session with user
    public void initialize(User user, Set<String> userPermissions) {
        this.currentUser = user;
        this.loginTime = LocalDateTime.now();
        this.permissions.clear();
        if (userPermissions != null) {
            this.permissions.addAll(userPermissions);
        }
    }

    // invalidate session
    public void invalidate() {
        this.currentUser = null;
        this.loginTime = null;
        this.permissions.clear();
    }

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
