package test.forms.reports;

import core.api.dao.DailyActivityReportDAO;
import models.dto.DailyActivityReportDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reports.controllers.DailyActivityController;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("DailyActivityForm")
@DisplayName("Daily Activity Report Form")
class DailyActivityFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("DailyActivityReportDTO bean round-trip")
    void reportDtoBeanRoundTrip() {
        DtoAssert.assertBean(DailyActivityReportDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("DailyActivityReportDAO.getDailyActivityReport")
    void daoGetReport() throws Exception {
        Assertions.assertNotNull(
                DailyActivityReportDAO.getInstance().getDailyActivityReport(today(), null, null));
    }

    @Test
    @Tag("controller")
    @DisplayName("DailyActivityController.getWarehouses")
    void controllerGetWarehouses() throws Exception {
        Assertions.assertNotNull(new DailyActivityController().getWarehouses());
    }

    @Test
    @Tag("form")
    @DisplayName("Daily Activity Form generate + validation")
    void formGenerateAndValidate() throws Exception {
        DailyActivityController controller = new DailyActivityController();
        Assertions.assertNotNull(controller.generateDailyActivityReport(today(), null, null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> controller.generateDailyActivityReport("", null, null),
                "empty date should throw IllegalArgumentException");
    }
}
