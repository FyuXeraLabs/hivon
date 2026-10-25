package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.api.ApiConfig;
import core.logging.Logger;
import models.dto.CycleCountDTO;
import models.dto.CycleCountItemDTO;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Cycle Count REST API endpoints.
 * Handles creating count plans, fetching count details, recording physical counts,
 * and completing counts.
 *
 * @author Piyumi
 */
public class CycleCountDAO {

    private static volatile CycleCountDAO instance;
    private final ApiClient apiClient;

    private CycleCountDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static CycleCountDAO getInstance() {
        if (instance == null) {
            synchronized (CycleCountDAO.class) {
                if (instance == null) {
                    instance = new CycleCountDAO();
                }
            }
        }
        return instance;
    }

    // POST /api/cycle-counts: create a cycle count plan (freezes target bins)
    public int createCycleCount(int warehouseId, Integer binId, String zoneCode, String countDate, String countType) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("warehouse_id", warehouseId);
        if (binId != null) {
            payload.addProperty("bin_id", binId);
        }
        if (zoneCode != null && !zoneCode.trim().isEmpty()) {
            payload.addProperty("zone_code", zoneCode.trim());
        }
        if (countDate != null && !countDate.trim().isEmpty()) {
            payload.addProperty("count_date", countDate.trim());
        }
        if (countType != null && !countType.trim().isEmpty()) {
            payload.addProperty("count_type", countType.trim());
        }

