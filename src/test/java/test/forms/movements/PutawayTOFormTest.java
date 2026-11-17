package test.forms.movements;

import core.api.dao.PutawayTODAO;
import models.dto.StorageBinDTO;
import movements.controllers.PutawayTOController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

import java.util.Collections;

@Tag("PutawayTOForm")
@DisplayName("Putaway Transfer Order Form")
class PutawayTOFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("Putaway material / suggestion / item bean round-trip")
    void dtoBeanRoundTrip() {
        DtoAssert.assertBean(PutawayTODAO.PutawayMaterialDTO.class);
        DtoAssert.assertBean(PutawayTODAO.PutawayBinSuggestionDTO.class);
        DtoAssert.assertBean(PutawayTODAO.PutawayTOItem.class);
        DtoAssert.assertBean(StorageBinDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("PutawayTODAO.getReceivingBins")
    void daoGetReceivingBins() throws Exception {
        Assertions.assertNotNull(PutawayTODAO.getInstance().getReceivingBins(firstWarehouseId()));
    }

    @Test
    @Tag("controller")
    @DisplayName("PutawayTOController.getReceivingBins")
    void controllerGetReceivingBins() throws Exception {
        Assertions.assertNotNull(new PutawayTOController().getReceivingBins(firstWarehouseId()));
    }

    @Test
    @Tag("form")
    @DisplayName("Putaway TO Form empty item validation")
    void formEmptyItemsGuard() {
        PutawayTOController controller = new PutawayTOController();
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> controller.createPutawayTransferOrder(1,
                        Collections.<PutawayTODAO.PutawayTOItem>emptyList(), null),
                "empty items should throw IllegalArgumentException");
    }
}
