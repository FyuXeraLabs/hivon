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
 * Controller for Daily Activity Report (REP61).
 * Orchestrates fetching daily activity data and warehouses list with retry logic.
 *
 * @author Sanod
 */
public class DailyActivityController {

    private final String username = UserSession.getInstance().getUsername();

    public DailyActivityController() {
    }

    /**
     * Generates daily activity report for the specified date, warehouse filter, and activity type.
     *
     * @param date Report date string (YYYY-MM-DD)
     * @param warehouseId Optional warehouse filter ID
     * @param activityType Optional activity type filter
     * @return DailyActivityReportDTO containing summary and table details
     * @throws Exception if fetching report fails
     */
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

    /**
     * Fetches list of active warehouses for filter dropdown.
     *
     * @return List of WarehouseDTO
     * @throws Exception if fetching warehouses fails
     */
    public List<WarehouseDTO> getWarehouses() throws Exception {
        return RetryHelper.executeWithRetry(
            () -> WarehouseDAO.getInstance().getWarehouses(),
            "failed to load warehouses"
        );
    }
}
