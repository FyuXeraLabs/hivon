package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.logging.Logger;
import models.dto.SalesOrderDTO;
import models.dto.SalesOrderItemDTO;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Goods Issue - Sales Order (OUT14) REST API endpoints.
 * Communicates with the PHP REST API via ApiClient.
 *
 * @author Navodya
 */
public class GISalesOrderDAO {

    private static volatile GISalesOrderDAO instance;
    private final ApiClient apiClient;

    private GISalesOrderDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static GISalesOrderDAO getInstance() {
        if (instance == null) {
            synchronized (GISalesOrderDAO.class) {
                if (instance == null) {
                    instance = new GISalesOrderDAO();
                }
            }
        }
        return instance;
    }

    // Line item to be included in Goods Issue shipment
    public static class GISalesOrderItem {
        private Integer soItemId;
        private int materialId;
        private String materialCode;
        private String materialName;
        private int fromBinId;
        private String binCode;
        private double quantity;
        private String uom;
        private Integer batchId;
        private String batchNumber;
        private String lineNotes;

        public GISalesOrderItem() {}

        public Integer getSoItemId() { return soItemId; }
        public void setSoItemId(Integer soItemId) { this.soItemId = soItemId; }

        public int getMaterialId() { return materialId; }
        public void setMaterialId(int materialId) { this.materialId = materialId; }

        public String getMaterialCode() { return materialCode; }
        public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

        public String getMaterialName() { return materialName; }
        public void setMaterialName(String materialName) { this.materialName = materialName; }

        public int getFromBinId() { return fromBinId; }
        public void setFromBinId(int fromBinId) { this.fromBinId = fromBinId; }

        public String getBinCode() { return binCode; }
        public void setBinCode(String binCode) { this.binCode = binCode; }

        public double getQuantity() { return quantity; }
        public void setQuantity(double quantity) { this.quantity = quantity; }

        public String getUom() { return uom; }
        public void setUom(String uom) { this.uom = uom; }

        public Integer getBatchId() { return batchId; }
        public void setBatchId(Integer batchId) { this.batchId = batchId; }

        public String getBatchNumber() { return batchNumber; }
        public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

        public String getLineNotes() { return lineNotes; }
        public void setLineNotes(String lineNotes) { this.lineNotes = lineNotes; }
    }

    // Batch suggestion model for FIFO/FEFO picking
    public static class BatchSuggestion {
        private int batchId;
        private String batchNumber;
        private String manufactureDate;
        private String expiryDate;
        private String qualityStatus;
        private double availableQty;

        public BatchSuggestion() {}

        public int getBatchId() { return batchId; }
        public void setBatchId(int batchId) { this.batchId = batchId; }

        public String getBatchNumber() { return batchNumber; }
        public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

        public String getManufactureDate() { return manufactureDate; }
        public void setManufactureDate(String manufactureDate) { this.manufactureDate = manufactureDate; }

        public String getExpiryDate() { return expiryDate; }
        public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

        public String getQualityStatus() { return qualityStatus; }
        public void setQualityStatus(String qualityStatus) { this.qualityStatus = qualityStatus; }

        public double getAvailableQty() { return availableQty; }
        public void setAvailableQty(double availableQty) { this.availableQty = availableQty; }

        @Override
        public String toString() {
            String exp = (expiryDate != null && !expiryDate.isEmpty()) ? " Exp: " + expiryDate : "";
            return batchNumber + " (Avail: " + availableQty + exp + ")";
        }
    }

