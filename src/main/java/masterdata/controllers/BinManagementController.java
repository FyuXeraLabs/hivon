package masterdata.controllers;

import core.api.dao.BinDAO;
import core.security.UserSession;
import core.logging.Logger;
import models.dto.StorageBinDTO;

import java.util.ArrayList;
import java.util.List;

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
            return BinDAO.getInstance().getBins(null);
        } catch (Exception e) {
            Logger.errlog("failed to get all bins: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // searches storage bins by the given search term
    public List<StorageBinDTO> searchBins(String searchTerm) {
        try {
            return BinDAO.getInstance().getBins(searchTerm);
        } catch (Exception e) {
            Logger.errlog("search bins failed: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // retrieves a single storage bin by its ID
    public StorageBinDTO getBinById(int binId) {
        try {
            return BinDAO.getInstance().getBinById(binId);
        } catch (Exception e) {
            Logger.errlog("get bin by id failed: " + e.getMessage(), e);
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
            int binId = BinDAO.getInstance().createBin(binDto);
            if (binId > 0) {
                Logger.log(username, "bin created successfully: " + binDto.getBinCode() + " (id: " + binId + ")");
            }
            return binId;
        } catch (Exception e) {
            Logger.errlog("create bin failed: " + e.getMessage(), e);
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
            boolean success = BinDAO.getInstance().updateBin(binDto);
            if (success) {
                Logger.log(username, "bin updated successfully: " + binDto.getBinCode() + " (id: " + binDto.getBinId() + ")");
            }
            return success;
        } catch (Exception e) {
            Logger.errlog("update bin failed: " + e.getMessage(), e);
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
            boolean success = BinDAO.getInstance().deleteBin(binId);
            if (success) {
                Logger.log(username, "bin deleted successfully: id=" + binId);
            }
            return success;
        } catch (Exception e) {
            Logger.errlog("failed to delete bin: id=" + binId + " - " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // generates a range of storage bins for a given warehouse and zone
    public List<StorageBinDTO> generateBins(int warehouseId, String zone, String prefix,
                                              int startNum, int endNum, int digits) {
        try {
            List<StorageBinDTO> bins = BinDAO.getInstance().generateBins(warehouseId, zone, prefix, startNum, endNum, digits);
            Logger.log(username, "generated " + bins.size() + " bins for warehouse " + warehouseId + " zone " + zone);
            return bins;
        } catch (Exception e) {
            Logger.errlog("generate bins failed: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}