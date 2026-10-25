/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import models.dto.MaterialStockDTO;
import models.dto.ScrapItemDTO;
import models.entity.ScrapReason;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Scrap Write-off REST API endpoints.
 * Handles fetching scrap reasons, searching material stock, posting write-offs, and loading write-off history.
 *
 * @author Sanod D. Mendis
 */
public class ScrapWriteoffDAO {

    private static volatile ScrapWriteoffDAO instance;
    private final ApiClient apiClient;

    private ScrapWriteoffDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static ScrapWriteoffDAO getInstance() {
        if (instance == null) {
            synchronized (ScrapWriteoffDAO.class) {
                if (instance == null) {
                    instance = new ScrapWriteoffDAO();
                }
            }
        }
        return instance;
    }

    // GET /api/scrap-writeoffs/reasons
    public List<ScrapReason> getScrapReasons() throws Exception {
        HttpRequest request = apiClient.authRequest("/scrap-writeoffs/reasons").GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        List<ScrapReason> list = new ArrayList<>();
        if (response != null && response.has("data") && response.get("data").isJsonArray()) {
            JsonArray arr = response.getAsJsonArray("data");
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                ScrapReason reason = new ScrapReason();
                if (obj.has("reason_id") && !obj.get("reason_id").isJsonNull()) {
                    reason.setReasonId(obj.get("reason_id").getAsInt());
                }
                if (obj.has("reason_code") && !obj.get("reason_code").isJsonNull()) {
                    reason.setReasonCode(obj.get("reason_code").getAsString());
                }
                if (obj.has("reason_description") && !obj.get("reason_description").isJsonNull()) {
                    reason.setReasonDescription(obj.get("reason_description").getAsString());
                }
                if (obj.has("default_disposal_method") && !obj.get("default_disposal_method").isJsonNull()) {
                    reason.setDefaultDisposalMethod(obj.get("default_disposal_method").getAsString());
                }
                list.add(reason);
            }
        }
        return list;
    }

    // GET /api/scrap-writeoffs/material-search?q={query}&warehouse_id={warehouseId}
    public List<MaterialStockDTO> searchMaterialStock(String query, Integer warehouseId) throws Exception {
        StringBuilder endpoint = new StringBuilder("/scrap-writeoffs/material-search");
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
                if (obj.has("batch_id") && !obj.get("batch_id").isJsonNull()) {
                    dto.setBatchId(obj.get("batch_id").getAsInt());
                }
                if (obj.has("batch_number") && !obj.get("batch_number").isJsonNull()) {
                    dto.setBatchNumber(obj.get("batch_number").getAsString());
                }
                if (obj.has("expiry_date") && !obj.get("expiry_date").isJsonNull()) {
                    dto.setExpiryDate(obj.get("expiry_date").getAsString());
                }
                list.add(dto);
            }
        }
        return list;
    }

    // POST /api/scrap-writeoffs
    public String saveScrapWriteoff(List<ScrapItemDTO> items, String reasonCode, String remarks, boolean submit) throws Exception {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Scrap write-off items list cannot be empty.");
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("reason_code", reasonCode != null ? reasonCode : "DAMAGED");
        payload.addProperty("remarks", remarks != null ? remarks : "");
        payload.addProperty("submit", submit);

        JsonArray itemsArray = new JsonArray();
        for (ScrapItemDTO itm : items) {
            JsonObject itmObj = new JsonObject();
            if (itm.getMaterialId() != null) itmObj.addProperty("material_id", itm.getMaterialId());
            if (itm.getMaterialCode() != null) itmObj.addProperty("material_code", itm.getMaterialCode());
            if (itm.getBinId() != null) itmObj.addProperty("bin_id", itm.getBinId());
            if (itm.getBinCode() != null) itmObj.addProperty("bin_code", itm.getBinCode());
            itmObj.addProperty("quantity", itm.getWriteoffQty());
            if (itm.getBaseUom() != null) itmObj.addProperty("uom", itm.getBaseUom());
            if (itm.getBatchId() != null) itmObj.addProperty("batch_id", itm.getBatchId());
            if (itm.getBatchNumber() != null) itmObj.addProperty("batch_number", itm.getBatchNumber());
            if (itm.getRemarks() != null) itmObj.addProperty("remarks", itm.getRemarks());
            itemsArray.add(itmObj);
        }
        payload.add("items", itemsArray);

        HttpRequest request = apiClient.authRequest("/scrap-writeoffs")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equalsIgnoreCase(response.get("status").getAsString())) {
            String msg = (response != null && response.has("message")) ? response.get("message").getAsString() : "Failed to save scrap write-off";
            throw new Exception(msg);
        }

        String movementNumber = "SCRAP-WRITE-OFF";
        if (response.has("data") && response.get("data").isJsonObject()) {
            JsonObject d = response.getAsJsonObject("data");
            if (d.has("movement_number")) {
                movementNumber = d.get("movement_number").getAsString();
            }
        }
        return movementNumber;
    }

    // GET /api/scrap-writeoffs/history
    public List<Object[]> getWriteoffHistory() throws Exception {
        HttpRequest request = apiClient.authRequest("/scrap-writeoffs/history").GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        List<Object[]> rows = new ArrayList<>();
        if (response != null && response.has("data") && response.get("data").isJsonArray()) {
            JsonArray arr = response.getAsJsonArray("data");
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                String matCode = obj.has("material_code") && !obj.get("material_code").isJsonNull() ? obj.get("material_code").getAsString() : "";
                double qty = obj.has("quantity") && !obj.get("quantity").isJsonNull() ? obj.get("quantity").getAsDouble() : 0.0;
                String reason = obj.has("reference_document") && !obj.get("reference_document").isJsonNull() ? obj.get("reference_document").getAsString() : "-";
                String date = obj.has("created_date") && !obj.get("created_date").isJsonNull() ? obj.get("created_date").getAsString() : "";

                rows.add(new Object[]{matCode, String.format("%.2f", qty), reason, date});
            }
        }
        return rows;
    }
}
