/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package reports.controllers;

import core.api.dao.DailyActivityReportDAO;
import core.api.dao.WarehouseDAO;
import core.logging.Logger;
import core.security.UserSession;
import core.utils.RetryHelper;
import models.dto.DailyActivityReportDTO;
import models.dto.WarehouseDTO;

import java.util.List;

/**
 *
 * @author Sanod
 */
public class DailyActivityController {

    private final String username = UserSession.getInstance().getUsername();

    public DailyActivityController() {
    }

    // generates daily activity report for the specified date, warehouse filter and activity type
    public DailyActivityReportDTO generateDailyActivityReport(String date, String warehouseId, String activityType) throws Exception {
        if (date == null || date.trim().isEmpty()) {
            throw new IllegalArgumentException("Report date must be selected.");
        }

        DailyActivityReportDTO dto = RetryHelper.executeWithRetry(
            () -> DailyActivityReportDAO.getInstance().getDailyActivityReport(date, warehouseId, activityType),
            "failed to generate daily activity report for date " + date
        );

        if (dto != null) {
            Logger.log(username, "generated daily activity report for date: " + date);
        }
        return dto;
    }

    // fetches list of active warehouses for filter dropdown
    public List<WarehouseDTO> getWarehouses() throws Exception {
        return RetryHelper.executeWithRetry(
            () -> WarehouseDAO.getInstance().getWarehouses(),
            "failed to load warehouses"
        );
    }
}
