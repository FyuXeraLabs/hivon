package masterdata.controllers;

import core.api.dao.VendorDAO;
import core.security.UserSession;
import core.logging.Logger;
import models.dto.VendorDTO;

import java.util.ArrayList;
import java.util.List;

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
            return VendorDAO.getInstance().getVendors(null);
        } catch (Exception e) {
            Logger.errlog("failed to get all vendors: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // searches vendors by the given search term
    public List<VendorDTO> searchVendors(String searchTerm) {
        try {
            return VendorDAO.getInstance().getVendors(searchTerm);
        } catch (Exception e) {
            Logger.errlog("search vendors failed: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // retrieves a single vendor by their ID
    public VendorDTO getVendorById(int vendorId) {
        try {
            return VendorDAO.getInstance().getVendorById(vendorId);
        } catch (Exception e) {
            Logger.errlog("get vendor by id failed: " + e.getMessage(), e);
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
            int vendorId = VendorDAO.getInstance().createVendor(vendorDto);
            if (vendorId > 0) {
                Logger.log(username, "vendor created successfully: " + vendorDto.getVendorCode() + " (id: " + vendorId + ")");
            }
            return vendorId;
        } catch (Exception e) {
            Logger.errlog("create vendor failed: " + e.getMessage(), e);
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
            boolean success = VendorDAO.getInstance().updateVendor(vendorDto);
            if (success) {
                Logger.log(username, "vendor updated successfully: " + vendorDto.getVendorCode() + " (id: " + vendorDto.getVendorId() + ")");
            }
            return success;
        } catch (Exception e) {
            Logger.errlog("update vendor failed: " + e.getMessage(), e);
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
            boolean success = VendorDAO.getInstance().deleteVendor(vendorId);
            if (success) {
                Logger.log(username, "vendor deleted successfully: id=" + vendorId);
            }
            return success;
        } catch (Exception e) {
            Logger.errlog("failed to delete vendor: id=" + vendorId + " - " + e.getMessage(), e);
            return false;
        }
    }
}