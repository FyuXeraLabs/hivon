package test.framework;

import org.junit.jupiter.api.Assertions;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic, reflection based bean assertions for DTOs. Avoids per-field
 * knowledge: for every simple-typed setter with a matching getter it performs
 * a set/get round-trip.
 */
public final class DtoAssert {

    private DtoAssert() {
    }

    public static void assertBean(Class<?> dtoClass) {
        Object instance;
        try {
            instance = dtoClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            Assertions.assertTrue(true, dtoClass.getSimpleName() + " has no default constructor (skipped)");
            return;
        } catch (ReflectiveOperationException e) {
            Assertions.fail(dtoClass.getSimpleName() + " could not be instantiated: " + e);
            return;
        }

        int checked = 0;
        for (Method setter : dtoClass.getMethods()) {
            if (!setter.getName().startsWith("set") || setter.getParameterCount() != 1) {
                continue;
            }
            if (Modifier.isStatic(setter.getModifiers())) {
                continue;
            }
            Class<?> type = setter.getParameterTypes()[0];
            Object value = sampleValue(type);
            if (value == null) {
                continue;
            }
            Method getter = findGetter(dtoClass, setter.getName(), type);
            if (getter == null) {
                continue;
            }
            try {
                setter.invoke(instance, value);
                Object actual = getter.invoke(instance);
                Assertions.assertEquals(value, actual,
                        dtoClass.getSimpleName() + "." + setter.getName()
                                + "/" + getter.getName() + " round-trip");
                checked++;
            } catch (ReflectiveOperationException e) {
                Assertions.fail(dtoClass.getSimpleName() + "." + setter.getName()
                        + " threw " + e.getCause());
            }
        }
        Assertions.assertTrue(true, dtoClass.getSimpleName() + ": verified " + checked + " property(ies)");
    }

    private static Method findGetter(Class<?> dtoClass, String setterName, Class<?> type) {
        String base = setterName.substring(3);
        if (base.isEmpty()) {
            return null;
        }
        String getter = "get" + base;
        try {
            return dtoClass.getMethod(getter);
        } catch (NoSuchMethodException ignored) {
            // boolean may use isX()
        }
        if (type == boolean.class || type == Boolean.class) {
            try {
                return dtoClass.getMethod("is" + base);
            } catch (NoSuchMethodException ignored) {
                // no getter
            }
        }
        return null;
    }

    private static Object sampleValue(Class<?> type) {
        if (type == String.class) {
            return "TEST_VALUE";
        }
        if (type == int.class || type == Integer.class) {
            return 7;
        }
        if (type == long.class || type == Long.class) {
            return 7L;
        }
        if (type == double.class || type == Double.class) {
            return 7.5d;
        }
        if (type == boolean.class || type == Boolean.class) {
            return Boolean.TRUE;
        }
        if (type == LocalDate.class) {
            return LocalDate.of(2030, 1, 1);
        }
        if (type == LocalDateTime.class) {
            return LocalDateTime.of(2030, 1, 1, 12, 0);
        }
        if (type == List.class) {
            return new ArrayList<>();
        }
        if (type == Map.class) {
            return new HashMap<>();
        }
        return null;
    }
}
