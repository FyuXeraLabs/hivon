package test.forms.masterdata;

import core.api.dao.UserDAO;
import masterdata.controllers.UserManagementController;
import models.dto.UserDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("UserManagementForm")
@DisplayName("User Management Form")
class UserManagementFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("UserDTO bean round-trip")
    void userDtoBeanRoundTrip() {
        DtoAssert.assertBean(UserDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("UserDAO.getUsers")
    void daoGetUsers() throws Exception {
        Assertions.assertNotNull(UserDAO.getInstance().getUsers(null));
    }

    @Test
    @Tag("controller")
    @DisplayName("UserManagementController.getAllUsers")
    void controllerGetAllUsers() {
        Assertions.assertNotNull(new UserManagementController().getAllUsers());
    }

    @Test
    @Tag("form")
    @DisplayName("User Management Form validation guards")
    void formValidationGuards() {
        UserManagementController controller = new UserManagementController();
        Assertions.assertEquals(0, controller.createUser(null, "Temp#1234", null),
                "createUser(null, ...) should return 0");
        Assertions.assertFalse(controller.deleteUser(-1), "deleteUser(-1) should return false");
        Assertions.assertFalse(controller.resetUserPassword(-1, ""),
                "resetUserPassword(-1, \"\") should return false");
    }
}
