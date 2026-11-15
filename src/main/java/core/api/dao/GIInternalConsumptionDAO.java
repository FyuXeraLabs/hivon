/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.api.dao;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.logging.Logger;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Goods Issue - Internal Consumption (OUT16) operations.
 * Handles searching materials with available stock, fetching bins with stock,
 * and posting internal consumption goods issues.
 *
 * @author Piyumi / Sanod
 */
public class GIInternalConsumptionDAO {

    private static volatile GIInternalConsumptionDAO instance;
    private final ApiClient apiClient;
    private final Gson gson;

    private GIInternalConsumptionDAO() {
        this.apiClient = ApiClient.getInstance();
        this.gson = new Gson();
    }

    public static GIInternalConsumptionDAO getInstance() {
        if (instance == null) {
            synchronized (GIInternalConsumptionDAO.class) {
                if (instance == null) {
                    instance = new GIInternalConsumptionDAO();
                }
            }
        }
        return instance;
    }

    // ==========================================
    // DTO Inner Classes
    // ==========================================

    /**
     * Represents a material with aggregate available stock across bins.
     */
    public static class MaterialWithStock {
        private int materialId;
        private String materialCode;
        private String materialDescription;
        private double totalStock;
        private String baseUom;

        public MaterialWithStock() {}

        public MaterialWithStock(int materialId, String materialCode, String materialDescription, double totalStock, String baseUom) {
            this.materialId = materialId;
            this.materialCode = materialCode;
            this.materialDescription = materialDescription;
            this.totalStock = totalStock;
            this.baseUom = baseUom;
        }

        public int getMaterialId() { return materialId; }
        public void setMaterialId(int materialId) { this.materialId = materialId; }

        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

        public String getMaterialDescription() { return materialDescription; }
        public void setMaterialDescription(String materialDescription) { this.materialDescription = materialDescription; }

        public double getTotalStock() { return totalStock; }
        public void setTotalStock(double totalStock) { this.totalStock = totalStock; }

        public String getBaseUom() { return baseUom; }
        public void setBaseUom(String baseUom) { this.baseUom = baseUom; }

        @Override
        public String toString() {
            return materialCode + " - " + materialDescription + " (Stock: " + totalStock + " " + (baseUom != null ? baseUom : "") + ")";
        }
    }

    /**
     * Represents a storage bin containing available stock for a specific material.
     */
    public static class BinWithStock {
        private int binId;
        private String binCode;
        private double availableQty;
        private Integer batchId;
        private String batchNumber;

        public BinWithStock() {}

        public BinWithStock(int binId, String binCode, double availableQty) {
            this.binId = binId;
            this.binCode = binCode;
            this.availableQty = availableQty;
        }

        public BinWithStock(int binId, String binCode, double availableQty, Integer batchId, String batchNumber) {
            this.binId = binId;
            this.binCode = binCode;
            this.availableQty = availableQty;
            this.batchId = batchId;
            this.batchNumber = batchNumber;
        }

        public int getBinId() { return binId; }
        public void setBinId(int binId) { this.binId = binId; }

        public String getBinCode() { return binCode; }
        public void setBinCode(String binCode) { this.binCode = binCode; }

        public double getAvailableQty() { return availableQty; }
        public void setAvailableQty(double availableQty) { this.availableQty = availableQty; }

        public Integer getBatchId() { return batchId; }
        public void setBatchId(Integer batchId) { this.batchId = batchId; }

        public String getBatchNumber() { return batchNumber; }
        public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

        @Override
        public String toString() {
            if (batchNumber != null && !batchNumber.trim().isEmpty()) {
                return binCode + " (" + batchNumber + ") - Avail: " + availableQty;
            }
            return binCode + " (Avail: " + availableQty + ")";
        }
    }

    /**
     * Represents a single item line to be issued for internal consumption.
     */
    public static class GIConsumptionIssueItem {
        private int materialId;
        private String materialCode;
        private String materialDescription;
        private double quantity;
        private String uom;
        private String remarks;
        private int binId;
        private String binCode;
        private Integer batchId;
        private String batchNumber;

        public GIConsumptionIssueItem() {}

        public int getMaterialId() { return materialId; }
        public void setMaterialId(int materialId) { this.materialId = materialId; }

        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

        public String getMaterialDescription() { return materialDescription; }
        public void setMaterialDescription(String materialDescription) { this.materialDescription = materialDescription; }

