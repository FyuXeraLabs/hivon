package test.forms.movements;

import core.api.dao.GISalesOrderDAO;
import movements.controllers.GISalesOrderController;
import models.dto.SalesOrderDTO;
import models.dto.SalesOrderItemDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("GISalesOrderForm")
@DisplayName("Goods Issue - Sales Order Form")
class GISalesOrderFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("SalesOrderDTO / SalesOrderItemDTO bean round-trip")
    void dtoBeanRoundTrip() {
        DtoAssert.assertBean(SalesOrderDTO.class);
        DtoAssert.assertBean(SalesOrderItemDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("GISalesOrderDAO.searchSalesOrders")
    void daoSearchSalesOrders() throws Exception {
        Assertions.assertNotNull(GISalesOrderDAO.getInstance().searchSalesOrders(null, null, null));
    }

    @Test
    @Tag("controller")
    @DisplayName("GISalesOrderController.searchSalesOrders")
    void controllerSearchSalesOrders() throws Exception {
        Assertions.assertNotNull(new GISalesOrderController().searchSalesOrders(null, null, null));
    }

    @Test
    @Tag("form")
    @DisplayName("GI Sales Order Form customer lookup flow")
    void formCustomerLookup() throws Exception {
        Assertions.assertNotNull(new GISalesOrderController().getCustomers(null));
    }
}
