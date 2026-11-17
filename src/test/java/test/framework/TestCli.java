package test.framework;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Interactive command line runner for the Hivon form-wise test suite.
 *
 * <p>Run interactively:  {@code java -jar target/hivon-tests.jar}
 * <br>Run non-interactively: {@code java -jar target/hivon-tests.jar --all}
 * or {@code ... CustomerForm controller}.</p>
 */
public class TestCli {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final Path testsDir;
    private final Path bugsDir;
    private boolean reportsEnabled = true;

    public TestCli() {
        Path base = Paths.get(System.getProperty("hivon.reports.dir", "docs/reports"));
        this.testsDir = base.resolve("tests");
        this.bugsDir = base.resolve("bugs");
    }

    public static void main(String[] args) throws Exception {
        configureHeadless();
        TestCli cli = new TestCli();
        cli.start(args);
    }

    private static void configureHeadless() {
        if (System.getProperty("java.awt.headless") == null) {
            System.setProperty("java.awt.headless", "true");
        }
        if ("true".equalsIgnoreCase(System.getProperty("gui.tests"))) {
            System.setProperty("java.awt.headless", "false");
        }
    }

    private void start(String[] args) throws Exception {
        printBanner();
        if (args != null && args.length > 0) {
            runFromArgs(args);
            return;
        }
        interactive();
    }

    // ------------------------------------------------------------------
    // Argument mode
    // ------------------------------------------------------------------

