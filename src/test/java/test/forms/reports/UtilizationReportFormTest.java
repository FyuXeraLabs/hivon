package test.forms.reports;

import core.api.dao.UtilizationReportDAO;
import models.dto.UtilizationReportDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reports.controllers.UtilizationReportController;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("UtilizationReportForm")
@DisplayName("Utilization Report Form")
class UtilizationReportFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("UtilizationReportDTO bean round-trip")
    void dtoBeanRoundTrip() {
        DtoAssert.assertBean(UtilizationReportDTO.class);
        DtoAssert.assertBean(UtilizationReportDTO.BinUtilizationItem.class);
        DtoAssert.assertBean(UtilizationReportDTO.ZoneUtilizationItem.class);
        DtoAssert.assertBean(UtilizationReportDTO.TopSpaceConsumerItem.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("UtilizationReportDAO.getUtilizationReport")
    void daoGetReport() throws Exception {
        Assertions.assertNotNull(UtilizationReportDAO.getInstance()
                .getUtilizationReport(firstWarehouseCode(), null, today(), today()));
    }

    @Test
    @Tag("controller")
    @DisplayName("UtilizationReportController.getWarehouses")
    void controllerGetWarehouses() throws Exception {
        Assertions.assertNotNull(new UtilizationReportController().getWarehouses());
    }

    @Test
    @Tag("form")
    @DisplayName("Utilization Report Form generate")
    void formGenerate() throws Exception {
        Assertions.assertNotNull(new UtilizationReportController()
                .generateUtilizationReport(firstWarehouseCode(), null, today(), today()));
    }
}
