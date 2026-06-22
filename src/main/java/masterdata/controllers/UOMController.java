package masterdata.controllers;

import core.api.dao.SystemParameterDAO;
import core.security.UserSession;
import core.logging.Logger;
import models.dto.UOMDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Unit of Measure (UOM) master data operations.
 * Handles fetching, searching, creating, updating, and deleting UOM system parameters.
 *
 * @author Sanod
 */
public class UOMController {

    private final String username = UserSession.getInstance().getUsername();

    public UOMController() {
    }

    // retrieves all UOM records from the system
    public List<UOMDTO> getAllUOMs() {
        try {
            return SystemParameterDAO.getInstance().getUOMs(null);
        } catch (Exception e) {
            Logger.errlog("failed to get all UOMs: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // searches UOM records by the given search term
    public List<UOMDTO> searchUOMs(String searchTerm) {
        try {
            return SystemParameterDAO.getInstance().getUOMs(searchTerm);
        } catch (Exception e) {
            Logger.errlog("search UOMs failed: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // creates a new UOM system parameter record
    public boolean createUOM(UOMDTO uomDto) {
        if (uomDto == null || uomDto.getParamKey() == null || uomDto.getParamKey().trim().isEmpty()) {
            Logger.errlog("create UOM failed: invalid UOM data", new IllegalArgumentException("Invalid UOM data"));
            return false;
        }
        try {
            boolean success = SystemParameterDAO.getInstance().createUOM(uomDto);
            if (success) {
                Logger.log(username, "UOM created successfully: " + uomDto.getParamKey());
            }
            return success;
        } catch (Exception e) {
            Logger.errlog("create UOM failed: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // updates an existing UOM system parameter record
    public boolean updateUOM(UOMDTO uomDto) {
        if (uomDto == null || uomDto.getParamKey() == null) {
            Logger.errlog("update UOM failed: invalid UOM data", new IllegalArgumentException("Invalid UOM data"));
            return false;
        }
        try {
            boolean success = SystemParameterDAO.getInstance().updateUOM(uomDto);
            if (success) {
                Logger.log(username, "UOM updated successfully: " + uomDto.getParamKey());
            }
            return success;
        } catch (Exception e) {
            Logger.errlog("update UOM failed: " + e.getMessage(), e);
            return false;
        }
    }

    // deletes a UOM system parameter by its key
    public boolean deleteUOM(String paramKey) {
        if (paramKey == null || paramKey.trim().isEmpty()) {
            Logger.errlog("delete UOM failed: invalid param key", new IllegalArgumentException("Invalid param key"));
            return false;
        }
        try {
            boolean success = SystemParameterDAO.getInstance().deleteUOM(paramKey);
            if (success) {
                Logger.log(username, "UOM deleted successfully: " + paramKey);
            }
            return success;
        } catch (Exception e) {
            Logger.errlog("failed to delete UOM: " + paramKey + " - " + e.getMessage(), e);
            return false;
        }
    }
}