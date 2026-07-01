package masterdata.controllers;

import com.google.gson.JsonObject;
import core.api.dao.BatchDAO;
import core.security.UserSession;
import core.logging.Logger;
import models.dto.BatchDTO;

import java.util.ArrayList;
import java.util.List;

import core.utils.RetryHelper;

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
            return RetryHelper.executeWithRetry(
                () -> BatchDAO.getInstance().getBatches(null, null),
                "failed to get all batches"
            );
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // searches batches by the given search term
    public List<BatchDTO> searchBatches(String searchTerm) {
        try {
            return RetryHelper.executeWithRetry(
                () -> BatchDAO.getInstance().getBatches(null, searchTerm),
                "search batches failed"
            );
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // retrieves all batches associated with a specific material
    public List<BatchDTO> getBatchesByMaterial(int materialId) {
        try {
            return RetryHelper.executeWithRetry(
                () -> BatchDAO.getInstance().getBatchesByMaterial(materialId),
                "get batches by material failed"
            );
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // retrieves a single batch by its ID
    public BatchDTO getBatchById(int batchId) {
        try {
            return RetryHelper.executeWithRetry(
                () -> BatchDAO.getInstance().getBatchById(batchId),
                "get batch by id failed"
            );
        } catch (Exception e) {
            return null;
        }
    }

    // creates a new batch record; returns the new batch ID on success
    public int createBatch(BatchDTO batchDto) {
        if (batchDto == null || batchDto.getBatchNumber() == null || batchDto.getBatchNumber().trim().isEmpty()) {
            Logger.errlog("create batch failed: invalid batch data", new IllegalArgumentException("Invalid batch data"));
            return 0;
        }
        try {
            int batchId = RetryHelper.executeWithRetry(
                () -> BatchDAO.getInstance().createBatch(batchDto),
                "create batch failed"
            );
            if (batchId > 0) {
                Logger.log(username, "batch created successfully: " + batchDto.getBatchNumber() + " (id: " + batchId + ")");
            }
            return batchId;
        } catch (Exception e) {
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
            boolean success = RetryHelper.executeWithRetry(
                () -> BatchDAO.getInstance().updateBatch(batchDto),
                "update batch failed"
            );
            if (success) {
                Logger.log(username, "batch updated successfully: " + batchDto.getBatchNumber() + " (id: " + batchDto.getBatchId() + ")");
            }
            return success;
        } catch (Exception e) {
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
            boolean success = RetryHelper.executeWithRetry(
                () -> BatchDAO.getInstance().deleteBatch(batchId),
                "failed to delete batch"
            );
            if (success) {
                Logger.log(username, "batch deleted successfully: id=" + batchId);
            }
            return success;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // retrieves stock information for a specific batch
    public List<JsonObject> getBatchStock(int batchId) {
        try {
            return RetryHelper.executeWithRetry(
                () -> BatchDAO.getInstance().getBatchStock(batchId),
                "get batch stock failed"
            );
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}