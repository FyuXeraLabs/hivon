package test.forms.movements;

import core.api.dao.CycleCountDAO;
import models.dto.CycleCountDTO;
import models.dto.CycleCountItemDTO;
import movements.controllers.CycleCountController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("CycleCountForm")
@DisplayName("Cycle Count Form")
class CycleCountFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("CycleCountDTO bean / CycleCountItemDTO key fields round-trip")
    void dtoBeanRoundTrip() {
        DtoAssert.assertBean(CycleCountDTO.class);

        CycleCountItemDTO item = new CycleCountItemDTO();
        item.setMaterialCode("MAT-0001");
        item.setSystemQuantity(10.0);
        item.setCountedQuantity(12.0);
        Assertions.assertEquals("MAT-0001", item.getMaterialCode());
        Assertions.assertEquals(10.0, item.getSystemQuantity());
        Assertions.assertEquals(Double.valueOf(12.0), item.getCountedQuantity());
        Assertions.assertEquals(Double.valueOf(2.0), item.getVarianceQuantity());
    }

    @Test
    @Tag("dao")
    @DisplayName("CycleCountDAO.listCycleCounts")
    void daoListCycleCounts() throws Exception {
        Assertions.assertNotNull(
                CycleCountDAO.getInstance().listCycleCounts(null, firstWarehouseId()));
    }

    @Test
    @Tag("controller")
    @DisplayName("CycleCountController.getWarehouses")
    void controllerGetWarehouses() throws Exception {
        Assertions.assertNotNull(new CycleCountController().getWarehouses());
    }

    @Test
    @Tag("form")
    @DisplayName("Cycle Count Form zones for warehouse")
    void formZonesForWarehouse() throws Exception {
        Assertions.assertNotNull(new CycleCountController().getZones(firstWarehouseId()));
    }
}
