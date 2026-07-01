package masterdata.controllers;

import core.api.dao.ZoneDAO;
import core.security.UserSession;
import core.logging.Logger;
import models.dto.ZoneDTO;

import java.util.ArrayList;
import java.util.List;

import core.utils.RetryHelper;

/**
 * Controller for Zone management operations.
 * Handles fetching and searching zones within a warehouse.
 *
 * @author Sanod
 */
public class ZoneManagementController {

    private final String username = UserSession.getInstance().getUsername();

    public ZoneManagementController() {
    }

    // retrieves all zones for a specific warehouse
    public List<ZoneDTO> getZonesByWarehouse(int warehouseId) {
        try {
            return RetryHelper.executeWithRetry(
                () -> ZoneDAO.getInstance().getZonesByWarehouse(warehouseId),
                "failed to get zones for warehouse " + warehouseId
            );
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // searches zones within a warehouse by the given search term
    public List<ZoneDTO> searchZonesByWarehouse(int warehouseId, String searchTerm) {
        try {
            return RetryHelper.executeWithRetry(
                () -> ZoneDAO.getInstance().searchZonesByWarehouse(warehouseId, searchTerm),
                "failed to search zones for warehouse " + warehouseId
            );
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}