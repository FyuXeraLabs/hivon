package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.api.ApiConfig;
import models.dto.WarehouseDTO;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Warehouse-related API endpoints.
 *
 * @author Sanod
 */
public class WarehouseDAO {

    private static volatile WarehouseDAO instance;
    private final ApiClient apiClient;

    private WarehouseDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static WarehouseDAO getInstance() {
        if (instance == null) {
            synchronized (WarehouseDAO.class) {
                if (instance == null) {
                    instance = new WarehouseDAO();
                }
            }
        }
        return instance;
    }

    // GET /api/warehouses
    public List<WarehouseDTO> getWarehouses() throws Exception {
        String endpoint = ApiConfig.WAREHOUSES;
        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<WarehouseDTO> list = new ArrayList<>();
        JsonArray dataArray = body.getAsJsonArray("data");
        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                list.add(jsonToWarehouseDTO(elem.getAsJsonObject()));
            }
        }
        return list;
    }

    private WarehouseDTO jsonToWarehouseDTO(JsonObject json) {
        WarehouseDTO dto = new WarehouseDTO();
        if (json.has("warehouse_id") && !json.get("warehouse_id").isJsonNull()) {
            dto.setWarehouseId(json.get("warehouse_id").getAsInt());
        }
        if (json.has("warehouse_code") && !json.get("warehouse_code").isJsonNull()) {
            dto.setWarehouseCode(json.get("warehouse_code").getAsString());
        }
        if (json.has("warehouse_name") && !json.get("warehouse_name").isJsonNull()) {
            dto.setWarehouseName(json.get("warehouse_name").getAsString());
        }
        if (json.has("location") && !json.get("location").isJsonNull()) {
            dto.setLocation(json.get("location").getAsString());
        }
        if (json.has("contact_person") && !json.get("contact_person").isJsonNull()) {
            dto.setContactPerson(json.get("contact_person").getAsString());
        }
        if (json.has("phone") && !json.get("phone").isJsonNull()) {
            dto.setPhone(json.get("phone").getAsString());
        }
        if (json.has("is_active") && !json.get("is_active").isJsonNull()) {
            dto.setIsActive(json.get("is_active").getAsBoolean());
        }
        return dto;
    }
}
