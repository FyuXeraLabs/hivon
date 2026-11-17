package test.forms.movements;

import core.api.dao.ReplenishmentTODAO;
import movements.controllers.ReplenishmentTOController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("ReplenishmentTOForm")
@DisplayName("Replenishment Transfer Order Form")
class ReplenishmentTOFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("Replenishment requirement / source bean round-trip")
    void dtoBeanRoundTrip() {
        DtoAssert.assertBean(ReplenishmentTODAO.ReplenishmentRequirement.class);
        DtoAssert.assertBean(ReplenishmentTODAO.ReplenishSource.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("ReplenishmentTODAO.getLowStockReplenishments")
    void daoGetLowStockReplenishments() throws Exception {
        Assertions.assertNotNull(
                ReplenishmentTODAO.getInstance().getLowStockReplenishments(firstWarehouseId()));
    }

    @Test
    @Tag("controller")
    @DisplayName("ReplenishmentTOController.getLowStockReplenishments")
    void controllerGetLowStockReplenishments() throws Exception {
        Assertions.assertNotNull(
                new ReplenishmentTOController().getLowStockReplenishments(firstWarehouseId()));
    }

    @Test
    @Tag("form")
    @DisplayName("Replenishment TO Form low-stock flow")
    void formLowStockFlow() throws Exception {
        int warehouseId = firstWarehouseId();
        Assertions.assertNotNull(
                new ReplenishmentTOController().getLowStockReplenishments(warehouseId));
    }
}
