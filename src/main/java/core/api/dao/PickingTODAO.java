package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.logging.Logger;
import models.dto.StorageBinDTO;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Picking Transfer Order REST API endpoints.
 * Handles fetching sales orders, materials to pick, pick bin auto-suggestions, and creating Picking TOs.
 *
 * @author Navodya
 */
public class PickingTODAO {

    private static volatile PickingTODAO instance;
    private final ApiClient apiClient;

    private PickingTODAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static PickingTODAO getInstance() {
        if (instance == null) {
            synchronized (PickingTODAO.class) {
                if (instance == null) {
                    instance = new PickingTODAO();
                }
            }
        }
        return instance;
    }

    // DTO for Sales Order summary dropdown
    public static class PickingSalesOrderDTO {
        private String soNumber;
        private String customerName;
        private String orderDate;
        private String status;

        public String getSoNumber() { return soNumber; }
        public void setSoNumber(String soNumber) { this.soNumber = soNumber; }

        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }

        public String getOrderDate() { return orderDate; }
        public void setOrderDate(String orderDate) { this.orderDate = orderDate; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        @Override
        public String toString() {
            return soNumber + " (" + customerName + ")";
        }
    }

    // DTO for picking task line items (materials to pick for an SO)
    public static class PickingTaskDTO {
        private Integer soItemId;
        private String soNumber;
        private Integer materialId;
        private String materialCode;
        private String materialName;
        private String baseUom;
        private double orderedQuantity;
        private double shippedQuantity;
        private double outstandingQuantity;

        public Integer getSoItemId() { return soItemId; }
        public void setSoItemId(Integer soItemId) { this.soItemId = soItemId; }

        public String getSoNumber() { return soNumber; }
        public void setSoNumber(String soNumber) { this.soNumber = soNumber; }

        public Integer getMaterialId() { return materialId; }
        public void setMaterialId(Integer materialId) { this.materialId = materialId; }

        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

        public String getMaterialName() { return materialName; }
        public void setMaterialName(String materialName) { this.materialName = materialName; }

        public String getBaseUom() { return baseUom; }
        public void setBaseUom(String baseUom) { this.baseUom = baseUom; }

        public double getOrderedQuantity() { return orderedQuantity; }
        public void setOrderedQuantity(double orderedQuantity) { this.orderedQuantity = orderedQuantity; }

        public double getShippedQuantity() { return shippedQuantity; }
        public void setShippedQuantity(double shippedQuantity) { this.shippedQuantity = shippedQuantity; }

        public double getOutstandingQuantity() { return outstandingQuantity; }
        public void setOutstandingQuantity(double outstandingQuantity) { this.outstandingQuantity = outstandingQuantity; }
    }

    // DTO for pick bin suggestion by FIFO/FEFO
    public static class PickBinSuggestionDTO {
        private Integer binId;
        private String binCode;
        private String zoneCode;
        private String binType;
        private Integer batchId;
        private String batchNumber;
        private String expiryDate;
        private double availableQty;

        public Integer getBinId() { return binId; }
        public void setBinId(Integer binId) { this.binId = binId; }

        public String getBinCode() { return binCode; }
        public void setBinCode(String binCode) { this.binCode = binCode; }

        public String getZoneCode() { return zoneCode; }
        public void setZoneCode(String zoneCode) { this.zoneCode = zoneCode; }

        public String getBinType() { return binType; }
        public void setBinType(String binType) { this.binType = binType; }

        public Integer getBatchId() { return batchId; }
        public void setBatchId(Integer batchId) { this.batchId = batchId; }

        public String getBatchNumber() { return batchNumber; }
        public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

        public String getExpiryDate() { return expiryDate; }
        public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

        public double getAvailableQty() { return availableQty; }
        public void setAvailableQty(double availableQty) { this.availableQty = availableQty; }
    }

    // DTO for item line in picking summary table
    public static class PickingTOItem {
        private Integer materialId;
        private String materialCode;
        private String materialName;
        private Integer batchId;
        private String batchNumber;
        private Integer fromBinId;
        private String fromBinCode;
        private Integer toBinId;
        private String toBinCode;
        private double pickQuantity;
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

        public double getPickQuantity() { return pickQuantity; }
        public void setPickQuantity(double pickQuantity) { this.pickQuantity = pickQuantity; }

        public String getUom() { return uom; }
        public void setUom(String uom) { this.uom = uom; }

