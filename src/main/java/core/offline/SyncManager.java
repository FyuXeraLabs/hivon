package core.offline;

import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.logging.Logger;
import ui.dialogs.SystemTrayPanel;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Centralized sequential synchronization manager.
 * Guarantees FIFO execution with no race conditions using a single-threaded executor and lock.
 * Updates SystemTrayPanel and callers with sync progress and details.
 *
 * @author Sanod
 */
public class SyncManager {

    private static volatile SyncManager instance;
    private final OfflineQueueDAO queueDAO;
    private final ExecutorService syncExecutor;
    private final ReentrantLock syncLock;
    private final java.util.concurrent.atomic.AtomicBoolean isSyncing;

    @FunctionalInterface
    public interface SyncProgressListener {
        void onProgress(int current, int total, String identifier);
        default void onItemComplete(int current, int total, OfflineRequest req, boolean success, String error) {}
    }

    private SyncManager() {
        this.queueDAO = OfflineQueueDAO.getInstance();
        this.isSyncing = new java.util.concurrent.atomic.AtomicBoolean(false);
        this.syncExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Hivon-SyncWorker");
            t.setDaemon(true);
            return t;
        });
        this.syncLock = new ReentrantLock();
    }

    public static SyncManager getInstance() {
        if (instance == null) {
            synchronized (SyncManager.class) {
                if (instance == null) {
                    instance = new SyncManager();
                }
            }
        }
        return instance;
    }

    public boolean isSyncing() {
        return isSyncing.get();
    }

    /**
     * Fast check to see if API endpoint is reachable.
     */
    public boolean isOnline() {
        try {
            JsonObject health = ApiClient.getInstance().checkHealth();
            return health != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Executes sequential synchronization of all pending offline requests synchronously on the caller's thread
     * (or inside a BackgroundTask).
     *
     * @param listener Optional listener to receive granular progress updates
     * @return Number of successfully synced requests
     * @throws Exception If network connectivity is lost during synchronization
     */
    public int syncAll(SyncProgressListener listener) throws Exception {
        if (!isSyncing.compareAndSet(false, true)) {
            Logger.log("SyncManager", "Sync already in progress. Skipping concurrent run.");
            return 0;
        }

        syncLock.lock();
        try {
            List<OfflineRequest> pending = queueDAO.getPendingRequests();
            int total = pending.size();

            if (total == 0) {
                SystemTrayPanel.hideSyncProgress();
                return 0;
            }

            SystemTrayPanel.showSyncProgress(total);
            Logger.log("SyncManager", "Starting sequential sync of " + total + " offline requests.");

            int successCount = 0;

            for (int i = 0; i < total; i++) {
                OfflineRequest req = pending.get(i);
                int currentIndex = i + 1;

                String displayId = (req.getIdentifier() != null && !req.getIdentifier().isEmpty())
                        ? req.getIdentifier()
                        : "Request #" + currentIndex;

                if (listener != null) {
                    listener.onProgress(currentIndex, total, displayId);
                }
                SystemTrayPanel.updateSyncProgress(currentIndex, total);
                SystemTrayPanel.updateDetail("Syncing " + displayId + " (" + currentIndex + "/" + total + ")");

                queueDAO.updateStatus(req.getRequestId(), OfflineDbConfig.STATUS_SYNCING, null);

                try {
                    boolean success = sendRequest(req);
                    if (success) {
                        queueDAO.updateStatus(req.getRequestId(), OfflineDbConfig.STATUS_COMPLETED, null);
                        successCount++;
                        if (listener != null) {
                            listener.onItemComplete(currentIndex, total, req, true, null);
                        }
                    } else {
                        queueDAO.recordFailure(req.getRequestId(), OfflineDbConfig.STATUS_FAILED_VALIDATION, "Server validation error");
                        if (listener != null) {
                            listener.onItemComplete(currentIndex, total, req, false, "Server validation error");
                        }
                    }

                } catch (IOException netErr) {
                    // Critical: Network dropped during sequential sync
                    queueDAO.recordFailure(req.getRequestId(), OfflineDbConfig.STATUS_FAILED_NETWORK, netErr.getMessage());
                    SystemTrayPanel.updateDetail("Sync halted: network connection lost");
                    if (listener != null) {
                        listener.onItemComplete(currentIndex, total, req, false, netErr.getMessage());
                    }
                    throw new IOException("Connection interrupted while syncing " + displayId + ": " + netErr.getMessage(), netErr);

                } catch (Exception apiErr) {
                    // Business rule error / validation failure
                    Logger.errlog("Sync rejected for " + displayId + ": " + apiErr.getMessage(), apiErr);
                    queueDAO.recordFailure(req.getRequestId(), OfflineDbConfig.STATUS_FAILED_VALIDATION, apiErr.getMessage());
                    if (listener != null) {
                        listener.onItemComplete(currentIndex, total, req, false, apiErr.getMessage());
                    }
                } finally {
                    pending.set(i, null); // Help GC free payload memory immediately
                }
            }

            queueDAO.deleteCompleted();
            OfflineDatabaseManager.getInstance().checkpointAndClean();
            SystemTrayPanel.updateDetail("Sync completed (" + successCount + "/" + total + " successful)");
            return successCount;

        } finally {
            isSyncing.set(false);
            syncLock.unlock();
        }
    }

    /**
     * Executes sync asynchronously in the background.
     */
    public void syncAllAsync(SyncProgressListener listener) {
        syncExecutor.submit(() -> {
            try {
                syncAll(listener);
            } catch (Exception e) {
                Logger.errlog("Background sync error: " + e.getMessage(), e);
            }
        });
    }

    private boolean sendRequest(OfflineRequest req) throws Exception {
        ApiClient api = ApiClient.getInstance();

        HttpRequest.Builder builder = api.authRequest(req.getEndpoint())
                .header("X-Sync-Replay", "true")
                .header("X-Idempotency-Key", req.getRequestId());

        String payload = req.getPayloadJson() != null ? req.getPayloadJson() : "{}";

        if ("POST".equalsIgnoreCase(req.getMethod())) {
            builder.POST(HttpRequest.BodyPublishers.ofString(payload));
        } else if ("PUT".equalsIgnoreCase(req.getMethod())) {
            builder.PUT(HttpRequest.BodyPublishers.ofString(payload));
        } else if ("DELETE".equalsIgnoreCase(req.getMethod())) {
            builder.DELETE();
        } else {
            builder.method(req.getMethod(), HttpRequest.BodyPublishers.ofString(payload));
        }

        JsonObject response = api.executeWithAuth(builder.build());
        return response != null
                && (!response.has("offline") || !response.get("offline").getAsBoolean())
                && "success".equalsIgnoreCase(
                response.has("status") ? response.get("status").getAsString() : ""
        );
    }
}
