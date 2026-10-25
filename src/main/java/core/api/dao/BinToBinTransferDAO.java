package core.api.dao;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.api.ApiConfig;
import core.logging.Logger;
import models.dto.BinToBinDestinationBinDTO;
import models.dto.BinToBinSourceBinDTO;
import models.dto.MaterialDTO;

import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Bin-to-Bin Transfer Order REST API endpoints.
 * Handles fetching available materials in a warehouse, source bins with stock,
 * destination bins, and completing transfers.
 *
 * @author Ishani
 */
public class BinToBinTransferDAO {

    private static volatile BinToBinTransferDAO instance;
    private final ApiClient apiClient;
    private final Gson gson;

    private BinToBinTransferDAO() {
        this.apiClient = ApiClient.getInstance();
        this.gson = new Gson();
    }

    public static BinToBinTransferDAO getInstance() {
        if (instance == null) {
            synchronized (BinToBinTransferDAO.class) {
                if (instance == null) {
                    instance = new BinToBinTransferDAO();
                }
            }
        }
        return instance;
    }

    // GET /api/transfer-orders/bin-to-bin/materials?warehouse_id=X&q=Y
    public List<MaterialDTO> searchMaterialsInWarehouse(int warehouseId, String query) throws Exception {
        String endpoint = ApiConfig.TO_BIN_TO_BIN_MATERIALS + "?warehouse_id=" + warehouseId;
        if (query != null && !query.trim().isEmpty()) {
            endpoint += "&q=" + URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        }

        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<MaterialDTO> materials = new ArrayList<>();
        if (body.has("data") && body.get("data").isJsonArray()) {
            JsonArray dataArray = body.getAsJsonArray("data");
            for (JsonElement elem : dataArray) {
                if (elem.isJsonObject()) {
                    JsonObject obj = elem.getAsJsonObject();
                    MaterialDTO dto = new MaterialDTO();
                    if (obj.has("material_id") && !obj.get("material_id").isJsonNull()) {
                        dto.setMaterialId(obj.get("material_id").getAsInt());
                    }
                    if (obj.has("material_code") && !obj.get("material_code").isJsonNull()) {
                        dto.setMaterialCode(obj.get("material_code").getAsString());
                    }
                    if (obj.has("material_description") && !obj.get("material_description").isJsonNull()) {
                        dto.setMaterialDescription(obj.get("material_description").getAsString());
                    }
                    if (obj.has("base_uom") && !obj.get("base_uom").isJsonNull()) {
                        dto.setBaseUom(obj.get("base_uom").getAsString());
                    }
                    if (obj.has("is_batch_managed") && !obj.get("is_batch_managed").isJsonNull()) {
                        dto.setIsBatchManaged(obj.get("is_batch_managed").getAsBoolean());
                    }
                    materials.add(dto);
                }
            }
        }
        return materials;
    }

    // GET /api/transfer-orders/bin-to-bin/source-bins?warehouse_id=X&material_id=Y
    public List<BinToBinSourceBinDTO> getSourceBins(int warehouseId, int materialId) throws Exception {
        String endpoint = ApiConfig.TO_BIN_TO_BIN_SOURCE_BINS + "?warehouse_id=" + warehouseId + "&material_id=" + materialId;
        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<BinToBinSourceBinDTO> sourceBins = new ArrayList<>();
        if (body.has("data") && body.get("data").isJsonArray()) {
            JsonArray dataArray = body.getAsJsonArray("data");
            for (JsonElement elem : dataArray) {
                BinToBinSourceBinDTO dto = gson.fromJson(elem, BinToBinSourceBinDTO.class);
                sourceBins.add(dto);
            }
        }
        return sourceBins;
    }

    // GET /api/transfer-orders/bin-to-bin/dest-bins?warehouse_id=X
    public List<BinToBinDestinationBinDTO> getDestinationBins(int warehouseId) throws Exception {
        String endpoint = ApiConfig.TO_BIN_TO_BIN_DEST_BINS + "?warehouse_id=" + warehouseId;
        HttpRequest request = apiClient.authRequest(endpoint).GET().build();
        JsonObject body = apiClient.executeWithAuth(request);

        List<BinToBinDestinationBinDTO> destBins = new ArrayList<>();
        if (body.has("data") && body.get("data").isJsonArray()) {
            JsonArray dataArray = body.getAsJsonArray("data");
            for (JsonElement elem : dataArray) {
                BinToBinDestinationBinDTO dto = gson.fromJson(elem, BinToBinDestinationBinDTO.class);
                destBins.add(dto);
            }
        }
        return destBins;
    }

    // POST /api/transfer-orders/bin-to-bin
    public JsonObject completeBinToBinTransfer(JsonObject payload) throws Exception {
        HttpRequest request = apiClient.authRequest(ApiConfig.TO_BIN_TO_BIN)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject body = apiClient.executeWithAuth(request);
        if (!"success".equalsIgnoreCase(body.get("status").getAsString())) {
            String message = body.has("message") ? body.get("message").getAsString() : "Transfer failed";
            throw new Exception(message);
        }
        return body;
    }
}
