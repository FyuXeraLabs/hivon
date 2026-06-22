package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.api.ApiConfig;
import core.logging.Logger;
import models.dto.UserDTO;
import models.entity.User;
import models.entity.User.UserRole;

import java.net.http.HttpRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User-related API endpoints.
 *
 * @author Sanod
 */
public class UserDAO {

    private static volatile UserDAO instance;
    private final ApiClient apiClient;

    private static final DateTimeFormatter DT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private UserDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static UserDAO getInstance() {
        if (instance == null) {
            synchronized (UserDAO.class) {
                if (instance == null) {
                    instance = new UserDAO();
                }
            }
        }
        return instance;
    }

    // GET /users or GET /users?search={term}
    public List<UserDTO> getUsers(String searchTerm) throws Exception {
        String endpoint = ApiConfig.USERS;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            endpoint += "?search=" + java.net.URLEncoder.encode(searchTerm, "UTF-8");
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<UserDTO> users = new ArrayList<>();
        JsonArray dataArray = body.getAsJsonArray("data");

        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                users.add(jsonToUserDTO(elem.getAsJsonObject()));
            }
        }
        return users;
    }

    // GET /users/{id}
    public UserDTO getUserById(int userId) throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.USERS + "/" + userId)
                .GET().build();
        JsonObject body = apiClient.executeWithAuth(request);
        JsonObject data = body.getAsJsonObject("data");
        return jsonToUserDTO(data);
    }

    // POST /users
    public int createUser(UserDTO userDto, String password,
                          List<String> permissions) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("username", userDto.getUsername());
        payload.addProperty("full_name", userDto.getFullName());
        payload.addProperty("email", userDto.getEmail());
        payload.addProperty("role", userDto.getRole());
        payload.addProperty("password", password);

        if (permissions != null && !permissions.isEmpty()) {
            JsonArray permsArray = new JsonArray();
            for (String p : permissions) {
                permsArray.add(p);
            }
            payload.add("permissions", permsArray);
        }

        HttpRequest request = apiClient.authRequest(ApiConfig.USERS)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        JsonObject data = body.getAsJsonObject("data");

        return data.get("user_id").getAsInt();
    }

    // PUT /users/{id}
    public boolean updateUser(UserDTO userDto, List<String> permissions) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("full_name", userDto.getFullName());
        if (userDto.getEmail() != null) {
            payload.addProperty("email", userDto.getEmail());
        }
        payload.addProperty("role", userDto.getRole());
        payload.addProperty("is_active", userDto.getIsActive());

        if (permissions != null) {
            JsonArray permsArray = new JsonArray();
            for (String p : permissions) {
                permsArray.add(p);
            }
            payload.add("permissions", permsArray);
        }

        HttpRequest request = apiClient.authRequest(ApiConfig.USERS + "/" + userDto.getUserId())
                .PUT(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    // DELETE /users/{id}
    public boolean deleteUser(int userId) throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.USERS + "/" + userId)
                .DELETE().build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    // PUT /users/{id}/activate
    public boolean activateUser(int userId) throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.USERS + "/" + userId + "/activate")
                .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    // PUT /users/{id}/deactivate
    public boolean deactivateUser(int userId) throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.USERS + "/" + userId + "/deactivate")
                .PUT(HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    // PUT /users/{id}/reset-password
    public boolean resetPassword(int userId, String newPassword) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("new_password", newPassword);

        HttpRequest request = apiClient.authRequest(ApiConfig.USERS + "/" + userId + "/reset-password")
                .PUT(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    // parse a JSON user object into UserDTO
    private UserDTO jsonToUserDTO(JsonObject json) {
        UserDTO dto = new UserDTO();

        if (json.has("user_id") && !json.get("user_id").isJsonNull()) {
            dto.setUserId(json.get("user_id").getAsInt());
        }
        if (json.has("username") && !json.get("username").isJsonNull()) {
            dto.setUsername(json.get("username").getAsString());
        }
        if (json.has("full_name") && !json.get("full_name").isJsonNull()) {
            dto.setFullName(json.get("full_name").getAsString());
        }
        if (json.has("email") && !json.get("email").isJsonNull()) {
            dto.setEmail(json.get("email").getAsString());
        }
        if (json.has("role") && !json.get("role").isJsonNull()) {
            dto.setRole(json.get("role").getAsString());
        }
        if (json.has("warehouse_id") && !json.get("warehouse_id").isJsonNull()) {
            dto.setWarehouseId(json.get("warehouse_id").getAsInt());
        }
        if (json.has("is_active") && !json.get("is_active").isJsonNull()) {
            dto.setIsActive(json.get("is_active").getAsBoolean());
        }
        if (json.has("last_login") && !json.get("last_login").isJsonNull()) {
            dto.setLastLogin(parseDateTime(json.get("last_login").getAsString()));
        }
        if (json.has("created_date") && !json.get("created_date").isJsonNull()) {
            dto.setCreatedDate(parseDateTime(json.get("created_date").getAsString()));
        }
        if (json.has("last_modified") && !json.get("last_modified").isJsonNull()) {
            dto.setModifiedDate(parseDateTime(json.get("last_modified").getAsString()));
        }

        return dto;
    }

    // parse a JSON user object into User entity (for login response)
    public User jsonToUser(JsonObject json) {
        User user = new User();

        if (json.has("user_id") && !json.get("user_id").isJsonNull()) {
            user.setUserId(json.get("user_id").getAsInt());
        }
        if (json.has("username") && !json.get("username").isJsonNull()) {
            user.setUsername(json.get("username").getAsString());
        }
        if (json.has("full_name") && !json.get("full_name").isJsonNull()) {
            user.setFullName(json.get("full_name").getAsString());
        }
        if (json.has("email") && !json.get("email").isJsonNull()) {
            user.setEmail(json.get("email").getAsString());
        }
        if (json.has("role") && !json.get("role").isJsonNull()) {
            try {
                user.setRole(UserRole.valueOf(json.get("role").getAsString()));
            } catch (IllegalArgumentException e) {
                user.setRole(UserRole.OPERATOR);
            }
        }
        if (json.has("warehouse_id") && !json.get("warehouse_id").isJsonNull()) {
            user.setWarehouseId(json.get("warehouse_id").getAsInt());
        }
        if (json.has("is_active") && !json.get("is_active").isJsonNull()) {
            user.setIsActive(json.get("is_active").getAsBoolean());
        }
        if (json.has("last_login") && !json.get("last_login").isJsonNull()) {
            user.setLastLogin(parseDateTime(json.get("last_login").getAsString()));
        }

        return user;
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || "null".equals(dateStr)) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr, DT_FORMATTER);
        } catch (Exception e) {
            // try ISO format
            try {
                return LocalDateTime.parse(dateStr);
            } catch (Exception e2) {
                Logger.errlog("Failed to parse date: " + dateStr, e2);
                return null;
            }
        }
    }
}