    private void runFromArgs(String[] args) throws Exception {
        String form = null;
        String layer = null;
        boolean all = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            String lower = arg.toLowerCase();
            switch (lower) {
                case "--all", "all" -> all = true;
                case "--no-report" -> reportsEnabled = false;
                case "--list", "list" -> {
                    printForms();
                    return;
                }
                case "--help", "-h" -> {
                    printMenu();
                    return;
                }
                case "dto", "dao", "controller", "form" -> layer = lower;
                case "--dto" -> layer = "dto";
                case "--dao" -> layer = "dao";
                case "--controller" -> layer = "controller";
                case "--form-layer" -> layer = "form";
                default -> {
                    String resolved = TestRegistry.resolveForm(arg);
                    if (resolved != null) {
                        form = resolved;
                    } else if (!lower.startsWith("--")) {
                        System.out.println("[warn] Unknown form '" + arg + "'. Use --list to see options.");
                        return;
                    }
                }
            }
        }
        execute(all ? null : form, layer, all);
    }

    // ------------------------------------------------------------------
    // Interactive mode
    // ------------------------------------------------------------------

    private void interactive() throws Exception {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                printMenu();
                System.out.print("Select> ");
                System.out.flush();
                if (!scanner.hasNextLine()) {
                    break;
                }
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    continue;
                }
                switch (input.toLowerCase()) {
                    case "1", "all" -> execute(null, null, true);
                    case "2" -> {
                        String form = promptForm(scanner);
                        if (form != null) {
                            execute(form, null, false);
                        }
                    }
                    case "3" -> {
                        String form = promptForm(scanner);
                        if (form != null) {
                            String layer = promptLayer(scanner);
                            if (layer != null) {
                                execute(form, layer, false);
                            }
                        }
                    }
                    case "4", "list" -> printForms();
                    case "5" -> {
                        reportsEnabled = !reportsEnabled;
                        System.out.println("Reports are now " + (reportsEnabled ? "ON" : "OFF") + ".");
                    }
                    case "h", "help" -> {
                        // menu already printed on next iteration
                    }
                    case "q", "quit", "exit" -> {
                        System.out.println("Bye.");
                        return;
                    }
                    default -> {
                        String form = TestRegistry.resolveForm(input);
                        if (form != null) {
                            execute(form, null, false);
                        } else {
                            System.out.println("Unknown selection: " + input);
                        }
                    }
                }
            }
        }
    }

    private String promptForm(Scanner scanner) {
        printForms();
        System.out.print("Form (name or number)> ");
        System.out.flush();
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return null;
        }
        try {
            int index = Integer.parseInt(input);
            String form = TestRegistry.formAt(index);
            if (form == null) {
                System.out.println("Invalid form number.");
            }
            return form;
        } catch (NumberFormatException e) {
            String form = TestRegistry.resolveForm(input);
            if (form == null) {
                System.out.println("No matching form for '" + input + "'.");
            }
            return form;
        }
    }

    private String promptLayer(Scanner scanner) {
        System.out.println("Layers: 1) dto  2) dao  3) controller  4) form  5) all layers");
        System.out.print("Layer> ");
        System.out.flush();
        String input = scanner.nextLine().trim().toLowerCase();
        return switch (input) {
            case "1", "dto" -> TestRegistry.LAYER_DTO;
            case "2", "dao" -> TestRegistry.LAYER_DAO;
            case "3", "controller" -> TestRegistry.LAYER_CONTROLLER;
            case "4", "form" -> TestRegistry.LAYER_FORM;
            case "5", "all", "" -> null;
            default -> {
                System.out.println("Unknown layer '" + input + "'; running all layers.");
                yield null;
            }
        };
    }

    // ------------------------------------------------------------------
    // Execution + reporting
    // ------------------------------------------------------------------

    private void execute(String form, String layer, boolean all) {
        String label = all || form == null ? "ALL" : form;
        String selection = "form=" + (form == null ? "ALL" : form)
                + ", layer=" + (layer == null ? "ALL" : layer);
        System.out.println("\n>>> Running tests [" + selection + "] ...");

        try {
            TestContext.ensureAuthenticated();
            System.out.println("    Authenticated as '" + TestContext.getUsername()
                    + "' @ " + TestContext.getBaseUrl());
        } catch (Exception e) {
            System.out.println("    [warn] pre-flight authentication failed: " + e.getMessage());
            System.out.println("    Tests will still run and surface the failure as a bug.");
        }

        RunReport report = TestRunner.run(form, layer, label);
        printSummary(report);

        if (reportsEnabled) {
            writeReports(report, label);
        } else {
            System.out.println("Reports disabled (enable from the menu or drop --no-report).");
        }
    }

    private void writeReports(RunReport report, String label) {
        String stamp = LocalDateTime.now().format(STAMP);
        List<Path> written = new ArrayList<>();
        try {
            written.add(TestReportWriter.write(report, testsDir, stamp));
        } catch (Exception e) {
            System.out.println("[error] Failed to write test report: " + e.getMessage());
        }
        try {
            written.add(BugReportWriter.write(report, bugsDir, stamp));
        } catch (Exception e) {
            System.out.println("[error] Failed to write bug report: " + e.getMessage());
        }
        for (Path path : written) {
            System.out.println("    report -> " + path.toAbsolutePath());
        }
    }

    private void printSummary(RunReport report) {
        System.out.println();
        System.out.println("+------------------------------------------------------------+");
        System.out.printf("| Total: %-4d  Passed: %-4d  Failed: %-4d  Skipped: %-4d%n",
                report.total(), report.passed, report.failed, report.skipped);
        System.out.printf("| Pass rate: %-8s  Duration: %d ms   Open bugs: %d%n",
                TestReportWriter.passRate(report.passed, report.total()),
                report.durationMs, report.bugs.size());
        System.out.println("+------------------------------------------------------------+");

        if (!report.bugs.isEmpty()) {
            System.out.println("Bugs:");
            for (Bug bug : report.bugs) {
                TestResult first = bug.first();
                System.out.printf("  %s [%s] %s/%s: %s%n",
                        bug.id, bug.severity, bug.form, bug.layer,
                        first == null ? "" : first.message);
            }
        }
    }

    // ------------------------------------------------------------------
    // Console helpers
    // ------------------------------------------------------------------

    private void printBanner() {
        System.out.println("============================================================");
        System.out.println(" Hivon Form-wise Test Runner");
        System.out.println(" (JUnit 5 live API suite -> xlsx test & bug reports)");
        System.out.println("============================================================");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("1) Run ALL forms");
        System.out.println("2) Run one form (all layers)");
        System.out.println("3) Run one layer of one form");
        System.out.println("4) List forms");
        System.out.println("5) Toggle reports (currently " + (reportsEnabled ? "ON" : "OFF") + ")");
        System.out.println("h) Help    q) Quit");
    }

    private void printForms() {
        System.out.println();
        System.out.println("Forms:");
        for (int i = 0; i < TestRegistry.FORMS.length; i++) {
            System.out.printf("  %2d) %s%n", i + 1, TestRegistry.FORMS[i]);
        }
    }
}
