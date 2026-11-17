package test.forms.reports;

import core.api.dao.FinancialReportDAO;
import models.dto.FinancialReportDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reports.controllers.FinancialReportController;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("FinancialReportForm")
@DisplayName("Financial Report Form")
class FinancialReportFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("FinancialReportDTO bean round-trip")
    void dtoBeanRoundTrip() {
        DtoAssert.assertBean(FinancialReportDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("FinancialReportDAO.getFinancialReport")
    void daoGetReport() throws Exception {
        Assertions.assertNotNull(FinancialReportDAO.getInstance()
                .getFinancialReport("Valuation", today(), today(), "WAC", false, false));
    }

    @Test
    @Tag("controller")
    @DisplayName("FinancialReportController.generateFinancialReport")
    void controllerGenerate() throws Exception {
        Assertions.assertNotNull(new FinancialReportController()
                .generateFinancialReport("Valuation", today(), today(), "WAC", false, false));
    }

    @Test
    @Tag("form")
    @DisplayName("Financial Report Form date validation guards")
    void formValidationGuards() {
        FinancialReportController controller = new FinancialReportController();
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> controller.generateFinancialReport("Valuation", "", today(), "WAC", false, false),
                "empty from date should throw IllegalArgumentException");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> controller.generateFinancialReport("Valuation", today(), "", "WAC", false, false),
                "empty to date should throw IllegalArgumentException");
    }
}
