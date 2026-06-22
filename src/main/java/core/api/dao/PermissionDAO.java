package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.api.ApiConfig;

import java.net.http.HttpRequest;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Data Access Object for Permission-related API endpoints.
 *
 * @author Sanod
 */
public class PermissionDAO {

    private static volatile PermissionDAO instance;
    private final ApiClient apiClient;

    private PermissionDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static PermissionDAO getInstance() {
        if (instance == null) {
            synchronized (PermissionDAO.class) {
                if (instance == null) {
                    instance = new PermissionDAO();
                }
            }
        }
        return instance;
    }

    // GET /permissions
    public Set<String> getAllPermissions() throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.PERMISSIONS).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        Set<String> permissions = new HashSet<>();
        JsonArray dataArray = body.getAsJsonArray("data");
        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                permissions.add(elem.getAsString());
            }
        }
        return permissions;
    }

    // GET /users/{userId}/permissions
    public Set<String> getUserPermissions(int userId) throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.USERS + "/" + userId + "/permissions")
                .GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        Set<String> permissions = new HashSet<>();
        JsonArray dataArray = body.getAsJsonArray("data");
        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                permissions.add(elem.getAsString());
            }
        }
        return permissions;
    }

    // PUT /users/{userId}/permissions
    public boolean syncPermissions(int userId, List<String> permissions) throws Exception {
        JsonObject payload = new JsonObject();
        JsonArray permsArray = new JsonArray();
        for (String p : permissions) {
            permsArray.add(p);
        }
        payload.add("permissions", permsArray);

        HttpRequest request = apiClient.authRequest(ApiConfig.USERS + "/" + userId + "/permissions")
                .PUT(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    // POST /users/{userId}/permissions
    public boolean assignPermission(int userId, String permissionCode) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("permission_code", permissionCode);

        HttpRequest request = apiClient.authRequest(ApiConfig.USERS + "/" + userId + "/permissions")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    // DELETE /users/{userId}/permissions/{code}
    public boolean removePermission(int userId, String permissionCode) throws Exception {
        HttpRequest request = apiClient.authRequest(
                ApiConfig.USERS + "/" + userId + "/permissions/" + permissionCode)
                .DELETE().build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    // Extract permissions from login response
    public Set<String> extractPermissions(JsonObject loginResponse) {
        Set<String> permissions = new HashSet<>();
        JsonObject data = loginResponse.getAsJsonObject("data");

        if (data != null && data.has("permissions")) {
            JsonArray permsArray = data.getAsJsonArray("permissions");
            if (permsArray != null) {
                for (JsonElement elem : permsArray) {
                    permissions.add(elem.getAsString());
                }
            }
        }
        return permissions;
    }
}
