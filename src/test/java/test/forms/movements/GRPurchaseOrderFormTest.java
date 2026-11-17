package test.forms.movements;

import core.api.dao.GRPurchaseOrderDAO;
import movements.controllers.GRPurchaseOrderController;
import models.dto.POItemDTO;
import models.dto.PurchaseOrderDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("GRPurchaseOrderForm")
@DisplayName("Goods Receipt - Purchase Order Form")
class GRPurchaseOrderFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("PurchaseOrderDTO / POItemDTO bean round-trip")
    void dtoBeanRoundTrip() {
        DtoAssert.assertBean(PurchaseOrderDTO.class);
        DtoAssert.assertBean(POItemDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("GRPurchaseOrderDAO.searchPurchaseOrders")
    void daoSearchPurchaseOrders() throws Exception {
        Assertions.assertNotNull(GRPurchaseOrderDAO.getInstance().searchPurchaseOrders((String) null));
    }

    @Test
    @Tag("controller")
    @DisplayName("GRPurchaseOrderController.searchPurchaseOrders")
    void controllerSearchPurchaseOrders() throws Exception {
        Assertions.assertNotNull(new GRPurchaseOrderController().searchPurchaseOrders((String) null));
    }

    @Test
    @Tag("form")
    @DisplayName("GR Purchase Order Form validation guard")
    void formValidationGuard() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new GRPurchaseOrderController().loadPurchaseOrder(""),
                "empty PO number should throw IllegalArgumentException");
    }
}
