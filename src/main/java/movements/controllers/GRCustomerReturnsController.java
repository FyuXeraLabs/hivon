package movements.controllers;

import core.api.dao.GRCustomerReturnsDAO;
import core.api.dao.GRCustomerReturnsDAO.CustomerReturnItem;
import core.logging.Logger;
import core.security.UserSession;
import models.dto.SalesOrderDTO;
import models.dto.StorageBinDTO;

import java.util.List;

import core.utils.RetryHelper;

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
        return RetryHelper.executeWithRetry(
            () -> GRCustomerReturnsDAO.getInstance().searchSalesOrders(criteria),
            "failed to search sales orders"
        );
    }

    // loads detailed sales order information by SO number
    public SalesOrderDTO loadSalesOrder(String soNumber) throws Exception {
        if (soNumber == null || soNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Sales Order number cannot be empty.");
        }

        return RetryHelper.executeWithRetry(
            () -> GRCustomerReturnsDAO.getInstance().getSalesOrderDetails(soNumber),
            "failed to load sales order " + soNumber
        );
    }

    // fetches active receiving bins for a warehouse
    public List<StorageBinDTO> getReceivingBins(Integer warehouseId) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> GRCustomerReturnsDAO.getInstance().getReceivingBins(warehouseId),
            "failed to fetch receiving bins"
        );
    }

    // submits customer return receipt details to post a goods receipt
    public boolean completeCustomerReturn(String soNumber, String returnReason, String returnAuthNumber, String returnDate, List<CustomerReturnItem> items) throws Exception {
        if (soNumber == null || soNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Sales Order number cannot be empty.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Return items list cannot be empty.");
        }

        boolean success = RetryHelper.executeWithRetry(
            () -> GRCustomerReturnsDAO.getInstance().createCustomerReturn(soNumber, returnReason, returnAuthNumber, returnDate, items),
            "failed to process customer return for sales order " + soNumber
        );
        if (success) {
            Logger.log(username, "customer return completed successfully for sales order: " + soNumber);
        }
        return success;
    }
}
