/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.dto;

import java.time.LocalDateTime;

/**
 *
 * @author Sanod
 */
public class UserDTO {

    private Integer userId;
    private String username;
    private String fullName;
    private String email;
    private String role;
    private Integer warehouseId;
    private Boolean isActive;
    private LocalDateTime lastLogin;
    private LocalDateTime createdDate;
    private LocalDateTime modifiedDate;

    // default constructor
    public UserDTO() {
        
    }

    // constructor for creating new user
    public UserDTO(String username, String fullName, String email, String role, Integer warehouseId) {
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.role = role;
        this.warehouseId = warehouseId;
        this.isActive = true;
    }

    // getters and setters
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Integer warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(LocalDateTime modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    // helper method to check if user is active
    public boolean isActive() {
        return isActive != null && isActive;
    }

    @Override
    public String toString() {
        return username + " - " + fullName + " (" + role + ")";
    }
}
