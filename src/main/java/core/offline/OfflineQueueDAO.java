package core.offline;

import core.logging.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for managing pending offline mutation requests in the encrypted database.
 *
 * @author Sanod
 */
public class OfflineQueueDAO {

    private static volatile OfflineQueueDAO instance;
    private final OfflineDatabaseManager dbManager;

    private OfflineQueueDAO() {
        this.dbManager = OfflineDatabaseManager.getInstance();
    }

    public static OfflineQueueDAO getInstance() {
        if (instance == null) {
            synchronized (OfflineQueueDAO.class) {
                if (instance == null) {
                    instance = new OfflineQueueDAO();
                }
            }
        }
        return instance;
    }

    /**
     * Enqueues a new mutation request into the pending queue.
     */
    public synchronized boolean enqueue(OfflineRequest req) {
        String sql = "INSERT INTO " + OfflineDbConfig.TABLE_PENDING_REQUESTS
                + " (request_id, method, endpoint, payload_json, identifier, request_type, status, retry_count, last_error, created_at, updated_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            Connection conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, req.getRequestId());
                stmt.setString(2, req.getMethod());
                stmt.setString(3, req.getEndpoint());
                stmt.setString(4, req.getPayloadJson());
                stmt.setString(5, req.getIdentifier());
                stmt.setString(6, req.getRequestType());
                stmt.setString(7, req.getStatus());
                stmt.setInt(8, req.getRetryCount());
                stmt.setString(9, req.getLastError());
                stmt.setLong(10, req.getCreatedAt());
                stmt.setLong(11, req.getUpdatedAt());

                return stmt.executeUpdate() > 0;
            }
        } catch (Exception e) {
            Logger.errlog("Failed to enqueue offline request " + req.getIdentifier(), e);
            return false;
        }
    }

    /**
     * Retrieves all pending requests in sequential FIFO order.
     */
    public synchronized List<OfflineRequest> getPendingRequests() {
        List<OfflineRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM " + OfflineDbConfig.TABLE_PENDING_REQUESTS
                + " WHERE status IN ('" + OfflineDbConfig.STATUS_PENDING + "', '"
                + OfflineDbConfig.STATUS_FAILED_NETWORK + "')"
                + " ORDER BY created_at ASC";

        try {
            Connection conn = dbManager.getConnection();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                while (rs.next()) {
                    OfflineRequest req = mapRow(rs);
                    list.add(req);
                }
            }
        } catch (Exception e) {
            Logger.errlog("Failed to query pending requests from offline queue", e);
        }
        return list;
    }

    /**
     * Updates the processing status and error of a queued request.
     */
    public synchronized boolean updateStatus(String requestId, String status, String lastError) {
        String sql = "UPDATE " + OfflineDbConfig.TABLE_PENDING_REQUESTS
                + " SET status = ?, last_error = ?, updated_at = ? WHERE request_id = ?";

        try {
            Connection conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, status);
                stmt.setString(2, lastError);
                stmt.setLong(3, System.currentTimeMillis());
                stmt.setString(4, requestId);

                return stmt.executeUpdate() > 0;
            }
        } catch (Exception e) {
            Logger.errlog("Failed to update status for offline request " + requestId, e);
            return false;
        }
    }

    /**
     * Increments retry count and updates the last error message.
     */
    public synchronized void recordFailure(String requestId, String status, String error) {
        String sql = "UPDATE " + OfflineDbConfig.TABLE_PENDING_REQUESTS
                + " SET status = ?, retry_count = retry_count + 1, last_error = ?, updated_at = ?"
                + " WHERE request_id = ?";

        try {
            Connection conn = dbManager.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, status);
                stmt.setString(2, error);
                stmt.setLong(3, System.currentTimeMillis());
                stmt.setString(4, requestId);
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            Logger.errlog("Failed to record failure for offline request " + requestId, e);
        }
    }

    /**
     * Deletes all requests marked as COMPLETED.
     */
    public synchronized int deleteCompleted() {
        String sql = "DELETE FROM " + OfflineDbConfig.TABLE_PENDING_REQUESTS
                + " WHERE status = '" + OfflineDbConfig.STATUS_COMPLETED + "'";

        try {
            Connection conn = dbManager.getConnection();
            try (Statement stmt = conn.createStatement()) {
                return stmt.executeUpdate(sql);
            }
        } catch (Exception e) {
            Logger.errlog("Failed to delete completed offline requests", e);
            return 0;
        }
    }

    /**
     * Counts the total number of pending or retryable requests.
     */
    public synchronized int getPendingCount() {
        String sql = "SELECT COUNT(*) FROM " + OfflineDbConfig.TABLE_PENDING_REQUESTS
                + " WHERE status IN ('" + OfflineDbConfig.STATUS_PENDING + "', '"
                + OfflineDbConfig.STATUS_FAILED_NETWORK + "')";

        try {
            Connection conn = dbManager.getConnection();
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            Logger.errlog("Failed to count pending offline requests", e);
        }
        return 0;
    }

    private OfflineRequest mapRow(ResultSet rs) throws SQLException {
        OfflineRequest req = new OfflineRequest();
        req.setId(rs.getLong("id"));
        req.setRequestId(rs.getString("request_id"));
        req.setMethod(rs.getString("method"));
        req.setEndpoint(rs.getString("endpoint"));
        req.setPayloadJson(rs.getString("payload_json"));
        req.setIdentifier(rs.getString("identifier"));
        req.setRequestType(rs.getString("request_type"));
        req.setStatus(rs.getString("status"));
        req.setRetryCount(rs.getInt("retry_count"));
        req.setLastError(rs.getString("last_error"));
        req.setCreatedAt(rs.getLong("created_at"));
        req.setUpdatedAt(rs.getLong("updated_at"));
        return req;
    }
}
