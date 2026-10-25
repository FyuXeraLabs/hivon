package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.api.ApiConfig;
import core.logging.Logger;
import models.dto.StorageBinDTO;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Putaway Transfer Order REST API endpoints.
 * Handles fetching receiving bins, available materials in bins, bin auto-suggestions, and creating Putaway TOs.
 *
 * @author Piyumi
 */
public class PutawayTODAO {

    private static volatile PutawayTODAO instance;
    private final ApiClient apiClient;

    private PutawayTODAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static PutawayTODAO getInstance() {
        if (instance == null) {
            synchronized (PutawayTODAO.class) {
                if (instance == null) {
                    instance = new PutawayTODAO();
                }
            }
        }
        return instance;
    }

    // Material item present in a receiving bin
    public static class PutawayMaterialDTO {
        private Integer inventoryId;
        private Integer materialId;
        private String materialCode;
        private String materialName;
        private String baseUom;
        private Integer batchId;
        private String batchNumber;
        private String expiryDate;
        private double quantity;
        private double committedQuantity;
        private double availableQuantity;

        public Integer getInventoryId() { return inventoryId; }
        public void setInventoryId(Integer inventoryId) { this.inventoryId = inventoryId; }

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

        public double getQuantity() { return quantity; }
        public void setQuantity(double quantity) { this.quantity = quantity; }

        public double getCommittedQuantity() { return committedQuantity; }
        public void setCommittedQuantity(double committedQuantity) { this.committedQuantity = committedQuantity; }

        public double getAvailableQuantity() { return availableQuantity; }
        public void setAvailableQuantity(double availableQuantity) { this.availableQuantity = availableQuantity; }
    }

    // Bin suggestion result with fitness score
    public static class PutawayBinSuggestionDTO {
        private Integer binId;
        private String binCode;
        private String zoneCode;
        private String binType;
        private double maxCapacity;
        private double currentCapacity;
        private double availableCapacity;
        private double fitnessScore;

        public Integer getBinId() { return binId; }
        public void setBinId(Integer binId) { this.binId = binId; }

        public String getBinCode() { return binCode; }
        public void setBinCode(String binCode) { this.binCode = binCode; }

        public String getZoneCode() { return zoneCode; }
        public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }

        public String getBinType() { return binType; }
        public void setBinType(String binType) { this.binType = binType; }

        public double getMaxCapacity() { return maxCapacity; }
        public void setMaxCapacity(double maxCapacity) { this.maxCapacity = maxCapacity; }

        public double getCurrentCapacity() { return currentCapacity; }
        public void setCurrentCapacity(double currentCapacity) { this.currentCapacity = currentCapacity; }

        public double getAvailableCapacity() { return availableCapacity; }
        public void setAvailableCapacity(double availableCapacity) { this.availableCapacity = availableCapacity; }

        public double getFitnessScore() { return fitnessScore; }
        public void setFitnessScore(double fitnessScore) { this.fitnessScore = fitnessScore; }

