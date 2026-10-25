/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package reports.controllers;

import core.api.dao.StockAgingReportDAO;
import core.api.dao.WarehouseDAO;
import core.logging.Logger;
import core.security.UserSession;
import core.utils.RetryHelper;
import models.dto.StockAgingReportDTO;
import models.dto.WarehouseDTO;

import java.util.List;

/**
 * Controller for Stock Aging Report (REP63)
 * Orchestrates stock aging analysis generation and warehouse retrieval
 *
 * @author Sanod
 */
public class StockAgingController {

    private final String username = UserSession.getInstance().getUsername();

    public StockAgingController() {
    }

    /**
     * Generates stock aging analysis report for specified filters
     */
    public StockAgingReportDTO generateStockAgingReport(
            String warehouse,
            String ageRange,
            boolean slowMovingOnly,
            boolean potentialObsoleteOnly) throws Exception {

        StockAgingReportDTO dto = RetryHelper.executeWithRetry(
            () -> StockAgingReportDAO.getInstance().getStockAgingReport(
                warehouse, ageRange, slowMovingOnly, potentialObsoleteOnly
            ),
            "failed to generate stock aging report"
        );

        if (dto != null) {
            Logger.log(username, "generated stock aging report for warehouse: " + (warehouse != null ? warehouse : "All")
                + ", ageRange: " + (ageRange != null ? ageRange : "All")
                + ", slowMovingOnly: " + slowMovingOnly
                + ", potentialObsoleteOnly: " + potentialObsoleteOnly);
        }
        return dto;
    }

    /**
     * Fetches list of active warehouses for filter dropdown
     */
    public List<WarehouseDTO> getWarehouses() throws Exception {
        return RetryHelper.executeWithRetry(
            () -> WarehouseDAO.getInstance().getWarehouses(),
            "failed to fetch warehouses in StockAgingController"
        );
    }
}
