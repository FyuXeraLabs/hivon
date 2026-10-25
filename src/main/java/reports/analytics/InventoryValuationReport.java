package reports.analytics;

import models.dto.InventoryValuationReportDTO;

/**
 * Core Inventory Valuation Report analytics and metrics aggregator.
 *
 * @author Sanod D. Mendis
 */
public class InventoryValuationReport {

    private InventoryValuationReportDTO reportData;

    public InventoryValuationReport() {
    }

    public InventoryValuationReport(InventoryValuationReportDTO reportData) {
        this.reportData = reportData;
    }

    public InventoryValuationReportDTO getReportData() {
        return reportData;
    }

    public void setReportData(InventoryValuationReportDTO reportData) {
        this.reportData = reportData;
    }

    /**
     * Calculates the average value per unit across all inventory items.
     *
     * @return Average unit cost, or 0.0 if total quantity is zero
     */
    public double calculateAverageUnitValue() {
        if (reportData == null || reportData.getTotalQuantity() <= 0) {
            return 0.0;
        }
        return reportData.getTotalValue() / reportData.getTotalQuantity();
    }
}
