package test.forms.masterdata;

import core.api.dao.VendorDAO;
import masterdata.controllers.VendorController;
import models.dto.VendorDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("VendorForm")
@DisplayName("Vendor Form")
class VendorFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("VendorDTO bean round-trip")
    void vendorDtoBeanRoundTrip() {
        DtoAssert.assertBean(VendorDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("VendorDAO.getVendors")
    void daoGetVendors() throws Exception {
        Assertions.assertNotNull(VendorDAO.getInstance().getVendors(null));
    }

    @Test
    @Tag("controller")
    @DisplayName("VendorController.getAllVendors")
    void controllerGetAllVendors() {
        Assertions.assertNotNull(new VendorController().getAllVendors());
    }

    @Test
    @Tag("form")
    @DisplayName("Vendor Form validation guards")
    void formValidationGuards() {
        VendorController controller = new VendorController();
        Assertions.assertEquals(0, controller.createVendor(null), "createVendor(null) should return 0");
        Assertions.assertFalse(controller.updateVendor(null), "updateVendor(null) should return false");
        Assertions.assertFalse(controller.deleteVendor(-1), "deleteVendor(-1) should return false");
    }
}
