package test.forms.movements;

import core.api.dao.GRCustomerReturnsDAO;
import movements.controllers.GRCustomerReturnsController;
import models.dto.CustomerDTO;
import models.dto.SalesOrderDTO;
import models.dto.SalesOrderItemDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("GRCustomerReturnsForm")
@DisplayName("Goods Receipt - Customer Returns Form")
class GRCustomerReturnsFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("SalesOrderDTO / SalesOrderItemDTO / CustomerDTO bean round-trip")
    void dtoBeanRoundTrip() {
        DtoAssert.assertBean(SalesOrderDTO.class);
        DtoAssert.assertBean(SalesOrderItemDTO.class);
        DtoAssert.assertBean(CustomerDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("GRCustomerReturnsDAO.searchSalesOrders")
    void daoSearchSalesOrders() throws Exception {
        Assertions.assertNotNull(GRCustomerReturnsDAO.getInstance().searchSalesOrders((String) null));
    }

    @Test
    @Tag("controller")
    @DisplayName("GRCustomerReturnsController.searchSalesOrders")
    void controllerSearchSalesOrders() throws Exception {
        Assertions.assertNotNull(new GRCustomerReturnsController().searchSalesOrders((String) null));
    }

    @Test
    @Tag("form")
    @DisplayName("GR Customer Returns Form validation guard")
    void formValidationGuard() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new GRCustomerReturnsController().loadSalesOrder(""),
                "empty Sales Order number should throw IllegalArgumentException");
    }
}
