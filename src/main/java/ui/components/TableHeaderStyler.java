package ui.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 * Applies the application wide column header colours to a table.
 *
 * The header background cannot be set from the NetBeans designer, so it is
 * applied in code. Two things are needed for the colour to actually show:
 *
 * - the header background itself, which fills the area to the right of the
 *   last column, and
 * - a wrapper around the default header renderer, because the focused header
 *   cell falls back to the "TableHeader.focusCellBackground" look and feel
 *   key instead of the header background.
 *
 * The wrapper delegates to the renderer it replaces, so the header font and
 * any sort icons are preserved.
 *
 * For the whole application use {@link #applyGlobally()}, which installs the
 * colours as look and feel defaults. Every table created afterwards picks them
 * up, so no form has to call anything.
 */
public final class TableHeaderStyler {

    /** Default column header backjground, rgb(178, 195, 207). */
    public static final Color DEFAULT_HEADER_BACKGROUND = new Color(178, 195, 207);

    /** Default column header foreground, kept dark for contrast. */
    public static final Color DEFAULT_HEADER_FOREGROUND = new Color(0, 0, 0);

    /** Marks a header that already carries the "UI" listener of this class. */
    private static final String UI_LISTENER_INSTALLED = "hivon.tableHeaderStyler.uiListener";

    private TableHeaderStyler() {
    }

    /**
     * Installs the header colours for the whole application.
     *
     * The colours are put into the look and feel defaults, so every table in
     * the application gets them, including the tables of forms that are
     * opened later and the tables of dialogs. Tables that are already open are
     * restyled as well.
     *
     * Call this again after the look and feel has been changed, because a new
     * look and feel reinstalls its own defaults.
     */
    public static void applyGlobally() {
        applyGlobally(DEFAULT_HEADER_BACKGROUND, DEFAULT_HEADER_FOREGROUND);
    }

    /**
     * Installs the given header colours for the whole application.
     *
     * @param background the header background colour
     * @param foreground the header foreground colour
     */
    public static void applyGlobally(Color background, Color foreground) {
        UIManager.put("TableHeader.background", background);
        // the focused header cell uses its own key, so it has to be set as well
        UIManager.put("TableHeader.focusCellBackground", background);
        UIManager.put("TableHeader.foreground", foreground);

        for (Window window : Window.getWindows()) {
            if (window instanceof Container) {
                applyToAllTables((Container) window);
            }
        }
    }

    /**
     * Applies the header colours to every table inside the given container.
     *
     * @param root the container to search, usually a frame or a dialog
     */
    public static void applyToAllTables(Container root) {
        if (root == null) {
            return;
        }
        if (root instanceof JTable) {
            apply((JTable) root);
        }
        for (Component child : root.getComponents()) {
            if (child instanceof Container) {
                applyToAllTables((Container) child);
            }
        }
    }

    /**
     * Applies the default header colours to the given table.
     *
     * @param table the table whose column headers should be styled
     */
    public static void apply(JTable table) {
        apply(table, DEFAULT_HEADER_BACKGROUND, DEFAULT_HEADER_FOREGROUND);
    }

    /**
     * Applies the given header colours to the given table.
     *
     * @param table the table whose column headers should be styled
     * @param background the header background colour
     * @param foreground the header foreground colour
     */
    public static void apply(JTable table, Color background, Color foreground) {
        if (table == null) {
            return;
        }

        final JTableHeader header = table.getTableHeader();
        if (header == null) {
            return;
        }

        final Color headerBackground = background;
        final Color headerForeground = foreground;

        // wrap the current renderer so the colours apply to every cell,
        // including the focused one, without losing the sort icons or font
        final TableCellRenderer delegate = header.getDefaultRenderer();
        if (!(delegate instanceof HeaderColorRenderer)) {
            header.setDefaultRenderer(new HeaderColorRenderer(delegate, headerBackground, headerForeground));
        }

        // the look and feel re-installs the header UI on every theme change,
        // which resets the header background and replaces the default renderer,
        // so re-apply it afterwards.
        // the listener is added only once per header, otherwise every call
        // of this method would add another one
        if (!Boolean.TRUE.equals(header.getClientProperty(UI_LISTENER_INSTALLED))) {
            header.putClientProperty(UI_LISTENER_INSTALLED, Boolean.TRUE);
            header.addPropertyChangeListener("UI", e -> {
                setColors(header, headerBackground, headerForeground);
                wrapRenderer(header, headerBackground, headerForeground);
            });
        }
        wrapRenderer(header, headerBackground, headerForeground);
        setColors(header, headerBackground, headerForeground);
    }

    /**
     * Wraps the current header renderer, unless it is already wrapped.
     */
    private static void wrapRenderer(JTableHeader header, Color background, Color foreground) {
        TableCellRenderer delegate = header.getDefaultRenderer();
        if (delegate instanceof HeaderColorRenderer) {
            return;
        }
        header.setDefaultRenderer(new HeaderColorRenderer(delegate, background, foreground));
    }

    private static void setColors(JTableHeader header, Color background, Color foreground) {
        header.setBackground(background);
        header.setForeground(foreground);
    }

    /**
     * Forces the header colours onto the cell renderer component, whatever
     * the look and feel decides to use for the focused cell.
     */
    private static final class HeaderColorRenderer implements TableCellRenderer {

        private final TableCellRenderer delegate;
        private final Color background;
        private final Color foreground;

        HeaderColorRenderer(TableCellRenderer delegate, Color background, Color foreground) {
            this.delegate = delegate;
            this.background = background;
            this.foreground = foreground;
        }

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            java.awt.Component comp = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            comp.setBackground(background);
            comp.setForeground(foreground);
            if (comp instanceof JComponent) {
                ((JComponent) comp).setOpaque(true);
            }
            return comp;
        }
    }
}
