/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package reports.controllers;

import core.api.dao.FinancialReportDAO;
import core.logging.Logger;
import core.security.UserSession;
import core.utils.RetryHelper;
import models.dto.FinancialReportDTO;

/**
 * Controller for Financial Report (REP66)
 * Orchestrates financial report generation across valuation, movement impact, cost variance, and period comparison
 *
 * @author Sanod
 */
public class FinancialReportController {

    private final String username = UserSession.getInstance().getUsername();

    public FinancialReportController() {
    }

    /**
     * Generates financial report for specified parameters
     */
    public FinancialReportDTO generateFinancialReport(
            String reportType,
            String fromDate,
            String toDate,
            String valuationMethod,
            boolean excludeDamaged,
            boolean includeVariance) throws Exception {

        if (fromDate == null || fromDate.trim().isEmpty()) {
            throw new IllegalArgumentException("From date must be selected.");
        }
        if (toDate == null || toDate.trim().isEmpty()) {
            throw new IllegalArgumentException("To date must be selected.");
        }

        FinancialReportDTO dto = RetryHelper.executeWithRetry(
            () -> FinancialReportDAO.getInstance().getFinancialReport(
                reportType, fromDate, toDate, valuationMethod, excludeDamaged, includeVariance
            ),
            "failed to generate financial report (" + reportType + ")"
        );

        if (dto != null) {
            Logger.log(username, "generated financial report [" + reportType + "] for range " + fromDate + " to " + toDate);
        }
        return dto;
    }
}
