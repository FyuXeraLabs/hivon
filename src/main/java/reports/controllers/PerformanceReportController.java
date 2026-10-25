/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package reports.controllers;

import core.api.dao.PerformanceReportDAO;
import core.api.dao.UserDAO;
import core.api.dao.WarehouseDAO;
import core.logging.Logger;
import core.security.UserSession;
import core.utils.RetryHelper;
import models.dto.PerformanceReportDTO;
import models.dto.UserDTO;
import models.dto.WarehouseDTO;

import java.util.List;

/**
 * Controller for Performance Report (REP64)
 * Orchestrates worker performance report generation, warehouse, and worker loading
 *
 * @author Sanod
 */
public class PerformanceReportController {

    private final String username = UserSession.getInstance().getUsername();

    public PerformanceReportController() {
    }

    /**
     * Generates worker performance report for specified parameters
     */
    public PerformanceReportDTO generatePerformanceReport(
            String fromDate,
            String toDate,
            String warehouse,
            String worker,
            String taskType) throws Exception {

        if (fromDate == null || fromDate.trim().isEmpty()) {
            throw new IllegalArgumentException("From date must be selected.");
        }
        if (toDate == null || toDate.trim().isEmpty()) {
            throw new IllegalArgumentException("To date must be selected.");
        }

        PerformanceReportDTO dto = RetryHelper.executeWithRetry(
            () -> PerformanceReportDAO.getInstance().getPerformanceReport(
                fromDate, toDate, warehouse, worker, taskType
            ),
            "failed to generate performance report for range " + fromDate + " to " + toDate
        );

        if (dto != null) {
            Logger.log(username, "generated performance report from: " + fromDate + " to: " + toDate);
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
     * Fetches list of users/workers for filter dropdown
     */
    public List<UserDTO> getWorkers() throws Exception {
        return RetryHelper.executeWithRetry(
            () -> UserDAO.getInstance().getUsers(null),
            "failed to load workers"
        );
    }
}
