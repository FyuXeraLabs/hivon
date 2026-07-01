package masterdata.controllers;

import core.api.dao.BinDAO;
import core.security.UserSession;
import core.logging.Logger;
import models.dto.StorageBinDTO;

import java.util.ArrayList;
import java.util.List;

import core.utils.RetryHelper;

/**
 * Controller for Storage Bin management operations.
 * Handles fetching, searching, creating, updating, deleting, and generating storage bins.
 *
 * @author Sanod
 */
public class BinManagementController {

    private final String username = UserSession.getInstance().getUsername();

    public BinManagementController() {
    }

    // retrieves all storage bins from the system
    public List<StorageBinDTO> getAllBins() {
        try {
            return RetryHelper.executeWithRetry(
                () -> BinDAO.getInstance().getBins(null),
                "failed to get all bins"
            );
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // searches storage bins by the given search term
    public List<StorageBinDTO> searchBins(String searchTerm) {
        try {
            return RetryHelper.executeWithRetry(
                () -> BinDAO.getInstance().getBins(searchTerm),
                "search bins failed"
            );
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // retrieves a single storage bin by its ID
    public StorageBinDTO getBinById(int binId) {
        try {
            return RetryHelper.executeWithRetry(
                () -> BinDAO.getInstance().getBinById(binId),
                "get bin by id failed"
            );
        } catch (Exception e) {
            return null;
        }
    }

    // creates a new storage bin; returns the new bin ID on success
    public int createBin(StorageBinDTO binDto) {
        if (binDto == null || binDto.getBinCode() == null || binDto.getBinCode().trim().isEmpty()) {
            Logger.errlog("create bin failed: invalid bin data", new IllegalArgumentException("Invalid bin data"));
            return 0;
        }
        try {
            int binId = RetryHelper.executeWithRetry(
                () -> BinDAO.getInstance().createBin(binDto),
                "create bin failed"
            );
            if (binId > 0) {
                Logger.log(username, "bin created successfully: " + binDto.getBinCode() + " (id: " + binId + ")");
            }
            return binId;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // updates an existing storage bin record
    public boolean updateBin(StorageBinDTO binDto) {
        if (binDto == null || binDto.getBinId() == null) {
            Logger.errlog("update bin failed: invalid bin data", new IllegalArgumentException("Invalid bin data"));
            throw new IllegalArgumentException("Invalid bin data");
        }
        try {
            boolean success = RetryHelper.executeWithRetry(
                () -> BinDAO.getInstance().updateBin(binDto),
                "update bin failed"
            );
            if (success) {
                Logger.log(username, "bin updated successfully: " + binDto.getBinCode() + " (id: " + binDto.getBinId() + ")");
            }
            return success;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // deletes a storage bin by its ID
    public boolean deleteBin(int binId) {
        if (binId <= 0) {
            Logger.errlog("delete bin failed: invalid bin id", new IllegalArgumentException("Invalid bin id"));
            throw new IllegalArgumentException("Invalid bin id");
        }
        try {
            boolean success = RetryHelper.executeWithRetry(
                () -> BinDAO.getInstance().deleteBin(binId),
                "failed to delete bin"
            );
            if (success) {
                Logger.log(username, "bin deleted successfully: id=" + binId);
            }
            return success;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // generates a range of storage bins for a given warehouse and zone
    public List<StorageBinDTO> generateBins(int warehouseId, String zone, String prefix,
                                              int startNum, int endNum, int digits) {
        try {
            List<StorageBinDTO> bins = RetryHelper.executeWithRetry(
                () -> BinDAO.getInstance().generateBins(warehouseId, zone, prefix, startNum, endNum, digits),
                "generate bins failed"
            );
            Logger.log(username, "generated " + bins.size() + " bins for warehouse " + warehouseId + " zone " + zone);
            return bins;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}