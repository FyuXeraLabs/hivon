package test.forms.masterdata;

import core.api.dao.SystemParameterDAO;
import masterdata.controllers.UOMController;
import models.dto.UOMDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("UOMForm")
@DisplayName("UOM Form")
class UOMFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("UOMDTO bean round-trip")
    void uomDtoBeanRoundTrip() {
        DtoAssert.assertBean(UOMDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("SystemParameterDAO.getUOMs")
    void daoGetUOMs() throws Exception {
        Assertions.assertNotNull(SystemParameterDAO.getInstance().getUOMs(null));
    }

    @Test
    @Tag("controller")
    @DisplayName("UOMController.getAllUOMs")
    void controllerGetAllUOMs() {
        Assertions.assertNotNull(new UOMController().getAllUOMs());
    }

    @Test
    @Tag("form")
    @DisplayName("UOM Form validation guards")
    void formValidationGuards() {
        UOMController controller = new UOMController();
        Assertions.assertFalse(controller.createUOM(null), "createUOM(null) should return false");
        Assertions.assertFalse(controller.updateUOM(null), "updateUOM(null) should return false");
        Assertions.assertFalse(controller.deleteUOM(""), "deleteUOM(\"\") should return false");
    }
}
