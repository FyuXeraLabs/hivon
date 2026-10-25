/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package movements.controllers;

import core.api.dao.MaterialDAO;
import core.api.dao.ScrapWriteoffDAO;
import core.logging.Logger;
import core.security.UserSession;
import core.utils.RetryHelper;
import models.dto.MaterialDTO;
import models.dto.MaterialStockDTO;
import models.dto.ScrapItemDTO;
import models.entity.ScrapReason;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Scrap Write-off operations.
 * Handles loading scrap reasons, searching material stock, posting write-offs, and managing approval workflows.
 *
 * @author Sanod D. Mendis
 */
public class ScrapWriteoffController {

    private final String username = UserSession.getInstance().getUsername();

    public ScrapWriteoffController() {
    }

    /**
     * Loads scrap reasons from database.
     */
    public List<ScrapReason> getScrapReasons() throws Exception {
        return RetryHelper.executeWithRetry(
                () -> ScrapWriteoffDAO.getInstance().getScrapReasons(),
                "failed to load scrap reasons"
        );
    }

    /**
     * Searches materials and their available stock levels and batch info.
     */
    public List<MaterialStockDTO> searchMaterialStock(String query, Integer warehouseId) throws Exception {
        return RetryHelper.executeWithRetry(
                () -> ScrapWriteoffDAO.getInstance().searchMaterialStock(query, warehouseId),
                "failed to search material stock"
        );
    }

    /**
     * Saves scrap write-off as draft or submits for supervisor/manager approval.
     */
    public String saveScrapWriteoff(List<ScrapItemDTO> items, String reasonCode, String remarks, boolean submit) throws Exception {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Scrap write-off items list cannot be empty.");
        }

        String movementNumber = RetryHelper.executeWithRetry(
                () -> ScrapWriteoffDAO.getInstance().saveScrapWriteoff(items, reasonCode, remarks, submit),
                "failed to process scrap write-off"
        );

        if (movementNumber != null) {
            String action = submit ? "submitted for approval" : "saved as draft";
            Logger.log(username, "scrap write-off " + action + ": " + movementNumber + " (" + items.size() + " items)");
        }
        return movementNumber;
    }

    /**
     * Fetches previous scrap write-off movements.
     */
    public List<Object[]> getWriteoffHistory() throws Exception {
        return RetryHelper.executeWithRetry(
                () -> ScrapWriteoffDAO.getInstance().getWriteoffHistory(),
                "failed to load scrap write-off history"
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
}
