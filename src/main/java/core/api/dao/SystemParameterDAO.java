package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.api.ApiConfig;
import models.dto.UOMDTO;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for System Parameter REST API endpoints.
 * Handles fetching, creating, updating, and deleting UOM (Unit of Measure) parameters.
 *
 * @author Sanod
 */
public class SystemParameterDAO {

    private static volatile SystemParameterDAO instance;
    private final ApiClient apiClient;

    private SystemParameterDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static SystemParameterDAO getInstance() {
        if (instance == null) {
            synchronized (SystemParameterDAO.class) {
                if (instance == null) {
                    instance = new SystemParameterDAO();
                }
            }
        }
        return instance;
    }

    // GET /api/system-parameters?category=uom&q={searchTerm}
    public List<UOMDTO> getUOMs(String searchTerm) throws Exception {
        String endpoint = ApiConfig.SYSTEM_PARAMETERS + "?category=uom";
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            endpoint += "&q=" + java.net.URLEncoder.encode(searchTerm, "UTF-8");
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<UOMDTO> uomList = new ArrayList<>();
        JsonArray dataArray = body.getAsJsonArray("data");

        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                uomList.add(jsonToUOMDTO(elem.getAsJsonObject()));
            }
        }
        return uomList;
    }

    // POST /api/system-parameters
    public boolean createUOM(UOMDTO uomDto) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("param_key", uomDto.getParamKey());
        payload.addProperty("param_value", uomDto.getParamValue());
        payload.addProperty("description", uomDto.getDescription());
        if (uomDto.getCategory() != null) {
            payload.addProperty("category", uomDto.getCategory());
        } else {
            payload.addProperty("category", "uom");
        }
        payload.addProperty("is_active", uomDto.getIsActive() != null ? uomDto.getIsActive() : true);

        HttpRequest request = apiClient.authRequest(ApiConfig.SYSTEM_PARAMETERS)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    // PUT /api/system-parameters/{paramKey}
    public boolean updateUOM(UOMDTO uomDto) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("param_key", uomDto.getParamKey());
        payload.addProperty("param_value", uomDto.getParamValue());
        if (uomDto.getDescription() != null) {
            payload.addProperty("description", uomDto.getDescription());
        }
        if (uomDto.getCategory() != null) {
            payload.addProperty("category", uomDto.getCategory());
        }
        if (uomDto.getIsActive() != null) {
            payload.addProperty("is_active", uomDto.getIsActive());
        }

        HttpRequest request = apiClient.authRequest(ApiConfig.SYSTEM_PARAMETERS + "/" + uomDto.getParamKey())
                .PUT(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    // DELETE /api/system-parameters/{paramKey}
    public boolean deleteUOM(String paramKey) throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.SYSTEM_PARAMETERS + "/" + paramKey)
                .DELETE().build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    // converts a JSON object from the API response into a UOMDTO
    private UOMDTO jsonToUOMDTO(JsonObject json) {
        UOMDTO dto = new UOMDTO();

        if (json.has("param_key") && !json.get("param_key").isJsonNull()) {
            dto.setParamKey(json.get("param_key").getAsString());
        }
        if (json.has("param_value") && !json.get("param_value").isJsonNull()) {
            dto.setParamValue(json.get("param_value").getAsString());
        }
        if (json.has("description") && !json.get("description").isJsonNull()) {
            dto.setDescription(json.get("description").getAsString());
        }
        if (json.has("category") && !json.get("category").isJsonNull()) {
            dto.setCategory(json.get("category").getAsString());
        }
        if (json.has("is_active") && !json.get("is_active").isJsonNull()) {
            dto.setIsActive(json.get("is_active").getAsBoolean());
        }

        return dto;
    }
}