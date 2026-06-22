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

    // execute HTTP request with automatic token refresh on 401
    public JsonObject executeWithAuth(HttpRequest request) throws Exception {
        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

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

                    response = httpClient.send(retryRequest,
                            HttpResponse.BodyHandlers.ofString());

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
}

