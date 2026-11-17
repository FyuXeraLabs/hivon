package test.forms.movements;

import core.api.dao.SplittingPackBreakDAO;
import models.dto.BinToBinDestinationBinDTO;
import models.dto.SplittingItemDTO;
import models.dto.SplittingMaterialDTO;
import models.dto.SplittingSourceBinDTO;
import movements.controllers.SplittingPackBreakController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("SplittingPackBreakForm")
@DisplayName("Splitting / Pack Break Form")
class SplittingPackBreakFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("Splitting material / bin / item bean round-trip")
    void dtoBeanRoundTrip() {
        DtoAssert.assertBean(SplittingMaterialDTO.class);
        DtoAssert.assertBean(SplittingSourceBinDTO.class);
        DtoAssert.assertBean(SplittingItemDTO.class);
        DtoAssert.assertBean(BinToBinDestinationBinDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("SplittingPackBreakDAO.searchMaterials")
    void daoSearchMaterials() throws Exception {
        Assertions.assertNotNull(
                SplittingPackBreakDAO.getInstance().searchMaterials(firstWarehouseId(), null));
    }

    @Test
    @Tag("controller")
    @DisplayName("SplittingPackBreakController.searchMaterials")
    void controllerSearchMaterials() throws Exception {
        Assertions.assertNotNull(
                new SplittingPackBreakController().searchMaterials(firstWarehouseId(), null));
    }

    @Test
    @Tag("form")
    @DisplayName("Splitting / Pack Break Form destination bins")
    void formDestinationBins() throws Exception {
        Assertions.assertNotNull(
                new SplittingPackBreakController().getDestinationBins(firstWarehouseId()));
    }
}
