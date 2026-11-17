package test.framework;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import core.api.ApiClient;
import core.api.ApiConfig;
import core.api.dao.UserDAO;
import core.security.UserSession;
import models.entity.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared runtime context for the live API test suite.
 * Performs a single authentication against the backend described by
 * {@link ApiConfig#BASE_URL} (which is derived from {@code SystemParameters.API_BASE_URL})
 * and stores the JWT tokens in both {@link ApiClient} and {@link UserSession}.
 *
 * Credentials mirror {@code tmp/run_api_tests.py}: qa_tester first, then testuser.
 *
 * @author Hivon QA
 */
public final class TestContext {

    private static boolean bootstrapped;
    private static String baseUrl;
    private static String username;
    private static final List<String> setupNotes =
            Collections.synchronizedList(new ArrayList<>());

    private TestContext() {
    }

    public static synchronized void ensureAuthenticated() throws Exception {
        if (bootstrapped) {
            return;
        }
        bootstrap();
    }

    private static void bootstrap() throws Exception {
        baseUrl = ApiConfig.BASE_URL;
        setupNotes.add("Base URL: " + baseUrl);

        List<String[]> candidates = new ArrayList<>();
        String propUser = System.getProperty("hivon.test.username");
        String propPass = System.getProperty("hivon.test.password");
        if (propUser != null && !propUser.trim().isEmpty()) {
            candidates.add(new String[]{propUser, propPass});
        }
        String qaPassword = System.getProperty("QA_TESTER_PASSWORD");
        if (qaPassword == null || qaPassword.isEmpty()) {
            qaPassword = System.getenv("QA_TESTER_PASSWORD");
        }
        if (qaPassword == null || qaPassword.isEmpty()) {
            qaPassword = "Qa#Tester2026!";
        }
        candidates.add(new String[]{"qa_tester", qaPassword});
        candidates.add(new String[]{"testuser", "123456"});

        Exception last = null;
        for (String[] candidate : candidates) {
            try {
                ApiClient api = ApiClient.getInstance();
                JsonObject response = api.login(candidate[0], candidate[1]);
                JsonObject data = response.getAsJsonObject("data");
                JsonObject userJson = data.getAsJsonObject("user");
                User user = UserDAO.getInstance().jsonToUser(userJson);
                Set<String> permissions = parsePermissions(data);
                String access = data.has("access_token") && !data.get("access_token").isJsonNull()
                        ? data.get("access_token").getAsString() : null;
                String refresh = data.has("refresh_token") && !data.get("refresh_token").isJsonNull()
                        ? data.get("refresh_token").getAsString() : null;

                api.setTokens(access, refresh);
                UserSession.getInstance().initialize(user, permissions, access, refresh);

                username = candidate[0];
                bootstrapped = true;
                setupNotes.add("Authenticated as '" + candidate[0] + "' (role="
                        + (user != null ? user.getRole() : "?")
                        + ", permissions=" + permissions.size() + ").");
                return;
            } catch (Exception e) {
                last = e;
                setupNotes.add("Login attempt '" + candidate[0] + "' failed: " + e.getMessage());
            }
        }
        throw new IllegalStateException(
                "Unable to authenticate against " + baseUrl
                        + " using qa_tester or testuser. Ensure the local API is running.", last);
    }

    private static Set<String> parsePermissions(JsonObject data) {
        Set<String> permissions = new HashSet<>();
        if (data.has("permissions") && data.get("permissions").isJsonArray()) {
            JsonArray array = data.getAsJsonArray("permissions");
            for (JsonElement element : array) {
                if (!element.isJsonNull()) {
                    permissions.add(element.getAsString());
                }
            }
        }
        return permissions;
    }

    public static String getUsername() {
        return username;
    }

    public static String getBaseUrl() {
        return baseUrl;
    }

    public static List<String> getSetupNotes() {
        synchronized (setupNotes) {
            return new ArrayList<>(setupNotes);
        }
    }
}
