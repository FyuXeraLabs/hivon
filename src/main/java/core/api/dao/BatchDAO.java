package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.api.ApiConfig;
import core.logging.Logger;
import models.dto.BatchDTO;

import java.net.http.HttpRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BatchDAO {

    private static volatile BatchDAO instance;
    private final ApiClient apiClient;

    private static final DateTimeFormatter DT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private BatchDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static BatchDAO getInstance() {
        if (instance == null) {
            synchronized (BatchDAO.class) {
                if (instance == null) {
                    instance = new BatchDAO();
                }
            }
        }
        return instance;
    }

    public List<BatchDTO> getBatches(Integer materialId, String searchTerm) throws Exception {
        String endpoint = ApiConfig.BATCHES;
        StringBuilder urlBuilder = new StringBuilder(endpoint);
        boolean hasParam = false;

        if (materialId != null) {
            urlBuilder.append("?material_id=").append(materialId);
            hasParam = true;
        }
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            urlBuilder.append(hasParam ? "&" : "?");
            urlBuilder.append("q=").append(java.net.URLEncoder.encode(searchTerm, "UTF-8"));
        }

        HttpRequest request = apiClient.authRequest(urlBuilder.toString()).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<BatchDTO> batches = new ArrayList<>();
        JsonArray dataArray = body.getAsJsonArray("data");

        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                batches.add(jsonToBatchDTO(elem.getAsJsonObject()));
            }
        }
        return batches;
    }

    public List<BatchDTO> getBatchesByMaterial(int materialId) throws Exception {
        HttpRequest request = apiClient.authRequest(
                ApiConfig.BATCHES + "/material/" + materialId).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<BatchDTO> batches = new ArrayList<>();
        JsonArray dataArray = body.getAsJsonArray("data");

        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                batches.add(jsonToBatchDTO(elem.getAsJsonObject()));
            }
        }
        return batches;
    }

    public BatchDTO getBatchById(int batchId) throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.BATCHES + "/" + batchId)
                .GET().build();
        JsonObject body = apiClient.executeWithAuth(request);
        JsonObject data = body.getAsJsonObject("data");
        return jsonToBatchDTO(data);
    }

    public int createBatch(BatchDTO batchDto) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("material_id", batchDto.getMaterialId());
        payload.addProperty("batch_number", batchDto.getBatchNumber());
        if (batchDto.getSupplierBatch() != null) {
            payload.addProperty("supplier_batch", batchDto.getSupplierBatch());
        }
        if (batchDto.getManufactureDate() != null) {
            payload.addProperty("manufacture_date", batchDto.getManufactureDate().toString());
        }
        if (batchDto.getExpiryDate() != null) {
            payload.addProperty("expiry_date", batchDto.getExpiryDate().toString());
        }
        payload.addProperty("quality_status",
                batchDto.getQualityStatus() != null ? batchDto.getQualityStatus() : "pending");

        HttpRequest request = apiClient.authRequest(ApiConfig.BATCHES)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        JsonObject data = body.getAsJsonObject("data");

        return data.get("batch_id").getAsInt();
    }

    public boolean updateBatch(BatchDTO batchDto) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("material_id", batchDto.getMaterialId());
        payload.addProperty("batch_number", batchDto.getBatchNumber());
        if (batchDto.getSupplierBatch() != null) {
            payload.addProperty("supplier_batch", batchDto.getSupplierBatch());
        }
        if (batchDto.getManufactureDate() != null) {
            payload.addProperty("manufacture_date", batchDto.getManufactureDate().toString());
        }
        if (batchDto.getExpiryDate() != null) {
            payload.addProperty("expiry_date", batchDto.getExpiryDate().toString());
        }
        if (batchDto.getQualityStatus() != null) {
            payload.addProperty("quality_status", batchDto.getQualityStatus());
        }

        HttpRequest request = apiClient.authRequest(ApiConfig.BATCHES + "/" + batchDto.getBatchId())
                .PUT(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    public boolean deleteBatch(int batchId) throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.BATCHES + "/" + batchId)
                .DELETE().build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    public List<JsonObject> getBatchStock(int batchId) throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.INVENTORY + "?batch_id=" + batchId)
                .GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<JsonObject> stock = new ArrayList<>();
        JsonArray dataArray = body.getAsJsonArray("data");
        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                stock.add(elem.getAsJsonObject());
            }
        }
        return stock;
    }

    private BatchDTO jsonToBatchDTO(JsonObject json) {
        BatchDTO dto = new BatchDTO();

        if (json.has("batch_id") && !json.get("batch_id").isJsonNull()) {
            dto.setBatchId(json.get("batch_id").getAsInt());
        }
        if (json.has("material_id") && !json.get("material_id").isJsonNull()) {
            dto.setMaterialId(json.get("material_id").getAsInt());
        }
        if (json.has("material_code") && !json.get("material_code").isJsonNull()) {
            dto.setMaterialCode(json.get("material_code").getAsString());
        }
        if (json.has("material_description") && !json.get("material_description").isJsonNull()) {
            dto.setMaterialDescription(json.get("material_description").getAsString());
        }
        if (json.has("batch_number") && !json.get("batch_number").isJsonNull()) {
            dto.setBatchNumber(json.get("batch_number").getAsString());
        }
        if (json.has("supplier_batch") && !json.get("supplier_batch").isJsonNull()) {
            dto.setSupplierBatch(json.get("supplier_batch").getAsString());
        }
        if (json.has("manufacture_date") && !json.get("manufacture_date").isJsonNull()) {
            dto.setManufactureDate(parseDate(json.get("manufacture_date").getAsString()));
        }
        if (json.has("expiry_date") && !json.get("expiry_date").isJsonNull()) {
            dto.setExpiryDate(parseDate(json.get("expiry_date").getAsString()));
        }
        if (json.has("quality_status") && !json.get("quality_status").isJsonNull()) {
            dto.setQualityStatus(json.get("quality_status").getAsString());
        }
        if (json.has("created_date") && !json.get("created_date").isJsonNull()) {
            dto.setCreatedDate(parseDateTime(json.get("created_date").getAsString()));
        }

        return dto;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || "null".equals(dateStr)) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (Exception e) {
            try {
                return LocalDate.parse(dateStr);
            } catch (Exception e2) {
                Logger.errlog("Failed to parse date: " + dateStr, e2);
                return null;
            }
        }
    }

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