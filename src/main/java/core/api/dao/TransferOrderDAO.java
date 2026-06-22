package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.logging.Logger;
import models.dto.TransferOrderDTO;
import models.dto.TransferOrderItemDTO;

import java.net.http.HttpRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Transfer Order REST API endpoints.
 * Handles loading TO details and posting goods receipt transfers.
 *
 * @author Sanod
 */
public class TransferOrderDAO {

    private static volatile TransferOrderDAO instance;
    private final ApiClient apiClient;

    private static final DateTimeFormatter DT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TransferOrderDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static TransferOrderDAO getInstance() {
        if (instance == null) {
            synchronized (TransferOrderDAO.class) {
                if (instance == null) {
                    instance = new TransferOrderDAO();
                }
            }
        }
        return instance;
    }

    // GET /api/movements/transfer-in/load/{toNumber}
    public TransferOrderDTO loadTransferOrder(String toNumber) throws Exception {
        String endpoint = "/movements/transfer-in/load/" + java.net.URLEncoder.encode(toNumber, "UTF-8");
        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equals(response.get("status").getAsString())) {
            throw new Exception("Failed to load transfer order details.");
        }

        JsonObject data = response.getAsJsonObject("data");
        if (data == null) {
            return null;
        }

        return jsonToTransferOrderDTO(data);
    }

    // POST /api/movements/transfer-in
    public boolean receiveGoods(String toNumber, List<TransferOrderItemDTO> items, String actualReceiptDate, String notes) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("to_number", toNumber);
        if (actualReceiptDate != null && !actualReceiptDate.trim().isEmpty()) {
            payload.addProperty("reference_date", actualReceiptDate);
        }
        if (notes != null && !notes.trim().isEmpty()) {
            payload.addProperty("notes", notes);
        }

        JsonArray itemsArray = new JsonArray();
        for (TransferOrderItemDTO item : items) {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("to_item_id", item.getToItemId());
            itemObj.addProperty("quantity", item.getReceivedQuantity());
            itemObj.addProperty("to_bin_id", item.getToBinId());
            itemObj.addProperty("uom", item.getUom());
            itemsArray.add(itemObj);
        }
        payload.add("items", itemsArray);

        HttpRequest request = apiClient.authRequest("/movements/transfer-in")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject response = apiClient.executeWithAuth(request);
        return response != null && "success".equals(response.get("status").getAsString());
    }

    private TransferOrderDTO jsonToTransferOrderDTO(JsonObject json) {
        TransferOrderDTO dto = new TransferOrderDTO();

        if (json.has("to_id") && !json.get("to_id").isJsonNull()) {
            dto.setToId(json.get("to_id").getAsInt());
        }
        if (json.has("to_number") && !json.get("to_number").isJsonNull()) {
            dto.setToNumber(json.get("to_number").getAsString());
        }
        if (json.has("to_type") && !json.get("to_type").isJsonNull()) {
            dto.setToType(json.get("to_type").getAsString());
        }
        if (json.has("status") && !json.get("status").isJsonNull()) {
            dto.setStatus(json.get("status").getAsString());
        }
        if (json.has("from_warehouse_id") && !json.get("from_warehouse_id").isJsonNull()) {
            dto.setFromWarehouseId(json.get("from_warehouse_id").getAsInt());
        }
        if (json.has("to_warehouse_id") && !json.get("to_warehouse_id").isJsonNull()) {
            dto.setToWarehouseId(json.get("to_warehouse_id").getAsInt());
        }
        if (json.has("created_by") && !json.get("created_by").isJsonNull()) {
            dto.setCreatedBy(json.get("created_by").getAsString());
        }
        if (json.has("created_date") && !json.get("created_date").isJsonNull()) {
            dto.setCreatedDate(parseDateTime(json.get("created_date").getAsString()));
        }
        if (json.has("started_date") && !json.get("started_date").isJsonNull()) {
            dto.setStartedDate(parseDateTime(json.get("started_date").getAsString()));
        }
        if (json.has("completed_date") && !json.get("completed_date").isJsonNull()) {
            dto.setCompletedDate(parseDateTime(json.get("completed_date").getAsString()));
        }
        if (json.has("assigned_to") && !json.get("assigned_to").isJsonNull()) {
            dto.setAssignedTo(json.get("assigned_to").getAsString());
        }
        if (json.has("notes") && !json.get("notes").isJsonNull()) {
            dto.setNotes(json.get("notes").getAsString());
        }

        if (json.has("items") && json.get("items").isJsonArray()) {
            JsonArray itemsArray = json.getAsJsonArray("items");
            List<TransferOrderItemDTO> itemsList = new ArrayList<>();
            for (JsonElement elem : itemsArray) {
                if (elem.isJsonObject()) {
                    itemsList.add(jsonToTransferOrderItemDTO(elem.getAsJsonObject()));
                }
            }
            dto.setItems(itemsList);
        }

        return dto;
    }

    private TransferOrderItemDTO jsonToTransferOrderItemDTO(JsonObject json) {
        TransferOrderItemDTO dto = new TransferOrderItemDTO();

        if (json.has("to_item_id") && !json.get("to_item_id").isJsonNull()) {
            dto.setToItemId(json.get("to_item_id").getAsInt());
        }
        if (json.has("to_id") && !json.get("to_id").isJsonNull()) {
            dto.setToId(json.get("to_id").getAsInt());
        }
        if (json.has("material_id") && !json.get("material_id").isJsonNull()) {
            dto.setMaterialId(json.get("material_id").getAsInt());
        }
        if (json.has("batch_id") && !json.get("batch_id").isJsonNull()) {
            dto.setBatchId(json.get("batch_id").getAsInt());
        }
        if (json.has("from_bin_id") && !json.get("from_bin_id").isJsonNull()) {
            dto.setFromBinId(json.get("from_bin_id").getAsInt());
        }
        if (json.has("to_bin_id") && !json.get("to_bin_id").isJsonNull()) {
            dto.setToBinId(json.get("to_bin_id").getAsInt());
        }
        if (json.has("required_quantity") && !json.get("required_quantity").isJsonNull()) {
            dto.setRequiredQuantity(json.get("required_quantity").getAsDouble());
        }
        if (json.has("confirmed_quantity") && !json.get("confirmed_quantity").isJsonNull()) {
            dto.setConfirmedQuantity(json.get("confirmed_quantity").getAsDouble());
        }
        if (json.has("uom") && !json.get("uom").isJsonNull()) {
            dto.setUom(json.get("uom").getAsString());
        }
        if (json.has("line_status") && !json.get("line_status").isJsonNull()) {
            dto.setLineStatus(json.get("line_status").getAsString());
        }
        if (json.has("sequence") && !json.get("sequence").isJsonNull()) {
            dto.setSequence(json.get("sequence").getAsInt());
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
        if (json.has("batch_number") && !json.get("batch_number").isJsonNull()) {
            dto.setBatchNumber(json.get("batch_number").getAsString());
        }
        if (json.has("expiry_date") && !json.get("expiry_date").isJsonNull()) {
            dto.setExpiryDate(json.get("expiry_date").getAsString());
        }
        if (json.has("from_bin_code") && !json.get("from_bin_code").isJsonNull()) {
            dto.setFromBinCode(json.get("from_bin_code").getAsString());
        }
        if (json.has("to_bin_code") && !json.get("to_bin_code").isJsonNull()) {
            dto.setToBinCode(json.get("to_bin_code").getAsString());
        }

        return dto;
    }

    private LocalDateTime parseDateTime(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || "null".equals(dateStr)) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateStr, DT_FORMATTER);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateStr);
            } catch (Exception e2) {
                Logger.errlog("Failed to parse date: " + dateStr, e2);
                return null;
            }
        }
    }
}
