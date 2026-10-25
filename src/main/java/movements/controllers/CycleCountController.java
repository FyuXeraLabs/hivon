package movements.controllers;

import core.api.dao.BinDAO;
import core.api.dao.CycleCountDAO;
import core.api.dao.WarehouseDAO;
import core.api.dao.ZoneDAO;
import models.dto.CycleCountDTO;
import models.dto.CycleCountItemDTO;
import models.dto.StorageBinDTO;
import models.dto.WarehouseDTO;
import models.dto.ZoneDTO;

import java.util.List;

/**
 * Controller for Cycle Count operations.
 * Handles warehouse/zone/bin lookups, creating count plans,
 * recording physical counts, and finalizing counts.
 *
 * @author Sanod
 */
public class CycleCountController {

    private final CycleCountDAO cycleCountDAO;
    private final WarehouseDAO warehouseDAO;
    private final ZoneDAO zoneDAO;
    private final BinDAO binDAO;

    public CycleCountController() {
        this.cycleCountDAO = CycleCountDAO.getInstance();
        this.warehouseDAO = WarehouseDAO.getInstance();
        this.zoneDAO = ZoneDAO.getInstance();
        this.binDAO = BinDAO.getInstance();
    }

    public List<WarehouseDTO> getWarehouses() throws Exception {
        return warehouseDAO.getWarehouses();
    }

    public List<ZoneDTO> getZones(int warehouseId) throws Exception {
        return zoneDAO.getZonesByWarehouse(warehouseId);
    }

    public List<StorageBinDTO> getBins(Integer warehouseId, String zoneCode) throws Exception {
        return binDAO.getBins(warehouseId, zoneCode);
    }

    public StorageBinDTO getBinById(int binId) throws Exception {
        return binDAO.getBinById(binId);
    }

    public boolean freezeBin(int binId) throws Exception {
        return binDAO.freezeBin(binId);
    }

    public boolean unfreezeBin(int binId) throws Exception {
        return binDAO.unfreezeBin(binId);
    }

    public CycleCountDTO startCycleCount(int warehouseId, Integer binId, String zoneCode, String countDate, String countType) throws Exception {
        int countId = cycleCountDAO.createCycleCount(warehouseId, binId, zoneCode, countDate, countType);
        return cycleCountDAO.getCycleCountById(countId);
    }

    public CycleCountDTO getCycleCount(int countId) throws Exception {
        return cycleCountDAO.getCycleCountById(countId);
    }

    public boolean recordCounts(int countId, List<CycleCountItemDTO> items) throws Exception {
        return cycleCountDAO.recordCounts(countId, items);
    }

    public boolean completeCycleCount(int countId) throws Exception {
        return cycleCountDAO.completeCycleCount(countId);
    }
}
