package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.api.ApiConfig;
import core.logging.Logger;
import models.dto.StorageBinDTO;

import java.net.http.HttpRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BinDAO {

    private static volatile BinDAO instance;
    private final ApiClient apiClient;

    private static final DateTimeFormatter DT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private BinDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static BinDAO getInstance() {
        if (instance == null) {
            synchronized (BinDAO.class) {
                if (instance == null) {
                    instance = new BinDAO();
                }
            }
        }
        return instance;
    }

    public List<StorageBinDTO> getBins(String searchTerm) throws Exception {
        String endpoint = ApiConfig.BINS;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            endpoint += "?q=" + java.net.URLEncoder.encode(searchTerm, "UTF-8");
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<StorageBinDTO> bins = new ArrayList<>();
        JsonArray dataArray = body.getAsJsonArray("data");

        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                bins.add(jsonToBinDTO(elem.getAsJsonObject()));
            }
        }
        return bins;
    }

    public List<StorageBinDTO> getBinsByZoneCode(String zoneCode) throws Exception {
        String endpoint = ApiConfig.BINS;
        if (zoneCode != null && !zoneCode.trim().isEmpty()) {
            endpoint += "?zone_code=" + java.net.URLEncoder.encode(zoneCode, "UTF-8");
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<StorageBinDTO> bins = new ArrayList<>();
        JsonArray dataArray = body.getAsJsonArray("data");

        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                bins.add(jsonToBinDTO(elem.getAsJsonObject()));
            }
        }
        return bins;
    }


    public StorageBinDTO getBinById(int binId) throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.BINS + "/" + binId)
                .GET().build();
        JsonObject body = apiClient.executeWithAuth(request);
        JsonObject data = body.getAsJsonObject("data");
        return jsonToBinDTO(data);
    }

    public int createBin(StorageBinDTO binDto) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("bin_code", binDto.getBinCode());
        payload.addProperty("warehouse_id", binDto.getWarehouseId());
        if (binDto.getZone() != null) {
            payload.addProperty("zone", binDto.getZone());
        }
        payload.addProperty("max_capacity", binDto.getMaxCapacity());
        payload.addProperty("used_capacity", binDto.getUsedCapacity());
        payload.addProperty("is_active", binDto.getIsActive() != null ? binDto.getIsActive() : true);

        HttpRequest request = apiClient.authRequest(ApiConfig.BINS)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        JsonObject data = body.getAsJsonObject("data");

        return data.get("bin_id").getAsInt();
    }

    public boolean updateBin(StorageBinDTO binDto) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("bin_code", binDto.getBinCode());
        payload.addProperty("warehouse_id", binDto.getWarehouseId());
        if (binDto.getZone() != null) {
            payload.addProperty("zone", binDto.getZone());
        }
        payload.addProperty("max_capacity", binDto.getMaxCapacity());
        payload.addProperty("used_capacity", binDto.getUsedCapacity());
        if (binDto.getIsActive() != null) {
            payload.addProperty("is_active", binDto.getIsActive());
        }

        HttpRequest request = apiClient.authRequest(ApiConfig.BINS + "/" + binDto.getBinId())
                .PUT(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    public boolean deleteBin(int binId) throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.BINS + "/" + binId)
                .DELETE().build();

        JsonObject body = apiClient.executeWithAuth(request);
        return "success".equals(body.get("status").getAsString());
    }

    public List<StorageBinDTO> generateBins(int warehouseId, String zone, String prefix,
                                              int startNum, int endNum, int digits) throws Exception {
        JsonObject payload = new JsonObject();
        payload.addProperty("warehouse_id", warehouseId);
        payload.addProperty("zone", zone);
        payload.addProperty("prefix", prefix);
        payload.addProperty("start_num", startNum);
        payload.addProperty("end_num", endNum);
        payload.addProperty("digits", digits);

        HttpRequest request = apiClient.authRequest(ApiConfig.BINS_GENERATE)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        JsonArray dataArray = body.getAsJsonArray("data");

        List<StorageBinDTO> bins = new ArrayList<>();
        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                bins.add(jsonToBinDTO(elem.getAsJsonObject()));
            }
        }
        return bins;
    }

    private StorageBinDTO jsonToBinDTO(JsonObject json) {
        StorageBinDTO dto = new StorageBinDTO();

        if (json.has("bin_id") && !json.get("bin_id").isJsonNull()) {
            dto.setBinId(json.get("bin_id").getAsInt());
        }
        if (json.has("bin_code") && !json.get("bin_code").isJsonNull()) {
            dto.setBinCode(json.get("bin_code").getAsString());
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
        if (json.has("zone") && !json.get("zone").isJsonNull()) {
            dto.setZone(json.get("zone").getAsString());
        } else if (json.has("zone_code") && !json.get("zone_code").isJsonNull()) {
            dto.setZone(json.get("zone_code").getAsString());
        }
        if (json.has("max_capacity") && !json.get("max_capacity").isJsonNull()) {
            dto.setMaxCapacity(json.get("max_capacity").getAsDouble());
        }
        if (json.has("used_capacity") && !json.get("used_capacity").isJsonNull()) {
            dto.setUsedCapacity(json.get("used_capacity").getAsDouble());
        }
        if (json.has("is_active") && !json.get("is_active").isJsonNull()) {
            dto.setIsActive(json.get("is_active").getAsBoolean());
        }
        if (json.has("created_date") && !json.get("created_date").isJsonNull()) {
            dto.setCreatedDate(parseDateTime(json.get("created_date").getAsString()));
        }
        if (json.has("last_modified") && !json.get("last_modified").isJsonNull()) {
            dto.setModifiedDate(parseDateTime(json.get("last_modified").getAsString()));
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