        @Override
        public String toString() {
            return binCode;
        }
    }

    // Item line planned for putaway TO creation
    public static class PutawayTOItem {
        private Integer materialId;
        private String materialCode;
        private String materialName;
        private Integer batchId;
        private String batchNumber;
        private Integer fromBinId;
        private String fromBinCode;
        private Integer toBinId;
        private String toBinCode;
        private double requiredQuantity;
        private String uom;
        private int sequence;

        public Integer getMaterialId() { return materialId; }
        public void setMaterialId(Integer materialId) { this.materialId = materialId; }

        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

        public String getMaterialName() { return materialName; }
        public void setMaterialName(String materialName) { this.materialName = materialName; }

        public Integer getBatchId() { return batchId; }
        public void setBatchId(Integer batchId) { this.batchId = batchId; }

        public String getBatchNumber() { return batchNumber; }
        public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

        public Integer getFromBinId() { return fromBinId; }
        public void setFromBinId(Integer fromBinId) { this.fromBinId = fromBinId; }

        public String getFromBinCode() { return fromBinCode; }
        public void setFromBinCode(String fromBinCode) { this.fromBinCode = fromBinCode; }

        public Integer getToBinId() { return toBinId; }
        public void setToBinId(Integer toBinId) { this.toBinId = toBinId; }

        public String getToBinCode() { return toBinCode; }
        public void setToBinCode(String toBinCode) { this.toBinCode = toBinCode; }

        public double getRequiredQuantity() { return requiredQuantity; }
        public void setRequiredQuantity(double requiredQuantity) { this.requiredQuantity = requiredQuantity; }

        public String getUom() { return uom; }
        public void setUom(String uom) { this.uom = uom; }

        public int getSequence() { return sequence; }
        public void setSequence(int sequence) { this.sequence = sequence; }
    }

    // GET /api/movements/goods-receipt/po/receiving-bins?warehouse_id={warehouseId}
    public List<StorageBinDTO> getReceivingBins(Integer warehouseId) throws Exception {
        String endpoint = "/movements/goods-receipt/po/receiving-bins";
        if (warehouseId != null) {
            endpoint += "?warehouse_id=" + warehouseId;
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equals(response.get("status").getAsString())) {
            throw new Exception("Failed to load receiving bins.");
        }

        List<StorageBinDTO> bins = new ArrayList<>();
        JsonArray dataArray = response.getAsJsonArray("data");
        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                if (elem.isJsonObject()) {
                    bins.add(jsonToStorageBinDTO(elem.getAsJsonObject()));
                }
            }
        }
        return bins;
    }

    // GET /api/bins
    public List<StorageBinDTO> getStorageBins(Integer warehouseId) throws Exception {
        String endpoint = ApiConfig.BINS;
        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equals(response.get("status").getAsString())) {
            throw new Exception("Failed to load storage bins.");
        }

        List<StorageBinDTO> bins = new ArrayList<>();
        JsonArray dataArray = response.getAsJsonArray("data");
        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                if (elem.isJsonObject()) {
                    StorageBinDTO bin = jsonToStorageBinDTO(elem.getAsJsonObject());
                    if (warehouseId == null || warehouseId.equals(bin.getWarehouseId())) {
                        if ("STORAGE".equalsIgnoreCase(bin.getBinType())) {
                            bins.add(bin);
                        }
                    }
                }
            }
        }
        return bins;
    }

    // GET /api/transfer-orders/putaway/materials-in-bin?bin_id={binId}
    public List<PutawayMaterialDTO> getMaterialsInBin(int binId) throws Exception {
        String endpoint = "/transfer-orders/putaway/materials-in-bin?bin_id=" + binId;
        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equals(response.get("status").getAsString())) {
            throw new Exception("Failed to query materials in receiving bin.");
        }

        List<PutawayMaterialDTO> list = new ArrayList<>();
        JsonArray dataArray = response.getAsJsonArray("data");
        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                if (elem.isJsonObject()) {
                    list.add(jsonToPutawayMaterialDTO(elem.getAsJsonObject()));
                }
            }
        }
        return list;
    }

    // GET /api/transfer-orders/putaway/suggest-bins?material_id={materialId}&quantity={qty}&warehouse_id={warehouseId}
    public List<PutawayBinSuggestionDTO> suggestBins(int materialId, double qty, int warehouseId) throws Exception {
        String endpoint = "/transfer-orders/putaway/suggest-bins?material_id=" + materialId 
                + "&quantity=" + qty + "&warehouse_id=" + warehouseId;
        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equals(response.get("status").getAsString())) {
            String msg = (response != null && response.has("message")) ? response.get("message").getAsString() : "Failed to get bin suggestions.";
            throw new Exception(msg);
        }

        List<PutawayBinSuggestionDTO> list = new ArrayList<>();
        JsonArray dataArray = response.getAsJsonArray("data");
        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                if (elem.isJsonObject()) {
                    list.add(jsonToBinSuggestionDTO(elem.getAsJsonObject()));
                }
            }
        }
        return list;
    }

    // POST /api/transfer-orders/putaway
    public JsonObject createPutawayTransferOrder(int fromBinId, List<PutawayTOItem> items, String notes) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("from_bin_id", fromBinId);

        JsonArray itemsArray = new JsonArray();
        for (PutawayTOItem item : items) {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("material_id", item.getMaterialId());
            if (item.getBatchId() != null) {
                itemObj.addProperty("batch_id", item.getBatchId());
            }
            if (item.getBatchNumber() != null && !item.getBatchNumber().isEmpty()) {
                itemObj.addProperty("batch_number", item.getBatchNumber());
            }
            itemObj.addProperty("to_bin_id", item.getToBinId());
            if (item.getToBinCode() != null) {
                itemObj.addProperty("to_bin_code", item.getToBinCode());
            }
            itemObj.addProperty("required_quantity", item.getRequiredQuantity());
            itemObj.addProperty("uom", item.getUom());
            itemsArray.add(itemObj);
        }
        payload.add("items", itemsArray);

        HttpRequest request = apiClient.authRequest("/transfer-orders/putaway")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject response = apiClient.executeWithAuth(request);
        if (response == null || !"success".equals(response.get("status").getAsString())) {
            String msg = (response != null && response.has("message")) ? response.get("message").getAsString() : "Failed to create Putaway TO.";
            throw new Exception(msg);
        }

        return response.getAsJsonObject("data");
    }

    private StorageBinDTO jsonToStorageBinDTO(JsonObject json) {
        StorageBinDTO dto = new StorageBinDTO();
        if (json.has("bin_id") && !json.get("bin_id").isJsonNull()) {
            dto.setBinId(json.get("bin_id").getAsInt());
        }
        if (json.has("warehouse_id") && !json.get("warehouse_id").isJsonNull()) {
            dto.setWarehouseId(json.get("warehouse_id").getAsInt());
        }
        if (json.has("bin_code") && !json.get("bin_code").isJsonNull()) {
            dto.setBinCode(json.get("bin_code").getAsString());
        }
        if (json.has("bin_description") && !json.get("bin_description").isJsonNull()) {
            dto.setBinDescription(json.get("bin_description").getAsString());
        }
        if (json.has("zone_code") && !json.get("zone_code").isJsonNull()) {
            dto.setZoneCode(json.get("zone_code").getAsString());
        }
        if (json.has("bin_type") && !json.get("bin_type").isJsonNull()) {
            dto.setBinType(json.get("bin_type").getAsString());
        }
        if (json.has("is_frozen") && !json.get("is_frozen").isJsonNull()) {
            dto.setIsFrozen(json.get("is_frozen").getAsBoolean());
        }
        return dto;
    }

    private PutawayMaterialDTO jsonToPutawayMaterialDTO(JsonObject json) {
        PutawayMaterialDTO dto = new PutawayMaterialDTO();
        if (json.has("inventory_id") && !json.get("inventory_id").isJsonNull()) {
            dto.setInventoryId(json.get("inventory_id").getAsInt());
        }
        if (json.has("material_id") && !json.get("material_id").isJsonNull()) {
            dto.setMaterialId(json.get("material_id").getAsInt());
        }
        if (json.has("material_code") && !json.get("material_code").isJsonNull()) {
            dto.setMaterialCode(json.get("material_code").getAsString());
        }
        if (json.has("material_name") && !json.get("material_name").isJsonNull()) {
            dto.setMaterialName(json.get("material_name").getAsString());
        }
        if (json.has("base_uom") && !json.get("base_uom").isJsonNull()) {
            dto.setBaseUom(json.get("base_uom").getAsString());
        }
        if (json.has("batch_id") && !json.get("batch_id").isJsonNull()) {
            dto.setBatchId(json.get("batch_id").getAsInt());
        }
        if (json.has("batch_number") && !json.get("batch_number").isJsonNull()) {
            dto.setBatchNumber(json.get("batch_number").getAsString());
        }
        if (json.has("expiry_date") && !json.get("expiry_date").isJsonNull()) {
            dto.setExpiryDate(json.get("expiry_date").getAsString());
        }
        if (json.has("quantity") && !json.get("quantity").isJsonNull()) {
            dto.setQuantity(json.get("quantity").getAsDouble());
        }
        if (json.has("committed_quantity") && !json.get("committed_quantity").isJsonNull()) {
            dto.setCommittedQuantity(json.get("committed_quantity").getAsDouble());
        }
        if (json.has("available_quantity") && !json.get("available_quantity").isJsonNull()) {
            dto.setAvailableQuantity(json.get("available_quantity").getAsDouble());
        } else {
            dto.setAvailableQuantity(dto.getQuantity() - dto.getCommittedQuantity());
        }
        return dto;
    }

    private PutawayBinSuggestionDTO jsonToBinSuggestionDTO(JsonObject json) {
        PutawayBinSuggestionDTO dto = new PutawayBinSuggestionDTO();
        if (json.has("bin_id") && !json.get("bin_id").isJsonNull()) {
            dto.setBinId(json.get("bin_id").getAsInt());
        }
        if (json.has("bin_code") && !json.get("bin_code").isJsonNull()) {
            dto.setBinCode(json.get("bin_code").getAsString());
        }
        if (json.has("zone_code") && !json.get("zone_code").isJsonNull()) {
            dto.setZoneCode(json.get("zone_code").getAsString());
        }
        if (json.has("bin_type") && !json.get("bin_type").isJsonNull()) {
            dto.setBinType(json.get("bin_type").getAsString());
        }
        if (json.has("max_capacity") && !json.get("max_capacity").isJsonNull()) {
            dto.setMaxCapacity(json.get("max_capacity").getAsDouble());
        }
        if (json.has("current_capacity") && !json.get("current_capacity").isJsonNull()) {
            dto.setCurrentCapacity(json.get("current_capacity").getAsDouble());
        }
        if (json.has("available_capacity") && !json.get("available_capacity").isJsonNull()) {
            dto.setAvailableCapacity(json.get("available_capacity").getAsDouble());
        }
        if (json.has("fitness_score") && !json.get("fitness_score").isJsonNull()) {
            dto.setFitnessScore(json.get("fitness_score").getAsDouble());
        }
        return dto;
    }
}
