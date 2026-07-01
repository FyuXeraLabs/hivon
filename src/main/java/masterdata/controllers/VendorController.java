package masterdata.controllers;

import core.api.dao.VendorDAO;
import core.security.UserSession;
import core.logging.Logger;
import models.dto.VendorDTO;

import java.util.ArrayList;
import java.util.List;

import core.utils.RetryHelper;

/**
 * Controller for Vendor master data operations.
 * Handles fetching, searching, creating, updating, and deleting vendors.
 *
 * @author Sanod
 */
public class VendorController {

    private final String username = UserSession.getInstance().getUsername();

    public VendorController() {
    }

    // retrieves all vendors from the system
    public List<VendorDTO> getAllVendors() {
        try {
            return RetryHelper.executeWithRetry(
                () -> VendorDAO.getInstance().getVendors(null),
                "failed to get all vendors"
            );
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // searches vendors by the given search term
    public List<VendorDTO> searchVendors(String searchTerm) {
        try {
            return RetryHelper.executeWithRetry(
                () -> VendorDAO.getInstance().getVendors(searchTerm),
                "search vendors failed"
            );
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // retrieves a single vendor by their ID
    public VendorDTO getVendorById(int vendorId) {
        try {
            return RetryHelper.executeWithRetry(
                () -> VendorDAO.getInstance().getVendorById(vendorId),
                "get vendor by id failed"
            );
        } catch (Exception e) {
            return null;
        }
    }

    // creates a new vendor record; returns the new vendor ID on success
    public int createVendor(VendorDTO vendorDto) {
        if (vendorDto == null || vendorDto.getVendorCode() == null || vendorDto.getVendorCode().trim().isEmpty()) {
            Logger.errlog("create vendor failed: invalid vendor data", new IllegalArgumentException("Invalid vendor data"));
            return 0;
        }
        try {
            int vendorId = RetryHelper.executeWithRetry(
                () -> VendorDAO.getInstance().createVendor(vendorDto),
                "create vendor failed"
            );
            if (vendorId > 0) {
                Logger.log(username, "vendor created successfully: " + vendorDto.getVendorCode() + " (id: " + vendorId + ")");
            }
            return vendorId;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // updates an existing vendor record
    public boolean updateVendor(VendorDTO vendorDto) {
        if (vendorDto == null || vendorDto.getVendorId() == null) {
            Logger.errlog("update vendor failed: invalid vendor data", new IllegalArgumentException("Invalid vendor data"));
            return false;
        }
        try {
            boolean success = RetryHelper.executeWithRetry(
                () -> VendorDAO.getInstance().updateVendor(vendorDto),
                "update vendor failed"
            );
            if (success) {
                Logger.log(username, "vendor updated successfully: " + vendorDto.getVendorCode() + " (id: " + vendorDto.getVendorId() + ")");
            }
            return success;
        } catch (Exception e) {
            return false;
        }
    }

    // deletes a vendor by their ID
    public boolean deleteVendor(int vendorId) {
        if (vendorId <= 0) {
            Logger.errlog("delete vendor failed: invalid vendor id", new IllegalArgumentException("Invalid vendor id"));
            return false;
        }
        try {
            boolean success = RetryHelper.executeWithRetry(
                () -> VendorDAO.getInstance().deleteVendor(vendorId),
                "failed to delete vendor"
            );
            if (success) {
                Logger.log(username, "vendor deleted successfully: id=" + vendorId);
            }
            return success;
        } catch (Exception e) {
            return false;
        }
    }
}