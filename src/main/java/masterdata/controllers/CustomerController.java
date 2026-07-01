package masterdata.controllers;

import core.api.dao.CustomerDAO;
import core.security.UserSession;
import core.logging.Logger;
import models.dto.CustomerDTO;

import java.util.ArrayList;
import java.util.List;

import core.utils.RetryHelper;

/**
 * Controller for Customer master data operations.
 * Handles fetching, searching, creating, updating, and deleting customers.
 *
 * @author Sanod
 */
public class CustomerController {

    private final String username = UserSession.getInstance().getUsername();

    public CustomerController() {
    }

    // retrieves all customers from the system
    public List<CustomerDTO> getAllCustomers() {
        try {
            return RetryHelper.executeWithRetry(
                () -> CustomerDAO.getInstance().getCustomers(null),
                "failed to get all customers"
            );
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // searches customers by the given search term
    public List<CustomerDTO> searchCustomers(String searchTerm) {
        try {
            return RetryHelper.executeWithRetry(
                () -> CustomerDAO.getInstance().getCustomers(searchTerm),
                "search customers failed"
            );
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // retrieves a single customer by their ID
    public CustomerDTO getCustomerById(int customerId) {
        try {
            return RetryHelper.executeWithRetry(
                () -> CustomerDAO.getInstance().getCustomerById(customerId),
                "get customer by id failed"
            );
        } catch (Exception e) {
            return null;
        }
    }

    // creates a new customer record; returns the new customer ID on success
    public int createCustomer(CustomerDTO customerDto) {
        if (customerDto == null || customerDto.getCustomerCode() == null || customerDto.getCustomerCode().trim().isEmpty()) {
            Logger.errlog("create customer failed: invalid customer data", new IllegalArgumentException("Invalid customer data"));
            return 0;
        }
        try {
            int customerId = RetryHelper.executeWithRetry(
                () -> CustomerDAO.getInstance().createCustomer(customerDto),
                "create customer failed"
            );
            if (customerId > 0) {
                Logger.log(username, "customer created successfully: " + customerDto.getCustomerCode() + " (id: " + customerId + ")");
            }
            return customerId;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // updates an existing customer record
    public boolean updateCustomer(CustomerDTO customerDto) {
        if (customerDto == null || customerDto.getCustomerId() == null) {
            Logger.errlog("update customer failed: invalid customer data", new IllegalArgumentException("Invalid customer data"));
            return false;
        }
        try {
            boolean success = RetryHelper.executeWithRetry(
                () -> CustomerDAO.getInstance().updateCustomer(customerDto),
                "update customer failed"
            );
            if (success) {
                Logger.log(username, "customer updated successfully: " + customerDto.getCustomerCode() + " (id: " + customerDto.getCustomerId() + ")");
            }
            return success;
        } catch (Exception e) {
            return false;
        }
    }

    // deletes a customer by their ID
    public boolean deleteCustomer(int customerId) {
        if (customerId <= 0) {
            Logger.errlog("delete customer failed: invalid customer id", new IllegalArgumentException("Invalid customer id"));
            return false;
        }
        try {
            boolean success = RetryHelper.executeWithRetry(
                () -> CustomerDAO.getInstance().deleteCustomer(customerId),
                "failed to delete customer"
            );
            if (success) {
                Logger.log(username, "customer deleted successfully: id=" + customerId);
            }
            return success;
        } catch (Exception e) {
            return false;
        }
    }
}