package core.offline;

import java.security.SecureRandom;

/**
 * In-memory manager for rotated SQLCipher database encryption keys.
 * Generates 256-bit cryptographically secure keys encoded in hexadecimal,
 * eliminating any single-quote escaping or SQL injection vulnerabilities in PRAGMA key.
 *
 * @author Sanod
 */
public final class OfflinePasswordManager {

    private static volatile OfflinePasswordManager instance;
    private final SecureRandom secureRandom;
    private volatile String currentPassword;

    private OfflinePasswordManager() {
        this.secureRandom = new SecureRandom();
    }

    public static OfflinePasswordManager getInstance() {
        if (instance == null) {
            synchronized (OfflinePasswordManager.class) {
                if (instance == null) {
                    instance = new OfflinePasswordManager();
                }
            }
        }
        return instance;
    }

    /**
     * Generates a fresh 256-bit (32 bytes) cryptographically secure hex password.
     *
     * @return 64-character hexadecimal key string
     */
    public String generateSecurePassword() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public synchronized void setCurrentPassword(String password) {
        this.currentPassword = password;
    }

    public synchronized String getCurrentPassword() {
        return this.currentPassword;
    }

    public synchronized void clear() {
        this.currentPassword = null;
    }
}
