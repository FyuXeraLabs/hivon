package movements.controllers;

import core.api.dao.GRCustomerReturnsDAO;
import core.api.dao.GRCustomerReturnsDAO.CustomerReturnItem;
import core.logging.Logger;
import core.security.UserSession;
import models.dto.SalesOrderDTO;
import models.dto.StorageBinDTO;

import java.util.List;

/**
 * Controller for Goods Receipt - Customer Returns operations.
 * Handles searching sales orders, loading details, fetching receiving bins, and posting customer returns.
 *
 * @author Sanod
 */
public class GRCustomerReturnsController {

    private final String username = UserSession.getInstance().getUsername();

    public GRCustomerReturnsController() {
    }

    // searches sales orders by the given criteria
    public List<SalesOrderDTO> searchSalesOrders(String criteria) throws Exception {
        try {
            Logger.log(username, "searching sales orders with criteria: " + criteria);
            return GRCustomerReturnsDAO.getInstance().searchSalesOrders(criteria);
        } catch (Exception e) {
            Logger.errlog("failed to search sales orders: " + e.getMessage(), e);
            throw e;
        }
    }

    // loads detailed sales order information by SO number
    public SalesOrderDTO loadSalesOrder(String soNumber) throws Exception {
        if (soNumber == null || soNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Sales Order number cannot be empty.");
        }

        try {
            Logger.log(username, "loading sales order: " + soNumber);
            return GRCustomerReturnsDAO.getInstance().getSalesOrderDetails(soNumber);
        } catch (Exception e) {
            Logger.errlog("failed to load sales order " + soNumber + ": " + e.getMessage(), e);
            throw e;
        }
    }

    // fetches active receiving bins for a warehouse
    public List<StorageBinDTO> getReceivingBins(Integer warehouseId) throws Exception {
        try {
            Logger.log(username, "fetching receiving bins for warehouse ID: " + warehouseId);
            return GRCustomerReturnsDAO.getInstance().getReceivingBins(warehouseId);
        } catch (Exception e) {
            Logger.errlog("failed to fetch receiving bins: " + e.getMessage(), e);
            throw e;
        }
    }

    // submits customer return receipt details to post a goods receipt
    public boolean completeCustomerReturn(String soNumber, String returnReason, String returnAuthNumber, String returnDate, List<CustomerReturnItem> items) throws Exception {
        if (soNumber == null || soNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Sales Order number cannot be empty.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Return items list cannot be empty.");
        }

        try {
            Logger.log(username, "submitting customer returns for sales order: " + soNumber);
            boolean success = GRCustomerReturnsDAO.getInstance().createCustomerReturn(soNumber, returnReason, returnAuthNumber, returnDate, items);
            if (success) {
                Logger.log(username, "customer return completed successfully for sales order: " + soNumber);
            }
            return success;
        } catch (Exception e) {
            Logger.errlog("failed to process customer return for sales order " + soNumber + ": " + e.getMessage(), e);
            throw e;
        }
    }
}
