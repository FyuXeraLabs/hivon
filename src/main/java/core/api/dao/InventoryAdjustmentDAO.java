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
import models.dto.AdjustmentItemDTO;
import models.dto.MaterialStockDTO;
import models.entity.AdjustmentReason;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Inventory Adjustment REST API endpoints.
 * Handles fetching adjustment reasons, searching material stock, posting adjustments, and fetching history.
 *
 * @author Sanod D. Mendis
 */
public class InventoryAdjustmentDAO {

    private static volatile InventoryAdjustmentDAO instance;
    private final ApiClient apiClient;

    private InventoryAdjustmentDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static InventoryAdjustmentDAO getInstance() {
        if (instance == null) {
            synchronized (InventoryAdjustmentDAO.class) {
                if (instance == null) {
                    instance = new InventoryAdjustmentDAO();
                }
            }
        }
        return instance;
    }

    // GET /api/adjustments/reasons
    public List<AdjustmentReason> getAdjustmentReasons() throws Exception {
        HttpRequest request = apiClient.authRequest("/adjustments/reasons").GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        List<AdjustmentReason> list = new ArrayList<>();
        if (response != null && response.has("data") && response.get("data").isJsonArray()) {
            JsonArray arr = response.getAsJsonArray("data");
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                AdjustmentReason reason = new AdjustmentReason();
                if (obj.has("reason_id") && !obj.get("reason_id").isJsonNull()) {
                    reason.setReasonId(obj.get("reason_id").getAsInt());
                }
                if (obj.has("reason_code") && !obj.get("reason_code").isJsonNull()) {
                    reason.setReasonCode(obj.get("reason_code").getAsString());
                }
                if (obj.has("reason_description") && !obj.get("reason_description").isJsonNull()) {
                    reason.setReasonDescription(obj.get("reason_description").getAsString());
                }
                if (obj.has("requires_approval") && !obj.get("requires_approval").isJsonNull()) {
                    reason.setRequiresApproval(obj.get("requires_approval").getAsInt() == 1);
                }
                if (obj.has("approval_threshold") && !obj.get("approval_threshold").isJsonNull()) {
                    reason.setApprovalThreshold(obj.get("approval_threshold").getAsDouble());
                }
                if (obj.has("is_active") && !obj.get("is_active").isJsonNull()) {
                    reason.setActive(obj.get("is_active").getAsInt() == 1);
                }
                list.add(reason);
            }
        }
        return list;
    }

    // GET /api/adjustments/material-search?q={query}&warehouse_id={warehouseId}
    public List<MaterialStockDTO> searchMaterialStock(String query, Integer warehouseId) throws Exception {
        StringBuilder endpoint = new StringBuilder("/adjustments/material-search");
        List<String> params = new ArrayList<>();
        if (query != null && !query.trim().isEmpty()) {
            params.add("q=" + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8));
        }
        if (warehouseId != null) {
            params.add("warehouse_id=" + warehouseId);
        }
        if (!params.isEmpty()) {
            endpoint.append("?").append(String.join("&", params));
        }

        HttpRequest request = apiClient.authRequest(endpoint.toString()).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        List<MaterialStockDTO> list = new ArrayList<>();
        if (response != null && response.has("data") && response.get("data").isJsonArray()) {
            JsonArray arr = response.getAsJsonArray("data");
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                MaterialStockDTO dto = new MaterialStockDTO();
                if (obj.has("material_id") && !obj.get("material_id").isJsonNull()) {
                    dto.setMaterialId(obj.get("material_id").getAsInt());
                }
                if (obj.has("material_code") && !obj.get("material_code").isJsonNull()) {
                    dto.setMaterialCode(obj.get("material_code").getAsString());
                }
                if (obj.has("material_description") && !obj.get("material_description").isJsonNull()) {
                    dto.setMaterialDescription(obj.get("material_description").getAsString());
                }
                if (obj.has("base_uom") && !obj.get("base_uom").isJsonNull()) {
                    dto.setBaseUom(obj.get("base_uom").getAsString());
                }
                if (obj.has("unit_cost") && !obj.get("unit_cost").isJsonNull()) {
                    dto.setUnitCost(obj.get("unit_cost").getAsDouble());
                }
                if (obj.has("is_batch_managed") && !obj.get("is_batch_managed").isJsonNull()) {
                    dto.setIsBatchManaged(obj.get("is_batch_managed").getAsBoolean());
                }
                if (obj.has("system_quantity") && !obj.get("system_quantity").isJsonNull()) {
                    dto.setSystemQuantity(obj.get("system_quantity").getAsDouble());
                }
                if (obj.has("primary_bin_code") && !obj.get("primary_bin_code").isJsonNull()) {
                    dto.setPrimaryBinCode(obj.get("primary_bin_code").getAsString());
                }
                if (obj.has("primary_bin_id") && !obj.get("primary_bin_id").isJsonNull()) {
                    dto.setPrimaryBinId(obj.get("primary_bin_id").getAsInt());
                }
                list.add(dto);
            }
        }
        return list;
    }

    // POST /api/adjustments
    public String saveAdjustment(List<AdjustmentItemDTO> items, String referenceDate, String authorizedBy, String remarks) throws Exception {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Adjustment items list cannot be empty.");
        }

        JsonObject payload = new JsonObject();
        if (referenceDate != null) payload.addProperty("reference_date", referenceDate);
        if (authorizedBy != null) payload.addProperty("authorized_by", authorizedBy);
        if (remarks != null) payload.addProperty("remarks", remarks);

        JsonArray itemsArray = new JsonArray();
        for (AdjustmentItemDTO itm : items) {
            JsonObject itmObj = new JsonObject();
            if (itm.getMaterialId() != null) itmObj.addProperty("material_id", itm.getMaterialId());
            if (itm.getMaterialCode() != null) itmObj.addProperty("material_code", itm.getMaterialCode());
            if (itm.getBinId() != null) itmObj.addProperty("bin_id", itm.getBinId());
            if (itm.getBinCode() != null) itmObj.addProperty("bin_code", itm.getBinCode());
            itmObj.addProperty("quantity", itm.getVarianceQty());
            if (itm.getReasonCode() != null) itmObj.addProperty("reason_code", itm.getReasonCode());
            if (itm.getBaseUom() != null) itmObj.addProperty("uom", itm.getBaseUom());
            if (itm.getRemarks() != null) itmObj.addProperty("remarks", itm.getRemarks());
            if (itm.getBatchId() != null) itmObj.addProperty("batch_id", itm.getBatchId());
            itemsArray.add(itmObj);
        }
        payload.add("items", itemsArray);

        HttpRequest request = apiClient.authRequest("/adjustments")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equalsIgnoreCase(response.get("status").getAsString())) {
            String msg = (response != null && response.has("message")) ? response.get("message").getAsString() : "Failed to save adjustment";
            throw new Exception(msg);
        }

        String movementNumber = "ADJUSTMENT-POSTED";
        if (response.has("data") && response.get("data").isJsonObject()) {
            JsonObject d = response.getAsJsonObject("data");
            if (d.has("movement_number")) {
                movementNumber = d.get("movement_number").getAsString();
            }
        }
        return movementNumber;
    }

    // GET /api/adjustments/history
    public List<Object[]> getAdjustmentHistory() throws Exception {
        HttpRequest request = apiClient.authRequest("/adjustments/history").GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        List<Object[]> rows = new ArrayList<>();
        if (response != null && response.has("data") && response.get("data").isJsonArray()) {
            JsonArray arr = response.getAsJsonArray("data");
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String matCode = obj.has("material_code") && !obj.get("material_code").isJsonNull() ? obj.get("material_code").getAsString() : "";
                String matName = obj.has("material_name") && !obj.get("material_name").isJsonNull() ? obj.get("material_name").getAsString() : "";
                String movNum = obj.has("movement_number") && !obj.get("movement_number").isJsonNull() ? obj.get("movement_number").getAsString() : "";
                double qty = obj.has("quantity") && !obj.get("quantity").isJsonNull() ? obj.get("quantity").getAsDouble() : 0.0;
                String binCode = obj.has("to_bin_code") && !obj.get("to_bin_code").isJsonNull() ? obj.get("to_bin_code").getAsString() :
                        (obj.has("from_bin_code") && !obj.get("from_bin_code").isJsonNull() ? obj.get("from_bin_code").getAsString() : "-");
                String date = obj.has("created_date") && !obj.get("created_date").isJsonNull() ? obj.get("created_date").getAsString() : "";
                String user = obj.has("created_by") && !obj.get("created_by").isJsonNull() ? obj.get("created_by").getAsString() : "";
                rows.add(new Object[]{matCode, matName, movNum, qty, binCode, date, user});
            }
        }
        return rows;
    }
}
