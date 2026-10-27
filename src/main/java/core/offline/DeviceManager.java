package core.offline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Manages unique, persistent device identification for the local machine.
 * Prevents key collision when multiple devices sync for the same user account.
 *
 * @author Sanod
 */
public final class DeviceManager {

    private static final Path DEVICE_FILE = Path.of(
            System.getProperty("user.home"),
            ".hivon",
            "device_id"
    );

    private static volatile String cachedDeviceId;

    private DeviceManager() {}

    /**
     * Retrieves the persistent unique device ID, generating and storing one if absent.
     *
     * @return 36-character UUID string representing this installation/machine.
     */
    public static synchronized String getDeviceId() {
        if (cachedDeviceId != null) {
            return cachedDeviceId;
        }

        try {
            if (Files.exists(DEVICE_FILE)) {
                String id = Files.readString(DEVICE_FILE).trim();
                if (!id.isEmpty()) {
                    cachedDeviceId = id;
                    return cachedDeviceId;
                }
            }

            // Generate new ID and persist
            String newId = UUID.randomUUID().toString();
            Files.createDirectories(DEVICE_FILE.getParent());
            Files.writeString(DEVICE_FILE, newId);
            cachedDeviceId = newId;
            return cachedDeviceId;

        } catch (IOException e) {
            // Fallback to in-memory ID if filesystem is restricted
            if (cachedDeviceId == null) {
                cachedDeviceId = UUID.randomUUID().toString();
            }
            return cachedDeviceId;
        }
    }
}
