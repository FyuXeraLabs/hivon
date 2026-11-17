package test.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Optional severity override for the bug report. When absent the severity is
 * derived from the failure type (5xx/timeout = High, assertion/4xx = Medium).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TestSeverity {
    String value();
}
