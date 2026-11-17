package test.forms.reports;

import core.api.dao.PerformanceReportDAO;
import models.dto.PerformanceReportDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reports.controllers.PerformanceReportController;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("PerformanceReportForm")
@DisplayName("Performance Report Form")
class PerformanceReportFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("PerformanceReportDTO bean round-trip")
    void reportDtoBeanRoundTrip() {
        DtoAssert.assertBean(PerformanceReportDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("PerformanceReportDAO.getPerformanceReport")
    void daoGetReport() throws Exception {
        Assertions.assertNotNull(PerformanceReportDAO.getInstance()
                .getPerformanceReport(today(), today(), null, null, null));
    }

    @Test
    @Tag("controller")
    @DisplayName("PerformanceReportController.getWorkers")
    void controllerGetWorkers() throws Exception {
        Assertions.assertNotNull(new PerformanceReportController().getWorkers());
    }

    @Test
    @Tag("form")
    @DisplayName("Performance Report Form generate + validation")
    void formGenerateAndValidate() throws Exception {
        PerformanceReportController controller = new PerformanceReportController();
        Assertions.assertNotNull(controller.generatePerformanceReport(today(), today(), null, null, null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> controller.generatePerformanceReport("", today(), null, null, null),
                "empty from-date should throw IllegalArgumentException");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> controller.generatePerformanceReport(today(), "", null, null, null),
                "empty to-date should throw IllegalArgumentException");
    }
}
