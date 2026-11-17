package test.framework;

import java.util.ArrayList;
import java.util.List;

/**
 * A single executed test case (or container-level setup failure).
 */
public class TestResult {

    public int id;
    public String displayName;
    public String className;
    public String methodName;
    public String form;
    public String layer;
    public String status;
    public long durationMs;
    public String message;
    public String stackTrace;
    public final List<String> tags = new ArrayList<>();

    public String caseKey() {
        return (className == null ? "?" : className) + "#"
                + (methodName == null ? displayName : methodName);
    }

    public boolean passed() {
        return "PASS".equals(status);
    }

    public boolean skipped() {
        return "SKIP".equals(status);
    }
}
