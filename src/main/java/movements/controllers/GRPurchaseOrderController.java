package movements.controllers;

import core.api.dao.GRPurchaseOrderDAO;
import core.api.dao.GRPurchaseOrderDAO.POReceiptItem;
import core.logging.Logger;
import core.security.UserSession;
import models.dto.PurchaseOrderDTO;
import models.dto.StorageBinDTO;

import java.util.List;

/**
 * Controller for Goods Receipt - Purchase Order operations.
 * Handles searching POs, loading details, fetching receiving bins, and posting goods receipts.
 *
 * @author Sanod
 */
public class GRPurchaseOrderController {

    private final String username = UserSession.getInstance().getUsername();

    public GRPurchaseOrderController() {
    }

    // searches purchase orders by the given status
    public List<PurchaseOrderDTO> searchPurchaseOrders(String status) throws Exception {
        try {
            Logger.log(username, "searching purchase orders with status: " + status);
            return GRPurchaseOrderDAO.getInstance().searchPurchaseOrders(status);
        } catch (Exception e) {
            Logger.errlog("failed to search purchase orders: " + e.getMessage(), e);
            throw e;
        }
    }

    // loads detailed purchase order information by PO number
    public PurchaseOrderDTO loadPurchaseOrder(String poNumber) throws Exception {
        if (poNumber == null || poNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Purchase Order number cannot be empty.");
        }

        try {
            Logger.log(username, "loading purchase order: " + poNumber);
            return GRPurchaseOrderDAO.getInstance().getPurchaseOrderDetails(poNumber);
        } catch (Exception e) {
            Logger.errlog("failed to load purchase order " + poNumber + ": " + e.getMessage(), e);
            throw e;
        }
    }

    // fetches active receiving bins for a warehouse
    public List<StorageBinDTO> getReceivingBins(Integer warehouseId) throws Exception {
        try {
            Logger.log(username, "fetching receiving bins for warehouse ID: " + warehouseId);
            return GRPurchaseOrderDAO.getInstance().getReceivingBins(warehouseId);
        } catch (Exception e) {
            Logger.errlog("failed to fetch receiving bins: " + e.getMessage(), e);
            throw e;
        }
    }

    // submits received quantities and items for a purchase order goods receipt
    public boolean receiveGoods(String poNumber, String referenceDate, String notes, List<POReceiptItem> items) throws Exception {
        if (poNumber == null || poNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Purchase Order number cannot be empty.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Receipt items list cannot be empty.");
        }

        try {
            Logger.log(username, "submitting goods receipt for purchase order: " + poNumber);
            boolean success = GRPurchaseOrderDAO.getInstance().createGRPurchaseOrder(poNumber, referenceDate, notes, items);
            if (success) {
                Logger.log(username, "goods receipt from purchase order completed successfully: " + poNumber);
            }
            return success;
        } catch (Exception e) {
            Logger.errlog("failed to process goods receipt for purchase order " + poNumber + ": " + e.getMessage(), e);
            throw e;
        }
    }
}
