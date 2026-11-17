# Hivon Test Suite Guide

Form-wise automated test suite for the Hivon WMS client. It runs JUnit 5
tests against the **live REST API**, grouped by form and by architectural
layer, and generates timestamped **Excel (`.xlsx`)** test and bug reports.

- Runner: interactive CLI (`test.framework.TestCli`) or command-line args
- Output: `docs/reports/tests/TEST_REPORT_*.xlsx`, `docs/reports/bugs/BUG_REPORT_*.xlsx`
- 27 wired forms x 4 layers = 108 tests

---

## 1. Prerequisites

| Requirement | Notes |
|---|---|
| Java 17+ | The project targets release 17 |
| Maven 3.8+ | Used to build the runnable test jar |
| Hivon API reachable | `http://hivon/api` must resolve and be running |
| Hosts entry | `SystemParameters.API_BASE_URL` is hardcoded to `http://hivon/api` |

The suite authenticates automatically. Credentials are tried in this order:

1. `-Dhivon.test.username` / `-Dhivon.test.password`
2. `qa_tester` / `QA_TESTER_PASSWORD` (env or `-D`), default `Qa#Tester2026!`
3. `testuser` / `123456`

---

## 2. Build

```powershell
# from the project root
mvn -DskipTests package
```

This produces `target/hivon-tests.jar` (the test runner) and the normal
`target/hivon-1.0-jar-with-dependencies.jar` (the app).

> Live tests are **excluded from Surefire**, so `mvn test` / `mvn package`
> never hit the backend. Use the CLI or your IDE to run the suite.

---

## 3. Running the suite

### 3.1 Interactive menu (no arguments)

```powershell
java -jar target/hivon-tests.jar
```

Menu options:

| Option | Action |
|---|---|
| `1` | Run ALL forms (all layers) |
| `2` | Run one form (all layers) |
| `3` | Run one layer of one form |
| `4` | List forms |
| `5` | Toggle report generation on/off |
| `h` | Show help / menu |
| `q` | Quit |

You may also type a form name directly at the prompt.

### 3.2 Command-line arguments

```powershell
# Everything
java -jar target/hivon-tests.jar --all

# One form, all layers
java -jar target/hivon-tests.jar CustomerForm

# One form, one layer
java -jar target/hivon-tests.jar CustomerForm controller

# Layer-only across all forms
java -jar target/hivon-tests.jar --controller

# Skip report generation
java -jar target/hivon-tests.jar --all --no-report

# List forms / help
java -jar target/hivon-tests.jar --list
java -jar target/hivon-tests.jar --help
```

Argument reference:

| Argument | Meaning |
|---|---|
| `--all` | Run every form and layer |
| `<FormName>` | Form to run (partial, case-insensitive match allowed) |
| `dto` / `dao` / `controller` / `form` | Layer filter (also `--dto`, `--dao`, `--controller`) |
| `--no-report` | Do not write `.xlsx` reports |
| `--list` | Print all supported forms |
| `--help` | Print usage |

---

## 4. Layers explained

Each form is exercised through four layers, selected via tags:

| Layer | What it validates | Network? |
|---|---|---|
| `dto` | Reflective bean round-trip of the form's data objects | No |
| `dao` | Direct data-access read calls | Yes |
| `controller` | Controller read methods | Yes |
| `form` | End-to-end form flow: read/validate paths (negative validation is offline) | Mostly |

Tests that require live data (e.g. a warehouse) **skip** (reported as `SKIP`)
when no suitable fixture exists, instead of failing.

Controllers/reports go through `RetryHelper`, which normally shows a modal
`ProgressDialog`. In a headless JVM that dialog cannot be created, so
`RetryHelper` detects `GraphicsEnvironment.isHeadless()` and runs the operation
directly — `controller`/`form` tests therefore execute fully with no dialog.
GUI behavior is unchanged. `test.framework.HeadlessSkipExtension` remains as a
safety net for any future dialog-bound path.

### Supported forms

Master data: `ZoneManagementForm`, `VendorForm`, `UserManagementForm`,
`UOMForm`, `MaterialMasterForm`, `CustomerForm`, `BinManagementForm`,
`BatchManagementForm`

Reports: `DailyActivityForm`, `InventoryValuationForm`, `StockAgingForm`,
`PerformanceReportForm`, `UtilizationReportForm`, `FinancialReportForm`

