package test.forms.reports;

import core.api.dao.InventoryValuationReportDAO;
import models.dto.InventoryValuationReportDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reports.controllers.InventoryValuationController;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("InventoryValuationForm")
@DisplayName("Inventory Valuation Report Form")
class InventoryValuationFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("InventoryValuationReportDTO bean round-trip")
    void reportDtoBeanRoundTrip() {
        DtoAssert.assertBean(InventoryValuationReportDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("InventoryValuationReportDAO.getInventoryValuationReport")
    void daoGetReport() throws Exception {
        Assertions.assertNotNull(InventoryValuationReportDAO.getInstance()
                .getInventoryValuationReport(today(), null, "FIFO", false, false));
    }

    @Test
    @Tag("controller")
    @DisplayName("InventoryValuationController.getWarehouses")
    void controllerGetWarehouses() throws Exception {
        Assertions.assertNotNull(new InventoryValuationController().getWarehouses());
    }

    @Test
    @Tag("form")
    @DisplayName("Inventory Valuation Form generate + validation")
    void formGenerateAndValidate() throws Exception {
        InventoryValuationController controller = new InventoryValuationController();
        Assertions.assertNotNull(controller.generateInventoryValuationReport(today(), null, "FIFO", false, false));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> controller.generateInventoryValuationReport("", null, "FIFO", false, false),
                "empty date should throw IllegalArgumentException");
    }
}
