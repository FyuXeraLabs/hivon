package reports.analytics;

import models.dto.DailyActivityReportDTO;

/**
 * Core Daily Activity Report generator and metrics aggregator.
 *
 * @author Sanod
 */
public class DailyActivityReport {

    private DailyActivityReportDTO reportData;

    public DailyActivityReport() {
    }

    public DailyActivityReport(DailyActivityReportDTO reportData) {
        this.reportData = reportData;
    }

    public DailyActivityReportDTO getReportData() {
        return reportData;
    }

    public void setReportData(DailyActivityReportDTO reportData) {
        this.reportData = reportData;
    }

    /**
     * Calculates net movement balance value across receipts, issues, and adjustments.
     *
     * @return Net monetary impact
     */
    public double calculateNetMovementValue() {
        if (reportData == null) return 0.0;
        return reportData.getTotalReceiptValue() - reportData.getTotalIssueValue() + reportData.getNetAdjustmentValue();
    }
}
