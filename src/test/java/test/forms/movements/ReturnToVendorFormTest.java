package test.forms.movements;

import core.api.dao.MaterialDAO;
import java.util.ArrayList;
import movements.controllers.ReturnToVendorController;
import models.dto.MaterialDTO;
import models.dto.VendorDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("ReturnToVendorForm")
@DisplayName("Return to Vendor Form")
class ReturnToVendorFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("MaterialDTO / VendorDTO bean round-trip")
    void dtoBeanRoundTrip() {
        DtoAssert.assertBean(MaterialDTO.class);
        DtoAssert.assertBean(VendorDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("MaterialDAO.getMaterials (form source lookup)")
    void daoGetMaterials() throws Exception {
        Assertions.assertNotNull(MaterialDAO.getInstance().getMaterials(null));
    }

    @Test
    @Tag("controller")
    @DisplayName("ReturnToVendorController.searchMaterials")
    void controllerSearchMaterials() throws Exception {
        Assertions.assertNotNull(new ReturnToVendorController().searchMaterials(null));
    }

    @Test
    @Tag("form")
    @DisplayName("Return to Vendor Form validation guard")
    void formValidationGuard() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ReturnToVendorController()
                        .completeReturnToVendor("", "REF-1", today(), new ArrayList<>()),
                "empty vendor code should throw IllegalArgumentException");
    }
}
