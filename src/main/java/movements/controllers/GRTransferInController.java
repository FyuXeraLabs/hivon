package movements.controllers;

import core.api.dao.TransferOrderDAO;
import core.logging.Logger;
import core.security.UserSession;
import models.dto.TransferOrderDTO;
import models.dto.TransferOrderItemDTO;

import java.util.List;

/**
 * Controller for Goods Receipt - Transfer In operations.
 * Handles loading transfer orders and posting goods receipts for incoming transfers.
 *
 * @author Sanod
 */
public class GRTransferInController {

    private final String username = UserSession.getInstance().getUsername();

    public GRTransferInController() {
    }

    // loads transfer order details by TO number
    public TransferOrderDTO loadTransferOrder(String toNumber) throws Exception {
        if (toNumber == null || toNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Transfer Order number cannot be empty.");
        }

        try {
            Logger.log(username, "loading transfer order: " + toNumber);
            return TransferOrderDAO.getInstance().loadTransferOrder(toNumber);
        } catch (Exception e) {
            Logger.errlog("failed to load transfer order " + toNumber + ": " + e.getMessage(), e);
            throw e;
        }
    }

    // submits received quantities for a transfer order goods receipt
    public boolean receiveGoods(String toNumber, List<TransferOrderItemDTO> items, String actualReceiptDate, String notes) throws Exception {
        if (toNumber == null || toNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Transfer Order number cannot be empty.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Items list cannot be empty.");
        }

        try {
            Logger.log(username, "submitting goods receipt for transfer order: " + toNumber);
            boolean success = TransferOrderDAO.getInstance().receiveGoods(toNumber, items, actualReceiptDate, notes);
            if (success) {
                Logger.log(username, "goods receipt from transfer order completed successfully: " + toNumber);
            }
            return success;
        } catch (Exception e) {
            Logger.errlog("failed to process goods receipt for transfer order " + toNumber + ": " + e.getMessage(), e);
            throw e;
        }
    }
}

