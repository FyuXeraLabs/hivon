package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.api.ApiConfig;
import models.dto.SalesOrderDTO;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Sales Order REST API endpoints.
 * Handles fetching sales orders filtered by customer.
 *
 * @author Sanod
 */
public class SalesOrderDAO {

    private static volatile SalesOrderDAO instance;
    private final ApiClient apiClient;

    private SalesOrderDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static SalesOrderDAO getInstance() {
        if (instance == null) {
            synchronized (SalesOrderDAO.class) {
                if (instance == null) {
                    instance = new SalesOrderDAO();
                }
            }
        }
        return instance;
    }

    // GET /api/sales-orders?customer_id={customerId}
    public List<SalesOrderDTO> getSalesOrdersByCustomerId(int customerId) throws Exception {
        String endpoint = ApiConfig.SALES_ORDERS + "?customer_id=" + customerId;

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<SalesOrderDTO> sos = new ArrayList<>();
        JsonArray dataArray = body.getAsJsonArray("data");

        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                sos.add(jsonToSalesOrderDTO(elem.getAsJsonObject()));
            }
        }
        return sos;
    }

    // converts a JSON object from the API response into a SalesOrderDTO
    private SalesOrderDTO jsonToSalesOrderDTO(JsonObject json) {
        SalesOrderDTO dto = new SalesOrderDTO();

        if (json.has("so_id") && !json.get("so_id").isJsonNull()) {
            dto.setSoId(json.get("so_id").getAsInt());
        }
        if (json.has("so_number") && !json.get("so_number").isJsonNull()) {
            dto.setSoNumber(json.get("so_number").getAsString());
        }
        if (json.has("customer_name") && !json.get("customer_name").isJsonNull()) {
            dto.setCustomerName(json.get("customer_name").getAsString());
        }
        if (json.has("order_date") && !json.get("order_date").isJsonNull()) {
            dto.setOrderDate(json.get("order_date").getAsString());
        }
        if (json.has("status") && !json.get("status").isJsonNull()) {
            dto.setStatus(json.get("status").getAsString());
        }

        return dto;
    }
}
