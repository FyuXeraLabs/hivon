package core.api.dao;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.api.ApiConfig;
import models.dto.BinToBinDestinationBinDTO;
import models.dto.SplittingMaterialDTO;
import models.dto.SplittingSourceBinDTO;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Package Splitting / Breaking (INT18).
 *
 * @author Ishani
 */
public class SplittingPackBreakDAO {

    private static volatile SplittingPackBreakDAO instance;
    private final ApiClient apiClient;
    private final Gson gson;

    private SplittingPackBreakDAO() {
        this.apiClient = ApiClient.getInstance();
        this.gson = new Gson();
    }

    public static SplittingPackBreakDAO getInstance() {
        if (instance == null) {
            synchronized (SplittingPackBreakDAO.class) {
                if (instance == null) {
                    instance = new SplittingPackBreakDAO();
                }
            }
        }
        return instance;
    }

    // GET /api/movements/splitting/materials?warehouse_id=X&q=Y
    public List<SplittingMaterialDTO> searchMaterials(Integer warehouseId, String query) throws Exception {
        String endpoint = ApiConfig.MOVEMENTS_SPLITTING_MATERIALS;
        List<String> queryParams = new ArrayList<>();
        if (warehouseId != null && warehouseId > 0) {
            queryParams.add("warehouse_id=" + warehouseId);
        }
        if (query != null && !query.trim().isEmpty()) {
            queryParams.add("q=" + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8));
        }
        if (!queryParams.isEmpty()) {
            endpoint += "?" + String.join("&", queryParams);
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<SplittingMaterialDTO> list = new ArrayList<>();
        if (body.has("data") && body.get("data").isJsonArray()) {
            JsonArray array = body.getAsJsonArray("data");
            for (JsonElement elem : array) {
                SplittingMaterialDTO dto = gson.fromJson(elem, SplittingMaterialDTO.class);
                list.add(dto);
            }
        }
        return list;
    }

    // GET /api/movements/splitting/source-bins?material_id=X&warehouse_id=Y
    public List<SplittingSourceBinDTO> getSourceBins(int materialId, Integer warehouseId) throws Exception {
        String endpoint = ApiConfig.MOVEMENTS_SPLITTING_SOURCE_BINS + "?material_id=" + materialId;
        if (warehouseId != null && warehouseId > 0) {
            endpoint += "&warehouse_id=" + warehouseId;
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<SplittingSourceBinDTO> list = new ArrayList<>();
        if (body.has("data") && body.get("data").isJsonArray()) {
            JsonArray array = body.getAsJsonArray("data");
            for (JsonElement elem : array) {
                SplittingSourceBinDTO dto = gson.fromJson(elem, SplittingSourceBinDTO.class);
                list.add(dto);
            }
        }
        return list;
    }

    // GET /api/movements/splitting/dest-bins?warehouse_id=X
    public List<BinToBinDestinationBinDTO> getDestinationBins(Integer warehouseId) throws Exception {
        String endpoint = ApiConfig.MOVEMENTS_SPLITTING_DEST_BINS;
        if (warehouseId != null && warehouseId > 0) {
            endpoint += "?warehouse_id=" + warehouseId;
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<BinToBinDestinationBinDTO> list = new ArrayList<>();
        if (body.has("data") && body.get("data").isJsonArray()) {
            JsonArray array = body.getAsJsonArray("data");
            for (JsonElement elem : array) {
                BinToBinDestinationBinDTO dto = gson.fromJson(elem, BinToBinDestinationBinDTO.class);
                list.add(dto);
            }
        }
        return list;
    }

    // POST /api/movements/splitting
    public JsonObject completePackageBreaking(JsonObject payload) throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.MOVEMENTS_SPLITTING)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        if (!"success".equalsIgnoreCase(body.get("status").getAsString())) {
            String message = body.has("message") ? body.get("message").getAsString() : "Breaking transaction failed";
            throw new Exception(message);
        }
        return body;
    }
}
