package movements.controllers;

import core.api.dao.GRPurchaseOrderDAO;
import core.api.dao.GRPurchaseOrderDAO.POReceiptItem;
import core.logging.Logger;
import core.security.UserSession;
import models.dto.PurchaseOrderDTO;
import models.dto.StorageBinDTO;

import java.util.List;

import core.utils.RetryHelper;

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
        return RetryHelper.executeWithRetry(
            () -> GRPurchaseOrderDAO.getInstance().searchPurchaseOrders(status),
            "failed to search purchase orders"
        );
    }

    // loads detailed purchase order information by PO number
    public PurchaseOrderDTO loadPurchaseOrder(String poNumber) throws Exception {
        if (poNumber == null || poNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Purchase Order number cannot be empty.");
        }

        return RetryHelper.executeWithRetry(
            () -> GRPurchaseOrderDAO.getInstance().getPurchaseOrderDetails(poNumber),
            "failed to load purchase order " + poNumber
        );
    }

    // fetches active receiving bins for a warehouse
    public List<StorageBinDTO> getReceivingBins(Integer warehouseId) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> GRPurchaseOrderDAO.getInstance().getReceivingBins(warehouseId),
            "failed to fetch receiving bins"
        );
    }

    // submits received quantities and items for a purchase order goods receipt
    public boolean receiveGoods(String poNumber, String referenceDate, String notes, List<POReceiptItem> items) throws Exception {
        if (poNumber == null || poNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Purchase Order number cannot be empty.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Receipt items list cannot be empty.");
        }

        boolean success = RetryHelper.executeWithRetry(
            () -> GRPurchaseOrderDAO.getInstance().createGRPurchaseOrder(poNumber, referenceDate, notes, items),
            "failed to process goods receipt for purchase order " + poNumber
        );
        if (success) {
            Logger.log(username, "goods receipt from purchase order completed successfully: " + poNumber);
        }
        return success;
    }
}
