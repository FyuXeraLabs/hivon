package test.framework;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Static catalogue of the forms under test and the layers each form is
 * exercised through. The strings here are also used as JUnit tags, so the CLI
 * can map a user selection to a {@code TagFilter}.
 */
public final class TestRegistry {

    public static final String LAYER_DTO = "dto";
    public static final String LAYER_DAO = "dao";
    public static final String LAYER_CONTROLLER = "controller";
    public static final String LAYER_FORM = "form";
    public static final String LAYER_SETUP = "setup";

    public static final String[] LAYERS = {
        LAYER_DTO, LAYER_DAO, LAYER_CONTROLLER, LAYER_FORM
    };

    public static final String[] FORMS = {
        // masterdata
        "ZoneManagementForm",
        "VendorForm",
        "UserManagementForm",
        "UOMForm",
        "MaterialMasterForm",
        "CustomerForm",
        "BinManagementForm",
        "BatchManagementForm",
        // reports
        "DailyActivityForm",
        "InventoryValuationForm",
        "StockAgingForm",
        "PerformanceReportForm",
        "UtilizationReportForm",
        "FinancialReportForm",
        // movements
        "GRPurchaseOrderForm",
        "GRCustomerReturnsForm",
        "GRTransferInForm",
        "GISalesOrderForm",
        "ReturnToVendorForm",
        "InventoryAdjustmentForm",
        "ScrapWriteoffForm",
        "ReplenishmentTOForm",
        "PutawayTOForm",
        "PickingTOForm",
        "SplittingPackBreakForm",
        "BinToBinTransferForm",
        "CycleCountForm"
    };

    private static final Set<String> FORM_LOOKUP =
            new LinkedHashSet<>(Arrays.asList(FORMS));
    private static final Set<String> LAYER_LOOKUP =
            new LinkedHashSet<>(Arrays.asList(LAYERS));

    private TestRegistry() {
    }

    public static boolean isForm(String tag) {
        return FORM_LOOKUP.contains(tag);
    }

    public static boolean isLayer(String tag) {
        return LAYER_LOOKUP.contains(tag);
    }

    public static boolean isFormIndex(int index) {
        return index >= 1 && index <= FORMS.length;
    }

    public static String formAt(int index) {
        return isFormIndex(index) ? FORMS[index - 1] : null;
    }

    public static String resolveForm(String input) {
        if (input == null) {
            return null;
        }
        String trimmed = input.trim();
        for (String form : FORMS) {
            if (form.equalsIgnoreCase(trimmed)) {
                return form;
            }
        }
        // case-insensitive contains match
        for (String form : FORMS) {
            if (form.toLowerCase().contains(trimmed.toLowerCase())) {
                return form;
            }
        }
        return null;
    }
}
