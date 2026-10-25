/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import models.dto.InventoryValuationReportDTO;
import models.dto.InventoryValuationReportDTO.CategoryBreakdownItem;
import models.dto.InventoryValuationReportDTO.TopMaterialItem;
import models.dto.InventoryValuationReportDTO.ValuationItem;
import models.dto.InventoryValuationReportDTO.WarehouseBreakdownItem;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Inventory Valuation Report REST API endpoints
 * Calls /api/reports/inventory-valuation and parses JSON into InventoryValuationReportDTO
 *
 * @author Sanod
 */
public class InventoryValuationReportDAO {

    private static volatile InventoryValuationReportDAO instance;
    private final ApiClient apiClient;

    private InventoryValuationReportDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static InventoryValuationReportDAO getInstance() {
        if (instance == null) {
            synchronized (InventoryValuationReportDAO.class) {
                if (instance == null) {
                    instance = new InventoryValuationReportDAO();
                }
            }
        }
        return instance;
    }

    /**
     * Retrieves inventory valuation report from API
     */
    public InventoryValuationReportDTO getInventoryValuationReport(
            String date,
            String warehouse,
            String valuationMethod,
            boolean includeDamaged,
            boolean groupByGroup) throws Exception {

        String endpoint = "/reports/inventory-valuation";
        List<String> queryParams = new ArrayList<>();

        if (date != null && !date.trim().isEmpty()) {
            queryParams.add("date=" + URLEncoder.encode(date.trim(), StandardCharsets.UTF_8));
        }
        if (warehouse != null && !warehouse.trim().isEmpty() && !"All".equalsIgnoreCase(warehouse.trim())) {
            queryParams.add("warehouse=" + URLEncoder.encode(warehouse.trim(), StandardCharsets.UTF_8));
        }
        if (valuationMethod != null && !valuationMethod.trim().isEmpty()) {
            queryParams.add("valuation_method=" + URLEncoder.encode(valuationMethod.trim(), StandardCharsets.UTF_8));
        }
        if (includeDamaged) {
            queryParams.add("include_damaged=true");
        }
        if (groupByGroup) {
            queryParams.add("group_by_material_group=true");
        }

        if (!queryParams.isEmpty()) {
            endpoint += "?" + String.join("&", queryParams);
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equalsIgnoreCase(getStringSafely(response, "status"))) {
            throw new Exception("Failed to fetch inventory valuation report from server.");
        }

        JsonObject dataObj = response.getAsJsonObject("data");
        if (dataObj == null) {
            return new InventoryValuationReportDTO();
        }

        InventoryValuationReportDTO dto = new InventoryValuationReportDTO();
        dto.setValuationDate(getStringSafely(dataObj, "valuation_date"));
        dto.setValuationMethod(getStringSafely(dataObj, "valuation_method"));
        dto.setTotalMaterialsCount(getIntSafely(dataObj, "total_materials_count"));
        dto.setTotalQuantity(getDoubleSafely(dataObj, "total_quantity"));
        dto.setTotalValue(getDoubleSafely(dataObj, "total_value"));
        dto.setCurrency(getStringSafely(dataObj, "currency", "$"));

        // Items list
        if (dataObj.has("items") && dataObj.get("items").isJsonArray()) {
            JsonArray itemsArray = dataObj.getAsJsonArray("items");
            List<ValuationItem> itemList = new ArrayList<>();
            for (JsonElement elem : itemsArray) {
                if (elem.isJsonObject()) {
                    JsonObject iObj = elem.getAsJsonObject();
                    itemList.add(new ValuationItem(
                        getStringSafely(iObj, "material_code"),
                        getStringSafely(iObj, "material_name"),
                        getStringSafely(iObj, "material_group"),
                        getDoubleSafely(iObj, "quantity"),
                        getStringSafely(iObj, "base_uom"),
                        getDoubleSafely(iObj, "unit_cost"),
                        getDoubleSafely(iObj, "total_value")
                    ));
                }
            }
            dto.setItems(itemList);
        }

        // Category breakdown (for pie chart)
        String catKey = dataObj.has("by_category") ? "by_category" : "by_group";
        if (dataObj.has(catKey) && dataObj.get(catKey).isJsonArray()) {
            JsonArray catArray = dataObj.getAsJsonArray(catKey);
            List<CategoryBreakdownItem> catList = new ArrayList<>();
            for (JsonElement elem : catArray) {
                if (elem.isJsonObject()) {
                    JsonObject cObj = elem.getAsJsonObject();
                    String name = getStringSafely(cObj, "category_name");
                    if (name.isEmpty()) {
                        name = getStringSafely(cObj, "material_group");
                    }
                    catList.add(new CategoryBreakdownItem(
                        name,
                        getDoubleSafely(cObj, "quantity"),
                        getDoubleSafely(cObj, "value"),
                        getDoubleSafely(cObj, "percentage")
                    ));
                }
            }
            dto.setByCategoryList(catList);
        }

        // Warehouse breakdown (for bar chart)
        if (dataObj.has("by_warehouse") && dataObj.get("by_warehouse").isJsonArray()) {
            JsonArray whArray = dataObj.getAsJsonArray("by_warehouse");
            List<WarehouseBreakdownItem> whList = new ArrayList<>();
            for (JsonElement elem : whArray) {
                if (elem.isJsonObject()) {
                    JsonObject wObj = elem.getAsJsonObject();
                    whList.add(new WarehouseBreakdownItem(
                        getStringSafely(wObj, "warehouse_code"),
                        getStringSafely(wObj, "warehouse_name"),
                        getDoubleSafely(wObj, "quantity"),
                        getDoubleSafely(wObj, "value"),
                        getDoubleSafely(wObj, "percentage")
                    ));
                }
            }
            dto.setByWarehouseList(whList);
        }

        // Top materials (for horizontal bar chart)
        if (dataObj.has("top_materials") && dataObj.get("top_materials").isJsonArray()) {
            JsonArray topArray = dataObj.getAsJsonArray("top_materials");
            List<TopMaterialItem> topList = new ArrayList<>();
            for (JsonElement elem : topArray) {
                if (elem.isJsonObject()) {
                    JsonObject tObj = elem.getAsJsonObject();
                    topList.add(new TopMaterialItem(
                        getStringSafely(tObj, "material_code"),
                        getStringSafely(tObj, "material_name"),
                        getDoubleSafely(tObj, "value"),
                        getDoubleSafely(tObj, "percentage")
                    ));
                }
            }
            dto.setTopMaterialsList(topList);
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
}
