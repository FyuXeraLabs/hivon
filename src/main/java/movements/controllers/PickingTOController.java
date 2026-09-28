package movements.controllers;

import com.google.gson.JsonObject;
import core.api.dao.PickingTODAO;
import core.api.dao.PickingTODAO.PickBinSuggestionDTO;
import core.api.dao.PickingTODAO.PickingSalesOrderDTO;
import core.api.dao.PickingTODAO.PickingTOItem;
import core.api.dao.PickingTODAO.PickingTaskDTO;
import core.logging.Logger;
import core.utils.RetryHelper;
import models.dto.StorageBinDTO;

import java.util.List;

/**
 * Controller for Picking Transfer Order (TR23) operations.
 *
 * @author Piyumi
 */
public class PickingTOController {

    private final PickingTODAO pickingTODAO;

    public PickingTOController() {
        this.pickingTODAO = PickingTODAO.getInstance();
    }

    public List<PickingSalesOrderDTO> getPendingSalesOrders() throws Exception {
        return RetryHelper.executeWithRetry(
                () -> pickingTODAO.getPendingSalesOrders(),
                "Failed to fetch pending sales orders"
        );
    }

    public List<PickingTaskDTO> getMaterialsToPickForSO(String soNumber) throws Exception {
        Logger.log("PickingTOController", "Fetching items to pick for SO: " + soNumber);
        return RetryHelper.executeWithRetry(
                () -> pickingTODAO.getMaterialsToPickForSO(soNumber),
                "Failed to fetch items to pick for SO " + soNumber
        );
    }

    public List<PickBinSuggestionDTO> suggestPickBins(int materialId, double qty, int warehouseId) throws Exception {
        Logger.log("PickingTOController", "Requesting pick bin suggestions for materialId: " + materialId + ", qty: " + qty);
        return RetryHelper.executeWithRetry(
                () -> pickingTODAO.suggestPickBins(materialId, qty, warehouseId),
                "Failed to suggest pick bins for material " + materialId
        );
    }

    public List<StorageBinDTO> getStorageBins(Integer warehouseId) throws Exception {
        return RetryHelper.executeWithRetry(
                () -> pickingTODAO.getStorageBins(warehouseId),
                "Failed to fetch storage bins"
        );
    }

    public JsonObject createPickingTransferOrder(String soNumber, int toBinId, List<PickingTOItem> items, String notes) throws Exception {
        Logger.log("PickingTOController", "Creating Picking Transfer Order for SO: " + soNumber + " with " + items.size() + " items");
        return RetryHelper.executeWithRetry(
                () -> pickingTODAO.createPickingTransferOrder(soNumber, toBinId, items, notes),
                "Failed to create Picking Transfer Order"
        );
    }
}
