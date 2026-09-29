package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.logging.Logger;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Cycle Count REST API endpoints.
 * Handles creating count plans, recording physical counts, and completing counts.
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

    // -- inner POJOs --

    // header info for a cycle count plan
    public static class CycleCountPlan {
        private Integer countId;
        private String countNumber;
        private Integer warehouseId;
        private Integer binId;
        private String zoneCode;
        private String countDate;
        private String countType;
        private String status;
        private String countedBy;
        private String createdBy;
        private String createdDate;
        private String completedDate;

        public Integer getCountId() { return countId; }
        public void setCountId(Integer countId) { this.countId = countId; }
        public String getCountNumber() { return countNumber; }
        public void setCountNumber(String countNumber) { this.countNumber = countNumber; }
        public Integer getWarehouseId() { return warehouseId; }
        public void setWarehouseId(Integer warehouseId) { this.warehouseId = warehouseId; }
        public Integer getBinId() { return binId; }
        public void setBinId(Integer binId) { this.binId = binId; }
        public String getZoneCode() { return zoneCode; }
        public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }
        public String getCountDate() { return countDate; }
        public void setCountDate(String countDate) { this.countDate = countDate; }
        public String getCountType() { return countType; }
        public void setCountType(String countType) { this.countType = countType; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getCountedBy() { return countedBy; }
        public void setCountedBy(String countedBy) { this.countedBy = countedBy; }
        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
        public String getCreatedDate() { return createdDate; }
        public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }
        public String getCompletedDate() { return completedDate; }
        public void setCompletedDate(String completedDate) { this.completedDate = completedDate; }

        @Override
        public String toString() {
            return countNumber + " [" + status + "]";
        }
    }

    // single item in a cycle count
    public static class CycleCountItem {
        private Integer countItemId;
        private Integer countId;
        private Integer binId;
        private Integer materialId;
        private String materialCode;
        private String materialName;
        private String baseUom;
        private Integer batchId;
        private String batchNumber;
        private String expiryDate;
        private Double systemQuantity;
        private Double countedQuantity;
        private Double varianceQuantity;
        private String varianceReason;
        private Integer adjustmentMovementId;
        private String countedDate;
        private Boolean recountRequired;

        public Integer getCountItemId() { return countItemId; }
        public void setCountItemId(Integer countItemId) { this.countItemId = countItemId; }
        public Integer getCountId() { return countId; }
        public void setCountId(Integer countId) { this.countId = countId; }
        public Integer getBinId() { return binId; }
        public void setBinId(Integer binId) { this.binId = binId; }
        public Integer getMaterialId() { return materialId; }
        public void setMaterialId(Integer materialId) { this.materialId = materialId; }
        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
        public String getMaterialName() { return materialName; }
        public void setMaterialName(String materialName) { this.materialName = materialName; }
        public String getBaseUom() { return baseUom; }
        public void setBaseUom(String baseUom) { this.baseUom = baseUom; }
        public Integer getBatchId() { return batchId; }
        public void setBatchId(Integer batchId) { this.batchId = batchId; }
        public String getBatchNumber() { return batchNumber; }
        public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
        public String getExpiryDate() { return expiryDate; }
        public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
        public Double getSystemQuantity() { return systemQuantity; }
        public void setSystemQuantity(Double systemQuantity) { this.systemQuantity = systemQuantity; }
        public Double getCountedQuantity() { return countedQuantity; }
        public void setCountedQuantity(Double countedQuantity) { this.countedQuantity = countedQuantity; }
        public Double getVarianceQuantity() { return varianceQuantity; }
        public void setVarianceQuantity(Double varianceQuantity) { this.varianceQuantity = varianceQuantity; }
        public String getVarianceReason() { return varianceReason; }
        public void setVarianceReason(String varianceReason) { this.varianceReason = varianceReason; }
        public Integer getAdjustmentMovementId() { return adjustmentMovementId; }
        public void setAdjustmentMovementId(Integer adjustmentMovementId) { this.adjustmentMovementId = adjustmentMovementId; }
        public String getCountedDate() { return countedDate; }
        public void setCountedDate(String countedDate) { this.countedDate = countedDate; }
        public Boolean getRecountRequired() { return recountRequired; }
        public void setRecountRequired(Boolean recountRequired) { this.recountRequired = recountRequired; }

        // computed variance percentage
        public Double getVariancePercentage() {
            if (systemQuantity == null || systemQuantity == 0) return null;
            if (varianceQuantity == null) return null;
            return (varianceQuantity / systemQuantity) * 100.0;
        }

        @Override
        public String toString() {
            return materialCode + " - " + materialName;
        }
    }

    // result from creating a new cycle count
    public static class CreateCountResult {
        private Integer countId;
        private String countNumber;

        public Integer getCountId() { return countId; }
        public void setCountId(Integer countId) { this.countId = countId; }
        public String getCountNumber() { return countNumber; }
        public void setCountNumber(String countNumber) { this.countNumber = countNumber; }
    }

    // -- API methods --

    // POST /api/cycle-counts - create a new cycle count plan
    public CreateCountResult createCycleCount(int warehouseId, Integer binId, String zoneCode,
                                               String countDate, String countType) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("warehouse_id", warehouseId);
        if (binId != null) {
            payload.addProperty("bin_id", binId);
        }
        if (zoneCode != null && !zoneCode.isEmpty()) {
            payload.addProperty("zone_code", zoneCode);
        }
        if (countDate != null && !countDate.isEmpty()) {
            payload.addProperty("count_date", countDate);
        }
        if (countType != null && !countType.isEmpty()) {
            payload.addProperty("count_type", countType);
        }

        HttpRequest request = apiClient.authRequest("/cycle-counts")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject response = apiClient.executeWithAuth(request);
        if (response != null && "success".equals(response.get("status").getAsString())) {
            JsonObject data = response.getAsJsonObject("data");
            CreateCountResult result = new CreateCountResult();
            if (data != null) {
                if (data.has("count_id") && !data.get("count_id").isJsonNull()) {
                    result.setCountId(data.get("count_id").getAsInt());
                }
                if (data.has("count_number") && !data.get("count_number").isJsonNull()) {
                    result.setCountNumber(data.get("count_number").getAsString());
                }
            }
            return result;
        }
        String msg = response != null && response.has("message") ? response.get("message").getAsString() : "Unknown error";
        throw new Exception("Failed to create cycle count: " + msg);
    }

    // GET /api/cycle-counts/{id} - get cycle count details with items
    public CycleCountPlan getCycleCountById(int countId) throws Exception {
        HttpRequest request = apiClient.authRequest("/cycle-counts/" + countId).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response != null && "success".equals(response.get("status").getAsString())) {
            JsonObject data = response.getAsJsonObject("data");
            if (data != null) {
                return jsonToCycleCountPlan(data);
            }
        }
        throw new Exception("Cycle count not found.");
    }

    // GET /api/cycle-counts/{id} - get items for a cycle count
    public List<CycleCountItem> getCycleCountItems(int countId) throws Exception {
        HttpRequest request = apiClient.authRequest("/cycle-counts/" + countId).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        List<CycleCountItem> items = new ArrayList<>();
        if (response != null && "success".equals(response.get("status").getAsString())) {
            JsonObject data = response.getAsJsonObject("data");
            if (data != null && data.has("items") && !data.get("items").isJsonNull()) {
                JsonArray itemsArr = data.getAsJsonArray("items");
                for (JsonElement elem : itemsArr) {
                    if (elem.isJsonObject()) {
                        items.add(jsonToCycleCountItem(elem.getAsJsonObject()));
                    }
                }
            }
        }
        return items;
    }

    // PUT /api/cycle-counts/{id}/count - record physical counts
    public void recordCounts(int countId, List<CycleCountItem> items) throws Exception {
        JsonObject payload = new JsonObject();
        JsonArray itemsArray = new JsonArray();

        for (CycleCountItem item : items) {
            JsonObject obj = new JsonObject();
            obj.addProperty("count_item_id", item.getCountItemId());
            obj.addProperty("counted_quantity", item.getCountedQuantity());
            if (item.getVarianceReason() != null && !item.getVarianceReason().isEmpty()) {
                obj.addProperty("variance_reason", item.getVarianceReason());
            }
            itemsArray.add(obj);
        }
        payload.add("items", itemsArray);

        HttpRequest request = apiClient.authRequest("/cycle-counts/" + countId + "/count")
                .PUT(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject response = apiClient.executeWithAuth(request);
        if (response == null || !"success".equals(response.get("status").getAsString())) {
            String msg = response != null && response.has("message") ? response.get("message").getAsString() : "Unknown error";
            throw new Exception("Failed to record counts: " + msg);
        }
    }

    // PUT /api/cycle-counts/{id}/complete - finalize cycle count
    public void completeCycleCount(int countId) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("count_id", countId);

        HttpRequest request = apiClient.authRequest("/cycle-counts/" + countId + "/complete")
                .PUT(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject response = apiClient.executeWithAuth(request);
        if (response == null || !"success".equals(response.get("status").getAsString())) {
            String msg = response != null && response.has("message") ? response.get("message").getAsString() : "Unknown error";
            throw new Exception("Failed to complete cycle count: " + msg);
        }
    }

    // GET /api/cycle-counts - list cycle counts
    public List<CycleCountPlan> listCycleCounts(Integer warehouseId, String status) throws Exception {
        StringBuilder endpoint = new StringBuilder("/cycle-counts?");
        if (warehouseId != null) {
            endpoint.append("warehouse_id=").append(warehouseId).append("&");
        }
        if (status != null && !status.isEmpty()) {
            endpoint.append("status=").append(java.net.URLEncoder.encode(status, "UTF-8")).append("&");
        }

        HttpRequest request = apiClient.authRequest(endpoint.toString()).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        List<CycleCountPlan> plans = new ArrayList<>();
        if (response != null && "success".equals(response.get("status").getAsString())) {
            JsonArray dataArray = response.getAsJsonArray("data");
            if (dataArray != null) {
                for (JsonElement elem : dataArray) {
                    if (elem.isJsonObject()) {
                        plans.add(jsonToCycleCountPlan(elem.getAsJsonObject()));
                    }
                }
            }
        }
        return plans;
    }

    // -- JSON parsers --

    private CycleCountPlan jsonToCycleCountPlan(JsonObject json) {
        CycleCountPlan p = new CycleCountPlan();
        if (json.has("count_id") && !json.get("count_id").isJsonNull()) p.setCountId(json.get("count_id").getAsInt());
        if (json.has("count_number") && !json.get("count_number").isJsonNull()) p.setCountNumber(json.get("count_number").getAsString());
        if (json.has("warehouse_id") && !json.get("warehouse_id").isJsonNull()) p.setWarehouseId(json.get("warehouse_id").getAsInt());
        if (json.has("bin_id") && !json.get("bin_id").isJsonNull()) p.setBinId(json.get("bin_id").getAsInt());
        if (json.has("zone_code") && !json.get("zone_code").isJsonNull()) p.setZoneCode(json.get("zone_code").getAsString());
        if (json.has("count_date") && !json.get("count_date").isJsonNull()) p.setCountDate(json.get("count_date").getAsString());
        if (json.has("count_type") && !json.get("count_type").isJsonNull()) p.setCountType(json.get("count_type").getAsString());
        if (json.has("status") && !json.get("status").isJsonNull()) p.setStatus(json.get("status").getAsString());
        if (json.has("counted_by") && !json.get("counted_by").isJsonNull()) p.setCountedBy(json.get("counted_by").getAsString());
        if (json.has("created_by") && !json.get("created_by").isJsonNull()) p.setCreatedBy(json.get("created_by").getAsString());
        if (json.has("created_date") && !json.get("created_date").isJsonNull()) p.setCreatedDate(json.get("created_date").getAsString());
        if (json.has("completed_date") && !json.get("completed_date").isJsonNull()) p.setCompletedDate(json.get("completed_date").getAsString());
        return p;
    }

    private CycleCountItem jsonToCycleCountItem(JsonObject json) {
        CycleCountItem item = new CycleCountItem();
        if (json.has("count_item_id") && !json.get("count_item_id").isJsonNull()) item.setCountItemId(json.get("count_item_id").getAsInt());
        if (json.has("count_id") && !json.get("count_id").isJsonNull()) item.setCountId(json.get("count_id").getAsInt());
        if (json.has("bin_id") && !json.get("bin_id").isJsonNull()) item.setBinId(json.get("bin_id").getAsInt());
        if (json.has("material_id") && !json.get("material_id").isJsonNull()) item.setMaterialId(json.get("material_id").getAsInt());
        if (json.has("material_code") && !json.get("material_code").isJsonNull()) item.setMaterialCode(json.get("material_code").getAsString());
        if (json.has("material_name") && !json.get("material_name").isJsonNull()) item.setMaterialName(json.get("material_name").getAsString());
        if (json.has("base_uom") && !json.get("base_uom").isJsonNull()) item.setBaseUom(json.get("base_uom").getAsString());
        if (json.has("batch_id") && !json.get("batch_id").isJsonNull()) item.setBatchId(json.get("batch_id").getAsInt());
        if (json.has("batch_number") && !json.get("batch_number").isJsonNull()) item.setBatchNumber(json.get("batch_number").getAsString());
        if (json.has("expiry_date") && !json.get("expiry_date").isJsonNull()) item.setExpiryDate(json.get("expiry_date").getAsString());
        if (json.has("system_quantity") && !json.get("system_quantity").isJsonNull()) item.setSystemQuantity(json.get("system_quantity").getAsDouble());
        if (json.has("counted_quantity") && !json.get("counted_quantity").isJsonNull()) item.setCountedQuantity(json.get("counted_quantity").getAsDouble());
        if (json.has("variance_quantity") && !json.get("variance_quantity").isJsonNull()) item.setVarianceQuantity(json.get("variance_quantity").getAsDouble());
        if (json.has("variance_reason") && !json.get("variance_reason").isJsonNull()) item.setVarianceReason(json.get("variance_reason").getAsString());
        if (json.has("adjustment_movement_id") && !json.get("adjustment_movement_id").isJsonNull()) item.setAdjustmentMovementId(json.get("adjustment_movement_id").getAsInt());
        if (json.has("counted_date") && !json.get("counted_date").isJsonNull()) item.setCountedDate(json.get("counted_date").getAsString());
        if (json.has("recount_required") && !json.get("recount_required").isJsonNull()) {
            item.setRecountRequired(json.get("recount_required").getAsInt() == 1);
        }
        return item;
    }
}
