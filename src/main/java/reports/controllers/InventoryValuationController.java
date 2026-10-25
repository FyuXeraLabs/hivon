/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package reports.controllers;

import core.api.dao.InventoryValuationReportDAO;
import core.api.dao.WarehouseDAO;
import core.logging.Logger;
import core.security.UserSession;
import core.utils.RetryHelper;
import models.dto.InventoryValuationReportDTO;
import models.dto.WarehouseDTO;

import java.util.List;

/**
 * Controller for Inventory Valuation Report (REP62)
 * Orchestrates valuation report generation and warehouse loading
 *
 * @author Sanod
 */
public class InventoryValuationController {

    private final String username = UserSession.getInstance().getUsername();

    public InventoryValuationController() {
    }

    /**
     * Generates inventory valuation report for specified parameters
     */
    public InventoryValuationReportDTO generateInventoryValuationReport(
            String date,
            String warehouse,
            String valuationMethod,
            boolean includeDamaged,
            boolean groupByGroup) throws Exception {

        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("Valuation date must be selected.");
        }

        InventoryValuationReportDTO dto = RetryHelper.executeWithRetry(
            () -> InventoryValuationReportDAO.getInstance().getInventoryValuationReport(
                date, warehouse, valuationMethod, includeDamaged, groupByGroup
            ),
            "failed to generate inventory valuation report for date " + date
        );

        if (dto != null) {
            Logger.log(username, "generated inventory valuation report for date: " + date + " using method: " + valuationMethod);
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
}
