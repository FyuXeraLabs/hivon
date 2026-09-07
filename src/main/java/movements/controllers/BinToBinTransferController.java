package movements.controllers;

import core.api.dao.BinToBinTransferDAO;
import core.api.dao.BinToBinTransferDAO.MaterialSearchResult;
import core.api.dao.BinToBinTransferDAO.SourceBinInfo;
import core.api.dao.BinToBinTransferDAO.DestBinInfo;
import core.api.dao.BinToBinTransferDAO.BinTransferItem;
import core.api.dao.WarehouseDAO;
import core.logging.Logger;
import core.security.UserSession;
import core.utils.RetryHelper;
import models.dto.WarehouseDTO;

import java.util.List;

/**
 * Controller for Bin-to-Bin Transfer operations.
 * Handles material search, bin lookups, and posting transfers.
 *
 * @author Ishani
 */
public class BinToBinTransferController {

    private final String username = UserSession.getInstance().getUsername();

    public BinToBinTransferController() {
    }

    // fetches all active warehouses
    public List<WarehouseDTO> getWarehouses() throws Exception {
        return RetryHelper.executeWithRetry(
            () -> WarehouseDAO.getInstance().getWarehouses(),
            "failed to load warehouses"
        );
    }

    // searches materials with available stock in a warehouse
    public List<MaterialSearchResult> searchMaterialsInWarehouse(int warehouseId, String query) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> BinToBinTransferDAO.getInstance().searchMaterialsInWarehouse(warehouseId, query),
            "failed to search materials in warehouse"
        );
    }

    // fetches source bins that have stock of a specific material
    public List<SourceBinInfo> getSourceBins(int warehouseId, int materialId) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> BinToBinTransferDAO.getInstance().getSourceBinsWithStock(warehouseId, materialId),
            "failed to load source bins"
        );
    }

    // fetches destination bins available in a warehouse
    public List<DestBinInfo> getDestinationBins(int warehouseId) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> BinToBinTransferDAO.getInstance().getDestinationBins(warehouseId),
            "failed to load destination bins"
        );
    }

    // posts a bin-to-bin transfer with multiple items
    public String completeBinToBinTransfer(List<BinTransferItem> items, String reason, String remarks) throws Exception {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Transfer items list cannot be empty.");
        }

        String toNumber = RetryHelper.executeWithRetry(
            () -> BinToBinTransferDAO.getInstance().completeBinToBinTransfer(items, reason, remarks),
            "failed to complete bin-to-bin transfer"
        );

        if (toNumber != null) {
            Logger.log(username, "bin-to-bin transfer completed successfully: " + toNumber);
        }
        return toNumber;
    }
}
