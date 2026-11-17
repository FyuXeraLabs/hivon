package test.framework;

import core.api.dao.WarehouseDAO;
import models.dto.WarehouseDTO;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;

/**
 * Base class for every API test class. Authenticates once per test class
 * against the live backend via {@link TestContext} and offers small fixtures.
 *
 * <p>{@link HeadlessSkipExtension} turns GUI/dialog {@code HeadlessException}s
 * into skips so the suite stays green in headless runs.</p>
 */
@ExtendWith(HeadlessSkipExtension.class)
public abstract class ApiTestBase {

    @BeforeAll
    static void authenticate() throws Exception {
        TestContext.ensureAuthenticated();
    }

    /** @return id of the first configured warehouse, or skips the test when none exist. */
    protected static int firstWarehouseId() throws Exception {
        List<WarehouseDTO> warehouses = WarehouseDAO.getInstance().getWarehouses();
        Assumptions.assumeTrue(warehouses != null && !warehouses.isEmpty(),
                "No warehouses configured on the server");
        Integer id = warehouses.get(0).getWarehouseId();
        Assumptions.assumeTrue(id != null, "First warehouse has no id");
        return id;
    }

    /** @return code of the first configured warehouse, or skips the test when none exist. */
    protected static String firstWarehouseCode() throws Exception {
        List<WarehouseDTO> warehouses = WarehouseDAO.getInstance().getWarehouses();
        Assumptions.assumeTrue(warehouses != null && !warehouses.isEmpty(),
                "No warehouses configured on the server");
        String code = warehouses.get(0).getWarehouseCode();
        Assumptions.assumeTrue(code != null && !code.isEmpty(), "First warehouse has no code");
        return code;
    }

    protected static String today() {
        return LocalDate.now().toString();
    }
}
