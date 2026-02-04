/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package masterdata.controllers;

import core.security.UserAuthentication;
import core.security.UserSession;
import database.dao.UserDAO;
import models.dto.UserDTO;
import models.entity.User;
import models.entity.User.UserRole;
import core.logging.Logger;

import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Sanod
 */
public class UserManagementController {

    private String username = UserSession.getInstance().getUsername();

    public UserManagementController() {
        
    }

    // get all users
    public List<UserDTO> getAllUsers() {
        UserDAO userDAO = new UserDAO();
        List<User> users = userDAO.getAllUsers();
        return users.stream().map(this::convertEntityToDto).collect(Collectors.toList());
    }

    // create new user
    public int createUser(UserDTO userDto, String initialPassword) {
        // validate input
        if (userDto == null || userDto.getUsername() == null || userDto.getUsername().trim().isEmpty()) {
            Logger.errlog("create user failed: invalid user data", new IllegalArgumentException("Invalid user data"));
            return 0;
        }

        UserDAO userDAO = new UserDAO();
        UserAuthentication authenticator = new UserAuthentication();

        // check if username already exists
        if (userDAO.usernameExists(userDto.getUsername())) {
            Logger.errlog("create user failed: username already exists - " + userDto.getUsername(), new IllegalArgumentException("Username already exists"));
            return 0;
        }

        // validate password complexity
        if (!authenticator.validatePasswordComplexity(initialPassword)) {
            Logger.errlog("create user failed: password does not meet complexity requirements", new IllegalArgumentException("Password does not meet complexity requirements"));
            return 0;
        }

        // convert dto to entity
        User user = convertDtoToEntity(userDto);

        // hash password
        String passwordHash = authenticator.hashPassword(initialPassword);

        // save to database
        int userId = userDAO.createUser(user, passwordHash);
        boolean success = userId > 0;

        if (userId > 0) {
            Logger.log(username, "user created successfully: " + userDto.getUsername() + " (id: " + userId + ")");
        } else {
            Logger.errlog("create user failed: database error for user - " + userDto.getUsername(), new IllegalStateException("Database error during user creation"));
        }

        return userId;
    }

    // update existing user
    public boolean updateUser(UserDTO userDto) {
        if (userDto == null || userDto.getUserId() == null) {
            Logger.errlog("update user failed: invalid user data", new IllegalArgumentException("Invalid user data"));
            return false;
        }

        UserDAO userDAO = new UserDAO();

        // get existing user
        User existingUser = userDAO.getUserById(userDto.getUserId());
        if (existingUser == null) {
            Logger.errlog("update user failed: user not found - id=" + userDto.getUserId(), new IllegalArgumentException("User not found"));
            return false;
        }

        // convert dto to entity
        User user = convertDtoToEntity(userDto);
        user.setUserId(userDto.getUserId());

        // update in database
        boolean success = userDAO.updateUser(user);

        if (success) {
            Logger.log(username, "user updated successfully: " + userDto.getUsername() + " (id: " + userDto.getUserId() + ")");
        } else {
            Logger.errlog("failed to update user: " + userDto.getUsername(), new IllegalStateException("Failed to update user in database"));
        }

        return success;
    }

    // delete user
    public boolean deleteUser(int userId) {
        if (userId <= 0) {
            Logger.errlog("delete user failed: invalid user id", new IllegalArgumentException("Invalid user id"));
            return false;
        }

        UserDAO userDAO = new UserDAO();
        boolean success = userDAO.deleteUser(userId);

        if (success) {
            Logger.log(username, "user deleted successfully: id=" + userId);
        } else {
            Logger.errlog("failed to delete user: id=" + userId, new IllegalStateException("Failed to delete user from database"));
        }

        return success;
    }

    // deactivate user
    public boolean deactivateUser(int userId) {
        if (userId <= 0) {
            Logger.errlog("deactivate user failed: invalid user id", new IllegalArgumentException("Invalid user id"));
            return false;
        }

        UserDAO userDAO = new UserDAO();
        boolean success = userDAO.deactivateUser(userId);

        if (success) {
            Logger.log(username, "user deactivated successfully: id=" + userId);
        } else {
            Logger.errlog("failed to deactivate user: id=" + userId, new IllegalStateException("Failed to deactivate user in database"));
        }

        return success;
    }

