package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.api.ApiConfig;
import models.dto.ZoneDTO;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Zone REST API endpoints.
 * Handles fetching and searching zones within a warehouse.
 *
 * @author Sanod
 */
public class ZoneDAO {

    private static volatile ZoneDAO instance;
    private final ApiClient apiClient;

    private ZoneDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static ZoneDAO getInstance() {
        if (instance == null) {
            synchronized (ZoneDAO.class) {
                if (instance == null) {
                    instance = new ZoneDAO();
                }
            }
        }
        return instance;
    }

    // GET /api/zones?warehouse_id={warehouseId}
    public List<ZoneDTO> getZonesByWarehouse(int warehouseId) throws Exception {
        String endpoint = ApiConfig.ZONES + "?warehouse_id=" + warehouseId;

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<ZoneDTO> zones = new ArrayList<>();
        JsonArray dataArray = body.getAsJsonArray("data");

        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                zones.add(jsonToZoneDTO(elem.getAsJsonObject()));
            }
        }
        return zones;
    }

    // GET /api/zones?warehouse_id={warehouseId}&q={searchTerm}
    public List<ZoneDTO> searchZonesByWarehouse(int warehouseId, String searchTerm) throws Exception {
        String endpoint = ApiConfig.ZONES + "?warehouse_id=" + warehouseId
                + "&q=" + java.net.URLEncoder.encode(searchTerm, "UTF-8");

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<ZoneDTO> zones = new ArrayList<>();
        JsonArray dataArray = body.getAsJsonArray("data");

        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                zones.add(jsonToZoneDTO(elem.getAsJsonObject()));
            }
        }
        return zones;
    }

    // converts a JSON object from the API response into a ZoneDTO
    private ZoneDTO jsonToZoneDTO(JsonObject json) {
        ZoneDTO dto = new ZoneDTO();

        if (json.has("zone_code") && !json.get("zone_code").isJsonNull()) {
            dto.setZoneCode(json.get("zone_code").getAsString());
        }
        if (json.has("zone_name") && !json.get("zone_name").isJsonNull()) {
            dto.setZoneName(json.get("zone_name").getAsString());
        }
        if (json.has("zone_type") && !json.get("zone_type").isJsonNull()) {
            dto.setZoneType(json.get("zone_type").getAsString());
        }
        if (json.has("description") && !json.get("description").isJsonNull()) {
            dto.setDescription(json.get("description").getAsString());
        }
        if (json.has("total_bins") && !json.get("total_bins").isJsonNull()) {
            dto.setTotalBins(json.get("total_bins").getAsInt());
        }
        if (json.has("total_capacity") && !json.get("total_capacity").isJsonNull()) {
            dto.setTotalCapacity(json.get("total_capacity").getAsBigDecimal());
        }
        if (json.has("current_utilization") && !json.get("current_utilization").isJsonNull()) {
            dto.setCurrentUtilization(json.get("current_utilization").getAsBigDecimal());
        }
        if (json.has("warehouse_id") && !json.get("warehouse_id").isJsonNull()) {
            dto.setWarehouseId(json.get("warehouse_id").getAsInt());
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