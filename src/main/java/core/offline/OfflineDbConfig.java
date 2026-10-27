package core.offline;

import java.nio.file.Path;

/**
 * Configuration and constants for SQLCipher encrypted offline storage.
 *
 * @author Sanod
 */
public final class OfflineDbConfig {

    public static final String TABLE_PENDING_REQUESTS = "pending_requests";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SYNCING = "SYNCING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED_NETWORK = "FAILED_NETWORK";
    public static final String STATUS_FAILED_VALIDATION = "FAILED_VALIDATION";

    private OfflineDbConfig() {}

    /**
     * Resolves the user-specific and device-specific encrypted database file path.
     *
     * @param userId Logged in user ID
     * @return Path to the user's SQLite database file
     */
    public static Path getDatabasePath(int userId) {
        return Path.of("data", "offline_user_" + userId + ".db");
    }
}
