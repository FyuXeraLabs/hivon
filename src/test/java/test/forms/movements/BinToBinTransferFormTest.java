package test.forms.movements;

import core.api.dao.BinToBinTransferDAO;
import models.dto.BinToBinDestinationBinDTO;
import models.dto.BinToBinSourceBinDTO;
import models.dto.BinToBinTransferItemDTO;
import models.dto.MaterialDTO;
import movements.controllers.BinToBinTransferController;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("BinToBinTransferForm")
@DisplayName("Bin to Bin Transfer Form")
class BinToBinTransferFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("Bin-to-bin source / destination / item bean round-trip")
    void dtoBeanRoundTrip() {
        DtoAssert.assertBean(BinToBinSourceBinDTO.class);
        DtoAssert.assertBean(BinToBinDestinationBinDTO.class);
        DtoAssert.assertBean(BinToBinTransferItemDTO.class);
        DtoAssert.assertBean(MaterialDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("BinToBinTransferDAO.getDestinationBins")
    void daoGetDestinationBins() throws Exception {
        Assertions.assertNotNull(
                BinToBinTransferDAO.getInstance().getDestinationBins(firstWarehouseId()));
    }

    @Test
    @Tag("controller")
    @DisplayName("BinToBinTransferController.getWarehouses")
    void controllerGetWarehouses() throws Exception {
        Assertions.assertNotNull(new BinToBinTransferController().getWarehouses());
    }

    @Test
    @Tag("form")
    @DisplayName("Bin to Bin Transfer Form destination bins")
    void formDestinationBins() throws Exception {
        Assertions.assertNotNull(
                new BinToBinTransferController().getDestinationBins(firstWarehouseId()));
    }
}
