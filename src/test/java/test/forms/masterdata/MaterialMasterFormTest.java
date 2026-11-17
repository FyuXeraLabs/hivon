package test.forms.masterdata;

import core.api.dao.MaterialDAO;
import masterdata.controllers.MaterialController;
import models.dto.MaterialDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("MaterialMasterForm")
@DisplayName("Material Master Form")
class MaterialMasterFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("MaterialDTO bean round-trip")
    void materialDtoBeanRoundTrip() {
        DtoAssert.assertBean(MaterialDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("MaterialDAO.getMaterials")
    void daoGetMaterials() throws Exception {
        Assertions.assertNotNull(MaterialDAO.getInstance().getMaterials(null));
    }

    @Test
    @Tag("controller")
    @DisplayName("MaterialController.getAllMaterials")
    void controllerGetAllMaterials() {
        Assertions.assertNotNull(new MaterialController().getAllMaterials());
    }

    @Test
    @Tag("form")
    @DisplayName("Material Master Form validation guards")
    void formValidationGuards() {
        MaterialController controller = new MaterialController();
        Assertions.assertEquals(0, controller.createMaterial(null), "createMaterial(null) should return 0");
        Assertions.assertThrows(IllegalArgumentException.class, () -> controller.updateMaterial(null),
                "updateMaterial(null) should throw IllegalArgumentException");
        Assertions.assertThrows(IllegalArgumentException.class, () -> controller.deleteMaterial(-1),
                "deleteMaterial(-1) should throw IllegalArgumentException");
    }
}
