
package masterdata.controllers;

import core.api.dao.UserDAO;
import core.api.dao.PermissionDAO;
import core.security.UserSession;
import models.dto.UserDTO;
import core.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import core.utils.RetryHelper;

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
            return RetryHelper.executeWithRetry(
                () -> UserDAO.getInstance().getUsers(null),
                "failed to get all users"
            );
        } catch (Exception e) {
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
            int userId = RetryHelper.executeWithRetry(
                () -> UserDAO.getInstance().createUser(userDto, initialPassword, null),
                "create user failed"
            );

            if (userId > 0) {
                Logger.log(username, "user created successfully: " + userDto.getUsername() + " (id: " + userId + ")");
            }

            return userId;

        } catch (Exception e) {
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
            boolean success = RetryHelper.executeWithRetry(
                () -> UserDAO.getInstance().updateUser(userDto, null),
                "update user failed"
            );

            if (success) {
                Logger.log(username, "user updated successfully: " + userDto.getUsername() + " (id: " + userDto.getUserId() + ")");
            }

            return success;

        } catch (Exception e) {
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
            boolean success = RetryHelper.executeWithRetry(
                () -> UserDAO.getInstance().deleteUser(userId),
                "failed to delete user"
            );

            if (success) {
                Logger.log(username, "user deleted successfully: id=" + userId);
            }

            return success;

        } catch (Exception e) {
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
            boolean success = RetryHelper.executeWithRetry(
                () -> UserDAO.getInstance().deactivateUser(userId),
                "failed to deactivate user"
            );

            if (success) {
                Logger.log(username, "user deactivated successfully: id=" + userId);
            }

            return success;

        } catch (Exception e) {
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
            boolean success = RetryHelper.executeWithRetry(
                () -> UserDAO.getInstance().activateUser(userId),
                "failed to activate user"
            );

            if (success) {
                Logger.log(username, "user activated successfully: id=" + userId);
            }

            return success;

        } catch (Exception e) {
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
            boolean success = RetryHelper.executeWithRetry(
                () -> UserDAO.getInstance().resetPassword(userId, newPassword),
                "failed to reset password"
            );

            if (success) {
                Logger.log(username, "password reset for user id: " + userId);
            }

            return success;

        } catch (Exception e) {
            return false;
        }
    }

    // search users by criteria
    public List<UserDTO> searchUsers(String searchTerm) {
        try {
            return RetryHelper.executeWithRetry(
                () -> UserDAO.getInstance().getUsers(searchTerm),
                "search users failed"
            );
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // get user by id as dto
    public UserDTO getUserById(int userId) {
        try {
            return RetryHelper.executeWithRetry(
                () -> UserDAO.getInstance().getUserById(userId),
                "get user by id failed"
            );
        } catch (Exception e) {
            return null;
        }
    }
}
