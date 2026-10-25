/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package movements.controllers;

import core.api.dao.BinDAO;
import core.api.dao.InventoryAdjustmentDAO;
import core.api.dao.MaterialDAO;
import core.logging.Logger;
import core.security.UserSession;
import core.utils.RetryHelper;
import models.dto.AdjustmentItemDTO;
import models.dto.MaterialDTO;
import models.dto.MaterialStockDTO;
import models.dto.StorageBinDTO;
import models.entity.AdjustmentReason;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Inventory Adjustment operations.
 * Handles loading variance reasons, searching materials with current stock, and posting inventory adjustments.
 *
 * @author Sanod D. Mendis
 */
public class InventoryAdjustmentController {

    private final String username = UserSession.getInstance().getUsername();

    public InventoryAdjustmentController() {
    }

    /**
     * Loads active adjustment reasons from database.
     */
    public List<AdjustmentReason> getAdjustmentReasons() throws Exception {
        return RetryHelper.executeWithRetry(
                () -> InventoryAdjustmentDAO.getInstance().getAdjustmentReasons(),
                "failed to load adjustment reasons"
        );
    }

    /**
     * Searches materials and their current stock levels.
     */
    public List<MaterialStockDTO> searchMaterialStock(String query, Integer warehouseId) throws Exception {
        return RetryHelper.executeWithRetry(
                () -> InventoryAdjustmentDAO.getInstance().searchMaterialStock(query, warehouseId),
                "failed to search material stock"
        );
    }

    /**
     * Submits inventory adjustments to REST API and updates physical stock.
     */
    public String saveAdjustment(List<AdjustmentItemDTO> items, String referenceDate, String authorizedBy, String remarks) throws Exception {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Adjustment items list cannot be empty.");
        }

        String movementNumber = RetryHelper.executeWithRetry(
                () -> InventoryAdjustmentDAO.getInstance().saveAdjustment(items, referenceDate, authorizedBy, remarks),
                "failed to process inventory adjustment"
        );

        if (movementNumber != null) {
            Logger.log(username, "inventory adjustment posted successfully: " + movementNumber + " (" + items.size() + " items)");
        }
        return movementNumber;
    }

    /**
     * Fetches adjustment movement history.
     */
    public List<Object[]> getAdjustmentHistory() throws Exception {
        return RetryHelper.executeWithRetry(
                () -> InventoryAdjustmentDAO.getInstance().getAdjustmentHistory(),
                "failed to load adjustment history"
        );
    }

    /**
     * Searches material codes and descriptions for AutoSuggest text field.
     */
    public List<String> searchMaterialCodes(String query) {
        List<String> suggestions = new ArrayList<>();
        try {
            List<MaterialDTO> materials = MaterialDAO.getInstance().getMaterials(query);
            if (materials != null) {
                for (MaterialDTO m : materials) {
                    if (m.getMaterialCode() != null) {
                        suggestions.add(m.getMaterialCode());
                    }
                }
            }
        } catch (Exception e) {
            Logger.errlog("Failed to load material suggestions", e);
        }
        return suggestions;
    }

    /**
     * Searches storage bin codes for AutoSuggest text field.
     */
    public List<String> searchBinCodes(String query) {
        List<String> suggestions = new ArrayList<>();
        try {
            List<StorageBinDTO> bins = BinDAO.getInstance().getBins(query);
            if (bins != null) {
                for (StorageBinDTO b : bins) {
                    if (b.getBinCode() != null) {
                        suggestions.add(b.getBinCode());
                    }
                }
            }
        } catch (Exception e) {
            Logger.errlog("Failed to load bin suggestions", e);
        }
        return suggestions;
    }
}
