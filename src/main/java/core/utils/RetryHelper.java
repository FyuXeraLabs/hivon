package core.utils;

import javax.swing.JOptionPane;
import java.util.concurrent.Callable;

/**
 * Utility helper to handle retries for database/DAO operations.
 *
 * @author Piyumi
 */
public class RetryHelper {

    public static <T> T executeWithRetry(Callable<T> operation, String errorMessage) throws Exception {
        while (true) {
            try {
                return operation.call();
            } catch (Exception e) {
                core.logging.Logger.errlog(errorMessage + ": " + e.getMessage(), e);
                Object[] options = {"Retry", "Exit"};
                int choice = JOptionPane.showOptionDialog(
                        null,
                        errorMessage + "\n" + e.getMessage() + "\nPlease check and try again!",
                        "Database Error",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        options,
                        options[0]
                );
                if (choice != 0) {
                    throw e;
                }
            }
        }
    }
}
