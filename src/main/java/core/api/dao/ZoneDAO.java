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

    // GET /api/zones
    public List<ZoneDTO> getAllZones() throws Exception {
        String endpoint = ApiConfig.ZONES;

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

    public int createZone(ZoneDTO zone) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("warehouse_id", zone.getWarehouseId());
        payload.addProperty("zone_code", zone.getZoneCode());
        payload.addProperty("zone_name", zone.getZoneName() != null ? zone.getZoneName() : zone.getZoneCode());
        if (zone.getZoneType() != null) {
            payload.addProperty("zone_type", zone.getZoneType());
        }
        if (zone.getDescription() != null) {
            payload.addProperty("description", zone.getDescription());
        }

        HttpRequest request = apiClient.authRequest(ApiConfig.ZONES)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        if (body.has("data") && body.get("data").isJsonObject()) {
            JsonObject data = body.getAsJsonObject("data");
            if (data.has("zone_id") && !data.get("zone_id").isJsonNull()) {
                return data.get("zone_id").getAsInt();
            }
        }
        return 1;
    }

    public boolean updateZone(ZoneDTO zone) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("warehouse_id", zone.getWarehouseId());
        payload.addProperty("zone_code", zone.getZoneCode());
        payload.addProperty("zone_name", zone.getZoneName() != null ? zone.getZoneName() : zone.getZoneCode());
        if (zone.getZoneType() != null) {
            payload.addProperty("zone_type", zone.getZoneType());
        }
        if (zone.getDescription() != null) {
            payload.addProperty("description", zone.getDescription());
        }

        String endpoint = ApiConfig.ZONES + "/" + (zone.getZoneId() != null ? zone.getZoneId() : 0);
        HttpRequest request = apiClient.authRequest(endpoint)
                .PUT(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    public boolean deleteZone(int zoneId) throws Exception {
        String endpoint = ApiConfig.ZONES + "/" + zoneId;
        HttpRequest request = apiClient.authRequest(endpoint)
                .DELETE()
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    // converts a JSON object from the API response into a ZoneDTO
    private ZoneDTO jsonToZoneDTO(JsonObject json) {
        ZoneDTO dto = new ZoneDTO();

        if (json.has("zone_id") && !json.get("zone_id").isJsonNull()) {
            dto.setZoneId(json.get("zone_id").getAsInt());
        }
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