        public int getSequence() { return sequence; }
        public void setSequence(int sequence) { this.sequence = sequence; }
    }

    // GET /api/movements/goods-issue/sales-order/search?status=ALL
    public List<PickingSalesOrderDTO> getPendingSalesOrders() throws Exception {
        String endpoint = "/movements/goods-issue/sales-order/search?status=ALL";
        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equals(response.get("status").getAsString())) {
            throw new Exception("Failed to query pending sales orders.");
        }

        List<PickingSalesOrderDTO> list = new ArrayList<>();
        JsonArray dataArray = response.getAsJsonArray("data");
        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                if (elem.isJsonObject()) {
                    JsonObject obj = elem.getAsJsonObject();
                    PickingSalesOrderDTO dto = new PickingSalesOrderDTO();
                    dto.setSoNumber(obj.has("so_number") ? obj.get("so_number").getAsString() : "");
                    dto.setCustomerName(obj.has("customer_name") ? obj.get("customer_name").getAsString() : "");
                    dto.setOrderDate(obj.has("order_date") ? obj.get("order_date").getAsString() : "");
                    dto.setStatus(obj.has("status") ? obj.get("status").getAsString() : "");
                    list.add(dto);
                }
            }
        }
        return list;
    }

    // GET /api/transfer-orders/picking/items-to-pick?so_number={soNumber}
    public List<PickingTaskDTO> getMaterialsToPickForSO(String soNumber) throws Exception {
        String encodedSo = java.net.URLEncoder.encode(soNumber, java.nio.charset.StandardCharsets.UTF_8.name());
        String endpoint = "/transfer-orders/picking/items-to-pick?so_number=" + encodedSo;
        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equals(response.get("status").getAsString())) {
            String msg = (response != null && response.has("message")) ? response.get("message").getAsString() : "Failed to query items to pick.";
            throw new Exception(msg);
        }

        List<PickingTaskDTO> list = new ArrayList<>();
        JsonArray dataArray = response.getAsJsonArray("data");
        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                if (elem.isJsonObject()) {
                    JsonObject obj = elem.getAsJsonObject();
                    PickingTaskDTO dto = new PickingTaskDTO();
                    dto.setSoItemId(obj.has("so_item_id") ? obj.get("so_item_id").getAsInt() : 0);
                    dto.setSoNumber(obj.has("so_number") ? obj.get("so_number").getAsString() : "");
                    dto.setMaterialId(obj.has("material_id") ? obj.get("material_id").getAsInt() : 0);
                    dto.setMaterialCode(obj.has("material_code") ? obj.get("material_code").getAsString() : "");
                    dto.setMaterialName(obj.has("material_name") ? obj.get("material_name").getAsString() : (obj.has("material_description") ? obj.get("material_description").getAsString() : ""));
                    dto.setBaseUom(obj.has("base_uom") ? obj.get("base_uom").getAsString() : (obj.has("uom") ? obj.get("uom").getAsString() : "PCS"));
                    dto.setOrderedQuantity(obj.has("ordered_quantity") ? obj.get("ordered_quantity").getAsDouble() : 0.0);
                    dto.setShippedQuantity(obj.has("shipped_quantity") ? obj.get("shipped_quantity").getAsDouble() : 0.0);
                    dto.setOutstandingQuantity(obj.has("outstanding_quantity") ? obj.get("outstanding_quantity").getAsDouble() : (dto.getOrderedQuantity() - dto.getShippedQuantity()));
                    list.add(dto);
                }
            }
        }
        return list;
    }

    // GET /api/transfer-orders/picking/suggest-bins?material_id={materialId}&quantity={qty}&warehouse_id={warehouseId}
    public List<PickBinSuggestionDTO> suggestPickBins(int materialId, double qty, int warehouseId) throws Exception {
        String endpoint = "/transfer-orders/picking/suggest-bins?material_id=" + materialId 
                + "&quantity=" + qty + "&warehouse_id=" + warehouseId;
        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equals(response.get("status").getAsString())) {
            String msg = (response != null && response.has("message")) ? response.get("message").getAsString() : "Failed to suggest pick bins.";
            throw new Exception(msg);
        }

        List<PickBinSuggestionDTO> list = new ArrayList<>();
        JsonArray dataArray = response.getAsJsonArray("data");
        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                if (elem.isJsonObject()) {
                    JsonObject obj = elem.getAsJsonObject();
                    PickBinSuggestionDTO dto = new PickBinSuggestionDTO();
                    dto.setBinId(obj.has("bin_id") ? obj.get("bin_id").getAsInt() : 0);
                    dto.setBinCode(obj.has("bin_code") ? obj.get("bin_code").getAsString() : "");
                    dto.setZoneCode(obj.has("zone_code") ? obj.get("zone_code").getAsString() : "");
                    dto.setBinType(obj.has("bin_type") ? obj.get("bin_type").getAsString() : "");
                    dto.setBatchId(obj.has("batch_id") && !obj.get("batch_id").isJsonNull() ? obj.get("batch_id").getAsInt() : null);
                    dto.setBatchNumber(obj.has("batch_number") && !obj.get("batch_number").isJsonNull() ? obj.get("batch_number").getAsString() : null);
                    dto.setExpiryDate(obj.has("expiry_date") && !obj.get("expiry_date").isJsonNull() ? obj.get("expiry_date").getAsString() : null);
                    dto.setAvailableQty(obj.has("available_qty") ? obj.get("available_qty").getAsDouble() : 0.0);
                    list.add(dto);
                }
            }
        }
        return list;
    }

    // GET /api/bins?warehouse_id={warehouseId} (fetch active storage/staging/picking bins)
    public List<StorageBinDTO> getStorageBins(Integer warehouseId) throws Exception {
        String endpoint = "/bins" + (warehouseId != null ? "?warehouse_id=" + warehouseId : "");
        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equals(response.get("status").getAsString())) {
            throw new Exception("Failed to query storage bins.");
        }

        List<StorageBinDTO> bins = new ArrayList<>();
        JsonArray dataArray = response.getAsJsonArray("data");
        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                if (elem.isJsonObject()) {
                    JsonObject json = elem.getAsJsonObject();
                    StorageBinDTO dto = new StorageBinDTO();
                    dto.setBinId(json.has("bin_id") ? json.get("bin_id").getAsInt() : 0);
                    dto.setWarehouseId(json.has("warehouse_id") ? json.get("warehouse_id").getAsInt() : 0);
                    dto.setBinCode(json.has("bin_code") ? json.get("bin_code").getAsString() : "");
                    dto.setBinDescription(json.has("bin_description") && !json.get("bin_description").isJsonNull() ? json.get("bin_description").getAsString() : "");
                    dto.setZoneCode(json.has("zone_code") && !json.get("zone_code").isJsonNull() ? json.get("zone_code").getAsString() : "");
                    dto.setBinType(json.has("bin_type") && !json.get("bin_type").isJsonNull() ? json.get("bin_type").getAsString() : "STORAGE");
                    dto.setMaxCapacity(json.has("max_capacity") && !json.get("max_capacity").isJsonNull() ? json.get("max_capacity").getAsDouble() : 0.0);
                    bins.add(dto);
                }
            }
        }
        return bins;
    }

    // POST /api/transfer-orders/picking
    public JsonObject createPickingTransferOrder(String soNumber, int toBinId, List<PickingTOItem> items, String notes) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("so_number", soNumber);
        payload.addProperty("to_bin_id", toBinId);

        JsonArray itemsArray = new JsonArray();
        for (PickingTOItem item : items) {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("material_id", item.getMaterialId());
            if (item.getBatchId() != null) {
                itemObj.addProperty("batch_id", item.getBatchId());
            }
            if (item.getBatchNumber() != null) {
                itemObj.addProperty("batch_number", item.getBatchNumber());
            }
            itemObj.addProperty("from_bin_id", item.getFromBinId());
            itemObj.addProperty("to_bin_id", toBinId);
            itemObj.addProperty("required_quantity", item.getPickQuantity());
            itemObj.addProperty("uom", item.getUom() != null ? item.getUom() : "PCS");
            itemsArray.add(itemObj);
        }
        payload.add("items", itemsArray);

        HttpRequest request = apiClient.authRequest("/transfer-orders/picking")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject response = apiClient.executeWithAuth(request);
        if (response == null || !"success".equals(response.get("status").getAsString())) {
            String message = (response != null && response.has("message")) ? response.get("message").getAsString() : "Failed to create Picking Transfer Order.";
            throw new Exception(message);
        }

        return response.has("data") ? response.getAsJsonObject("data") : response;
    }
}