Movements: `GRPurchaseOrderForm`, `GRCustomerReturnsForm`, `GRTransferInForm`,
`GISalesOrderForm`, `ReturnToVendorForm`, `InventoryAdjustmentForm`,
`ScrapWriteoffForm`, `ReplenishmentTOForm`, `PutawayTOForm`, `PickingTOForm`,
`SplittingPackBreakForm`, `BinToBinTransferForm`, `CycleCountForm`

---

## 5. Reports

Reports are written under `docs/reports/`:

```
docs/reports/
  tests/TEST_REPORT[_<Form>]_<yyyyMMdd_HHmmss>.xlsx
  bugs/BUG_REPORT[_<Form>]_<yyyyMMdd_HHmmss>.xlsx
```

Override the base directory with `-Dhivon.reports.dir=<path>`.

### Test report sheets

| Sheet | Contents |
|---|---|
| Summary | Run metadata, totals, pass rate, open bugs |
| By Form | Per-form totals / pass rate |
| By Layer | Per-layer totals / pass rate |
| Test Cases | One row per test: ID, form, layer, class, status, duration, message |
| Notes | Environment / setup notes |

### Bug report sheets

| Sheet | Contents |
|---|---|
| Summary | Bug counts by severity and by form |
| Bugs | Bug ID, severity, form, layer, test case, message |
| Reproduction | Bug ID, test ID, status, duration, stack trace |

Severity is derived automatically:

- **High** — HTTP 5xx, connection/timeout, `HeadlessException`, `NullPointer`
- **Medium** — other assertion / 4xx failures

Override per test with `@TestSeverity("High")` (or `Low`).

---

## 6. Adding tests for a new form

1. Create a test class under
   `src/test/java/test/forms/<group>/<FormName>Test.java` extending
   `test.framework.ApiTestBase`.
2. Tag the class with `@Tag("<FormName>")`.
3. Tag each method with a layer: `@Tag("dto")`, `"dao"`, `"controller"`, or `"form"`.
4. Register the form name in `test.framework.TestRegistry.FORMS` (so the CLI
   menu and form resolution pick it up).

Example:

```java
package test.forms.masterdata;

import org.junit.jupiter.api.*;
import test.framework.ApiTestBase;

@Tag("CustomerForm")
class CustomerFormTest extends ApiTestBase {

    @Test @Tag("dto")
    void dtoRoundTrip() {
        test.framework.DtoAssert.assertBean(models.dto.CustomerDTO.class);
    }

    @Test @Tag("controller")
    void loadAll() {
        Assertions.assertNotNull(new masterdata.controllers.CustomerController().getAllCustomers());
    }
}
```

Then rebuild:

```powershell
mvn -DskipTests package
java -jar target/hivon-tests.jar CustomerForm
```

---

## 7. Configuration reference

| Property | Default | Purpose |
|---|---|---|
| `-Dhivon.test.username` / `-Dhivon.test.password` | — | Primary credentials |
| `QA_TESTER_PASSWORD` (env or `-D`) | `Qa#Tester2026!` | qa_tester password |
| `-Dhivon.reports.dir` | `docs/reports` | Report output base |
| `-Dgui.tests=true` | `false` | Disable headless mode |

---

## 8. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `Unable to authenticate ...` | API not running or hosts entry missing; verify `http://hivon/api/health` |
| `controller`/`form` run without dialogs | Expected: `RetryHelper` bypasses the modal `ProgressDialog` when headless |
| Tests fail with `HeadlessException` | Should be caught by `HeadlessSkipExtension`; if it surfaces, run on a display or report it |
| `SKIP` results | Expected when the server has no fixture data (e.g. no warehouses) |
| `Log4j2 could not find a logging implementation` | Harmless; the CLI uses a simple logger |
| Reports not written | Reports disabled (`5` in menu or `--no-report`), or check `-Dhivon.reports.dir` |
| `mvn test` runs nothing | Expected; the live suite is excluded from Surefire by design |

---

## 9. Safety

- Tests are **non-destructive**: they perform reads and offline validation
  guards. No create/update/delete calls are issued against real records.
- All tests run in a single JVM with `java.awt.headless=true` so no Swing
  dialogs can block the run. `RetryHelper` executes DAO operations directly
  when headless; its GUI (modal `ProgressDialog`) path is untouched.

> Note: `*.md` is listed in `.gitignore`, so this file (and other markdown)
> is intentionally untracked by git.
