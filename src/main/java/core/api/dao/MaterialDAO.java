package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.api.ApiConfig;
import core.logging.Logger;
import models.dto.MaterialDTO;

import java.net.http.HttpRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Material REST API endpoints.
 * Handles fetching, creating, updating, and deleting materials.
 *
 * @author Sanod
 */
public class MaterialDAO {

    private static volatile MaterialDAO instance;
    private final ApiClient apiClient;

    private static final DateTimeFormatter DT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private MaterialDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static MaterialDAO getInstance() {
        if (instance == null) {
            synchronized (MaterialDAO.class) {
                if (instance == null) {
                    instance = new MaterialDAO();
                }
            }
        }
        return instance;
    }

    // GET /api/materials or GET /api/materials/search?q={searchTerm}
    public List<MaterialDTO> getMaterials(String searchTerm) throws Exception {
        String endpoint = ApiConfig.MATERIALS;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            endpoint = "/materials/search?q=" + java.net.URLEncoder.encode(searchTerm, "UTF-8");
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<MaterialDTO> materials = new ArrayList<>();
        JsonArray dataArray = body.getAsJsonArray("data");

        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                materials.add(jsonToMaterialDTO(elem.getAsJsonObject()));
            }
        }
        return materials;
    }

    // GET /api/materials/{materialId}
    public MaterialDTO getMaterialById(int materialId) throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.MATERIALS + "/" + materialId)
                .GET().build();
        JsonObject body = apiClient.executeWithAuth(request);
        JsonObject data = body.getAsJsonObject("data");
        return jsonToMaterialDTO(data);
    }

    // POST /api/materials
    public int createMaterial(MaterialDTO materialDto) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("material_code", materialDto.getMaterialCode());
        payload.addProperty("material_description", materialDto.getMaterialDescription());
        if (materialDto.getMaterialType() != null) {
            payload.addProperty("material_type", materialDto.getMaterialType());
        }
        if (materialDto.getUnitOfMeasure() != null) {
            payload.addProperty("unit_of_measure", materialDto.getUnitOfMeasure());
        }
        if (materialDto.getUnitWeight() != null) {
            payload.addProperty("unit_weight", materialDto.getUnitWeight());
        }
        if (materialDto.getCategory() != null) {
            payload.addProperty("category", materialDto.getCategory());
        }
        if (materialDto.getStorageCondition() != null) {
            payload.addProperty("storage_condition", materialDto.getStorageCondition());
        }
        payload.addProperty("is_active", materialDto.getIsActive() != null ? materialDto.getIsActive() : true);

        HttpRequest request = apiClient.authRequest(ApiConfig.MATERIALS)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        JsonObject data = body.getAsJsonObject("data");

        return data.get("material_id").getAsInt();
    }

    // PUT /api/materials/{materialId}
    public boolean updateMaterial(MaterialDTO materialDto) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("material_code", materialDto.getMaterialCode());
        payload.addProperty("material_description", materialDto.getMaterialDescription());
        if (materialDto.getMaterialType() != null) {
            payload.addProperty("material_type", materialDto.getMaterialType());
        }
        if (materialDto.getUnitOfMeasure() != null) {
            payload.addProperty("unit_of_measure", materialDto.getUnitOfMeasure());
        }
        if (materialDto.getUnitWeight() != null) {
            payload.addProperty("unit_weight", materialDto.getUnitWeight());
        }
        if (materialDto.getCategory() != null) {
            payload.addProperty("category", materialDto.getCategory());
        }
        if (materialDto.getStorageCondition() != null) {
            payload.addProperty("storage_condition", materialDto.getStorageCondition());
        }
        if (materialDto.getIsActive() != null) {
            payload.addProperty("is_active", materialDto.getIsActive());
        }

        HttpRequest request = apiClient.authRequest(ApiConfig.MATERIALS + "/" + materialDto.getMaterialId())
                .PUT(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    // DELETE /api/materials/{materialId}
    public boolean deleteMaterial(int materialId) throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.MATERIALS + "/" + materialId)
                .DELETE().build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    // Converts a JSON object from the API response into a MaterialDTO
    private MaterialDTO jsonToMaterialDTO(JsonObject json) {
        MaterialDTO dto = new MaterialDTO();

        if (json.has("material_id") && !json.get("material_id").isJsonNull()) {
            dto.setMaterialId(json.get("material_id").getAsInt());
        }
        if (json.has("material_code") && !json.get("material_code").isJsonNull()) {
            dto.setMaterialCode(json.get("material_code").getAsString());
        }
        if (json.has("material_description") && !json.get("material_description").isJsonNull()) {
            dto.setMaterialDescription(json.get("material_description").getAsString());
        }
        if (json.has("material_type") && !json.get("material_type").isJsonNull()) {
            dto.setMaterialType(json.get("material_type").getAsString());
        }
        if (json.has("unit_of_measure") && !json.get("unit_of_measure").isJsonNull()) {
            dto.setUnitOfMeasure(json.get("unit_of_measure").getAsString());
        }
        if (json.has("unit_weight") && !json.get("unit_weight").isJsonNull()) {
            dto.setUnitWeight(json.get("unit_weight").getAsDouble());
        }
        if (json.has("category") && !json.get("category").isJsonNull()) {
            dto.setCategory(json.get("category").getAsString());
        }
        if (json.has("storage_condition") && !json.get("storage_condition").isJsonNull()) {
            dto.setStorageCondition(json.get("storage_condition").getAsString());
        }
        if (json.has("is_batch_managed") && !json.get("is_batch_managed").isJsonNull()) {
            dto.setIsBatchManaged(json.get("is_batch_managed").getAsBoolean());
        }
        if (json.has("is_active") && !json.get("is_active").isJsonNull()) {
            dto.setIsActive(json.get("is_active").getAsBoolean());
        }
        if (json.has("min_stock_level") && !json.get("min_stock_level").isJsonNull()) {
            dto.setMinStockLevel(json.get("min_stock_level").getAsDouble());
        }
        if (json.has("max_stock_level") && !json.get("max_stock_level").isJsonNull()) {
            dto.setMaxStockLevel(json.get("max_stock_level").getAsDouble());
        }
        if (json.has("reorder_point") && !json.get("reorder_point").isJsonNull()) {
            dto.setReorderPoint(json.get("reorder_point").getAsDouble());
        }
        if (json.has("unit_cost") && !json.get("unit_cost").isJsonNull()) {
            dto.setUnitCost(json.get("unit_cost").getAsDouble());
        }
        if (json.has("volume") && !json.get("volume").isJsonNull()) {
            dto.setVolume(json.get("volume").getAsDouble());
        }
        if (json.has("created_date") && !json.get("created_date").isJsonNull()) {
            dto.setCreatedDate(parseDateTime(json.get("created_date").getAsString()));
        }
        if (json.has("last_modified") && !json.get("last_modified").isJsonNull()) {
            dto.setModifiedDate(parseDateTime(json.get("last_modified").getAsString()));
        }

        return dto;
    }

    // Parses a date-time string into LocalDateTime, trying multiple formats
    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || "null".equals(dateStr)) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr, DT_FORMATTER);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateStr);
            } catch (Exception e2) {
                Logger.errlog("Failed to parse date: " + dateStr, e2);
                return null;
            }
        }
    }
}