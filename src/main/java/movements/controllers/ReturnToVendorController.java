package movements.controllers;

import core.api.dao.ReturnToVendorDAO;
import core.api.dao.ReturnToVendorDAO.ReturnItem;
import core.api.dao.MaterialDAO;
import core.api.dao.VendorDAO;
import core.api.dao.InventoryDAO;
import core.logging.Logger;
import core.security.UserSession;
import core.utils.RetryHelper;
import models.dto.MaterialDTO;
import models.dto.VendorDTO;

import java.util.List;

/**
 * Controller for Return to Vendor (RTV) operations.
 * Handles searching materials, loading available inventory stock lines,
 * fetching vendors, and submitting return movements.
 *
 * @author Navodya
 */
public class ReturnToVendorController {

    private final String username = UserSession.getInstance().getUsername();

    public ReturnToVendorController() {
    }

    // Searches materials matching search term
    public List<MaterialDTO> searchMaterials(String query) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> MaterialDAO.getInstance().getMaterials(query),
            "failed to search materials"
        );
    }

    // Searches active vendors
    public List<VendorDTO> getVendors(String query) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> VendorDAO.getInstance().getVendors(query),
            "failed to load vendors"
        );
    }

    // Fetches inventory stock lines for a specific material
    public List<Object[]> getStockByMaterialId(int materialId) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> InventoryDAO.getInstance().getStockByMaterialId(materialId),
            "failed to load stock for material"
        );
    }

    // Completes and submits Return to Vendor
    public boolean completeReturnToVendor(String vendorCode, String referenceDoc, String referenceDate, String notes, List<ReturnItem> items) throws Exception {
        if (vendorCode == null || vendorCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Vendor code cannot be empty.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Return items list cannot be empty.");
        }

        boolean success = RetryHelper.executeWithRetry(
            () -> ReturnToVendorDAO.getInstance().completeReturnToVendor(vendorCode, referenceDoc, referenceDate, notes, items),
            "failed to process return to vendor"
        );

        if (success) {
            Logger.log(username, "return to vendor completed successfully for vendor: " + vendorCode);
        }
        return success;
    }
}
