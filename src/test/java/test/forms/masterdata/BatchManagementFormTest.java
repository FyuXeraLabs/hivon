package test.forms.masterdata;

import core.api.dao.BatchDAO;
import masterdata.controllers.BatchController;
import models.dto.BatchDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("BatchManagementForm")
@DisplayName("Batch Management Form")
class BatchManagementFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("BatchDTO bean round-trip")
    void batchDtoBeanRoundTrip() {
        DtoAssert.assertBean(BatchDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("BatchDAO.getBatches")
    void daoGetBatches() throws Exception {
        Assertions.assertNotNull(BatchDAO.getInstance().getBatches(null, null));
    }

    @Test
    @Tag("controller")
    @DisplayName("BatchController.getAllBatches")
    void controllerGetAllBatches() {
        Assertions.assertNotNull(new BatchController().getAllBatches());
    }

    @Test
    @Tag("form")
    @DisplayName("Batch Management Form validation guards")
    void formValidationGuards() {
        BatchController controller = new BatchController();
        Assertions.assertEquals(0, controller.createBatch(null), "createBatch(null) should return 0");
        Assertions.assertThrows(IllegalArgumentException.class, () -> controller.updateBatch(null),
                "updateBatch(null) should throw IllegalArgumentException");
        Assertions.assertThrows(IllegalArgumentException.class, () -> controller.deleteBatch(-1),
                "deleteBatch(-1) should throw IllegalArgumentException");
    }
}
