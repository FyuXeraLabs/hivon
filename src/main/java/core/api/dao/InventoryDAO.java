package core.api.dao;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.api.ApiConfig;
import core.logging.Logger;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for querying inventory stock levels via REST API.
 *
 * @author Sanod
 */
public class InventoryDAO {

    private static volatile InventoryDAO instance;
    private final ApiClient apiClient;

    private InventoryDAO() {
        this.apiClient = ApiClient.getInstance();
    }

    public static InventoryDAO getInstance() {
        if (instance == null) {
            synchronized (InventoryDAO.class) {
                if (instance == null) {
                    instance = new InventoryDAO();
                }
            }
        }
        return instance;
    }

    // GET /api/inventory/stock?bin_id={binId}
    public List<Object[]> getStockByBinId(int binId) throws Exception {
        String endpoint = ApiConfig.INVENTORY + "?bin_id=" + binId;

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<Object[]> stockRows = new ArrayList<>();
        JsonArray dataArray = body.getAsJsonArray("data");

        if (dataArray != null) {
            for (JsonElement elem : dataArray) {
                JsonObject row = elem.getAsJsonObject();

                String materialCode = row.has("material_code") && !row.get("material_code").isJsonNull()
                        ? row.get("material_code").getAsString() : "";
                String materialDesc = row.has("material_description") && !row.get("material_description").isJsonNull()
                        ? row.get("material_description").getAsString() : "";
                String batchNumber = row.has("batch_number") && !row.get("batch_number").isJsonNull()
                        ? row.get("batch_number").getAsString() : "-";
                double quantity = row.has("quantity") && !row.get("quantity").isJsonNull()
                        ? row.get("quantity").getAsDouble() : 0.0;
                String uom = row.has("base_uom") && !row.get("base_uom").isJsonNull()
                        ? row.get("base_uom").getAsString() : "";

                stockRows.add(new Object[]{materialCode, materialDesc, batchNumber, quantity, uom});
            }
        }
        return stockRows;
    }
}