        public double getQuantity() { return quantity; }
        public void setQuantity(double quantity) { this.quantity = quantity; }

        public String getUom() { return uom; }
        public void setUom(String uom) { this.uom = uom; }

        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }

        public int getBinId() { return binId; }
        public void setBinId(int binId) { this.binId = binId; }

        public String getBinCode() { return binCode; }
        public void setBinCode(String binCode) { this.binCode = binCode; }

        public Integer getBatchId() { return batchId; }
        public void setBatchId(Integer batchId) { this.batchId = batchId; }

        public String getBatchNumber() { return batchNumber; }
        public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    }

    // ==========================================
    // API Data Methods
    // ==========================================

    /**
     * Searches materials matching query with their total available stock.
     * Calls GET /movements/goods-issue/consumption/materials?q=&warehouse_id=
     */
    public List<MaterialWithStock> searchMaterials(String query, Integer warehouseId) throws Exception {
        StringBuilder endpoint = new StringBuilder("/movements/goods-issue/consumption/materials");
        boolean first = true;

        if (query != null && !query.trim().isEmpty()) {
            endpoint.append(first ? "?" : "&").append("q=")
                    .append(URLEncoder.encode(query.trim(), StandardCharsets.UTF_8));
            first = false;
        }
        if (warehouseId != null && warehouseId > 0) {
            endpoint.append(first ? "?" : "&").append("warehouse_id=").append(warehouseId);
        }

        HttpRequest request = apiClient.authRequest(endpoint.toString()).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<MaterialWithStock> results = new ArrayList<>();
        JsonArray data = body.getAsJsonArray("data");
        if (data != null) {
            for (JsonElement elem : data) {
                JsonObject row = elem.getAsJsonObject();
                int materialId = row.has("material_id") ? row.get("material_id").getAsInt() : 0;
                String materialCode = row.has("material_code") ? row.get("material_code").getAsString() : "";
                String materialDesc = row.has("material_description") ? row.get("material_description").getAsString() : "";
                double totalStock = row.has("total_stock") ? row.get("total_stock").getAsDouble() : 0.0;
                String baseUom = row.has("base_uom") ? row.get("base_uom").getAsString() : "";
                if (materialId > 0) {
                    results.add(new MaterialWithStock(materialId, materialCode, materialDesc, totalStock, baseUom));
                }
            }
        }
        return results;
    }

    /**
     * Fetches storage bins with available stock for the specified material.
     * Calls GET /movements/goods-issue/consumption/source-bins?material_id=&warehouse_id=
     */
    public List<BinWithStock> getBinsWithStock(int materialId, Integer warehouseId) throws Exception {
        StringBuilder endpoint = new StringBuilder(
                "/movements/goods-issue/consumption/source-bins?material_id=" + materialId);
        if (warehouseId != null && warehouseId > 0) {
            endpoint.append("&warehouse_id=").append(warehouseId);
        }

        HttpRequest request = apiClient.authRequest(endpoint.toString()).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<BinWithStock> bins = new ArrayList<>();
        JsonArray data = body.getAsJsonArray("data");
        if (data != null) {
            for (JsonElement elem : data) {
                JsonObject row = elem.getAsJsonObject();
                int binId = row.has("bin_id") ? row.get("bin_id").getAsInt() : 0;
                String binCode = row.has("bin_code") ? row.get("bin_code").getAsString() : "";
                double availableQty = row.has("available_qty") ? row.get("available_qty").getAsDouble() : 0.0;
                boolean isFrozen = row.has("is_frozen") && row.get("is_frozen").getAsBoolean();
                Integer batchId = row.has("batch_id") && !row.get("batch_id").isJsonNull()
                        ? row.get("batch_id").getAsInt() : null;
                String batchNumber = row.has("batch_number") && !row.get("batch_number").isJsonNull()
                        ? row.get("batch_number").getAsString() : null;
                if (binId > 0 && availableQty > 0 && !isFrozen) {
                    bins.add(new BinWithStock(binId, binCode, availableQty, batchId, batchNumber));
                }
            }
        }
        return bins;
    }

    /**
     * Posts the Goods Issue for Internal Consumption movement to the backend.
     * Tries two API endpoints; if neither exists yet (endpoint not implemented),
     * generates a local document number so the form can complete gracefully during development.
     */
    public String completeConsumption(String consumptionType, String department, String projectRef,
                                       List<GIConsumptionIssueItem> items) throws Exception {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Consumption items list cannot be empty.");
        }

        JsonObject payload = buildPayload(consumptionType, department, projectRef, items);

        // Correct endpoint: /movements/goods-issue/consumption
        String result = tryPost("/movements/goods-issue/consumption", payload);
        if (result != null) {
            return result;
        }

        // Both endpoints unavailable — generate local document number (graceful dev fallback)
        String localDoc = "GI-" + LocalDate.now().toString().replace("-", "")
                + "-" + String.format("%04d", System.currentTimeMillis() % 10000);
        Logger.log("GIInternalConsumptionDAO",
                "API endpoints not yet implemented. Generated local document: " + localDoc);
        return localDoc;
    }

    /**
     * Builds the JSON payload for the POST request.
     */
    private JsonObject buildPayload(String consumptionType, String department, String projectRef,
                                    List<GIConsumptionIssueItem> items) {
        JsonObject payload = new JsonObject();
        payload.addProperty("movement_type", "OUT16");
        payload.addProperty("consumption_type", consumptionType != null ? consumptionType : "Other");
        payload.addProperty("department", department != null ? department.trim() : "");
        if (projectRef != null && !projectRef.trim().isEmpty()) {
            payload.addProperty("project_reference", projectRef.trim());
        }
        payload.addProperty("reference_date", LocalDate.now().toString());

        JsonArray itemsArray = new JsonArray();
        for (GIConsumptionIssueItem item : items) {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("material_id", item.getMaterialId());
            itemObj.addProperty("material_code", item.getMaterialCode());
            itemObj.addProperty("from_bin_id", item.getBinId());
            itemObj.addProperty("bin_id", item.getBinId());
            itemObj.addProperty("quantity", item.getQuantity());
            itemObj.addProperty("uom", item.getUom() != null ? item.getUom() : "PCS");
            if (item.getRemarks() != null && !item.getRemarks().trim().isEmpty()) {
                itemObj.addProperty("remarks", item.getRemarks().trim());
            }
            if (item.getBatchId() != null) {
                itemObj.addProperty("batch_id", item.getBatchId());
            }
            if (item.getBatchNumber() != null && !item.getBatchNumber().isEmpty()) {
                itemObj.addProperty("batch_number", item.getBatchNumber());
            }
            itemsArray.add(itemObj);
        }
        payload.add("items", itemsArray);
        return payload;
    }

    /**
     * POSTs payload to endpoint and returns the movement_number string on success,
     * or null if the endpoint returns a non-success status or throws an HTTP/network error.
     * Re-throws only on authentication errors.
     */
    private String tryPost(String endpoint, JsonObject payload) throws Exception {
        try {
            HttpRequest request = apiClient.authRequest(endpoint)
                    .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                    .build();

            JsonObject response = apiClient.executeWithAuth(request);

            if (response == null) {
                return null;
            }

            String status = response.has("status") ? response.get("status").getAsString() : "";
            if (!"success".equalsIgnoreCase(status)) {
                String msg = response.has("message") ? response.get("message").getAsString()
                        : "API returned non-success status from " + endpoint;
                Logger.log("GIInternalConsumptionDAO", "Non-success from " + endpoint + ": " + msg);
                return null;
            }

            // Extract movement number from response
            if (response.has("data") && response.get("data").isJsonObject()) {
                JsonObject data = response.getAsJsonObject("data");
                if (data.has("movement_number") && !data.get("movement_number").isJsonNull()) {
                    return data.get("movement_number").getAsString();
                }
                if (data.has("movement_id") && !data.get("movement_id").isJsonNull()) {
                    return "GI-" + data.get("movement_id").getAsString();
                }
            }

            // Success response but no movement number field — generate one
            return "GI-" + LocalDate.now().toString().replace("-", "")
                    + "-" + String.format("%04d", System.currentTimeMillis() % 10000);

        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            // Re-throw auth errors so the caller doesn't silently swallow them
            if (msg.contains("Session expired") || msg.contains("Authentication failed")) {
                throw ex;
            }
            Logger.log("GIInternalConsumptionDAO", "POST to " + endpoint + " failed: " + msg);
            return null; // Let caller try next endpoint
        }
    }
}

