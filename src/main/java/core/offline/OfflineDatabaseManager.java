package core.offline;

import core.logging.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLCipher database connections, encryption keys, pragmas, and schema initialization.
 * Follows the proven sqlcipher-java-maven connection pattern.
 *
 * @author Sanod
 */
public final class OfflineDatabaseManager {

    private static volatile OfflineDatabaseManager instance;
    private Connection activeConnection;
    private Path activeDbPath;

    private OfflineDatabaseManager() {}

    public static OfflineDatabaseManager getInstance() {
        if (instance == null) {
            synchronized (OfflineDatabaseManager.class) {
                if (instance == null) {
                    instance = new OfflineDatabaseManager();
                }
            }
        }
        return instance;
    }

    /**
     * Opens and authenticates with a SQLCipher encrypted database.
     *
     * @param dbPath Path to the SQLite database file
     * @param password Encryption key (hex encoded or escaped)
     * @return Open, authenticated JDBC Connection
     * @throws Exception if opening or authenticating fails
     */
    public synchronized Connection openDatabase(Path dbPath, String password) throws Exception {
        if (activeConnection != null && !activeConnection.isClosed() && dbPath.equals(activeDbPath)) {
            return activeConnection;
        }

        closeDatabase();

        if (dbPath.getParent() != null) {
            Files.createDirectories(dbPath.getParent());
        }

        String normalizedPath = dbPath.toAbsolutePath().toString().replace('\\', '/');
        String url = "jdbc:sqlcipher:" + normalizedPath;

        String safePassword = password.replace("'", "''");

        Connection conn = DriverManager.getConnection(url);

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA key = '" + safePassword + "'");

            // Verify key by accessing sqlite_master
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM sqlite_master")) {
                rs.next();
            }

            // Set concurrency and memory optimization pragmas
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA busy_timeout = 5000");
            stmt.execute("PRAGMA cache_size = -2000"); // Cap in-memory page cache to 2MB
            stmt.execute("PRAGMA temp_store = MEMORY");

            // Initialize schema
            stmt.execute(
                    "CREATE TABLE IF NOT EXISTS " + OfflineDbConfig.TABLE_PENDING_REQUESTS + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "request_id TEXT UNIQUE NOT NULL, "
                    + "method TEXT NOT NULL, "
                    + "endpoint TEXT NOT NULL, "
                    + "payload_json TEXT, "
                    + "identifier TEXT, "
                    + "request_type TEXT, "
                    + "status TEXT NOT NULL DEFAULT 'PENDING', "
                    + "retry_count INTEGER DEFAULT 0, "
                    + "last_error TEXT, "
                    + "created_at INTEGER NOT NULL, "
                    + "updated_at INTEGER NOT NULL"
                    + ")"
            );

            stmt.execute("CREATE INDEX IF NOT EXISTS idx_pending_status ON "
                    + OfflineDbConfig.TABLE_PENDING_REQUESTS + "(status)");
        }

        this.activeConnection = conn;
        this.activeDbPath = dbPath;
        Logger.log("OfflineDatabaseManager", "Encrypted database opened successfully: " + dbPath);
        return activeConnection;
    }

    public synchronized boolean isDatabaseOpen() {
        try {
            return activeConnection != null && !activeConnection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Gets the currently active connection, opening it if necessary using the in-memory password.
     */
    public synchronized Connection getConnection() throws Exception {
        if (activeConnection != null && !activeConnection.isClosed()) {
            return activeConnection;
        }

        // Auto-reopen if session and password are present in memory
        core.security.UserSession session = core.security.UserSession.getInstance();
        String pwd = OfflinePasswordManager.getInstance().getCurrentPassword();
        if (session.isValid() && session.getUserId() != null && pwd != null && !pwd.isEmpty()) {
            Path dbPath = OfflineDbConfig.getDatabasePath(session.getUserId());
            return openDatabase(dbPath, pwd);
        }

        throw new IllegalStateException("Offline database is not opened. Call openDatabase first.");
    }

    /**
     * Safely closes the database connection and releases OS file locks.
     */
    public synchronized void closeDatabase() {
        if (activeConnection != null) {
            try {
                if (!activeConnection.isClosed()) {
                    activeConnection.close();
                }
            } catch (SQLException e) {
                Logger.errlog("Error closing offline database connection", e);
            } finally {
                activeConnection = null;
                activeDbPath = null;
            }
        }
    }

    /**
     * Checks if a database file exists on disk.
     */
    public boolean databaseExists(Path dbPath) {
        return Files.exists(dbPath);
    }

    /**
     * Truncates the write-ahead log and reclaims database memory and disk space.
     */
    public synchronized void checkpointAndClean() {
        if (activeConnection != null) {
            try {
                if (!activeConnection.isClosed()) {
                    try (Statement stmt = activeConnection.createStatement()) {
                        stmt.execute("PRAGMA wal_checkpoint(TRUNCATE)");
                    }
                }
            } catch (SQLException ignored) {
            }
        }
    }

    /**
     * Safely closes connection and deletes the database file and its WAL journals on disk.
     */
    public synchronized boolean deleteDatabaseFile(Path dbPath) {
        closeDatabase();

        boolean deleted = true;
        try {
            if (Files.exists(dbPath)) {
                deleted = Files.deleteIfExists(dbPath);
            }
            Path wal = Path.of(dbPath.toString() + "-wal");
            Path shm = Path.of(dbPath.toString() + "-shm");
            Files.deleteIfExists(wal);
            Files.deleteIfExists(shm);
        } catch (IOException e) {
            Logger.errlog("Failed to delete offline database file: " + dbPath, e);
            return false;
        }
        return deleted;
    }
}
