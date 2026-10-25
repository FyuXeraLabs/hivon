/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package reports.controllers;

import core.api.dao.UtilizationReportDAO;
import core.api.dao.WarehouseDAO;
import core.api.dao.ZoneDAO;
import core.logging.Logger;
import core.security.UserSession;
import core.utils.RetryHelper;
import models.dto.UtilizationReportDTO;
import models.dto.WarehouseDTO;
import models.dto.ZoneDTO;

import java.util.List;

/**
 * Controller for Warehouse Utilization Report (REP65)
 * Orchestrates warehouse utilization report generation, warehouse, and zone loading
 *
 * @author Sanod
 */
public class UtilizationReportController {

    private final String username = UserSession.getInstance().getUsername();

    public UtilizationReportController() {
    }

    /**
     * Generates warehouse utilization report for specified parameters
     */
    public UtilizationReportDTO generateUtilizationReport(
            String warehouse,
            String zone,
            String fromDate,
            String toDate) throws Exception {

        UtilizationReportDTO dto = RetryHelper.executeWithRetry(
            () -> UtilizationReportDAO.getInstance().getUtilizationReport(
                warehouse, zone, fromDate, toDate
            ),
            "failed to generate warehouse utilization report"
        );

        if (dto != null) {
            Logger.log(username, "generated warehouse utilization report for warehouse: " + warehouse + ", zone: " + zone);
        }
        return dto;
    }

    /**
     * Fetches list of active warehouses for filter dropdown
     */
    public List<WarehouseDTO> getWarehouses() throws Exception {
        return RetryHelper.executeWithRetry(
            () -> WarehouseDAO.getInstance().getWarehouses(),
            "failed to load warehouses"
        );
    }

    /**
     * Fetches list of zones for filter dropdown
     */
    public List<ZoneDTO> getZones(Integer warehouseId) throws Exception {
        return RetryHelper.executeWithRetry(
            () -> (warehouseId != null && warehouseId > 0)
                ? ZoneDAO.getInstance().getZonesByWarehouse(warehouseId)
                : ZoneDAO.getInstance().getAllZones(),
            "failed to load zones"
        );
    }
}
