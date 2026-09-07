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
 * Data Access Object for Bin-to-Bin Transfer REST API endpoints.
 * Handles material search, source/dest bin lookups, and posting transfers.
 *
 * @author Ishani
 */
public class BinToBinTransferDAO {

    private static volatile BinToBinTransferDAO instance;
    private final ApiClient apiClient;

    private BinToBinTransferDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static BinToBinTransferDAO getInstance() {
        if (instance == null) {
            synchronized (BinToBinTransferDAO.class) {
                if (instance == null) {
                    instance = new BinToBinTransferDAO();
                }
            }
        }
        return instance;
    }

    // -- inner POJOs --

    // material found in a warehouse's inventory
    public static class MaterialSearchResult {
        private Integer materialId;
        private String materialCode;
        private String materialDescription;
        private String baseUom;
        private Boolean isBatchManaged;
        private Double totalAvailableQty;

        public Integer getMaterialId() { return materialId; }
        public void setMaterialId(Integer materialId) { this.materialId = materialId; }
        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
        public String getMaterialDescription() { return materialDescription; }
        public void setMaterialDescription(String materialDescription) { this.materialDescription = materialDescription; }
        public String getBaseUom() { return baseUom; }
        public void setBaseUom(String baseUom) { this.baseUom = baseUom; }
        public Boolean getIsBatchManaged() { return isBatchManaged; }
        public void setIsBatchManaged(Boolean isBatchManaged) { this.isBatchManaged = isBatchManaged; }
        public Double getTotalAvailableQty() { return totalAvailableQty; }
        public void setTotalAvailableQty(Double totalAvailableQty) { this.totalAvailableQty = totalAvailableQty; }

        @Override
        public String toString() {
            return materialCode + " - " + materialDescription;
        }
    }

    // source bin that has stock of a material
    public static class SourceBinInfo {
        private Integer binId;
        private String binCode;
        private String zoneCode;
        private String binType;
        private Boolean isFrozen;
        private Double quantity;
        private Double committedQty;
        private Double availableQty;
        private Integer batchId;
        private String batchNumber;
        private String expiryDate;
        private String batchStatus;