    // GET /api/movements/goods-issue/sales-order/search
    public List<SalesOrderDTO> searchSalesOrders(String criteria, Integer customerId, String status) throws Exception {
        String endpoint = "/movements/goods-issue/sales-order/search";
        List<String> queryParams = new ArrayList<>();
        if (criteria != null && !criteria.trim().isEmpty()) {
            queryParams.add("criteria=" + java.net.URLEncoder.encode(criteria.trim(), "UTF-8"));
        }
        if (customerId != null) {
            queryParams.add("customer_id=" + customerId);
        }
        if (status != null && !status.trim().isEmpty()) {
            queryParams.add("status=" + java.net.URLEncoder.encode(status.trim(), "UTF-8"));
        }
        if (!queryParams.isEmpty()) {
            endpoint += "?" + String.join("&", queryParams);
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equals(response.get("status").getAsString())) {
            throw new Exception("Failed to search sales orders.");
        }

        List<SalesOrderDTO> list = new ArrayList<>();
        JsonArray dataArray = response.getAsJsonArray("data");
        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                if (elem.isJsonObject()) {
                    list.add(jsonToSalesOrderDTO(elem.getAsJsonObject()));
                }
            }
        }
        return list;
    }

    // GET /api/movements/goods-issue/sales-order/{soNumber}
    public SalesOrderDTO getSalesOrderDetails(String soNumber) throws Exception {
        if (soNumber == null || soNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Sales Order number cannot be empty.");
        }

        String endpoint = "/movements/goods-issue/sales-order/" + java.net.URLEncoder.encode(soNumber.trim(), "UTF-8");
        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equals(response.get("status").getAsString())) {
            throw new Exception("Failed to load sales order details.");
        }

        JsonObject data = response.getAsJsonObject("data");
        if (data == null) {
            return null;
        }

        return jsonToSalesOrderDTO(data);
    }

    // GET /api/movements/goods-issue/sales-order/suggest-batches?material_id={materialId}
    public List<BatchSuggestion> suggestBatchesByFIFO(int materialId) throws Exception {
        String endpoint = "/movements/goods-issue/sales-order/suggest-batches?material_id=" + materialId;
        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        List<BatchSuggestion> batches = new ArrayList<>();
        if (response != null && "success".equals(response.get("status").getAsString())) {
            JsonArray dataArray = response.getAsJsonArray("data");
            if (dataArray != null) {
                for (JsonElement elem : dataArray) {
                    if (elem.isJsonObject()) {
                        JsonObject obj = elem.getAsJsonObject();
                        BatchSuggestion b = new BatchSuggestion();
                        if (obj.has("batch_id") && !obj.get("batch_id").isJsonNull()) {
                            b.setBatchId(obj.get("batch_id").getAsInt());
                        }
                        if (obj.has("batch_number") && !obj.get("batch_number").isJsonNull()) {
                            b.setBatchNumber(obj.get("batch_number").getAsString());
                        }
                        if (obj.has("manufacture_date") && !obj.get("manufacture_date").isJsonNull()) {
                            b.setManufactureDate(obj.get("manufacture_date").getAsString());
                        }
                        if (obj.has("expiry_date") && !obj.get("expiry_date").isJsonNull()) {
                            b.setExpiryDate(obj.get("expiry_date").getAsString());
                        }
                        if (obj.has("quality_status") && !obj.get("quality_status").isJsonNull()) {
                            b.setQualityStatus(obj.get("quality_status").getAsString());
                        }
                        if (obj.has("available_qty") && !obj.get("available_qty").isJsonNull()) {
                            b.setAvailableQty(obj.get("available_qty").getAsDouble());
                        }
                        batches.add(b);
                    }
                }
            }
        }
        return batches;
    }

    // POST /api/movements/goods-issue/sales-order
    public String createGISalesOrder(String soNumber, String refDate, String notes, List<GISalesOrderItem> items) throws Exception {
        if (soNumber == null || soNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Sales Order number is required.");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Shipment items list cannot be empty.");
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("so_number", soNumber.trim());
        if (refDate != null && !refDate.trim().isEmpty()) {
            payload.addProperty("reference_date", refDate.trim());
        }
        if (notes != null && !notes.trim().isEmpty()) {
            payload.addProperty("notes", notes.trim());
        }

        JsonArray itemsArray = new JsonArray();
        for (GISalesOrderItem item : items) {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("so_item_id", item.getSoItemId());
            itemObj.addProperty("material_id", item.getMaterialId());
            itemObj.addProperty("from_bin_id", item.getFromBinId());
            itemObj.addProperty("quantity", item.getQuantity());
            itemObj.addProperty("uom", item.getUom() != null ? item.getUom() : "PCS");

            if (item.getBatchId() != null) {
                itemObj.addProperty("batch_id", item.getBatchId());
            }
            if (item.getBatchNumber() != null && !item.getBatchNumber().isEmpty()) {
                itemObj.addProperty("batch_number", item.getBatchNumber());
            }
            if (item.getLineNotes() != null && !item.getLineNotes().trim().isEmpty()) {
                itemObj.addProperty("line_notes", item.getLineNotes());
            }

            itemsArray.add(itemObj);
        }
        payload.add("items", itemsArray);

        HttpRequest request = apiClient.authRequest("/movements/goods-issue/sales-order")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject response = apiClient.executeWithAuth(request);
        if (response != null && "success".equals(response.get("status").getAsString())) {
            if (response.has("data") && response.get("data").isJsonObject()) {
                JsonObject data = response.getAsJsonObject("data");
                if (data.has("movement_number")) {
                    return data.get("movement_number").getAsString();
                }
            }
            return "GI-SUCCESS";
        }
        throw new Exception(response != null && response.has("message") ? response.get("message").getAsString() : "Failed to create Goods Issue.");
    }

    private SalesOrderDTO jsonToSalesOrderDTO(JsonObject json) {
        SalesOrderDTO dto = new SalesOrderDTO();

        if (json.has("so_id") && !json.get("so_id").isJsonNull()) {
            dto.setSoId(json.get("so_id").getAsInt());
        }
        if (json.has("so_number") && !json.get("so_number").isJsonNull()) {
            dto.setSoNumber(json.get("so_number").getAsString());
        }
        if (json.has("customer_id") && !json.get("customer_id").isJsonNull()) {
            dto.setCustomerId(json.get("customer_id").getAsInt());
        }
        if (json.has("customer_code") && !json.get("customer_code").isJsonNull()) {
            dto.setCustomerCode(json.get("customer_code").getAsString());
        }
        if (json.has("customer_name") && !json.get("customer_name").isJsonNull()) {
            dto.setCustomerName(json.get("customer_name").getAsString());
        }
        if (json.has("contact_person") && !json.get("contact_person").isJsonNull()) {
            dto.setContactPerson(json.get("contact_person").getAsString());
        }
        if (json.has("phone") && !json.get("phone").isJsonNull()) {
            dto.setPhone(json.get("phone").getAsString());
        }
        if (json.has("delivery_address") && !json.get("delivery_address").isJsonNull()) {
            dto.setDeliveryAddress(json.get("delivery_address").getAsString());
        }
        if (json.has("order_date") && !json.get("order_date").isJsonNull()) {
            dto.setOrderDate(json.get("order_date").getAsString());
        }
        if (json.has("delivery_date") && !json.get("delivery_date").isJsonNull()) {
            dto.setDeliveryDate(json.get("delivery_date").getAsString());
        }
        if (json.has("status") && !json.get("status").isJsonNull()) {
            dto.setStatus(json.get("status").getAsString());
        }

        if (json.has("items") && json.get("items").isJsonArray()) {
            JsonArray itemsArray = json.getAsJsonArray("items");
            for (JsonElement elem : itemsArray) {
                if (elem.isJsonObject()) {
                    dto.addItem(jsonToSalesOrderItemDTO(elem.getAsJsonObject()));
                }
            }
        }

        return dto;
    }

    private SalesOrderItemDTO jsonToSalesOrderItemDTO(JsonObject json) {
        SalesOrderItemDTO dto = new SalesOrderItemDTO();

        if (json.has("so_item_id") && !json.get("so_item_id").isJsonNull()) {
            dto.setSoItemId(json.get("so_item_id").getAsInt());
        }
        if (json.has("so_number") && !json.get("so_number").isJsonNull()) {
            dto.setSoNumber(json.get("so_number").getAsString());
        }
        if (json.has("material_id") && !json.get("material_id").isJsonNull()) {
            dto.setMaterialId(json.get("material_id").getAsInt());
        }
        if (json.has("material_code") && !json.get("material_code").isJsonNull()) {
            dto.setMaterialCode(json.get("material_code").getAsString());
        }
        if (json.has("material_name") && !json.get("material_name").isJsonNull()) {
            dto.setMaterialName(json.get("material_name").getAsString());
        } else if (json.has("material_description") && !json.get("material_description").isJsonNull()) {
            dto.setMaterialName(json.get("material_description").getAsString());
        }
        if (json.has("ordered_quantity") && !json.get("ordered_quantity").isJsonNull()) {
            dto.setOrderedQuantity(json.get("ordered_quantity").getAsDouble());
        }
        if (json.has("shipped_quantity") && !json.get("shipped_quantity").isJsonNull()) {
            dto.setShippedQuantity(json.get("shipped_quantity").getAsDouble());
        }
        if (json.has("returned_quantity") && !json.get("returned_quantity").isJsonNull()) {
            dto.setReturnedQuantity(json.get("returned_quantity").getAsDouble());
        }
        if (json.has("uom") && !json.get("uom").isJsonNull()) {
            dto.setUom(json.get("uom").getAsString());
        } else if (json.has("base_uom") && !json.get("base_uom").isJsonNull()) {
            dto.setUom(json.get("base_uom").getAsString());
        }
        if (json.has("is_batch_managed") && !json.get("is_batch_managed").isJsonNull()) {
            dto.setIsBatchManaged(json.get("is_batch_managed").getAsBoolean());
        }

        return dto;
    }
}
