package movements.controllers;

import core.api.dao.CustomerDAO;
import core.api.dao.GISalesOrderDAO;
import core.api.dao.GISalesOrderDAO.BatchSuggestion;
import core.api.dao.GISalesOrderDAO.GISalesOrderItem;
import core.api.dao.InventoryDAO;
import core.utils.RetryHelper;
import models.dto.CustomerDTO;
import models.dto.SalesOrderDTO;

import java.util.List;

/**
 * Controller for Goods Issue - Sales Order (OUT14) form operations.
 * Handles communication between UI and DAO layers with retry mechanisms.
 *
 * @author Navodya / Sanod
 */
public class GISalesOrderController {

    public List<SalesOrderDTO> searchSalesOrders(String criteria, Integer customerId, String status) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> GISalesOrderDAO.getInstance().searchSalesOrders(criteria, customerId, status),
            "Failed to search sales orders"
        );
    }

    public List<SalesOrderDTO> searchSalesOrders(String status, String criteria) throws Exception {
        return searchSalesOrders(criteria, null, status);
    }

    public SalesOrderDTO loadSalesOrder(String soNumber) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> GISalesOrderDAO.getInstance().getSalesOrderDetails(soNumber),
            "Failed to load sales order details"
        );
    }

    public List<CustomerDTO> getCustomers(String searchTerm) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> CustomerDAO.getInstance().getCustomers(searchTerm),
            "Failed to fetch customers"
        );
    }

    public List<Object[]> getStockByMaterialId(int materialId) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> InventoryDAO.getInstance().getStockByMaterialId(materialId),
            "Failed to fetch inventory stock for material"
        );
    }

    public List<BatchSuggestion> suggestBatchesByFIFO(int materialId) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> GISalesOrderDAO.getInstance().suggestBatchesByFIFO(materialId),
            "Failed to suggest FIFO batches for material"
        );
    }

    public String completeGISalesOrder(String soNumber, String refDate, String notes, List<GISalesOrderItem> items) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> GISalesOrderDAO.getInstance().createGISalesOrder(soNumber, refDate, notes, items),
            "Failed to complete Goods Issue for Sales Order"
        );
    }
}
