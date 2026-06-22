package masterdata.controllers;

import com.google.gson.JsonObject;
import core.api.dao.BatchDAO;
import core.security.UserSession;
import core.logging.Logger;
import models.dto.BatchDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller for Batch master data operations.
 * Handles fetching, searching, creating, updating, deleting batches and retrieving batch stock.
 *
 * @author Sanod
 */
public class BatchController {

    private final String username = UserSession.getInstance().getUsername();

    public BatchController() {
    }

    // retrieves all batches from the system
    public List<BatchDTO> getAllBatches() {
        try {
            return BatchDAO.getInstance().getBatches(null, null);
        } catch (Exception e) {
            Logger.errlog("failed to get all batches: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // searches batches by the given search term
    public List<BatchDTO> searchBatches(String searchTerm) {
        try {
            return BatchDAO.getInstance().getBatches(null, searchTerm);
        } catch (Exception e) {
            Logger.errlog("search batches failed: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // retrieves all batches associated with a specific material
    public List<BatchDTO> getBatchesByMaterial(int materialId) {
        try {
            return BatchDAO.getInstance().getBatchesByMaterial(materialId);
        } catch (Exception e) {
            Logger.errlog("get batches by material failed: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // retrieves a single batch by its ID
    public BatchDTO getBatchById(int batchId) {
        try {
            return BatchDAO.getInstance().getBatchById(batchId);
        } catch (Exception e) {
            Logger.errlog("get batch by id failed: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // creates a new batch record; returns the new batch ID on success
    public int createBatch(BatchDTO batchDto) {
        if (batchDto == null || batchDto.getBatchNumber() == null || batchDto.getBatchNumber().trim().isEmpty()) {
            Logger.errlog("create batch failed: invalid batch data", new IllegalArgumentException("Invalid batch data"));
            return 0;
        }
        try {
            int batchId = BatchDAO.getInstance().createBatch(batchDto);
            if (batchId > 0) {
                Logger.log(username, "batch created successfully: " + batchDto.getBatchNumber() + " (id: " + batchId + ")");
            }
            return batchId;
        } catch (Exception e) {
            Logger.errlog("create batch failed: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // updates an existing batch record
    public boolean updateBatch(BatchDTO batchDto) {
        if (batchDto == null || batchDto.getBatchId() == null) {
            Logger.errlog("update batch failed: invalid batch data", new IllegalArgumentException("Invalid batch data"));
            throw new IllegalArgumentException("Invalid batch data");
        }
        try {
            boolean success = BatchDAO.getInstance().updateBatch(batchDto);
            if (success) {
                Logger.log(username, "batch updated successfully: " + batchDto.getBatchNumber() + " (id: " + batchDto.getBatchId() + ")");
            }
            return success;
        } catch (Exception e) {
            Logger.errlog("update batch failed: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // deletes a batch by its ID
    public boolean deleteBatch(int batchId) {
        if (batchId <= 0) {
            Logger.errlog("delete batch failed: invalid batch id", new IllegalArgumentException("Invalid batch id"));
            throw new IllegalArgumentException("Invalid batch id");
        }
        try {
            boolean success = BatchDAO.getInstance().deleteBatch(batchId);
            if (success) {
                Logger.log(username, "batch deleted successfully: id=" + batchId);
            }
            return success;
        } catch (Exception e) {
            Logger.errlog("failed to delete batch: id=" + batchId + " - " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // retrieves stock information for a specific batch
    public List<JsonObject> getBatchStock(int batchId) {
        try {
            return BatchDAO.getInstance().getBatchStock(batchId);
        } catch (Exception e) {
            Logger.errlog("get batch stock failed: " + e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}