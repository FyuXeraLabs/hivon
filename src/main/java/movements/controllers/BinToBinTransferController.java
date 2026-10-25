package movements.controllers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import core.api.dao.BinToBinTransferDAO;
import core.api.dao.WarehouseDAO;
import models.dto.BinToBinDestinationBinDTO;
import models.dto.BinToBinSourceBinDTO;
import models.dto.BinToBinTransferItemDTO;
import models.dto.MaterialDTO;
import models.dto.WarehouseDTO;

import java.util.List;

/**
 * Controller for Bin-to-Bin Transfer operations.
 * Coordinates warehouse retrieval, material searching within a warehouse,
 * source bin inventory lookups, destination bin validation, and transfer order processing.
 *
 * @author Sanod
 */
public class BinToBinTransferController {

    private final WarehouseDAO warehouseDAO;
    private final BinToBinTransferDAO transferDAO;

    public BinToBinTransferController() {
        this.warehouseDAO = WarehouseDAO.getInstance();
        this.transferDAO = BinToBinTransferDAO.getInstance();
    }

    public List<WarehouseDTO> getWarehouses() throws Exception {
        return warehouseDAO.getWarehouses();
    }

    public List<MaterialDTO> searchMaterialsInWarehouse(int warehouseId, String query) throws Exception {
        return transferDAO.searchMaterialsInWarehouse(warehouseId, query);
    }

    public List<BinToBinSourceBinDTO> getSourceBins(int warehouseId, int materialId) throws Exception {
        return transferDAO.getSourceBins(warehouseId, materialId);
    }

    public List<BinToBinDestinationBinDTO> getDestinationBins(int warehouseId) throws Exception {
        return transferDAO.getDestinationBins(warehouseId);
    }

    public JsonObject completeBinToBinTransfer(List<BinToBinTransferItemDTO> items, String reason, String remarks) throws Exception {
        if (items == null || items.isEmpty()) {
            throw new Exception("No transfer items to process");
        }

        JsonObject payload = new JsonObject();
        String docInfo = "";
        if (reason != null && !reason.trim().isEmpty()) {
            docInfo += reason.trim();
        }
        if (remarks != null && !remarks.trim().isEmpty()) {
            docInfo = docInfo.isEmpty() ? remarks.trim() : (docInfo + " - " + remarks.trim());
        }
        payload.addProperty("source_document", docInfo);

        JsonArray itemsArray = new JsonArray();
        for (BinToBinTransferItemDTO item : items) {
            JsonObject itemObj = new JsonObject();
            itemObj.addProperty("material_id", item.getMaterialId());
            itemObj.addProperty("from_bin_id", item.getFromBinId());
            itemObj.addProperty("to_bin_id", item.getToBinId());
            itemObj.addProperty("quantity", item.getQuantity());
            itemObj.addProperty("uom", item.getUom() != null && !item.getUom().isEmpty() ? item.getUom() : "EA");
            if (item.getBatchId() != null) {
                itemObj.addProperty("batch_id", item.getBatchId());
            }
            itemsArray.add(itemObj);
        }
        payload.add("items", itemsArray);

        return transferDAO.completeBinToBinTransfer(payload);
    }
}
