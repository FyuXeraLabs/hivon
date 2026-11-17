package test.forms.masterdata;

import core.api.dao.BinDAO;
import masterdata.controllers.BinManagementController;
import models.dto.StorageBinDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("BinManagementForm")
@DisplayName("Bin Management Form")
class BinManagementFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("StorageBinDTO bean round-trip")
    void storageBinDtoBeanRoundTrip() {
        DtoAssert.assertBean(StorageBinDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("BinDAO.getBins")
    void daoGetBins() throws Exception {
        Assertions.assertNotNull(BinDAO.getInstance().getBins(null));
    }

    @Test
    @Tag("controller")
    @DisplayName("BinManagementController.getAllBins")
    void controllerGetAllBins() {
        Assertions.assertNotNull(new BinManagementController().getAllBins());
    }

    @Test
    @Tag("form")
    @DisplayName("Bin Management Form validation guards")
    void formValidationGuards() {
        BinManagementController controller = new BinManagementController();
        Assertions.assertEquals(0, controller.createBin(null), "createBin(null) should return 0");
        Assertions.assertThrows(IllegalArgumentException.class, () -> controller.updateBin(null),
                "updateBin(null) should throw IllegalArgumentException");
        Assertions.assertThrows(IllegalArgumentException.class, () -> controller.deleteBin(-1),
                "deleteBin(-1) should throw IllegalArgumentException");
    }
}
