/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import models.dto.MaterialMonitoringReportDTO;
import models.dto.MaterialMonitoringReportDTO.MaterialMonitoringItem;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Material Monitoring Report REST API endpoints
 * Calls /api/reports/material-monitoring and parses JSON into MaterialMonitoringReportDTO
 *
 * @author Sanod
 */
public class MaterialMonitoringReportDAO {

    private static volatile MaterialMonitoringReportDAO instance;
    private final ApiClient apiClient;

    private MaterialMonitoringReportDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static MaterialMonitoringReportDAO getInstance() {
        if (instance == null) {
            synchronized (MaterialMonitoringReportDAO.class) {
                if (instance == null) {
                    instance = new MaterialMonitoringReportDAO();
                }
            }
        }
        return instance;
    }

    /**
     * Retrieves material monitoring report from API
     */
    public MaterialMonitoringReportDTO getMaterialMonitoringReport(
            String material,
            String batch,
            String bin,
            boolean highlightExpiring) throws Exception {

        String endpoint = "/reports/material-monitoring";
        List<String> queryParams = new ArrayList<>();

        if (material != null && !material.trim().isEmpty() && !"All".equalsIgnoreCase(material.trim())) {
            queryParams.add("material=" + URLEncoder.encode(material.trim(), StandardCharsets.UTF_8));
        }
        if (batch != null && !batch.trim().isEmpty() && !"All".equalsIgnoreCase(batch.trim())) {
            queryParams.add("batch=" + URLEncoder.encode(batch.trim(), StandardCharsets.UTF_8));
        }
        if (bin != null && !bin.trim().isEmpty() && !"All".equalsIgnoreCase(bin.trim())) {
            queryParams.add("bin=" + URLEncoder.encode(bin.trim(), StandardCharsets.UTF_8));
        }
        if (highlightExpiring) {
            queryParams.add("highlight_expiring=true");
        }

        if (!queryParams.isEmpty()) {
            endpoint += "?" + String.join("&", queryParams);
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equalsIgnoreCase(getStringSafely(response, "status"))) {
            throw new Exception("Failed to fetch material monitoring report from server.");
        }

        JsonObject dataObj = response.getAsJsonObject("data");
        if (dataObj == null) {
            return new MaterialMonitoringReportDTO();
        }

        MaterialMonitoringReportDTO dto = new MaterialMonitoringReportDTO();
        dto.setGeneratedAt(getStringSafely(dataObj, "generated_at"));
        dto.setActiveInventorySkus(getIntSafely(dataObj, "active_inventory_skus"));
        dto.setTotalQuantity(getDoubleSafely(dataObj, "total_quantity"));
        dto.setTotalInventoryValue(getDoubleSafely(dataObj, "total_inventory_value"));
        dto.setExpiringCount(getIntSafely(dataObj, "expiring_count"));
        dto.setAvgBinCapacityUsage(getDoubleSafely(dataObj, "avg_bin_capacity_usage"));
        dto.setCurrency(getStringSafely(dataObj, "currency", "$"));

        if (dataObj.has("items") && dataObj.get("items").isJsonArray()) {
            JsonArray itemsArray = dataObj.getAsJsonArray("items");
            List<MaterialMonitoringItem> itemList = new ArrayList<>();
            for (JsonElement elem : itemsArray) {
                if (elem.isJsonObject()) {
                    JsonObject iObj = elem.getAsJsonObject();
                    MaterialMonitoringItem item = new MaterialMonitoringItem();
                    item.setInventoryId(getIntSafely(iObj, "inventory_id"));
                    item.setMaterialCode(getStringSafely(iObj, "material_code"));
                    item.setMaterialDescription(getStringSafely(iObj, "material_description"));
                    item.setBaseUom(getStringSafely(iObj, "base_uom"));
                    item.setBatchNumber(getStringSafely(iObj, "batch_number"));
                    item.setStorageBin(getStringSafely(iObj, "storage_bin"));
                    item.setQuantity(getDoubleSafely(iObj, "quantity"));
                    item.setBinCapacityPct(getDoubleSafely(iObj, "bin_capacity_pct"));
                    item.setManufactureDate(getStringSafely(iObj, "manufacture_date"));
                    item.setExpiryDate(getStringSafely(iObj, "expiry_date"));
                    if (iObj.has("days_to_expiry") && !iObj.get("days_to_expiry").isJsonNull()) {
                        item.setDaysToExpiry(iObj.get("days_to_expiry").getAsInt());
                    }
                    item.setExpired(getBooleanSafely(iObj, "is_expired"));
                    item.setExpiringSoon(getBooleanSafely(iObj, "is_expiring_soon"));
                    item.setBatchCost(getDoubleSafely(iObj, "batch_cost"));
                    item.setLineValue(getDoubleSafely(iObj, "line_value"));
                    itemList.add(item);
                }
            }
            dto.setItems(itemList);
        }

        return dto;
    }

    private String getStringSafely(JsonObject obj, String key) {
        return getStringSafely(obj, key, "");
    }

    private String getStringSafely(JsonObject obj, String key, String defaultVal) {
        return (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsString() : defaultVal;
    }

    private int getIntSafely(JsonObject obj, String key) {
        return (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsInt() : 0;
    }

    private double getDoubleSafely(JsonObject obj, String key) {
        return (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) ? obj.get(key).getAsDouble() : 0.0;
    }

    private boolean getBooleanSafely(JsonObject obj, String key) {
        return (obj != null && obj.has(key) && !obj.get(key).isJsonNull()) && obj.get(key).getAsBoolean();
    }
}
