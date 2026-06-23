package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.logging.Logger;
import models.dto.PurchaseOrderDTO;
import models.dto.POItemDTO;
import models.dto.StorageBinDTO;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Goods Receipt - Purchase Order REST API endpoints.
 * Handles fetching PO search lists, loading detailed POs, getting receiving bins, and posting receipts.
 *
 * @author Sanod
 */
public class GRPurchaseOrderDAO {

    private static volatile GRPurchaseOrderDAO instance;
    private final ApiClient apiClient;

    private GRPurchaseOrderDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static GRPurchaseOrderDAO getInstance() {
        if (instance == null) {
            synchronized (GRPurchaseOrderDAO.class) {
                if (instance == null) {
                    instance = new GRPurchaseOrderDAO();
                }
            }
        }
        return instance;
    }

    // PO receipt summary item representing a receipt line to be posted
    public static class POReceiptItem {
        private Integer poItemId;
        private double quantity;
        private Integer toBinId;
        private String uom;
        private String batchNumber;
        private String expiryDate;
        private String qualityStatus;
        private String lineNotes;

        public POReceiptItem() {}

        public Integer getPoItemId() { return poItemId; }
        public void setPoItemId(Integer poItemId) { this.poItemId = poItemId; }

        public double getQuantity() { return quantity; }
        public void setQuantity(double quantity) { this.quantity = quantity; }

        public Integer getToBinId() { return toBinId; }
        public void setToBinId(Integer toBinId) { this.toBinId = toBinId; }

        public String getUom() { return uom; }
        public void setUom(String uom) { this.uom = uom; }

        public String getBatchNumber() { return batchNumber; }
        public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

        public String getExpiryDate() { return expiryDate; }
        public void setExpiryDate(String expiryDate) { this.expiryDate = expiryDate; }

        public String getQualityStatus() { return qualityStatus; }
        public void setQualityStatus(String qualityStatus) { this.qualityStatus = qualityStatus; }

        public String getLineNotes() { return lineNotes; }
        public void setLineNotes(String lineNotes) { this.lineNotes = lineNotes; }
    }

