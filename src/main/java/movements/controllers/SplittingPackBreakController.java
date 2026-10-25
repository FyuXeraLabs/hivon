package movements.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import core.api.dao.SplittingPackBreakDAO;
import models.dto.BinToBinDestinationBinDTO;
import models.dto.SplittingItemDTO;
import models.dto.SplittingMaterialDTO;
import models.dto.SplittingSourceBinDTO;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Controller for Package Splitting / Pack Break (INT18).
 *
 * @author Ishani
 */
public class SplittingPackBreakController {

    private final SplittingPackBreakDAO dao;

    public SplittingPackBreakController() {
        this.dao = SplittingPackBreakDAO.getInstance();
    }

    public List<SplittingMaterialDTO> searchMaterials(Integer warehouseId, String query) throws Exception {
        return dao.searchMaterials(warehouseId, query);
    }

    public List<SplittingSourceBinDTO> getSourceBins(int materialId, Integer warehouseId) throws Exception {
        return dao.getSourceBins(materialId, warehouseId);
    }

    public List<BinToBinDestinationBinDTO> getDestinationBins(Integer warehouseId) throws Exception {
        return dao.getDestinationBins(warehouseId);
    }

    public JsonObject completePackageBreaking(List<SplittingItemDTO> items, String referenceDoc) throws Exception {
        if (items == null || items.isEmpty()) {
            throw new Exception("Please add packages to break before completing transaction.");
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("reference_document", referenceDoc != null && !referenceDoc.trim().isEmpty() ? referenceDoc.trim() : "SPLIT-" + System.currentTimeMillis());
        payload.addProperty("reference_date", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));

        JsonArray itemsArray = new JsonArray();
        for (SplittingItemDTO item : items) {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("material_id", item.getMaterialId());
            itemObj.addProperty("bin_id", item.getSourceBinId());
            itemObj.addProperty("dest_bin_id", item.getDestBinId());
            itemObj.addProperty("packs", item.getPacks());
            itemObj.addProperty("units_per_pack", item.getUnitsPerPack());
            itemObj.addProperty("pack_uom", item.getPackUom());
            itemObj.addProperty("base_uom", item.getBaseUom());
            if (item.getBatchId() != null) {
                itemObj.addProperty("batch_id", item.getBatchId());
            }
            itemsArray.add(itemObj);
        }
        payload.add("items", itemsArray);

        return dao.completePackageBreaking(payload);
    }
}
