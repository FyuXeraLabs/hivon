package test.forms.movements;

import core.api.dao.InventoryAdjustmentDAO;
import java.util.ArrayList;
import movements.controllers.InventoryAdjustmentController;
import models.dto.AdjustmentItemDTO;
import models.dto.MaterialStockDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("InventoryAdjustmentForm")
@DisplayName("Inventory Adjustment Form")
class InventoryAdjustmentFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("AdjustmentItemDTO / MaterialStockDTO bean round-trip")
    void dtoBeanRoundTrip() {
        DtoAssert.assertBean(AdjustmentItemDTO.class);
        DtoAssert.assertBean(MaterialStockDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("InventoryAdjustmentDAO.getAdjustmentReasons")
    void daoGetAdjustmentReasons() throws Exception {
        Assertions.assertNotNull(InventoryAdjustmentDAO.getInstance().getAdjustmentReasons());
    }

    @Test
    @Tag("controller")
    @DisplayName("InventoryAdjustmentController.getAdjustmentReasons")
    void controllerGetAdjustmentReasons() throws Exception {
        Assertions.assertNotNull(new InventoryAdjustmentController().getAdjustmentReasons());
    }

    @Test
    @Tag("form")
    @DisplayName("Inventory Adjustment Form validation guard")
    void formValidationGuard() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new InventoryAdjustmentController()
                        .saveAdjustment(new ArrayList<>(), today(), "qa", "test"),
                "empty adjustment items should throw IllegalArgumentException");
    }
}
