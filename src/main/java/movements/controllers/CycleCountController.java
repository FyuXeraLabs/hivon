package movements.controllers;

import core.api.dao.CycleCountDAO;
import core.api.dao.CycleCountDAO.CycleCountPlan;
import core.api.dao.CycleCountDAO.CycleCountItem;
import core.api.dao.CycleCountDAO.CreateCountResult;
import core.api.dao.WarehouseDAO;
import core.api.dao.ZoneDAO;
import core.api.dao.BinDAO;
import core.logging.Logger;
import core.security.UserSession;
import core.utils.RetryHelper;
import models.dto.WarehouseDTO;
import models.dto.ZoneDTO;
import models.dto.StorageBinDTO;

import java.util.List;

/**
 * Controller for Cycle Count operations.
 * Handles warehouse/zone/bin lookups, creating count plans,
 * recording physical counts, and finalizing counts.
 *
 * @author Piyumi
 */
public class CycleCountController {

    private final String username = UserSession.getInstance().getUsername();

    public CycleCountController() {
    }

    // fetches all active warehouses
    public List<WarehouseDTO> getWarehouses() throws Exception {
        return RetryHelper.executeWithRetry(
            () -> WarehouseDAO.getInstance().getWarehouses(),
            "failed to load warehouses"
        );
    }

    // fetches zones for a warehouse
    public List<ZoneDTO> getZonesByWarehouse(int warehouseId) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> ZoneDAO.getInstance().getZonesByWarehouse(warehouseId),
            "failed to load zones"
        );
    }

    // fetches bins for a zone
    public List<StorageBinDTO> getBinsByZone(String zoneCode) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> BinDAO.getInstance().getBinsByZoneCode(zoneCode),
            "failed to load bins"
        );
    }

    // creates a new cycle count plan (freezes bins, pulls system stock)
    public CreateCountResult createCycleCount(int warehouseId, Integer binId, String zoneCode,
                                                String countDate, String countType) throws Exception {
        CreateCountResult result = RetryHelper.executeWithRetry(
            () -> CycleCountDAO.getInstance().createCycleCount(warehouseId, binId, zoneCode, countDate, countType),
            "failed to create cycle count plan"
        );

        if (result != null) {
            Logger.log(username, "Cycle count plan created: " + result.getCountNumber());
        }
        return result;
    }

    // gets cycle count plan details
    public CycleCountPlan getCycleCountById(int countId) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> CycleCountDAO.getInstance().getCycleCountById(countId),
            "failed to load cycle count details"
        );
    }

    // gets items for a cycle count
    public List<CycleCountItem> getCycleCountItems(int countId) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> CycleCountDAO.getInstance().getCycleCountItems(countId),
            "failed to load cycle count items"
        );
    }

    // records physical counted quantities
    public void recordCounts(int countId, List<CycleCountItem> items) throws Exception {
        RetryHelper.executeWithRetry(
            () -> {
                CycleCountDAO.getInstance().recordCounts(countId, items);
                return true;
            },
            "failed to record physical counts"
        );

        Logger.log(username, "Physical counts recorded for cycle count ID: " + countId + " (" + items.size() + " items)");
    }

    // completes cycle count (posts variance adjustments, unfreezes bins)
    public void completeCycleCount(int countId) throws Exception {
        RetryHelper.executeWithRetry(
            () -> {
                CycleCountDAO.getInstance().completeCycleCount(countId);
                return true;
            },
            "failed to complete cycle count"
        );

        Logger.log(username, "Cycle count completed: ID " + countId);
    }

    // lists cycle counts with optional filters
    public List<CycleCountPlan> listCycleCounts(Integer warehouseId, String status) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> CycleCountDAO.getInstance().listCycleCounts(warehouseId, status),
            "failed to list cycle counts"
        );
    }
}
