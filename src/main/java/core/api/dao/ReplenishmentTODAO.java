/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
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
 * Data Access Object for Replenishment Transfer Order REST API endpoints.
 * Handles fetching low-stock replenishment requirements and creating replenishment TOs.
 *
 * @author Sanod
 */
public class ReplenishmentTODAO {

    private static volatile ReplenishmentTODAO instance;
    private final ApiClient apiClient;

    private ReplenishmentTODAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static ReplenishmentTODAO getInstance() {
        if (instance == null) {
            synchronized (ReplenishmentTODAO.class) {
                if (instance == null) {
                    instance = new ReplenishmentTODAO();
                }
            }
        }
        return instance;
    }

    public static class ReplenishSource {
        private int fromBinId;
        private String fromBinCode;
        private Integer batchId;
        private String batchNumber;
        private double quantity;

        public int getFromBinId() { return fromBinId; }
        public void setFromBinId(int fromBinId) { this.fromBinId = fromBinId; }

        public String getFromBinCode() { return fromBinCode; }
        public void setFromBinCode(String fromBinCode) { this.fromBinCode = fromBinCode; }

        public Integer getBatchId() { return batchId; }
        public void setBatchId(Integer batchId) { this.batchId = batchId; }

        public String getBatchNumber() { return batchNumber; }
        public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

        public double getQuantity() { return quantity; }
        public void setQuantity(double quantity) { this.quantity = quantity; }
    }

    public static class ReplenishmentRequirement {
        private int materialId;
        private String materialCode;
        private String materialName;
        private double minStockLevel;
        private double maxStockLevel;
        private double currentPickingQty;
        private double replenishQuantity;
        private int toBinId;
        private String toBinCode;
        private String uom;
        private List<ReplenishSource> sources = new ArrayList<>();

        public int getMaterialId() { return materialId; }
        public void setMaterialId(int materialId) { this.materialId = materialId; }

        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

        public String getMaterialName() { return materialName; }
        public void setMaterialName(String materialName) { this.materialName = materialName; }

        public double getMinStockLevel() { return minStockLevel; }
        public void setMinStockLevel(double minStockLevel) { this.minStockLevel = minStockLevel; }

        public double getMaxStockLevel() { return maxStockLevel; }
        public void setMaxStockLevel(double maxStockLevel) { this.maxStockLevel = maxStockLevel; }

        public double getCurrentPickingQty() { return currentPickingQty; }
        public void setCurrentPickingQty(double currentPickingQty) { this.currentPickingQty = currentPickingQty; }

        public double getReplenishQuantity() { return replenishQuantity; }
        public void setReplenishQuantity(double replenishQuantity) { this.replenishQuantity = replenishQuantity; }

        public int getToBinId() { return toBinId; }
        public void setToBinId(int toBinId) { this.toBinId = toBinId; }

        public String getToBinCode() { return toBinCode; }
        public void setToBinCode(String toBinCode) { this.toBinCode = toBinCode; }

        public String getUom() { return uom; }
        public void setUom(String uom) { this.uom = uom; }

        public List<ReplenishSource> getSources() { return sources; }
        public void setSources(List<ReplenishSource> sources) { this.sources = sources; }
    }

    // GET /api/transfer-orders/replenishment/low-stock?warehouse_id={warehouseId}
    public List<ReplenishmentRequirement> getLowStockReplenishments(int warehouseId) throws Exception {
        String endpoint = "/transfer-orders/replenishment/low-stock?warehouse_id=" + warehouseId;
        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equals(response.get("status").getAsString())) {
            throw new Exception("Failed to fetch low-stock replenishments.");
        }

        List<ReplenishmentRequirement> list = new ArrayList<>();
        JsonArray dataArray = response.getAsJsonArray("data");
        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                if (elem.isJsonObject()) {
                    list.add(jsonToRequirement(elem.getAsJsonObject()));
                }
            }
        }
        return list;
    }

    // POST /api/transfer-orders/replenishment
    public JsonObject createReplenishmentTransferOrder(List<JsonObject> items, String notes) throws Exception {
        JsonObject payload = new JsonObject();
        JsonArray itemsArray = new JsonArray();
        for (JsonObject item : items) {
            itemsArray.add(item);
        }
        payload.add("items", itemsArray);
        if (notes != null && !notes.trim().isEmpty()) {
            payload.addProperty("notes", notes);
        }

        HttpRequest request = apiClient.authRequest("/transfer-orders/replenishment")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject response = apiClient.executeWithAuth(request);
        if (response == null || !"success".equals(response.get("status").getAsString())) {
            String msg = (response != null && response.has("message")) ? response.get("message").getAsString() : "Failed to create Replenishment TO.";
            throw new Exception(msg);
        }
        return response.getAsJsonObject("data");
    }

    private ReplenishmentRequirement jsonToRequirement(JsonObject json) {
        ReplenishmentRequirement req = new ReplenishmentRequirement();
        req.setMaterialId(json.get("material_id").getAsInt());
        req.setMaterialCode(json.get("material_code").getAsString());
        req.setMaterialName(json.get("material_name").getAsString());
        req.setMinStockLevel(json.get("min_stock_level").getAsDouble());
        req.setMaxStockLevel(json.get("max_stock_level").getAsDouble());
        req.setCurrentPickingQty(json.get("current_picking_qty").getAsDouble());
        req.setReplenishQuantity(json.get("replenish_quantity").getAsDouble());
        req.setToBinId(json.get("to_bin_id").getAsInt());
        req.setToBinCode(json.get("to_bin_code").getAsString());
        req.setUom(json.get("uom").getAsString());

        if (json.has("sources") && json.get("sources").isJsonArray()) {
            JsonArray sourcesArray = json.getAsJsonArray("sources");
            List<ReplenishSource> sourcesList = new ArrayList<>();
            for (JsonElement srcElem : sourcesArray) {
                if (srcElem.isJsonObject()) {
                    JsonObject srcJson = srcElem.getAsJsonObject();
                    ReplenishSource src = new ReplenishSource();
                    src.setFromBinId(srcJson.get("from_bin_id").getAsInt());
                    src.setFromBinCode(srcJson.get("from_bin_code").getAsString());
                    if (srcJson.has("batch_id") && !srcJson.get("batch_id").isJsonNull()) {
                        src.setBatchId(srcJson.get("batch_id").getAsInt());
                    }
                    if (srcJson.has("batch_number") && !srcJson.get("batch_number").isJsonNull()) {
                        src.setBatchNumber(srcJson.get("batch_number").getAsString());
                    }
                    src.setQuantity(srcJson.get("quantity").getAsDouble());
                    sourcesList.add(src);
                }
            }
            req.setSources(sourcesList);
        }

        return req;
    }
}
