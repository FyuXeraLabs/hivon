package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.api.ApiConfig;
import models.dto.PurchaseOrderDTO;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Purchase Order REST API endpoints.
 * Handles fetching purchase orders filtered by vendor.
 *
 * @author Sanod
 */
public class PurchaseOrderDAO {

    private static volatile PurchaseOrderDAO instance;
    private final ApiClient apiClient;

    private PurchaseOrderDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static PurchaseOrderDAO getInstance() {
        if (instance == null) {
            synchronized (PurchaseOrderDAO.class) {
                if (instance == null) {
                    instance = new PurchaseOrderDAO();
                }
            }
        }
        return instance;
    }

    // GET /api/purchase-orders?vendor_id={vendorId}
    public List<PurchaseOrderDTO> getPurchaseOrdersByVendorId(int vendorId) throws Exception {
        String endpoint = ApiConfig.PURCHASE_ORDERS + "?vendor_id=" + vendorId;

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<PurchaseOrderDTO> pos = new ArrayList<>();
        JsonArray dataArray = body.getAsJsonArray("data");

        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                pos.add(jsonToPurchaseOrderDTO(elem.getAsJsonObject()));
            }
        }
        return pos;
    }

    // Converts a JSON object from the API response into a PurchaseOrderDTO
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

        return dto;
    }
}
