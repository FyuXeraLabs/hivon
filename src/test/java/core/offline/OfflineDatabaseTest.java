package core.offline;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class OfflineDatabaseTest {

    private Path testDbPath;
    private String testPassword;
    private OfflineDatabaseManager dbMgr;
    private OfflineQueueDAO queueDAO;

    @BeforeEach
    public void setUp() {
        testDbPath = Path.of("target", "test-data", "test_offline.db");
        dbMgr = OfflineDatabaseManager.getInstance();
        queueDAO = OfflineQueueDAO.getInstance();
        testPassword = OfflinePasswordManager.getInstance().generateSecurePassword();
        dbMgr.deleteDatabaseFile(testDbPath);
    }

    @AfterEach
    public void tearDown() {
        dbMgr.closeDatabase();
        dbMgr.deleteDatabaseFile(testDbPath);
    }

    @Test
    public void testEncryptedDatabaseCreationAndVerification() throws Exception {
        // Step 1: Open and initialize encrypted DB
        Connection conn = dbMgr.openDatabase(testDbPath, testPassword);
        assertNotNull(conn);
        assertFalse(conn.isClosed());
        assertTrue(Files.exists(testDbPath));

        // Step 2: Enqueue an offline request
        String reqId = UUID.randomUUID().toString();
        OfflineRequest req = new OfflineRequest(
                reqId,
                "POST",
                "/api/transfer-orders/bin-to-bin",
                "{\"warehouse_id\":1,\"reason\":\"REORGANIZATION\"}",
                "TO-2026-TEST",
                "BIN_TO_BIN"
        );

        boolean enqueued = queueDAO.enqueue(req);
        assertTrue(enqueued, "Request should be enqueued into encrypted SQLite DB");

        // Step 3: Read back request
        List<OfflineRequest> pending = queueDAO.getPendingRequests();
        assertEquals(1, pending.size());
        assertEquals(reqId, pending.get(0).getRequestId());
        assertEquals("TO-2026-TEST", pending.get(0).getIdentifier());

        // Step 4: Close DB and reopen with the same password
        dbMgr.closeDatabase();
        Connection reopened = dbMgr.openDatabase(testDbPath, testPassword);
        assertNotNull(reopened);

        List<OfflineRequest> reopenedPending = queueDAO.getPendingRequests();
        assertEquals(1, reopenedPending.size());
        assertEquals("TO-2026-TEST", reopenedPending.get(0).getIdentifier());

        dbMgr.closeDatabase();

        // Step 5: Verify that opening with a WRONG password fails to decrypt
        String wrongPassword = OfflinePasswordManager.getInstance().generateSecurePassword();
        assertThrows(SQLException.class, () -> {
            dbMgr.openDatabase(testDbPath, wrongPassword);
        }, "Opening an encrypted database with an incorrect key must throw an SQLException");
    }

    @Test
    public void testDeviceManagerPersistence() {
        String id1 = DeviceManager.getDeviceId();
        assertNotNull(id1);
        assertFalse(id1.trim().isEmpty());

        String id2 = DeviceManager.getDeviceId();
        assertEquals(id1, id2, "Device ID must remain stable across calls");
    }

    @Test
    public void testAutomaticOfflineMutationInterception() throws Exception {
        dbMgr.openDatabase(testDbPath, testPassword);

        // Simulate an unreachable local endpoint (port 59999 where no server is running)
        java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("http://127.0.0.1:59999/api/users/2"))
                .header("Content-Type", "application/json")
                .PUT(java.net.http.HttpRequest.BodyPublishers.ofString("{\"username\":\"sanod\",\"full_name\":\"Sanod Mendis\"}"))
                .build();

        com.google.gson.JsonObject response = core.api.ApiClient.getInstance().executeWithAuth(req);
        assertNotNull(response);
        assertEquals("success", response.get("status").getAsString());
        assertTrue(response.get("offline").getAsBoolean());

        List<OfflineRequest> pending = queueDAO.getPendingRequests();
        assertEquals(1, pending.size());
        assertEquals("PUT", pending.get(0).getMethod());
        assertEquals("sanod", pending.get(0).getIdentifier());
    }
}
