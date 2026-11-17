package test.forms.movements;

import core.api.dao.TransferOrderDAO;
import movements.controllers.GRTransferInController;
import models.dto.TransferOrderDTO;
import models.dto.TransferOrderItemDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("GRTransferInForm")
@DisplayName("Goods Receipt - Transfer In Form")
class GRTransferInFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("TransferOrderDTO / TransferOrderItemDTO bean round-trip")
    void dtoBeanRoundTrip() {
        DtoAssert.assertBean(TransferOrderDTO.class);
        DtoAssert.assertBean(TransferOrderItemDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("TransferOrderDAO.searchTransferOrders")
    void daoSearchTransferOrders() throws Exception {
        Assertions.assertNotNull(TransferOrderDAO.getInstance().searchTransferOrders((String) null));
    }

    @Test
    @Tag("controller")
    @DisplayName("GRTransferInController.searchTransferOrders")
    void controllerSearchTransferOrders() throws Exception {
        Assertions.assertNotNull(new GRTransferInController().searchTransferOrders((String) null));
    }

    @Test
    @Tag("form")
    @DisplayName("GR Transfer In Form validation guard")
    void formValidationGuard() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new GRTransferInController().loadTransferOrder(""),
                "empty Transfer Order number should throw IllegalArgumentException");
    }
}
