/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package movements.controllers;

import core.api.dao.GIInternalConsumptionDAO;
import core.api.dao.GIInternalConsumptionDAO.BinWithStock;
import core.api.dao.GIInternalConsumptionDAO.GIConsumptionIssueItem;
import core.api.dao.GIInternalConsumptionDAO.MaterialWithStock;
import core.logging.Logger;
import core.security.UserSession;
import core.utils.RetryHelper;

import java.util.List;

/**
 * Controller for Goods Issue - Internal Consumption (OUT16) operations.
 * Handles searching material stock, fetching bins with stock, posting goods issues,
 * and performing business validations with retry support.
 *
 * @author Piyumi / Sanod
 */
public class GIInternalConsumptionController {

    private final String username = UserSession.getInstance().getUsername();

    public GIInternalConsumptionController() {
    }

    /**
     * Searches materials and their available stock levels across warehouse bins.
     */
    public List<MaterialWithStock> searchMaterial(String query) throws Exception {
        Integer warehouseId = null;
        try {
            if (UserSession.getInstance().getCurrentUser() != null) {
                warehouseId = UserSession.getInstance().getCurrentUser().getWarehouseId();
            }
        } catch (Exception ignored) {}

        final Integer whId = warehouseId;
        return RetryHelper.executeWithRetry(
            () -> GIInternalConsumptionDAO.getInstance().searchMaterials(query, whId),
            "Failed to search materials for internal consumption"
        );
    }

    /**
     * Alias for searchMaterial.
     */
    public List<MaterialWithStock> searchMaterials(String query) throws Exception {
        return searchMaterial(query);
    }

    /**
     * Fetches bins with available stock for the given material and warehouse.
     */
    public List<BinWithStock> getBinsWithStock(int materialId, Integer warehouseId) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> GIInternalConsumptionDAO.getInstance().getBinsWithStock(materialId, warehouseId),
            "Failed to load bins with available stock"
        );
    }

    /**
     * Validates issue quantity against available stock.
     */
    public boolean validateIssueQuantity(double availableQty, double issueQty) {
        return issueQty > 0 && issueQty <= availableQty;
    }

    /**
     * Posts Goods Issue for Internal Consumption.
     */
    public String completeConsumption(String consumptionType, String department, String projectRef,
                                      List<GIConsumptionIssueItem> items) throws Exception {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Issue item list cannot be empty.");
        }
        if (consumptionType == null || consumptionType.trim().isEmpty() || consumptionType.startsWith("--")) {
            throw new IllegalArgumentException("Please select a valid Consumption Type.");
        }
        if (department == null || department.trim().isEmpty()) {
            throw new IllegalArgumentException("Department is required.");
        }

        String movementNumber = RetryHelper.executeWithRetry(
            () -> GIInternalConsumptionDAO.getInstance().completeConsumption(consumptionType, department, projectRef, items),
            "Failed to post internal consumption Goods Issue"
        );

        if (movementNumber != null) {
            Logger.log(username, "Internal consumption posted: " + movementNumber + " (" + items.size() + " items)");
        }
        return movementNumber;
    }

    /**
     * Completes consumption with standard parameters.
     */
    public String completeGIInternalConsumption(List<GIConsumptionIssueItem> items, String consumptionType, String department) throws Exception {
        return completeConsumption(consumptionType, department, "", items);
    }
}
