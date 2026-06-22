
package masterdata.controllers;

import core.api.dao.UserDAO;
import core.api.dao.PermissionDAO;
import core.security.UserSession;
import models.dto.UserDTO;
import core.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Controller for User management operations.
 * Handles fetching, creating, updating, deleting, activating, deactivating users and resetting passwords.
 *
 * @author Sanod
 */
public class UserManagementController {

    private String username = UserSession.getInstance().getUsername();

    public UserManagementController() {

    }

    // get all users
    public List<UserDTO> getAllUsers() {
        try {
            return UserDAO.getInstance().getUsers(null);
        } catch (Exception e) {
            Logger.errlog("failed to get all users: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // create new user
    public int createUser(UserDTO userDto, String initialPassword) {
        // validate input
        if (userDto == null || userDto.getUsername() == null || userDto.getUsername().trim().isEmpty()) {
            Logger.errlog("create user failed: invalid user data", new IllegalArgumentException("Invalid user data"));
            return 0;
        }

        try {
            int userId = UserDAO.getInstance().createUser(userDto, initialPassword, null);

            if (userId > 0) {
                Logger.log(username, "user created successfully: " + userDto.getUsername() + " (id: " + userId + ")");
            }

            return userId;

        } catch (Exception e) {
            Logger.errlog("create user failed: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // update existing user
    public boolean updateUser(UserDTO userDto) {
        if (userDto == null || userDto.getUserId() == null) {
            Logger.errlog("update user failed: invalid user data", new IllegalArgumentException("Invalid user data"));
            return false;
        }

        try {
            boolean success = UserDAO.getInstance().updateUser(userDto, null);

            if (success) {
                Logger.log(username, "user updated successfully: " + userDto.getUsername() + " (id: " + userDto.getUserId() + ")");
            }

            return success;

        } catch (Exception e) {
            Logger.errlog("update user failed: " + e.getMessage(), e);
            return false;
        }
    }

    // delete user
    public boolean deleteUser(int userId) {
        if (userId <= 0) {
            Logger.errlog("delete user failed: invalid user id", new IllegalArgumentException("Invalid user id"));
            return false;
        }

        try {
            boolean success = UserDAO.getInstance().deleteUser(userId);

            if (success) {
                Logger.log(username, "user deleted successfully: id=" + userId);
            }

            return success;

        } catch (Exception e) {
            Logger.errlog("failed to delete user: id=" + userId + " - " + e.getMessage(), e);
            return false;
        }
    }

    // deactivate user
    public boolean deactivateUser(int userId) {
        if (userId <= 0) {
            Logger.errlog("deactivate user failed: invalid user id", new IllegalArgumentException("Invalid user id"));
            return false;
        }

        try {
            boolean success = UserDAO.getInstance().deactivateUser(userId);

            if (success) {
                Logger.log(username, "user deactivated successfully: id=" + userId);
            }

            return success;

        } catch (Exception e) {
            Logger.errlog("failed to deactivate user: id=" + userId + " - " + e.getMessage(), e);
            return false;
        }
    }

    // activate user
    public boolean activateUser(int userId) {
        if (userId <= 0) {
            Logger.errlog("activate user failed: invalid user id", new IllegalArgumentException("Invalid user id"));
            return false;
        }

        try {
            boolean success = UserDAO.getInstance().activateUser(userId);

            if (success) {
                Logger.log(username, "user activated successfully: id=" + userId);
            }

            return success;

        } catch (Exception e) {
            Logger.errlog("failed to activate user: id=" + userId + " - " + e.getMessage(), e);
            return false;
        }
    }

    // reset user password
    public boolean resetUserPassword(int userId, String newPassword) {
        if (userId <= 0 || newPassword == null || newPassword.trim().isEmpty()) {
            Logger.errlog("reset password failed: invalid parameters", new IllegalArgumentException("Invalid parameters for password reset"));
            return false;
        }

        try {
            boolean success = UserDAO.getInstance().resetPassword(userId, newPassword);

            if (success) {
                Logger.log(username, "password reset for user id: " + userId);
            }

            return success;

        } catch (Exception e) {
            Logger.errlog("failed to reset password for user id: " + userId + " - " + e.getMessage(), e);
            return false;
        }
    }

    // search users by criteria
    public List<UserDTO> searchUsers(String searchTerm) {
        try {
            return UserDAO.getInstance().getUsers(searchTerm);
        } catch (Exception e) {
            Logger.errlog("search users failed: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    // get user by id as dto
    public UserDTO getUserById(int userId) {
        try {
            return UserDAO.getInstance().getUserById(userId);
        } catch (Exception e) {
            Logger.errlog("get user by id failed: " + e.getMessage(), e);
            return null;
        }
    }
}