    // GET /api/movements/goods-receipt/po/search?status={status}
    public List<PurchaseOrderDTO> searchPurchaseOrders(String status) throws Exception {
        String endpoint = "/movements/goods-receipt/po/search";
        if (status != null && !status.trim().isEmpty()) {
            endpoint += "?status=" + java.net.URLEncoder.encode(status, "UTF-8");
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equals(response.get("status").getAsString())) {
            throw new Exception("Failed to search purchase orders.");
        }

        List<PurchaseOrderDTO> list = new ArrayList<>();
        JsonArray dataArray = response.getAsJsonArray("data");
        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                if (elem.isJsonObject()) {
                    list.add(jsonToPurchaseOrderDTO(elem.getAsJsonObject()));
                }
            }
        }
        return list;
    }

    // GET /api/movements/goods-receipt/po/{poNumber}
    public PurchaseOrderDTO getPurchaseOrderDetails(String poNumber) throws Exception {
        if (poNumber == null || poNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("PO number cannot be empty.");
        }

        String endpoint = "/movements/goods-receipt/po/" + java.net.URLEncoder.encode(poNumber, "UTF-8");
        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equals(response.get("status").getAsString())) {
            throw new Exception("Failed to load purchase order details.");
        }

        JsonObject data = response.getAsJsonObject("data");
        if (data == null) {
            return null;
        }

        return jsonToPurchaseOrderDTO(data);
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

    // POST /api/movements/goods-receipt/po
    public boolean createGRPurchaseOrder(String poNumber, String referenceDate, String notes, List<POReceiptItem> items) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("po_number", poNumber);
        if (referenceDate != null && !referenceDate.trim().isEmpty()) {
            payload.addProperty("reference_date", referenceDate);
        }
        if (notes != null && !notes.trim().isEmpty()) {
            payload.addProperty("notes", notes);
        }

        JsonArray itemsArray = new JsonArray();
        for (POReceiptItem item : items) {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("po_item_id", item.getPoItemId());
            itemObj.addProperty("quantity", item.getQuantity());
            itemObj.addProperty("to_bin_id", item.getToBinId());
            itemObj.addProperty("uom", item.getUom());

            if (item.getBatchNumber() != null && !item.getBatchNumber().trim().isEmpty()) {
                itemObj.addProperty("batch_number", item.getBatchNumber());
            }
            if (item.getExpiryDate() != null && !item.getExpiryDate().trim().isEmpty()) {
                itemObj.addProperty("expiry_date", item.getExpiryDate());
            }
            if (item.getQualityStatus() != null && !item.getQualityStatus().trim().isEmpty()) {
                itemObj.addProperty("quality_status", item.getQualityStatus());
            }
            if (item.getLineNotes() != null && !item.getLineNotes().trim().isEmpty()) {
                itemObj.addProperty("line_notes", item.getLineNotes());
            }

            itemsArray.add(itemObj);
        }
        payload.add("items", itemsArray);

        HttpRequest request = apiClient.authRequest("/movements/goods-receipt/po")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject response = apiClient.executeWithAuth(request);
        return response != null && "success".equals(response.get("status").getAsString());
    }

    private PurchaseOrderDTO jsonToPurchaseOrderDTO(JsonObject json) {
        PurchaseOrderDTO dto = new PurchaseOrderDTO();

        if (json.has("po_id") && !json.get("po_id").isJsonNull()) {
            dto.setPoId(json.get("po_id").getAsInt());
        }
        if (json.has("po_number") && !json.get("po_number").isJsonNull()) {
            dto.setPoNumber(json.get("po_number").getAsString());
        }
        if (json.has("vendor_id") && !json.get("vendor_id").isJsonNull()) {
            dto.setVendorId(json.get("vendor_id").getAsInt());
        }
        if (json.has("vendor_name") && !json.get("vendor_name").isJsonNull()) {
            dto.setVendorName(json.get("vendor_name").getAsString());
        }
        if (json.has("vendor_code") && !json.get("vendor_code").isJsonNull()) {
            dto.setVendorCode(json.get("vendor_code").getAsString());
        }
        if (json.has("address") && !json.get("address").isJsonNull()) {
            dto.setAddress(json.get("address").getAsString());
        }
        if (json.has("contact_person") && !json.get("contact_person").isJsonNull()) {
            dto.setContactPerson(json.get("contact_person").getAsString());
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
        if (json.has("created_date") && !json.get("created_date").isJsonNull()) {
            dto.setCreatedDate(json.get("created_date").getAsString());
        }

        if (json.has("items") && json.get("items").isJsonArray()) {
            JsonArray itemsArray = json.getAsJsonArray("items");
            for (JsonElement elem : itemsArray) {
                if (elem.isJsonObject()) {
                    dto.addItem(jsonToPOItemDTO(elem.getAsJsonObject()));
                }
            }
        }

        return dto;
    }

    private POItemDTO jsonToPOItemDTO(JsonObject json) {
        POItemDTO dto = new POItemDTO();

        if (json.has("po_item_id") && !json.get("po_item_id").isJsonNull()) {
            dto.setPoItemId(json.get("po_item_id").getAsInt());
        }
        if (json.has("po_number") && !json.get("po_number").isJsonNull()) {
            dto.setPoNumber(json.get("po_number").getAsString());
        }
        if (json.has("material_id") && !json.get("material_id").isJsonNull()) {
            dto.setMaterialId(json.get("material_id").getAsInt());
        }
        if (json.has("ordered_quantity") && !json.get("ordered_quantity").isJsonNull()) {
            dto.setOrderedQuantity(json.get("ordered_quantity").getAsDouble());
        }
        if (json.has("received_quantity") && !json.get("received_quantity").isJsonNull()) {
            dto.setReceivedQuantity(json.get("received_quantity").getAsDouble());
        }
        if (json.has("unit_price") && !json.get("unit_price").isJsonNull()) {
            dto.setUnitPrice(json.get("unit_price").getAsDouble());
        }
        if (json.has("material_code") && !json.get("material_code").isJsonNull()) {
            dto.setMaterialCode(json.get("material_code").getAsString());
        }
        if (json.has("material_description") && !json.get("material_description").isJsonNull()) {
            dto.setMaterialDescription(json.get("material_description").getAsString());
        }
        if (json.has("base_uom") && !json.get("base_uom").isJsonNull()) {
            dto.setBaseUom(json.get("base_uom").getAsString());
        }
        if (json.has("is_batch_managed") && !json.get("is_batch_managed").isJsonNull()) {
            // PHP may return 1/0 or true/false, GSON getAsBoolean handles numeric 1/0 or boolean values gracefully.
            dto.setIsBatchManaged(json.get("is_batch_managed").getAsBoolean());
        }

        return dto;
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
        if (json.has("is_active") && !json.get("is_active").isJsonNull()) {
            dto.setIsActive(json.get("is_active").getAsBoolean());
        }
        if (json.has("warehouse_code") && !json.get("warehouse_code").isJsonNull()) {
            dto.setWarehouseCode(json.get("warehouse_code").getAsString());
        }
        if (json.has("warehouse_name") && !json.get("warehouse_name").isJsonNull()) {
            dto.setWarehouseName(json.get("warehouse_name").getAsString());
        }

        return dto;
    }
}
