package core.offline;

import java.io.IOException;

/**
 * Exception thrown when a network failure occurs on a mutation and the request
 * has been safely enqueued to the encrypted offline database.
 *
 * @author Sanod
 */
public class OfflineQueuedException extends IOException {

    private final String requestId;
    private final String identifier;

    public OfflineQueuedException(String requestId, String identifier, String message) {
        super(message);
        this.requestId = requestId;
        this.identifier = identifier;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getIdentifier() {
        return identifier;
    }
}
