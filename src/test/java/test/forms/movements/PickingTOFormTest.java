package test.forms.movements;

import core.api.dao.PickingTODAO;
import movements.controllers.PickingTOController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("PickingTOForm")
@DisplayName("Picking Transfer Order Form")
class PickingTOFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("Picking sales order / task / suggestion / item bean round-trip")
    void dtoBeanRoundTrip() {
        DtoAssert.assertBean(PickingTODAO.PickingSalesOrderDTO.class);
        DtoAssert.assertBean(PickingTODAO.PickingTaskDTO.class);
        DtoAssert.assertBean(PickingTODAO.PickBinSuggestionDTO.class);
        DtoAssert.assertBean(PickingTODAO.PickingTOItem.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("PickingTODAO.getPendingSalesOrders")
    void daoGetPendingSalesOrders() throws Exception {
        Assertions.assertNotNull(PickingTODAO.getInstance().getPendingSalesOrders());
    }

    @Test
    @Tag("controller")
    @DisplayName("PickingTOController.getPendingSalesOrders")
    void controllerGetPendingSalesOrders() throws Exception {
        Assertions.assertNotNull(new PickingTOController().getPendingSalesOrders());
    }

    @Test
    @Tag("form")
    @DisplayName("Picking TO Form pending orders flow")
    void formPendingOrdersFlow() throws Exception {
        Assertions.assertNotNull(new PickingTOController().getPendingSalesOrders());
    }
}
