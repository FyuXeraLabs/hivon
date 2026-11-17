package test.framework;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregated outcome of a single CLI run.
 */
public class RunReport {

    public final List<TestResult> results = new ArrayList<>();
    public final List<Bug> bugs = new ArrayList<>();

    public String baseUrl;
    public String username;
    public String runLabel;
    public String runDate;
    public long durationMs;
    public int passed;
    public int failed;
    public int skipped;
    public List<String> setupNotes = new ArrayList<>();

    public int total() {
        return results.size();
    }
}