        public Integer getBinId() { return binId; }
        public void setBinId(Integer binId) { this.binId = binId; }
        public String getBinCode() { return binCode; }
        public void setBinCode(String binCode) { this.binCode = binCode; }
        public String getZoneCode() { return zoneCode; }
        public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }
        public String getBinType() { return binType; }
        public void setBinType(String binType) { this.binType = binType; }
        public Boolean getIsFrozen() { return isFrozen; }
        public void setIsFrozen(Boolean isFrozen) { this.isFrozen = isFrozen; }
        public Double getQuantity() { return quantity; }
        public void setQuantity(Double quantity) { this.quantity = quantity; }
        public Double getCommittedQty() { return committedQty; }
        public void setCommittedQty(Double committedQty) { this.committedQty = committedQty; }
        public Double getAvailableQty() { return availableQty; }
        public void setAvailableQty(Double availableQty) { this.availableQty = availableQty; }
        public Integer getBatchId() { return batchId; }
        public void setBatchId(Integer batchId) { this.batchId = batchId; }
        public String getBatchNumber() { return batchNumber; }
        public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
        public String getExpiryDate() { return expiryDate; }
        public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }
        public String getBatchStatus() { return batchStatus; }
        public void setBatchStatus(String batchStatus) { this.batchStatus = batchStatus; }

        @Override
        public String toString() {
            String label = binCode;
            if (batchNumber != null && !batchNumber.isEmpty()) {
                label += " [" + batchNumber + "]";
            }
            label += " (Avail: " + String.format("%.0f", availableQty) + ")";
            return label;
        }
    }

    // destination bin with capacity info
    public static class DestBinInfo {
        private Integer binId;
        private String binCode;
        private String zoneCode;
        private String binType;
        private Double maxCapacity;
        private Double usedCapacity;

        public Integer getBinId() { return binId; }
        public void setBinId(Integer binId) { this.binId = binId; }
        public String getBinCode() { return binCode; }
        public void setBinCode(String binCode) { this.binCode = binCode; }
        public String getZoneCode() { return zoneCode; }
        public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }
        public String getBinType() { return binType; }
        public void setBinType(String binType) { this.binType = binType; }
        public Double getMaxCapacity() { return maxCapacity; }
        public void setMaxCapacity(Double maxCapacity) { this.maxCapacity = maxCapacity; }
        public Double getUsedCapacity() { return usedCapacity; }
        public void setUsedCapacity(Double usedCapacity) { this.usedCapacity = usedCapacity; }

        @Override
        public String toString() {
            return binCode + " [" + zoneCode + "]";
        }
    }

    // single line item for posting a transfer
    public static class BinTransferItem {
        private Integer materialId;
        private String materialCode;
        private String materialDescription;
        private Integer fromBinId;
        private String fromBinCode;
        private Integer toBinId;
        private String toBinCode;
        private Double quantity;
        private String uom;
        private Integer batchId;
        private String batchNumber;
        private String status;

        public Integer getMaterialId() { return materialId; }
        public void setMaterialId(Integer materialId) { this.materialId = materialId; }
        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
        public String getMaterialDescription() { return materialDescription; }
        public void setMaterialDescription(String materialDescription) { this.materialDescription = materialDescription; }
        public Integer getFromBinId() { return fromBinId; }
        public void setFromBinId(Integer fromBinId) { this.fromBinId = fromBinId; }
        public String getFromBinCode() { return fromBinCode; }
        public void setFromBinCode(String fromBinCode) { this.fromBinCode = fromBinCode; }
        public Integer getToBinId() { return toBinId; }
        public void setToBinId(Integer toBinId) { this.toBinId = toBinId; }
        public String getToBinCode() { return toBinCode; }
        public void setToBinCode(String toBinCode) { this.toBinCode = toBinCode; }
        public Double getQuantity() { return quantity; }
        public void setQuantity(Double quantity) { this.quantity = quantity; }
        public String getUom() { return uom; }
        public void setUom(String uom) { this.uom = uom; }
        public Integer getBatchId() { return batchId; }
        public void setBatchId(Integer batchId) { this.batchId = batchId; }
        public String getBatchNumber() { return batchNumber; }
        public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // -- API methods --

    // GET /api/transfer-orders/bin-to-bin/materials?warehouse_id=X&q=searchterm
    public List<MaterialSearchResult> searchMaterialsInWarehouse(int warehouseId, String query) throws Exception {
        String endpoint = "/transfer-orders/bin-to-bin/materials?warehouse_id=" + warehouseId;
        if (query != null && !query.trim().isEmpty()) {
            endpoint += "&q=" + java.net.URLEncoder.encode(query, "UTF-8");
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equals(response.get("status").getAsString())) {
            throw new Exception("Failed to search materials in warehouse.");
        }

        List<MaterialSearchResult> list = new ArrayList<>();
        JsonArray dataArray = response.getAsJsonArray("data");
        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                if (elem.isJsonObject()) {
                    list.add(jsonToMaterialSearchResult(elem.getAsJsonObject()));
                }
            }
        }
        return list;
    }

    // GET /api/transfer-orders/bin-to-bin/source-bins?warehouse_id=X&material_id=Y
    public List<SourceBinInfo> getSourceBinsWithStock(int warehouseId, int materialId) throws Exception {
        String endpoint = "/transfer-orders/bin-to-bin/source-bins?warehouse_id=" + warehouseId + "&material_id=" + materialId;

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equals(response.get("status").getAsString())) {
            throw new Exception("Failed to load source bins.");
        }

        List<SourceBinInfo> list = new ArrayList<>();
        JsonArray dataArray = response.getAsJsonArray("data");
        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                if (elem.isJsonObject()) {
                    list.add(jsonToSourceBinInfo(elem.getAsJsonObject()));
                }
            }
        }
        return list;
    }

    // GET /api/transfer-orders/bin-to-bin/dest-bins?warehouse_id=X
    public List<DestBinInfo> getDestinationBins(int warehouseId) throws Exception {
        String endpoint = "/transfer-orders/bin-to-bin/dest-bins?warehouse_id=" + warehouseId;

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equals(response.get("status").getAsString())) {
            throw new Exception("Failed to load destination bins.");
        }

        List<DestBinInfo> list = new ArrayList<>();
        JsonArray dataArray = response.getAsJsonArray("data");
        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                if (elem.isJsonObject()) {
                    list.add(jsonToDestBinInfo(elem.getAsJsonObject()));
                }
            }
        }
        return list;
    }

    // POST /api/transfer-orders/bin-to-bin
    public String completeBinToBinTransfer(List<BinTransferItem> items, String reason, String remarks) throws Exception {
        JsonObject payload = new JsonObject();
        if (reason != null && !reason.trim().isEmpty()) {
            payload.addProperty("notes", reason + (remarks != null && !remarks.isEmpty() ? " | " + remarks : ""));
        }

        JsonArray itemsArray = new JsonArray();
        for (BinTransferItem item : items) {
            JsonObject obj = new JsonObject();
            obj.addProperty("material_id", item.getMaterialId());
            obj.addProperty("from_bin_id", item.getFromBinId());
            obj.addProperty("to_bin_id", item.getToBinId());
            obj.addProperty("quantity", item.getQuantity());
            obj.addProperty("uom", item.getUom());
            if (item.getBatchId() != null) {
                obj.addProperty("batch_id", item.getBatchId());
            }
            itemsArray.add(obj);
        }
        payload.add("items", itemsArray);

        HttpRequest request = apiClient.authRequest("/transfer-orders/bin-to-bin")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject response = apiClient.executeWithAuth(request);
        if (response != null && "success".equals(response.get("status").getAsString())) {
            JsonObject data = response.getAsJsonObject("data");
            if (data != null && data.has("to_number")) {
                return data.get("to_number").getAsString();
            }
            return "OK";
        }
        throw new Exception("Transfer failed: " + (response != null ? response.toString() : "No response"));
    }

    // -- JSON parsers --

    private MaterialSearchResult jsonToMaterialSearchResult(JsonObject json) {
        MaterialSearchResult r = new MaterialSearchResult();
        if (json.has("material_id") && !json.get("material_id").isJsonNull()) {
            r.setMaterialId(json.get("material_id").getAsInt());
        }
        if (json.has("material_code") && !json.get("material_code").isJsonNull()) {
            r.setMaterialCode(json.get("material_code").getAsString());
        }
        if (json.has("material_description") && !json.get("material_description").isJsonNull()) {
            r.setMaterialDescription(json.get("material_description").getAsString());
        }
        if (json.has("base_uom") && !json.get("base_uom").isJsonNull()) {
            r.setBaseUom(json.get("base_uom").getAsString());
        }
        if (json.has("is_batch_managed") && !json.get("is_batch_managed").isJsonNull()) {
            r.setIsBatchManaged(json.get("is_batch_managed").getAsBoolean());
        }
        if (json.has("total_available_qty") && !json.get("total_available_qty").isJsonNull()) {
            r.setTotalAvailableQty(json.get("total_available_qty").getAsDouble());
        }
        return r;
    }

    private SourceBinInfo jsonToSourceBinInfo(JsonObject json) {
        SourceBinInfo s = new SourceBinInfo();
        if (json.has("bin_id") && !json.get("bin_id").isJsonNull()) {
            s.setBinId(json.get("bin_id").getAsInt());
        }
        if (json.has("bin_code") && !json.get("bin_code").isJsonNull()) {
            s.setBinCode(json.get("bin_code").getAsString());
        }
        if (json.has("zone_code") && !json.get("zone_code").isJsonNull()) {
            s.setZoneCode(json.get("zone_code").getAsString());
        }
        if (json.has("bin_type") && !json.get("bin_type").isJsonNull()) {
            s.setBinType(json.get("bin_type").getAsString());
        }
        if (json.has("is_frozen") && !json.get("is_frozen").isJsonNull()) {
            s.setIsFrozen(json.get("is_frozen").getAsBoolean());
        }
        if (json.has("quantity") && !json.get("quantity").isJsonNull()) {
            s.setQuantity(json.get("quantity").getAsDouble());
        }
        if (json.has("committed_quantity") && !json.get("committed_quantity").isJsonNull()) {
            s.setCommittedQty(json.get("committed_quantity").getAsDouble());
        }
        if (json.has("available_qty") && !json.get("available_qty").isJsonNull()) {
            s.setAvailableQty(json.get("available_qty").getAsDouble());
        }
        if (json.has("batch_id") && !json.get("batch_id").isJsonNull()) {
            s.setBatchId(json.get("batch_id").getAsInt());
        }
        if (json.has("batch_number") && !json.get("batch_number").isJsonNull()) {
            s.setBatchNumber(json.get("batch_number").getAsString());
        }
        if (json.has("expiry_date") && !json.get("expiry_date").isJsonNull()) {
            s.setExpiryDate(json.get("expiry_date").getAsString());
        }
        if (json.has("batch_status") && !json.get("batch_status").isJsonNull()) {
            s.setBatchStatus(json.get("batch_status").getAsString());
        }
        return s;
    }

    private DestBinInfo jsonToDestBinInfo(JsonObject json) {
        DestBinInfo d = new DestBinInfo();
        if (json.has("bin_id") && !json.get("bin_id").isJsonNull()) {
            d.setBinId(json.get("bin_id").getAsInt());
        }
        if (json.has("bin_code") && !json.get("bin_code").isJsonNull()) {
            d.setBinCode(json.get("bin_code").getAsString());
        }
        if (json.has("zone_code") && !json.get("zone_code").isJsonNull()) {
            d.setZoneCode(json.get("zone_code").getAsString());
        }
        if (json.has("bin_type") && !json.get("bin_type").isJsonNull()) {
            d.setBinType(json.get("bin_type").getAsString());
        }
        if (json.has("max_capacity") && !json.get("max_capacity").isJsonNull()) {
            d.setMaxCapacity(json.get("max_capacity").getAsDouble());
        }
        if (json.has("used_capacity") && !json.get("used_capacity").isJsonNull()) {
            d.setUsedCapacity(json.get("used_capacity").getAsDouble());
        }
        return d;
    }
}
