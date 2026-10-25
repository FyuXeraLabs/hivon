package movements.controllers;

import core.api.dao.PutawayTODAO;
import core.api.dao.PutawayTODAO.PutawayBinSuggestionDTO;
import core.api.dao.PutawayTODAO.PutawayMaterialDTO;
import core.api.dao.PutawayTODAO.PutawayTOItem;
import core.logging.Logger;
import core.security.UserSession;
import core.utils.RetryHelper;
import models.dto.StorageBinDTO;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Controller for Putaway Transfer Orders.
 * Handles business logic for fetching receiving bins, available stock in bins,
 * storage bin auto-suggestions, and creating Putaway Transfer Orders.
 *
 * @author Piyumi
 */
public class PutawayTOController {

    private final String username = UserSession.getInstance().getUsername();

    public PutawayTOController() {
    }

    // Fetches active receiving bins for a warehouse
    public List<StorageBinDTO> getReceivingBins(Integer warehouseId) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> PutawayTODAO.getInstance().getReceivingBins(warehouseId),
            "failed to fetch receiving bins"
        );
    }

    // Fetches storage bins available for putaway destination in a warehouse
    public List<StorageBinDTO> getStorageBins(Integer warehouseId) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> PutawayTODAO.getInstance().getStorageBins(warehouseId),
            "failed to fetch storage bins"
        );
    }

    // Loads materials currently residing in a receiving bin ready for putaway
    public List<PutawayMaterialDTO> getMaterialsInBin(int binId) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> PutawayTODAO.getInstance().getMaterialsInBin(binId),
            "failed to query materials in bin ID " + binId
        );
    }

    // Recommends optimal storage bins based on material, quantity and warehouse location rules
    public List<PutawayBinSuggestionDTO> suggestBins(int materialId, double qty, int warehouseId) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> PutawayTODAO.getInstance().suggestBins(materialId, qty, warehouseId),
            "failed to get bin suggestions for material ID " + materialId
        );
    }

    // Submits putaway items and creates a Putaway Transfer Order
    public JsonObject createPutawayTransferOrder(int fromBinId, List<PutawayTOItem> items, String notes) throws Exception {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Putaway transfer order items list cannot be empty.");
        }

        JsonObject result = RetryHelper.executeWithRetry(
            () -> PutawayTODAO.getInstance().createPutawayTransferOrder(fromBinId, items, notes),
            "failed to create Putaway Transfer Order"
        );

        if (result != null && result.has("to_number")) {
            Logger.log(username, "Putaway transfer order created successfully: " + result.get("to_number").getAsString());
        }

        return result;
    }
}
