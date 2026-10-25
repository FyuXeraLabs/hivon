/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import models.dto.StockAgingReportDTO;
import models.dto.StockAgingReportDTO.StockAgingItem;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Stock Aging Report REST API endpoints
 * Calls /api/reports/stock-aging and parses JSON into StockAgingReportDTO
 *
 * @author Sanod
 */
public class StockAgingReportDAO {

    private static volatile StockAgingReportDAO instance;
    private final ApiClient apiClient;

    private StockAgingReportDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static StockAgingReportDAO getInstance() {
        if (instance == null) {
            synchronized (StockAgingReportDAO.class) {
                if (instance == null) {
                    instance = new StockAgingReportDAO();
                }
            }
        }
        return instance;
    }

    /**
     * Retrieves stock aging report from API
     */
    public StockAgingReportDTO getStockAgingReport(
            String warehouse,
            String ageRange,
            boolean slowMovingOnly,
            boolean potentialObsoleteOnly) throws Exception {

        String endpoint = "/reports/stock-aging";
        List<String> queryParams = new ArrayList<>();

        if (warehouse != null && !warehouse.trim().isEmpty() && !"All".equalsIgnoreCase(warehouse.trim())) {
            queryParams.add("warehouse=" + URLEncoder.encode(warehouse.trim(), StandardCharsets.UTF_8));
        }
        if (ageRange != null && !ageRange.trim().isEmpty() && !"All".equalsIgnoreCase(ageRange.trim())) {
            queryParams.add("age_range=" + URLEncoder.encode(ageRange.trim(), StandardCharsets.UTF_8));
        }
        if (slowMovingOnly) {
            queryParams.add("slow_moving_only=true");
        }
        if (potentialObsoleteOnly) {
            queryParams.add("potential_obsolete_only=true");
        }

        if (!queryParams.isEmpty()) {
            endpoint += "?" + String.join("&", queryParams);
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equalsIgnoreCase(getStringSafely(response, "status"))) {
            throw new Exception("Failed to fetch stock aging report from server.");
        }

        JsonObject dataObj = response.getAsJsonObject("data");
        if (dataObj == null) {
            return new StockAgingReportDTO();
        }

        StockAgingReportDTO dto = new StockAgingReportDTO();
        dto.setGeneratedAt(getStringSafely(dataObj, "generated_at"));
        dto.setTotalMaterialsAnalyzed(getIntSafely(dataObj, "total_materials_analyzed"));
        dto.setAverageAgeOfInventory(getDoubleSafely(dataObj, "average_age_of_inventory"));
        dto.setSlowMovingMaterialsCount(getIntSafely(dataObj, "slow_moving_materials_count"));
        dto.setPotentiallyObsoleteCount(getIntSafely(dataObj, "potentially_obsolete_count"));
        dto.setHighValueObsolete(getDoubleSafely(dataObj, "high_value_obsolete"));
        dto.setCurrency(getStringSafely(dataObj, "currency", "$"));

        if (dataObj.has("items") && dataObj.get("items").isJsonArray()) {
            JsonArray itemsArray = dataObj.getAsJsonArray("items");
            List<StockAgingItem> itemList = new ArrayList<>();
            for (JsonElement elem : itemsArray) {
                if (elem.isJsonObject()) {
                    JsonObject iObj = elem.getAsJsonObject();
                    StockAgingItem item = new StockAgingItem();
                    item.setMaterialId(getIntSafely(iObj, "material_id"));
                    item.setMaterialCode(getStringSafely(iObj, "material_code"));
                    item.setMaterialName(getStringSafely(iObj, "material_name"));
                    item.setCurrentQty(getDoubleSafely(iObj, "current_qty"));
                    item.setDaysInStock(getIntSafely(iObj, "days_in_stock"));
                    item.setLastMovementDate(getStringSafely(iObj, "last_movement_date"));
                    item.setAvgMonthlyUsage(getDoubleSafely(iObj, "avg_monthly_usage"));
                    item.setDaysOfSupply(getDoubleSafely(iObj, "days_of_supply"));
                    item.setTurnoverRate(getDoubleSafely(iObj, "turnover_rate"));
                    item.setRecommendation(getStringSafely(iObj, "recommendation"));
                    item.setUnitCost(getDoubleSafely(iObj, "unit_cost"));
                    item.setTotalValue(getDoubleSafely(iObj, "total_value"));
                    item.setSlowMoving(getBooleanSafely(iObj, "is_slow_moving"));
                    item.setObsolete(getBooleanSafely(iObj, "is_obsolete"));
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
