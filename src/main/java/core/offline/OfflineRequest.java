package core.offline;

/**
 * Represents a queued API mutation request stored in the SQLCipher database.
 *
 * @author Sanod
 */
public class OfflineRequest {

    private long id;
    private String requestId;
    private String method;
    private String endpoint;
    private String payloadJson;
    private String identifier;
    private String requestType;
    private String status;
    private int retryCount;
    private String lastError;
    private long createdAt;
    private long updatedAt;

    public OfflineRequest() {
        this.status = OfflineDbConfig.STATUS_PENDING;
        this.retryCount = 0;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public OfflineRequest(String requestId, String method, String endpoint, String payloadJson,
                          String identifier, String requestType) {
        this();
        this.requestId = requestId;
        this.method = method;
        this.endpoint = endpoint;
        this.payloadJson = payloadJson;
        this.identifier = identifier;
        this.requestType = requestType;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
