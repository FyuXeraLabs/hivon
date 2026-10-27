/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import core.logging.Logger;
import core.security.UserSession;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * centralized REST API HTTP client with JWT token management
 *
 * @author Sanod
 */
public class ApiClient {

    private static volatile ApiClient instance;
    private final HttpClient httpClient;

    private String accessToken;
    private String refreshToken;

    private ApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(ApiConfig.CONNECT_TIMEOUT))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public static ApiClient getInstance() {
        if (instance == null) {
            synchronized (ApiClient.class) {
                if (instance == null) {
                    instance = new ApiClient();
                }
            }
        }
        return instance;
    }

    // token management

    public void setTokens(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void clearTokens() {
        this.accessToken = null;
        this.refreshToken = null;
    }

    // core http methods

    private HttpRequest.Builder baseRequest(String endpoint) {
        return HttpRequest.newBuilder()
                .uri(URI.create(ApiConfig.BASE_URL + endpoint))
                .timeout(Duration.ofSeconds(ApiConfig.READ_TIMEOUT))
                .header("Content-Type", "application/json");
    }

    public HttpRequest.Builder authRequest(String endpoint) {
        return baseRequest(endpoint)
                .header("Authorization", "Bearer " + accessToken);
    }

    // execute HTTP request with automatic token refresh on 401 and offline queueing on network errors
    public JsonObject executeWithAuth(HttpRequest request) throws Exception {
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.io.IOException netErr) {
            boolean isReplay = request.headers().firstValue("X-Sync-Replay").isPresent()
                    || core.offline.SyncManager.getInstance().isSyncing();
            String method = request.method().toUpperCase();
            if (!isReplay && isMutationMethod(method) && !isExcludedFromOffline(request)) {
                return handleOfflineEnqueue(request, netErr);
            }
            throw netErr;
        }

        // update activity on every API call
        UserSession session = UserSession.getInstance();
        if (session.isValid()) {
            session.updateActivity();
        }

        if (response.statusCode() == 401) {
            // check if token expired - try refresh
            JsonObject errorBody = tryParseJson(response.body());
            if (errorBody != null && "TOKEN_EXPIRED".equals(
                    errorBody.has("code") ? errorBody.get("code").getAsString() : null)) {

                // attempt token refresh
                if (refreshAccessToken()) {
                    // rebuild request with new token
                    HttpRequest retryRequest = HttpRequest.newBuilder(request.uri())
                            .timeout(Duration.ofSeconds(ApiConfig.READ_TIMEOUT))
                            .header("Content-Type", "application/json")
                            .header("Authorization", "Bearer " + accessToken)
                            .method(request.method(),
                                    request.bodyPublisher().orElse(
                                            HttpRequest.BodyPublishers.noBody()))
                            .build();

                    try {
                        response = httpClient.send(retryRequest,
                                HttpResponse.BodyHandlers.ofString());
                    } catch (java.io.IOException netErr) {
                        boolean isReplay = retryRequest.headers().firstValue("X-Sync-Replay").isPresent()
                                || core.offline.SyncManager.getInstance().isSyncing();
                        String method = request.method().toUpperCase();
                        if (!isReplay && isMutationMethod(method) && !isExcludedFromOffline(request)) {
                            return handleOfflineEnqueue(request, netErr);
                        }
                        throw netErr;
                    }

                    if (response.statusCode() == 401) {
                        throw new Exception("Session expired. Please login again.");
                    }
                } else {
                    throw new Exception("Session expired. Please login again.");
                }
            } else {
                String msg = errorBody != null && errorBody.has("message")
                        ? errorBody.get("message").getAsString()
                        : "Authentication failed";
                throw new Exception(msg);
            }
        }

        JsonObject body = parseResponse(response);
        return body;
    }

    private boolean isMutationMethod(String method) {
        return "POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method) || "PATCH".equals(method);
    }

    private boolean isExcludedFromOffline(HttpRequest request) {
        String path = request.uri().getPath();
        return path.contains("/auth/login") || path.contains("/auth/refresh") || path.contains("/users/offline-db-key");
    }

    private JsonObject handleOfflineEnqueue(HttpRequest request, java.io.IOException cause) throws Exception {
        try {
            String uriStr = request.uri().toString();
            String endpoint = uriStr.startsWith(ApiConfig.BASE_URL)
                    ? uriStr.substring(ApiConfig.BASE_URL.length())
                    : request.uri().getPath();

            String payload = extractRequestBody(request);
            String identifier = extractIdentifier(request.method(), endpoint, payload);
            String requestId = java.util.UUID.randomUUID().toString();

            core.offline.OfflineRequest offlineReq = new core.offline.OfflineRequest(
                    requestId,
                    request.method(),
                    endpoint,
                    payload,
                    identifier,
                    request.method() + "_" + endpoint.replaceAll("[^A-Za-z0-9]", "_")
            );

            boolean enqueued = core.offline.OfflineQueueDAO.getInstance().enqueue(offlineReq);
            if (enqueued) {
                Logger.log("ApiClient", "Network offline: Mutation queued into SQLCipher offline DB [" + identifier + "]");
                
                int pendingCount = core.offline.OfflineQueueDAO.getInstance().getPendingCount();
                ui.dialogs.SystemTrayPanel.updateDetail(pendingCount + " item(s) yet to sync");

                JsonObject successResp = new JsonObject();
                successResp.addProperty("status", "success");
                successResp.addProperty("offline", true);
                successResp.addProperty("message", "Request saved in encrypted offline database (pending sync)");

                JsonObject data = new JsonObject();
                data.addProperty("offline", true);
                data.addProperty("request_id", requestId);
                data.addProperty("identifier", identifier);

                JsonObject parsedPayload = tryParseJson(payload);
                if (parsedPayload != null) {
                    for (String key : new String[]{"user_id", "material_id", "warehouse_id", "po_number", "to_number"}) {
                        if (parsedPayload.has(key)) {
                            data.add(key, parsedPayload.get(key));
                        }
                    }
                }
                successResp.add("data", data);
                return successResp;
            }
        } catch (Exception ex) {
            Logger.errlog("Failed to enqueue request offline", ex);
        }
        throw cause;
    }

    private String extractRequestBody(HttpRequest request) {
        if (request.bodyPublisher().isEmpty()) {
            return "{}";
        }
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.util.concurrent.CompletableFuture<String> future = new java.util.concurrent.CompletableFuture<>();
            request.bodyPublisher().get().subscribe(new java.util.concurrent.Flow.Subscriber<java.nio.ByteBuffer>() {
                @Override
                public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) {
                    subscription.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(java.nio.ByteBuffer item) {
                    byte[] bytes = new byte[item.remaining()];
                    item.get(bytes);
                    baos.write(bytes, 0, bytes.length);
                }

                @Override
                public void onError(Throwable throwable) {
                    future.complete("{}");
                }

                @Override
                public void onComplete() {
                    future.complete(baos.toString(java.nio.charset.StandardCharsets.UTF_8));
                }
            });
            return future.get(1, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String extractIdentifier(String method, String endpoint, String payload) {
        if (payload != null && !payload.isEmpty()) {
            JsonObject json = tryParseJson(payload);
            if (json != null) {
                for (String key : new String[]{"po_number", "poNumber", "to_number", "toNumber", "so_number", "soNumber", "username", "full_name", "material_code", "batch_number"}) {
                    if (json.has(key) && !json.get(key).isJsonNull()) {
                        return json.get(key).getAsString();
                    }
                }
            }
        }
        if (endpoint.contains("/users/")) {
            return "User #" + endpoint.substring(endpoint.lastIndexOf('/') + 1);
        }
        return method + " " + endpoint;
    }

    private JsonObject parseResponse(HttpResponse<String> response) throws Exception {
        JsonObject body = tryParseJson(response.body());

        if (body == null) {
            Logger.errlog("Invalid JSON response from server. Raw body: " + response.body(), new Exception());
            throw new Exception("Invalid JSON response from server");
        }

        if (response.statusCode() >= 400) {
            String message = body.has("message")
                    ? body.get("message").getAsString()
                    : "API error (HTTP " + response.statusCode() + ")";
            throw new Exception(message);
        }

        return body;
    }

    private JsonObject tryParseJson(String json) {
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    // auth endpoints

    /**
     * POST /auth/login
     * returns full login response including user, permissions, tokens
     */
    public JsonObject login(String username, String password) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("username", username);
        payload.addProperty("password", password);

        HttpRequest request = baseRequest(ApiConfig.AUTH_LOGIN)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        JsonObject body = parseResponse(response);

        // extract and store tokens
        JsonObject data = body.getAsJsonObject("data");
        this.accessToken = data.get("access_token").getAsString();
        this.refreshToken = data.get("refresh_token").getAsString();

        return body;
    }

    /**
     * POST /auth/refresh
     * refreshes the access token using the refresh token
     */
    private boolean refreshAccessToken() {
        try {
            if (refreshToken == null || refreshToken.isEmpty()) {
                return false;
            }

            JsonObject payload = new JsonObject();
            payload.addProperty("refresh_token", refreshToken);

            HttpRequest request = baseRequest(ApiConfig.AUTH_REFRESH)
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return false;
            }

            JsonObject body = tryParseJson(response.body());
            if (body == null || !"success".equals(
                    body.has("status") ? body.get("status").getAsString() : null)) {
                return false;
            }

            JsonObject data = body.getAsJsonObject("data");
            this.accessToken = data.get("access_token").getAsString();
            this.refreshToken = data.get("refresh_token").getAsString();

            // update session tokens
            UserSession session = UserSession.getInstance();
            if (session.isValid()) {
                session.setAccessToken(this.accessToken);
                session.setRefreshToken(this.refreshToken);
            }

            Logger.log("ApiClient", "Access token refreshed successfully");
            return true;

        } catch (Exception e) {
            Logger.errlog("Failed to refresh access token: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * check API and database health
     * GET /api/health
     */
    public JsonObject checkHealth() throws Exception {
        HttpRequest request = baseRequest("/health")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        JsonObject body = tryParseJson(response.body());
        
        if (body == null) {
            throw new Exception("Invalid JSON response from health check endpoint (HTTP " + response.statusCode() + "): " + response.body());
        }
        
        return body;
    }

    /**
     * Store rotated offline DB encryption key on the server
     */
    public void saveOfflineDbKey(String deviceId, String dbPassword) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("device_id", deviceId);
        payload.addProperty("db_password", dbPassword);

        HttpRequest request = authRequest(ApiConfig.USERS_OFFLINE_DB_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        executeWithAuth(request);
    }

    /**
     * Retrieve offline DB encryption key from the server for previous session
     */
    public String getOfflineDbKey(String deviceId) throws Exception {
        String endpoint = ApiConfig.USERS_OFFLINE_DB_KEY + "?device_id="
                + java.net.URLEncoder.encode(deviceId, java.nio.charset.StandardCharsets.UTF_8);

        HttpRequest request = authRequest(endpoint)
                .GET()
                .build();

        try {
            JsonObject body = executeWithAuth(request);
            if (body != null && body.has("data") && body.get("data").isJsonObject()) {
                JsonObject data = body.getAsJsonObject("data");
                if (data.has("db_password")) {
                    return data.get("db_password").getAsString();
                }
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("not found")) {
                return null;
            }
            throw e;
        }
        return null;
    }
}

