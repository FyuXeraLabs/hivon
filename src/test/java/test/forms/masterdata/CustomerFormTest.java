package test.forms.masterdata;

import core.api.dao.CustomerDAO;
import masterdata.controllers.CustomerController;
import models.dto.CustomerDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("CustomerForm")
@DisplayName("Customer Form")
class CustomerFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("CustomerDTO bean round-trip")
    void customerDtoBeanRoundTrip() {
        DtoAssert.assertBean(CustomerDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("CustomerDAO.getCustomers")
    void daoGetCustomers() throws Exception {
        Assertions.assertNotNull(CustomerDAO.getInstance().getCustomers(null));
    }

    @Test
    @Tag("controller")
    @DisplayName("CustomerController.getAllCustomers")
    void controllerGetAllCustomers() {
        Assertions.assertNotNull(new CustomerController().getAllCustomers());
    }

    @Test
    @Tag("form")
    @DisplayName("Customer Form validation guards")
    void formValidationGuards() {
        CustomerController controller = new CustomerController();
        Assertions.assertEquals(0, controller.createCustomer(null), "createCustomer(null) should return 0");
        Assertions.assertFalse(controller.updateCustomer(null), "updateCustomer(null) should return false");
        Assertions.assertFalse(controller.deleteCustomer(-1), "deleteCustomer(-1) should return false");
    }
}
