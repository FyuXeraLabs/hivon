package test.framework;

import org.junit.platform.engine.FilterResult;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.PostDiscoveryFilter;
import org.junit.platform.launcher.TagFilter;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Discovers and executes the JUnit 5 test suite via the JUnit Platform
 * Launcher, collecting a rich {@link RunReport} for the CLI and the xlsx
 * report writers.
 */
public final class TestRunner {

    private static final DateTimeFormatter RUN_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TestRunner() {
    }

    /**
     * Runs the suite. {@code form} and {@code layer} may be null to broaden the
     * selection. When both are null every test under {@code test.*} runs.
     */
    public static RunReport run(String form, String layer, String label) {
        LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectPackage("test"));

        boolean hasForm = form != null && !form.isEmpty();
        boolean hasLayer = layer != null && !layer.isEmpty();
        if (hasForm && hasLayer) {
            PostDiscoveryFilter formFilter = TagFilter.includeTags(form);
            PostDiscoveryFilter layerFilter = TagFilter.includeTags(layer);
            builder.filters(and(formFilter, layerFilter));
        } else if (hasForm) {
            builder.filters(TagFilter.includeTags(form));
        } else if (hasLayer) {
            builder.filters(TagFilter.includeTags(layer));
        }

        LauncherDiscoveryRequest request = builder.build();
        Launcher launcher = LauncherFactory.create();
        CollectingListener listener = new CollectingListener();
        long start = System.currentTimeMillis();
        launcher.execute(request, listener);
        long duration = System.currentTimeMillis() - start;

        return listener.buildReport(label, duration);
    }

    private static PostDiscoveryFilter and(PostDiscoveryFilter first, PostDiscoveryFilter second) {
        return new PostDiscoveryFilter() {
            @Override
            public FilterResult apply(TestDescriptor descriptor) {
                FilterResult a = first.apply(descriptor);
                if (!a.included()) {
                    return a;
                }
                return second.apply(descriptor);
            }

            @Override
            public String toString() {
                return first + " and " + second;
            }
        };
    }

    static final class CollectingListener implements TestExecutionListener {

        private final Map<String, Long> starts = new HashMap<>();
        private final List<TestResult> results = new ArrayList<>();

        @Override
        public void testPlanExecutionStarted(TestPlan testPlan) {
            // no-op
        }

        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            if (testIdentifier.isTest()) {
                starts.put(testIdentifier.getUniqueId(), System.currentTimeMillis());
            }
        }

        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
            if (!testIdentifier.isTest()) {
                if (result.getStatus() == TestExecutionResult.Status.FAILED) {
                    TestResult setupFailure = fromIdentifier(testIdentifier, result, 0L);
                    setupFailure.layer = TestRegistry.LAYER_SETUP;
                    setupFailure.form = TestRegistry.LAYER_SETUP;
                    results.add(setupFailure);
                }
                return;
            }
            Long started = starts.remove(testIdentifier.getUniqueId());
            long duration = started == null ? 0L : System.currentTimeMillis() - started;
            results.add(fromIdentifier(testIdentifier, result, duration));
        }

        private TestResult fromIdentifier(TestIdentifier id, TestExecutionResult result, long duration) {
            TestResult tr = new TestResult();
            tr.displayName = id.getDisplayName();
            tr.durationMs = duration;
            tr.status = switch (result.getStatus()) {
                case SUCCESSFUL -> "PASS";
                case ABORTED -> "SKIP";
                case FAILED -> "FAIL";
            };

            id.getTags().forEach(tag -> tr.tags.add(tag.getName()));
            for (String tag : tr.tags) {
                if (TestRegistry.isForm(tag)) {
                    tr.form = tag;
                } else if (TestRegistry.isLayer(tag)) {
                    tr.layer = tag;
                }
            }
            if (tr.form == null) {
                tr.form = "n/a";
            }
            if (tr.layer == null) {
                tr.layer = "n/a";
            }

            TestSource source = id.getSource().orElse(null);
            if (source instanceof MethodSource methodSource) {
                tr.className = methodSource.getClassName();
                tr.methodName = methodSource.getMethodName();
            } else if (source instanceof ClassSource classSource) {
                tr.className = classSource.getClassName();
                tr.methodName = tr.displayName;
            } else {
                tr.className = "?";
                tr.methodName = tr.displayName;
            }

            result.getThrowable().ifPresent(throwable -> {
                tr.message = describe(throwable);
                tr.stackTrace = stackTrace(throwable);
            });
            return tr;
        }

        RunReport buildReport(String label, long duration) {
            RunReport report = new RunReport();
            report.runLabel = label == null ? "ALL" : label;
            report.runDate = LocalDateTime.now().format(RUN_DATE);
            report.durationMs = duration;
            report.baseUrl = TestContext.getBaseUrl();
            report.username = TestContext.getUsername();
            report.setupNotes = TestContext.getSetupNotes();

            int id = 1;
            for (TestResult tr : results) {
                tr.id = id++;
                report.results.add(tr);
                if (tr.passed()) {
                    report.passed++;
                } else if (tr.skipped()) {
                    report.skipped++;
                } else {
                    report.failed++;
                }
            }

            Map<String, Bug> byCase = new LinkedHashMap<>();
            for (TestResult tr : report.results) {
                if (tr.passed() || tr.skipped()) {
                    continue;
                }
                Bug bug = byCase.get(tr.caseKey());
                if (bug == null) {
                    bug = new Bug();
                    bug.form = tr.form;
                    bug.layer = tr.layer;
                    bug.severity = classifySeverity(tr);
                    byCase.put(tr.caseKey(), bug);
                }
                bug.cases.add(tr);
            }
            int bugId = 1;
            for (Bug bug : byCase.values()) {
                bug.id = String.format("BUG-%03d", bugId++);
                report.bugs.add(bug);
            }
            return report;
        }
    }

    static String classifySeverity(TestResult tr) {
        String msg = tr.message == null ? "" : tr.message;
        String lower = msg.toLowerCase();
        if (lower.contains("headless") || lower.contains("500")
                || lower.contains("502") || lower.contains("503")
                || lower.contains("504") || lower.contains("connectexception")
                || lower.contains("sockettimeout") || lower.contains("timeout")
                || lower.contains("unknownhost") || lower.contains("nullpointer")) {
            return "High";
        }
        return "Medium";
    }

    private static String describe(Throwable throwable) {
        String message = throwable.getMessage();
        String type = throwable.getClass().getSimpleName();
        return message == null || message.isEmpty() ? type : type + ": " + message;
    }

    private static String stackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }
}
