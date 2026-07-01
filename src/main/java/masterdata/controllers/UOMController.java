package masterdata.controllers;

import core.api.dao.SystemParameterDAO;
import core.security.UserSession;
import core.logging.Logger;
import models.dto.UOMDTO;

import java.util.ArrayList;
import java.util.List;

import core.utils.RetryHelper;

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
            return RetryHelper.executeWithRetry(
                () -> SystemParameterDAO.getInstance().getUOMs(null),
                "failed to get all UOMs"
            );
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // searches UOM records by the given search term
    public List<UOMDTO> searchUOMs(String searchTerm) {
        try {
            return RetryHelper.executeWithRetry(
                () -> SystemParameterDAO.getInstance().getUOMs(searchTerm),
                "search UOMs failed"
            );
        } catch (Exception e) {
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
            boolean success = RetryHelper.executeWithRetry(
                () -> SystemParameterDAO.getInstance().createUOM(uomDto),
                "create UOM failed"
            );
            if (success) {
                Logger.log(username, "UOM created successfully: " + uomDto.getParamKey());
            }
            return success;
        } catch (Exception e) {
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
            boolean success = RetryHelper.executeWithRetry(
                () -> SystemParameterDAO.getInstance().updateUOM(uomDto),
                "update UOM failed"
            );
            if (success) {
                Logger.log(username, "UOM updated successfully: " + uomDto.getParamKey());
            }
            return success;
        } catch (Exception e) {
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
            boolean success = RetryHelper.executeWithRetry(
                () -> SystemParameterDAO.getInstance().deleteUOM(paramKey),
                "failed to delete UOM"
            );
            if (success) {
                Logger.log(username, "UOM deleted successfully: " + paramKey);
            }
            return success;
        } catch (Exception e) {
            return false;
        }
    }
}