package test.forms.reports;

import core.api.dao.StockAgingReportDAO;
import models.dto.StockAgingReportDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import reports.controllers.StockAgingController;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("StockAgingForm")
@DisplayName("Stock Aging Report Form")
class StockAgingFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("StockAgingReportDTO bean round-trip")
    void reportDtoBeanRoundTrip() {
        DtoAssert.assertBean(StockAgingReportDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("StockAgingReportDAO.getStockAgingReport")
    void daoGetReport() throws Exception {
        Assertions.assertNotNull(StockAgingReportDAO.getInstance()
                .getStockAgingReport(null, null, false, false));
    }

    @Test
    @Tag("controller")
    @DisplayName("StockAgingController.getWarehouses")
    void controllerGetWarehouses() throws Exception {
        Assertions.assertNotNull(new StockAgingController().getWarehouses());
    }

    @Test
    @Tag("form")
    @DisplayName("Stock Aging Form generate flow")
    void formGenerate() throws Exception {
        Assertions.assertNotNull(new StockAgingController()
                .generateStockAgingReport(null, null, false, false));
    }
}
