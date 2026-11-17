package test.forms.masterdata;

import core.api.dao.ZoneDAO;
import masterdata.controllers.ZoneManagementController;
import models.dto.ZoneDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("ZoneManagementForm")
@DisplayName("Zone Management Form")
class ZoneManagementFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("ZoneDTO bean round-trip")
    void zoneDtoBeanRoundTrip() {
        DtoAssert.assertBean(ZoneDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("ZoneDAO.getZonesByWarehouse")
    void daoGetZonesByWarehouse() throws Exception {
        int warehouseId = firstWarehouseId();
        Assertions.assertNotNull(ZoneDAO.getInstance().getZonesByWarehouse(warehouseId));
    }

    @Test
    @Tag("controller")
    @DisplayName("ZoneManagementController.getZonesByWarehouse")
    void controllerGetZonesByWarehouse() throws Exception {
        int warehouseId = firstWarehouseId();
        Assertions.assertNotNull(new ZoneManagementController().getZonesByWarehouse(warehouseId));
    }

    @Test
    @Tag("form")
    @DisplayName("Zone Management Form search flow")
    void formSearchZonesByWarehouse() throws Exception {
        int warehouseId = firstWarehouseId();
        Assertions.assertNotNull(new ZoneManagementController().searchZonesByWarehouse(warehouseId, "A"));
    }
}
