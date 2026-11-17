package core.utils;

import java.util.concurrent.Callable;
import ui.dialogs.ProgressDialog;

/**
 * Utility helper to handle retries for database/DAO operations.
 *
 * @author Piyumi
 */
public class RetryHelper {

    private static final ThreadLocal<Boolean> SILENT_MODE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static final class RetryResult<T> {
        volatile boolean succeeded;
        volatile Exception error;
        volatile T value;
    }

    /**
     * Enables or disables silent execution on the current thread.
     * When silent, executeWithRetry executes operations directly without opening ProgressDialog.
     */
    public static void setSilent(boolean silent) {
        SILENT_MODE.set(silent);
    }

    public static boolean isSilent() {
        return Boolean.TRUE.equals(SILENT_MODE.get());
    }

    /**
     * Executes an operation silently without showing modal ProgressDialog.
     */
    public static <T> T executeSilently(Callable<T> operation) throws Exception {
        boolean prev = isSilent();
        try {
            setSilent(true);
            return operation.call();
        } finally {
            setSilent(prev);
        }
    }

    /**
     * Execute an operation with retry support using the ProgressDialog.
     * On failure, shows Retry/Cancel buttons on the progress dialog.
     *
     * @param parent the parent component for the progress dialog
     * @param taskName the name displayed on the progress dialog
     * @param operation the operation to execute
     * @param errorMessage the error message prefix shown on failure
     * @return the result of the operation
     * @throws Exception if the operation fails and user cancels
     */
    public static <T> T executeWithRetry(java.awt.Component parent, String taskName,
            Callable<T> operation, String errorMessage) throws Exception {
        // If headless, silent (e.g. AutoSuggest), or already running on a background worker
        // thread (non-EDT), execute directly without spawning a nested or non-EDT modal ProgressDialog.
        if (java.awt.GraphicsEnvironment.isHeadless() || isSilent() || !javax.swing.SwingUtilities.isEventDispatchThread()) {
            return operation.call();
        }

        RetryResult<T> result = new RetryResult<>();

        core.workers.BackgroundTask task = createRetryTask(parent, taskName, operation, errorMessage, result);
        task.executeWithRetry();

        if (result.succeeded) {
            return result.value;
        } else if (result.error != null) {
            throw result.error;
        } else {
            throw new Exception("Operation cancelled");
        }
    }

    /**
     * Legacy overload — executes with retry using a frameless progress dialog.
     * Prefer the overload with parent component.
     */
    public static <T> T executeWithRetry(Callable<T> operation, String errorMessage) throws Exception {
        return executeWithRetry(null, errorMessage, operation, errorMessage);
    }

    /**
     * Formats an error phrase (e.g. "failed to fetch receiving bins")
     * into a clean title (e.g. "Fetch receiving bins").
     */
    public static String formatTaskTitle(String msg) {
        if (msg == null || msg.trim().isEmpty()) {
            return "Please wait";
        }
        String s = msg.trim();
        String lower = s.toLowerCase();
        if (lower.startsWith("failed to ")) {
            s = s.substring(10).trim();
        } else if (lower.startsWith("failed ")) {
            s = s.substring(7).trim();
        } else if (lower.startsWith("error ")) {
            s = s.substring(6).trim();
        }
        if (!s.isEmpty()) {
            s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
        return s;
    }

    /**
     * Formats an error phrase (e.g. "failed to fetch receiving bins")
     * into a positive in-progress message (e.g. "Fetching receiving bins...").
     */
    public static String formatProgressMessage(String msg) {
        if (msg == null || msg.trim().isEmpty()) {
            return "Processing...";
        }
        String s = msg.trim();
        String lower = s.toLowerCase();
        if (lower.startsWith("failed to ")) {
            s = s.substring(10).trim();
        } else if (lower.startsWith("failed ")) {
            s = s.substring(7).trim();
        } else if (lower.startsWith("error ")) {
            s = s.substring(6).trim();
        }

        s = convertToPresentParticiple(s);

        if (!s.isEmpty()) {
            s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
        }
        if (!s.endsWith("...")) {
            s += "...";
        }
        return s;
    }

    private static String convertToPresentParticiple(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String[] parts = text.split("\\s+", 2);
        String verb = parts[0].toLowerCase();
        String rest = parts.length > 1 ? " " + parts[1] : "";

        String participle;
        switch (verb) {
            case "fetch": participle = "Fetching"; break;
            case "load": participle = "Loading"; break;
            case "search": participle = "Searching"; break;
            case "process": participle = "Processing"; break;
            case "complete": participle = "Completing"; break;
            case "create": participle = "Creating"; break;
            case "update": participle = "Updating"; break;
            case "delete": participle = "Deleting"; break;
            case "save": participle = "Saving"; break;
            case "post": participle = "Posting"; break;
            case "get": participle = "Getting"; break;
            case "submit": participle = "Submitting"; break;
            case "start": participle = "Starting"; break;
            case "suggest": participle = "Suggesting"; break;
            default:
                if (verb.endsWith("e") && !verb.endsWith("ee")) {
                    participle = verb.substring(0, verb.length() - 1) + "ing";
                } else if (!verb.endsWith("ing")) {
                    participle = verb + "ing";
                } else {
                    participle = verb;
                }
                participle = Character.toUpperCase(participle.charAt(0)) + participle.substring(1);
                break;
        }
        return participle + rest;
    }

    private static <T> core.workers.BackgroundTask createRetryTask(
            java.awt.Component parent, String taskName,
            Callable<T> operation, String errorMessage,
            RetryResult<T> result) {
        String cleanTitle = formatTaskTitle(taskName);
        String cleanProgress = formatProgressMessage(taskName);

        return new core.workers.BackgroundTask((java.awt.Component) parent, cleanTitle) {
            @Override
            protected Boolean performTask() throws Exception {
                updateProgress(cleanProgress);
                T value = operation.call();
                result.value = value;
                result.succeeded = true;
                return true;
            }

            @Override
            protected void onSuccess() {
            }

            @Override
            protected void onFailure(Exception e) {
                result.error = e;
            }

            @Override
            protected void onCancel() {
                result.error = new Exception("Operation cancelled by user");
            }

            @Override
            protected core.workers.BackgroundTask createRetryTask() {
                return RetryHelper.createRetryTask(parent, taskName, operation, errorMessage, result);
            }
        };
    }
}