        HttpRequest request = apiClient.authRequest(ApiConfig.CYCLE_COUNTS)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject response = apiClient.executeWithAuth(request);
        if ("success".equalsIgnoreCase(response.get("status").getAsString())) {
            JsonObject data = response.getAsJsonObject("data");
            return data.get("count_id").getAsInt();
        } else {
            String msg = response.has("message") ? response.get("message").getAsString() : "Failed to create cycle count";
            throw new Exception(msg);
        }
    }

    // GET /api/cycle-counts/{id}: get cycle count and items
    public CycleCountDTO getCycleCountById(int countId) throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.CYCLE_COUNTS + "/" + countId)
                .GET()
                .build();

        JsonObject response = apiClient.executeWithAuth(request);
        if ("success".equalsIgnoreCase(response.get("status").getAsString())) {
            JsonObject data = response.getAsJsonObject("data");
            return jsonToCycleCountDTO(data);
        } else {
            String msg = response.has("message") ? response.get("message").getAsString() : "Failed to load cycle count";
            throw new Exception(msg);
        }
    }

    // PUT /api/cycle-counts/{id}/count: update physical counts
    public boolean recordCounts(int countId, List<CycleCountItemDTO> items) throws Exception {
        JsonObject payload = new JsonObject();
        JsonArray itemsArray = new JsonArray();

        for (CycleCountItemDTO item : items) {
            if (item.getCountedQuantity() != null) {
                JsonObject itemObj = new JsonObject();
                itemObj.addProperty("count_item_id", item.getCountItemId());
                itemObj.addProperty("counted_quantity", item.getCountedQuantity());
                if (item.getVarianceReason() != null) {
                    itemObj.addProperty("variance_reason", item.getVarianceReason());
                }
                itemsArray.add(itemObj);
            }
        }
        payload.add("items", itemsArray);

        HttpRequest request = apiClient.authRequest(ApiConfig.CYCLE_COUNTS + "/" + countId + "/count")
                .PUT(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject response = apiClient.executeWithAuth(request);
        return "success".equalsIgnoreCase(response.get("status").getAsString());
    }

    // PUT /api/cycle-counts/{id}/complete: finalize cycle count, post adjustments, unfreeze bins
    public boolean completeCycleCount(int countId) throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.CYCLE_COUNTS + "/" + countId + "/complete")
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        JsonObject response = apiClient.executeWithAuth(request);
        if ("success".equalsIgnoreCase(response.get("status").getAsString())) {
            return true;
        } else {
            String msg = response.has("message") ? response.get("message").getAsString() : "Failed to complete cycle count";
            throw new Exception(msg);
        }
    }

    // GET /api/cycle-counts: list cycle count plans with optional status/warehouse
    public List<CycleCountDTO> listCycleCounts(String status, Integer warehouseId) throws Exception {
        StringBuilder url = new StringBuilder(ApiConfig.CYCLE_COUNTS);
        boolean hasParam = false;
        if (status != null && !status.trim().isEmpty()) {
            url.append("?status=").append(status);
            hasParam = true;
        }
        if (warehouseId != null) {
            url.append(hasParam ? "&" : "?").append("warehouse_id=").append(warehouseId);
        }

        HttpRequest request = apiClient.authRequest(url.toString())
                .GET()
                .build();

        JsonObject response = apiClient.executeWithAuth(request);
        List<CycleCountDTO> list = new ArrayList<>();
        if (response.has("data") && response.get("data").isJsonArray()) {
            JsonArray arr = response.getAsJsonArray("data");
            for (JsonElement el : arr) {
                list.add(jsonToCycleCountDTO(el.getAsJsonObject()));
            }
        }
        return list;
    }

    private CycleCountDTO jsonToCycleCountDTO(JsonObject json) {
        CycleCountDTO dto = new CycleCountDTO();
        if (json.has("count_id") && !json.get("count_id").isJsonNull()) {
            dto.setCountId(json.get("count_id").getAsInt());
        }
        if (json.has("count_number") && !json.get("count_number").isJsonNull()) {
            dto.setCountNumber(json.get("count_number").getAsString());
        }
        if (json.has("warehouse_id") && !json.get("warehouse_id").isJsonNull()) {
            dto.setWarehouseId(json.get("warehouse_id").getAsInt());
        }
        if (json.has("warehouse_code") && !json.get("warehouse_code").isJsonNull()) {
            dto.setWarehouseCode(json.get("warehouse_code").getAsString());
        }
        if (json.has("warehouse_name") && !json.get("warehouse_name").isJsonNull()) {
            dto.setWarehouseName(json.get("warehouse_name").getAsString());
        }
        if (json.has("bin_id") && !json.get("bin_id").isJsonNull()) {
            dto.setBinId(json.get("bin_id").getAsInt());
        }
        if (json.has("bin_code") && !json.get("bin_code").isJsonNull()) {
            dto.setBinCode(json.get("bin_code").getAsString());
        }
        if (json.has("bin_zone_code") && !json.get("bin_zone_code").isJsonNull()) {
            dto.setBinZoneCode(json.get("bin_zone_code").getAsString());
        }
        if (json.has("aisle") && !json.get("aisle").isJsonNull()) {
            dto.setAisle(json.get("aisle").getAsString());
        }
        if (json.has("shelf") && !json.get("shelf").isJsonNull()) {
            dto.setShelf(json.get("shelf").getAsString());
        }
        if (json.has("level") && !json.get("level").isJsonNull()) {
            dto.setLevel(json.get("level").getAsString());
        }
        if (json.has("is_frozen") && !json.get("is_frozen").isJsonNull()) {
            dto.setIsFrozen(json.get("is_frozen").getAsInt() == 1 || json.get("is_frozen").getAsBoolean());
        }
        if (json.has("zone_code") && !json.get("zone_code").isJsonNull()) {
            dto.setZoneCode(json.get("zone_code").getAsString());
        }
        if (json.has("count_date") && !json.get("count_date").isJsonNull()) {
            dto.setCountDate(json.get("count_date").getAsString());
        }
        if (json.has("count_type") && !json.get("count_type").isJsonNull()) {
            dto.setCountType(json.get("count_type").getAsString());
        }
        if (json.has("status") && !json.get("status").isJsonNull()) {
            dto.setStatus(json.get("status").getAsString());
        }
        if (json.has("counted_by") && !json.get("counted_by").isJsonNull()) {
            dto.setCountedBy(json.get("counted_by").getAsString());
        }
        if (json.has("created_by") && !json.get("created_by").isJsonNull()) {
            dto.setCreatedBy(json.get("created_by").getAsString());
        }
        if (json.has("created_date") && !json.get("created_date").isJsonNull()) {
            dto.setCreatedDate(json.get("created_date").getAsString());
        }
        if (json.has("completed_date") && !json.get("completed_date").isJsonNull()) {
            dto.setCompletedDate(json.get("completed_date").getAsString());
        }

        if (json.has("items") && json.get("items").isJsonArray()) {
            List<CycleCountItemDTO> itemsList = new ArrayList<>();
            JsonArray arr = json.getAsJsonArray("items");
            for (JsonElement el : arr) {
                itemsList.add(jsonToCycleCountItemDTO(el.getAsJsonObject()));
            }
            dto.setItems(itemsList);
        }

        return dto;
    }

    private CycleCountItemDTO jsonToCycleCountItemDTO(JsonObject json) {
        CycleCountItemDTO item = new CycleCountItemDTO();
        if (json.has("count_item_id") && !json.get("count_item_id").isJsonNull()) {
            item.setCountItemId(json.get("count_item_id").getAsInt());
        }
        if (json.has("count_id") && !json.get("count_id").isJsonNull()) {
            item.setCountId(json.get("count_id").getAsInt());
        }
        if (json.has("bin_id") && !json.get("bin_id").isJsonNull()) {
            item.setBinId(json.get("bin_id").getAsInt());
        }
        if (json.has("bin_code") && !json.get("bin_code").isJsonNull()) {
            item.setBinCode(json.get("bin_code").getAsString());
        }
        if (json.has("material_id") && !json.get("material_id").isJsonNull()) {
            item.setMaterialId(json.get("material_id").getAsInt());
        }
        if (json.has("material_code") && !json.get("material_code").isJsonNull()) {
            item.setMaterialCode(json.get("material_code").getAsString());
        }
        if (json.has("material_name") && !json.get("material_name").isJsonNull()) {
            item.setMaterialName(json.get("material_name").getAsString());
        }
        if (json.has("base_uom") && !json.get("base_uom").isJsonNull()) {
            item.setBaseUom(json.get("base_uom").getAsString());
        }
        if (json.has("batch_id") && !json.get("batch_id").isJsonNull()) {
            item.setBatchId(json.get("batch_id").getAsInt());
        }
        if (json.has("batch_number") && !json.get("batch_number").isJsonNull()) {
            item.setBatchNumber(json.get("batch_number").getAsString());
        }
        if (json.has("expiry_date") && !json.get("expiry_date").isJsonNull()) {
            item.setExpiryDate(json.get("expiry_date").getAsString());
        }
        if (json.has("system_quantity") && !json.get("system_quantity").isJsonNull()) {
            item.setSystemQuantity(json.get("system_quantity").getAsDouble());
        }
        if (json.has("counted_quantity") && !json.get("counted_quantity").isJsonNull()) {
            item.setCountedQuantity(json.get("counted_quantity").getAsDouble());
        }
        if (json.has("variance_quantity") && !json.get("variance_quantity").isJsonNull()) {
            item.setVarianceQuantity(json.get("variance_quantity").getAsDouble());
        }
        if (json.has("variance_reason") && !json.get("variance_reason").isJsonNull()) {
            item.setVarianceReason(json.get("variance_reason").getAsString());
        }
        if (json.has("adjustment_movement_id") && !json.get("adjustment_movement_id").isJsonNull()) {
            item.setAdjustmentMovementId(json.get("adjustment_movement_id").getAsInt());
        }
        if (json.has("counted_date") && !json.get("counted_date").isJsonNull()) {
            item.setCountedDate(json.get("counted_date").getAsString());
        }
        if (json.has("recount_required") && !json.get("recount_required").isJsonNull()) {
            item.setRecountRequired(json.get("recount_required").getAsInt() == 1 || json.get("recount_required").getAsBoolean());
        }

        return item;
    }
}
