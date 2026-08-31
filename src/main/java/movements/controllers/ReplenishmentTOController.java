/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package movements.controllers;

import core.api.dao.ReplenishmentTODAO;
import core.api.dao.ReplenishmentTODAO.ReplenishmentRequirement;
import core.logging.Logger;
import core.security.UserSession;
import core.utils.RetryHelper;
import com.google.gson.JsonObject;

import java.util.List;

/**
 * Controller for Replenishment Transfer Orders.
 * Handles business logic for loading low-stock replenishment requirements and creating TOs.
 *
 * @author Sanod
 */
public class ReplenishmentTOController {

    private final String username = UserSession.getInstance().getUsername();

    public ReplenishmentTOController() {
    }

    // Loads all low-stock replenishment requirements for a given warehouse ID
    public List<ReplenishmentRequirement> getLowStockReplenishments(int warehouseId) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> ReplenishmentTODAO.getInstance().getLowStockReplenishments(warehouseId),
            "failed to load low-stock replenishment requirements"
        );
    }

    // Submits the planned replenishment items and optional notes to create a replenishment TO
    public JsonObject createReplenishmentTransferOrder(List<JsonObject> items, String notes) throws Exception {
        JsonObject result = RetryHelper.executeWithRetry(
            () -> ReplenishmentTODAO.getInstance().createReplenishmentTransferOrder(items, notes),
            "failed to create replenishment transfer order"
        );
        if (result != null && result.has("to_number")) {
            Logger.log(username, "replenishment transfer order created successfully: " + result.get("to_number").getAsString());
        }
        return result;
    }
}
