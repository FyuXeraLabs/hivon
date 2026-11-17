package test.forms.movements;

import core.api.dao.ScrapWriteoffDAO;
import java.util.ArrayList;
import movements.controllers.ScrapWriteoffController;
import models.dto.MaterialStockDTO;
import models.dto.ScrapItemDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import test.framework.ApiTestBase;
import test.framework.DtoAssert;

@Tag("ScrapWriteoffForm")
@DisplayName("Scrap / Write-off Form")
class ScrapWriteoffFormTest extends ApiTestBase {

    @Test
    @Tag("dto")
    @DisplayName("ScrapItemDTO / MaterialStockDTO bean round-trip")
    void dtoBeanRoundTrip() {
        DtoAssert.assertBean(ScrapItemDTO.class);
        DtoAssert.assertBean(MaterialStockDTO.class);
    }

    @Test
    @Tag("dao")
    @DisplayName("ScrapWriteoffDAO.getScrapReasons")
    void daoGetScrapReasons() throws Exception {
        Assertions.assertNotNull(ScrapWriteoffDAO.getInstance().getScrapReasons());
    }

    @Test
    @Tag("controller")
    @DisplayName("ScrapWriteoffController.getScrapReasons")
    void controllerGetScrapReasons() throws Exception {
        Assertions.assertNotNull(new ScrapWriteoffController().getScrapReasons());
    }

    @Test
    @Tag("form")
    @DisplayName("Scrap / Write-off Form validation guard")
    void formValidationGuard() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ScrapWriteoffController().saveScrapWriteoff(new ArrayList<>(), "R", "test", false),
                "empty scrap items should throw IllegalArgumentException");
    }
}
