package core.logging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 *
 * @author Sanod
 */
public class UsernameFormatter extends Formatter {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();

        sb.append(LocalDateTime.now().format(FORMATTER))
          .append(" [").append(record.getLevel()).append("] ")
          .append(record.getMessage())
          .append(System.lineSeparator());

        if (record.getThrown() != null) {
            Throwable t = record.getThrown();
            sb.append(t).append(System.lineSeparator());
            for (StackTraceElement el : t.getStackTrace()) {
                sb.append("\tat ").append(el)
                  .append(System.lineSeparator());
            }
        }

        return sb.toString();
    }
}