    // activate user
    public boolean activateUser(int userId) {
        if (userId <= 0) {
            Logger.errlog("activate user failed: invalid user id", new IllegalArgumentException("Invalid user id"));
            return false;
        }

        UserDAO userDAO = new UserDAO();
        boolean success = userDAO.activateUser(userId);

        if (success) {
            Logger.log(username, "user activated successfully: id=" + userId);
        } else {
            Logger.errlog("failed to activate user: id=" + userId, new IllegalStateException("Failed to activate user in database"));
        }

        return success;
    }

    // reset user password
    public boolean resetUserPassword(int userId, String newPassword) {
        if (userId <= 0 || newPassword == null || newPassword.trim().isEmpty()) {
            Logger.errlog("reset password failed: invalid parameters", new IllegalArgumentException("Invalid parameters for password reset"));
            return false;
        }

        UserAuthentication authenticator = new UserAuthentication();
        UserDAO userDAO = new UserDAO();

        // validate password complexity
        if (!authenticator.validatePasswordComplexity(newPassword)) {
            Logger.errlog("reset password failed: password does not meet complexity requirements", new IllegalArgumentException("Password does not meet complexity requirements"));
            return false;
        }

        User user = userDAO.getUserById(userId);
        if (user == null) {
            Logger.errlog("reset password failed: user not found - id=" + userId, new IllegalArgumentException("User not found"));
            return false;
        }

        boolean success = authenticator.resetPassword(user.getUsername(), newPassword);

        if (success) {
            Logger.log(username, "password reset for user: " + user.getUsername());
        } else {
            Logger.errlog("failed to reset password for user: " + user.getUsername(), new IllegalStateException("Failed to reset user password in database"));
        }

        return success;
    }

    // get all active users as dtos
    public List<UserDTO> getAllActiveUsers() {
        UserDAO userDAO = new UserDAO();
        List<User> users = userDAO.getAllActiveUsers();
        return users.stream().map(this::convertEntityToDto).collect(Collectors.toList());
    }

    // search users by criteria
    public List<UserDTO> searchUsers(String searchTerm) {
        UserDAO userDAO = new UserDAO();
        List<User> allUsers = userDAO.getAllActiveUsers();

        return allUsers.stream().filter(user -> matchesSearch(user, searchTerm)).map(this::convertEntityToDto).collect(Collectors.toList());
    }

    // get user by id as dto
    public UserDTO getUserById(int userId) {
        UserDAO userDAO = new UserDAO();
        User user = userDAO.getUserById(userId);
        return user != null ? convertEntityToDto(user) : null;
    }

    // get user by username as dto
    public UserDTO getUserByUsername(String username) {
        UserDAO userDAO = new UserDAO();
        User user = userDAO.getUserByUsername(username);
        return user != null ? convertEntityToDto(user) : null;
    }

    // convert dto to entity
    private User convertDtoToEntity(UserDTO dto) {
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());

        // convert string role to enum
        if (dto.getRole() != null) {
            try {
                UserRole role = UserRole.valueOf(dto.getRole().toUpperCase());
                user.setRole(role);
            } catch (IllegalArgumentException e) {
                // default to operator if invalid
                user.setRole(UserRole.OPERATOR);
            }
        } else {
            user.setRole(UserRole.OPERATOR);
        }

        user.setWarehouseId(dto.getWarehouseId());
        user.setIsActive(dto.getIsActive());

        return user;
    }

    // convert entity to dto
    private UserDTO convertEntityToDto(User user) {
        UserDTO dto = new UserDTO();
        dto.setUserId(user.getUserId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().toString());
        dto.setWarehouseId(user.getWarehouseId());
        dto.setIsActive(user.getIsActive());
        dto.setLastLogin(user.getLastLogin());
        dto.setCreatedDate(user.getCreatedDate());
        dto.setModifiedDate(user.getLastModified());

        return dto;
    }

    // check if user matches search term
    private boolean matchesSearch(User user, String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return true;
        }

        String term = searchTerm.toLowerCase();
        return user.getUsername().toLowerCase().contains(term) || user.getFullName().toLowerCase().contains(term) || (user.getEmail() != null && user.getEmail().toLowerCase().contains(term));
    }
}
