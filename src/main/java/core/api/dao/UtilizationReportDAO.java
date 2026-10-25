/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import models.dto.UtilizationReportDTO;
import models.dto.UtilizationReportDTO.BinUtilizationItem;
import models.dto.UtilizationReportDTO.TopSpaceConsumerItem;
import models.dto.UtilizationReportDTO.ZoneUtilizationItem;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Warehouse Utilization Report (REP65) REST API endpoints
 * Calls /api/reports/warehouse-utilization and parses JSON into UtilizationReportDTO
 *
 * @author Sanod
 */
public class UtilizationReportDAO {

    private static volatile UtilizationReportDAO instance;
    private final ApiClient apiClient;

    private UtilizationReportDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static UtilizationReportDAO getInstance() {
        if (instance == null) {
            synchronized (UtilizationReportDAO.class) {
                if (instance == null) {
                    instance = new UtilizationReportDAO();
                }
            }
        }
        return instance;
    }

    /**
     * Retrieves warehouse utilization report from API
     */
    public UtilizationReportDTO getUtilizationReport(
            String warehouse,
            String zone,
            String fromDate,
            String toDate) throws Exception {

        String endpoint = "/reports/warehouse-utilization";
        List<String> queryParams = new ArrayList<>();

        if (warehouse != null && !warehouse.trim().isEmpty() && !"All".equalsIgnoreCase(warehouse.trim())) {
            queryParams.add("warehouse=" + URLEncoder.encode(warehouse.trim(), StandardCharsets.UTF_8));
        }
        if (zone != null && !zone.trim().isEmpty() && !"All".equalsIgnoreCase(zone.trim())) {
            queryParams.add("zone=" + URLEncoder.encode(zone.trim(), StandardCharsets.UTF_8));
        }
        if (fromDate != null && !fromDate.trim().isEmpty()) {
            queryParams.add("from_date=" + URLEncoder.encode(fromDate.trim(), StandardCharsets.UTF_8));
        }
        if (toDate != null && !toDate.trim().isEmpty()) {
            queryParams.add("to_date=" + URLEncoder.encode(toDate.trim(), StandardCharsets.UTF_8));
        }

        if (!queryParams.isEmpty()) {
            endpoint += "?" + String.join("&", queryParams);
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject response = apiClient.executeWithAuth(request);

        if (response == null || !"success".equalsIgnoreCase(getStringSafely(response, "status"))) {
            throw new Exception("Failed to fetch warehouse utilization report from server.");
        }

        JsonObject dataObj = response.getAsJsonObject("data");
        if (dataObj == null) {
            return new UtilizationReportDTO();
        }

        UtilizationReportDTO dto = new UtilizationReportDTO();
        dto.setGeneratedAt(getStringSafely(dataObj, "generated_at"));
        dto.setTotalWarehouseCapacity(getDoubleSafely(dataObj, "total_warehouse_capacity"));
        dto.setCurrentStockValue(getDoubleSafely(dataObj, "current_stock_value"));
        dto.setCurrency(getStringSafely(dataObj, "currency", "$"));
        dto.setOverallUtilizationPct(getDoubleSafely(dataObj, "overall_utilization_pct"));
        dto.setEmptyBins(getIntSafely(dataObj, "empty_bins"));
        dto.setFullyOccupiedBins(getIntSafely(dataObj, "fully_occupied_bins"));
        dto.setAverageBinUtilization(getDoubleSafely(dataObj, "average_bin_utilization"));

        // Bin utilization list
        if (dataObj.has("bin_utilization") && dataObj.get("bin_utilization").isJsonArray()) {
            JsonArray binArray = dataObj.getAsJsonArray("bin_utilization");
            List<BinUtilizationItem> binList = new ArrayList<>();
            for (JsonElement elem : binArray) {
                if (elem.isJsonObject()) {
                    JsonObject bObj = elem.getAsJsonObject();
                    BinUtilizationItem item = new BinUtilizationItem(
                        getIntSafely(bObj, "bin_id"),
                        getStringSafely(bObj, "bin_code"),
                        getStringSafely(bObj, "zone"),
                        getIntSafely(bObj, "current_items"),
                        getDoubleSafely(bObj, "current_qty"),
                        getDoubleSafely(bObj, "max_capacity"),
                        getDoubleSafely(bObj, "utilization_pct"),
                        getStringSafely(bObj, "status", "Optimal")
                    );
                    binList.add(item);
                }
            }
            dto.setBinUtilizationList(binList);
        }

        // Zone utilization list (for bar chart)
        if (dataObj.has("zone_utilization") && dataObj.get("zone_utilization").isJsonArray()) {
            JsonArray zoneArray = dataObj.getAsJsonArray("zone_utilization");
            List<ZoneUtilizationItem> zoneList = new ArrayList<>();
            for (JsonElement elem : zoneArray) {
                if (elem.isJsonObject()) {
                    JsonObject zObj = elem.getAsJsonObject();
                    ZoneUtilizationItem item = new ZoneUtilizationItem(
                        getStringSafely(zObj, "zone_name"),
                        getIntSafely(zObj, "total_bins"),
                        getDoubleSafely(zObj, "total_capacity"),
                        getDoubleSafely(zObj, "current_qty"),
                        getDoubleSafely(zObj, "utilization_pct")
                    );
                    zoneList.add(item);
                }
            }
            dto.setZoneUtilizationList(zoneList);
        }

        // Top space consumers list
        if (dataObj.has("top_space_consumers") && dataObj.get("top_space_consumers").isJsonArray()) {
            JsonArray topArray = dataObj.getAsJsonArray("top_space_consumers");
            List<TopSpaceConsumerItem> topList = new ArrayList<>();
            for (JsonElement elem : topArray) {
                if (elem.isJsonObject()) {
                    JsonObject tObj = elem.getAsJsonObject();
                    TopSpaceConsumerItem item = new TopSpaceConsumerItem(
                        getStringSafely(tObj, "material_code"),
                        getStringSafely(tObj, "material_name"),
                        getIntSafely(tObj, "bins_used"),
                        getDoubleSafely(tObj, "volume_qty"),
                        getStringSafely(tObj, "space_pct", "0.0%")
                    );
                    topList.add(item);
                }
            }
            dto.setTopSpaceConsumersList(topList);
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
