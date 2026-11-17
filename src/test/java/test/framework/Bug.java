package test.framework;

import java.util.ArrayList;
import java.util.List;

/**
 * A defect derived from one or more failing test cases.
 */
public class Bug {

    public String id;
    public String severity;
    public String form;
    public String layer;
    public final List<TestResult> cases = new ArrayList<>();

    public TestResult first() {
        return cases.isEmpty() ? null : cases.get(0);
    }
}
