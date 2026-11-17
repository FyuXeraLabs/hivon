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
        // A headless JVM or a silent background operation (such as AutoSuggest)
        // runs the operation directly instead of showing a modal ProgressDialog.
        if (java.awt.GraphicsEnvironment.isHeadless() || isSilent()) {
            return operation.call();
        }

        final Object[] result = new Object[1];
        final Exception[] error = new Exception[1];
        final boolean[] succeeded = new boolean[1];

        core.workers.BackgroundTask task = createRetryTask(parent, taskName, operation, errorMessage, result, error, succeeded);
        task.executeWithRetry();

        if (succeeded[0]) {
            @SuppressWarnings("unchecked")
            T typedResult = (T) result[0];
            return typedResult;
        } else if (error[0] != null) {
            throw error[0];
        } else {
            throw new Exception("Operation cancelled");
        }
    }

    /**
     * Legacy overload â€” executes with retry using a frameless progress dialog.
     * Prefer the overload with parent component.
     */
    public static <T> T executeWithRetry(Callable<T> operation, String errorMessage) throws Exception {
        return executeWithRetry(null, errorMessage, operation, errorMessage);
    }

    private static <T> core.workers.BackgroundTask createRetryTask(
            java.awt.Component parent, String taskName,
            Callable<T> operation, String errorMessage,
            Object[] result, Exception[] error, boolean[] succeeded) {
        return new core.workers.BackgroundTask((java.awt.Component) parent, taskName) {
            @Override
            protected Boolean performTask() throws Exception {
                updateProgress(errorMessage + "...");
                result[0] = operation.call();
                return true;
            }

            @Override
            protected void onSuccess() {
                succeeded[0] = true;
            }

            @Override
            protected void onFailure(Exception e) {
                // handled by retry/cancel on progress dialog
            }

            @Override
            protected void onCancel() {
                error[0] = new Exception("Operation cancelled by user");
            }

            @Override
            protected core.workers.BackgroundTask createRetryTask() {
                return RetryHelper.createRetryTask(parent, taskName, operation, errorMessage, result, error, succeeded);
            }
        };
    }
}
