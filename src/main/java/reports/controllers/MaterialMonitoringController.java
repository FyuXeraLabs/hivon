/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package reports.controllers;

import core.api.dao.BatchDAO;
import core.api.dao.BinDAO;
import core.api.dao.MaterialDAO;
import core.api.dao.MaterialMonitoringReportDAO;
import core.logging.Logger;
import core.security.UserSession;
import core.utils.RetryHelper;
import models.dto.BatchDTO;
import models.dto.MaterialDTO;
import models.dto.MaterialMonitoringReportDTO;
import models.dto.StorageBinDTO;

import java.util.List;

/**
 * Controller for Material Monitoring & Batch Traceability Report (REP67)
 * Orchestrates report generation and filter options retrieval
 *
 * @author Sanod
 */
public class MaterialMonitoringController {

    private final String username = UserSession.getInstance().getUsername();

    public MaterialMonitoringController() {
    }

    /**
     * Generates material monitoring report for specified filters
     */
    public MaterialMonitoringReportDTO generateMaterialMonitoringReport(
            String material,
            String batch,
            String bin,
            boolean highlightExpiring) throws Exception {

        MaterialMonitoringReportDTO dto = RetryHelper.executeWithRetry(
            () -> MaterialMonitoringReportDAO.getInstance().getMaterialMonitoringReport(
                material, batch, bin, highlightExpiring
            ),
            "failed to generate material monitoring report"
        );

        if (dto != null) {
            Logger.log(username, "generated material monitoring report for material: " + (material != null ? material : "All")
                + ", batch: " + (batch != null ? batch : "All")
                + ", bin: " + (bin != null ? bin : "All")
                + ", highlightExpiring: " + highlightExpiring);
        }
        return dto;
    }

    /**
     * Fetches active materials for filter dropdown
     */
    public List<MaterialDTO> getMaterials() throws Exception {
        return RetryHelper.executeWithRetry(
            () -> MaterialDAO.getInstance().getMaterials(null),
            "failed to fetch materials in MaterialMonitoringController"
        );
    }

    /**
     * Fetches active batches for filter dropdown
     */
    public List<BatchDTO> getBatches() throws Exception {
        return RetryHelper.executeWithRetry(
            () -> BatchDAO.getInstance().getBatches(null, null),
            "failed to fetch batches in MaterialMonitoringController"
        );
    }

    /**
     * Fetches storage bins for filter dropdown
     */
    public List<StorageBinDTO> getBins() throws Exception {
        return RetryHelper.executeWithRetry(
            () -> BinDAO.getInstance().getBins(null),
            "failed to fetch storage bins in MaterialMonitoringController"
        );
    }
}
