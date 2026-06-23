package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.logging.Logger;
import models.dto.SalesOrderDTO;
import models.dto.SalesOrderItemDTO;
import models.dto.StorageBinDTO;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Goods Receipt - Customer Returns REST API endpoints
 * Communicates with the PHP REST API via ApiClient
 *
 * @author Sanod
 */
public class GRCustomerReturnsDAO {

    private static volatile GRCustomerReturnsDAO instance;
    private final ApiClient apiClient;

    private GRCustomerReturnsDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static GRCustomerReturnsDAO getInstance() {
        if (instance == null) {
            synchronized (GRCustomerReturnsDAO.class) {
                if (instance == null) {
                    instance = new GRCustomerReturnsDAO();
                }
            }
        }
        return instance;
    }

    // customer return item representing a return line to be posted
    public static class CustomerReturnItem {
        private Integer soItemId;
        private double quantity;
        private Integer toBinId;
        private String uom;
        private String qualityStatus;
        private String remarks;

        public CustomerReturnItem() {}

        public Integer getSoItemId() { return soItemId; }
        public void setSoItemId(Integer soItemId) { this.soItemId = soItemId; }

        public double getQuantity() { return quantity; }
        public void setQuantity(double quantity) { this.quantity = quantity; }

        public Integer getToBinId() { return toBinId; }
        public void setToBinId(Integer toBinId) { this.toBinId = toBinId; }

        public String getUom() { return uom; }
        public void setUom(String uom) { this.uom = uom; }

        public String getQualityStatus() { return qualityStatus; }
        public void setQualityStatus(String qualityStatus) { this.qualityStatus = qualityStatus; }

        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
    }

    // GET /api/movements/customer-returns/so/search?criteria={criteria}
    public List<SalesOrderDTO> searchSalesOrders(String criteria) throws Exception {
        String endpoint = "/movements/customer-returns/so/search";
        if (criteria != null && !criteria.trim().isEmpty()) {
            endpoint += "?criteria=" + java.net.URLEncoder.encode(criteria, "UTF-8");
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

    // GET /api/movements/customer-returns/so/{soNumber}
    public SalesOrderDTO getSalesOrderDetails(String soNumber) throws Exception {
        if (soNumber == null || soNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Sales Order number cannot be empty.");
        }

        String endpoint = "/movements/customer-returns/so/" + java.net.URLEncoder.encode(soNumber, "UTF-8");
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

    // GET /api/movements/customer-returns/receiving-bins?warehouse_id={warehouseId}
    public List<StorageBinDTO> getReceivingBins(Integer warehouseId) throws Exception {
        String endpoint = "/movements/customer-returns/receiving-bins";
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

    // POST /api/movements/customer-returns
    public boolean createCustomerReturn(String soNumber, String returnReason, String returnAuthNumber, String returnDate, List<CustomerReturnItem> items) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("so_number", soNumber);
        payload.addProperty("return_reason", returnReason);
        payload.addProperty("return_authorization_number", returnAuthNumber);
        payload.addProperty("return_date", returnDate);

        JsonArray itemsArray = new JsonArray();
        for (CustomerReturnItem item : items) {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("so_item_id", item.getSoItemId());
            itemObj.addProperty("quantity", item.getQuantity());
            itemObj.addProperty("to_bin_id", item.getToBinId());
            itemObj.addProperty("uom", item.getUom());
            itemObj.addProperty("quality_status", item.getQualityStatus());
            if (item.getRemarks() != null && !item.getRemarks().trim().isEmpty()) {
                itemObj.addProperty("remarks", item.getRemarks());
            }
            itemsArray.add(itemObj);
        }
        payload.add("items", itemsArray);

        HttpRequest request = apiClient.authRequest("/movements/customer-returns")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject response = apiClient.executeWithAuth(request);
        return response != null && "success".equals(response.get("status").getAsString());
    }

    private SalesOrderDTO jsonToSalesOrderDTO(JsonObject json) {
        SalesOrderDTO dto = new SalesOrderDTO();

        if (json.has("so_id") && !json.get("so_id").isJsonNull()) {
            dto.setSoId(json.get("so_id").getAsInt());
        }
        if (json.has("so_number") && !json.get("so_number").isJsonNull()) {
            dto.setSoNumber(json.get("so_number").getAsString());
        }
        if (json.has("customer_name") && !json.get("customer_name").isJsonNull()) {
            dto.setCustomerName(json.get("customer_name").getAsString());
        }
        if (json.has("order_date") && !json.get("order_date").isJsonNull()) {
            dto.setOrderDate(json.get("order_date").getAsString());
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
        }
        if (json.has("is_batch_managed") && !json.get("is_batch_managed").isJsonNull()) {
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
