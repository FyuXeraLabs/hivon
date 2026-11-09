/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.utils.export;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import core.logging.Logger;
import core.workers.BackgroundTask;

/**
 * Centralized PDF exporter for form summaries.
 * Generates a PDF containing form detail fields and a summary table.
 * Supports both direct printing and save-as-PDF workflows.
 * <p>
 * Usage:
 * <pre>
 *   FormSummaryPdfExporter.builder()
 *       .title("Goods Receipt - Purchase Order (IN11)")
 *       .subtitle("PO Number: PO-2026-001")
 *       .addDetail("PO Number", "PO-2026-001")
 *       .addDetail("PO Date", "2026-10-24")
 *       .tableColumns(new String[]{"Material", "Qty", "Batch", "Bin", "Quality"})
 *       .addTableRow(new String[]{"MAT-001 - Widget", "100.00", "B001", "BIN-01", "Accepted"})
 *       .showExportDialog(parentComponent);
 * </pre>
 *
 * @author Sanod
 */
public class FormSummaryPdfExporter {

    // pdf layout constants
    private static final float PAGE_WIDTH = PDRectangle.A4.getHeight();  // landscape
    private static final float PAGE_HEIGHT = PDRectangle.A4.getWidth();
    private static final float MARGIN_LEFT = 50;
    private static final float MARGIN_RIGHT = 50;
    private static final float MARGIN_TOP = 40;
    private static final float MARGIN_BOTTOM = 50;
    private static final float CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT;

    // color scheme (same as DailyActivityReportExportUtils)
    private static final float[] COLOR_PRIMARY = {0.12f, 0.23f, 0.54f};   // dark blue
    private static final float[] COLOR_TEXT = {0.2f, 0.2f, 0.2f};          // dark grey
    private static final float[] COLOR_LIGHT_TEXT = {0.4f, 0.4f, 0.4f};    // medium grey
    private static final float[] COLOR_CARD_BG = {0.97f, 0.98f, 1.0f};     // light blue bg
    private static final float[] COLOR_WHITE = {1f, 1f, 1f};

    private final String title;
    private final String subtitle;
    private final List<DetailField> details;
    private final String[] tableColumns;
    private final List<String[]> tableRows;

    private FormSummaryPdfExporter(Builder builder) {
        this.title = builder.title;
        this.subtitle = builder.subtitle;
        this.details = builder.details;
        this.tableColumns = builder.tableColumns;
        this.tableRows = builder.tableRows;
    }

    /**
     * Creates a new builder instance.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Shows a dialog with Print / Save as PDF / Cancel options.
     * Generates the PDF and handles the chosen action.
     *
     * @param parent the parent component for dialog positioning
     */
    public void showExportDialog(Component parent) {
        Object[] options = {"Print", "Save as PDF", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
                parent,
                "How would you like to export the summary?",
                "Export Summary",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]
        );

        if (choice == 0) {
            // Print
            printDocument(parent);
        } else if (choice == 1) {
            // Save as PDF
            saveAsPdf(parent);
        }
        // Cancel or close = do nothing
    }

    /**
     * Directly exports the PDF to the specified destination file using BackgroundTask.
     *
     * @param file the destination PDF file
     * @param parent the parent component
     */
    public void exportToFile(File file, Component parent) {
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            file = new File(file.getAbsolutePath() + ".pdf");
        }
        final File targetFile = file;

        BackgroundTask task = new BackgroundTask(parent, "Generating PDF") {
            @Override
            protected Boolean performTask() throws Exception {
                updateProgress("Generating PDF document...");
                generatePdf(targetFile);
                return true;
            }

            @Override
            protected void onSuccess() {
                JOptionPane.showMessageDialog(parent,
                        "PDF saved successfully to:\n" + targetFile.getAbsolutePath(),
                        "Save Complete", JOptionPane.INFORMATION_MESSAGE);
            }

            @Override
            protected void onFailure(Exception e) {
                Logger.errlog("PDF export failed", e);
                JOptionPane.showMessageDialog(parent,
                        "Failed to save PDF: " + e.getMessage(),
                        "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        };
        task.executeWithDialog();
    }

    /**
     * Generates a PDF in a temp file and opens the system print dialog.
     */
    private void printDocument(Component parent) {
        try {
            final File tempFile = File.createTempFile("hivon_summary_", ".pdf");
            tempFile.deleteOnExit();

            BackgroundTask task = new BackgroundTask(parent, "Preparing Document for Print") {
                @Override
                protected Boolean performTask() throws Exception {
                    updateProgress("Generating PDF document...");
                    generatePdf(tempFile);
                    return true;
                }

                @Override
                protected void onSuccess() {
                    try (PDDocument doc = org.apache.pdfbox.Loader.loadPDF(tempFile)) {
                        PrinterJob printerJob = PrinterJob.getPrinterJob();
                        printerJob.setPageable(new org.apache.pdfbox.printing.PDFPageable(doc));
                        if (printerJob.printDialog()) {
                            printerJob.print();
                            JOptionPane.showMessageDialog(parent,
                                    "Document sent to printer successfully.",
                                    "Print Complete", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (PrinterException e) {
                        Logger.errlog("Printing failed", e);
                        JOptionPane.showMessageDialog(parent,
                                "Printing failed: " + e.getMessage(),
                                "Print Error", JOptionPane.ERROR_MESSAGE);
                    } catch (Exception e) {
                        Logger.errlog("PDF print failed", e);
                        JOptionPane.showMessageDialog(parent,
                                "Failed to print PDF: " + e.getMessage(),
                                "Print Error", JOptionPane.ERROR_MESSAGE);
                    }
                }

                @Override
                protected void onFailure(Exception e) {
                    Logger.errlog("PDF generation for print failed", e);
                    JOptionPane.showMessageDialog(parent,
                            "Failed to generate PDF for printing: " + e.getMessage(),
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            };
            task.executeWithDialog();
        } catch (Exception e) {
            Logger.errlog("Failed to create temporary file for printing", e);
            JOptionPane.showMessageDialog(parent,
                    "Failed to initialize print: " + e.getMessage(),
                    "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Shows a file chooser and saves the PDF to the selected location using BackgroundTask.
     */
    private void saveAsPdf(Component parent) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Summary as PDF");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files (*.pdf)", "pdf"));

        // build a default filename from the title
        String safeName = (title != null ? title : "Summary")
                .replaceAll("[^a-zA-Z0-9\\-_ ]", "")
                .replace(" ", "_");
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        fileChooser.setSelectedFile(new File(safeName + "_" + timestamp + ".pdf"));

        int result = fileChooser.showSaveDialog(parent);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selected = fileChooser.getSelectedFile();
            exportToFile(selected, parent);
        }
    }

    /**
     * Core PDF generation logic.
     */
    private void generatePdf(File file) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            Cursor cursor = new Cursor(doc, regular, bold);

            // ---- Title section ----
            cursor.text(title != null ? title : "Form Summary",
                    MARGIN_LEFT, cursor.y, 16, true, COLOR_PRIMARY);
            cursor.down(6);
            if (subtitle != null && !subtitle.isEmpty()) {
                cursor.text(subtitle, MARGIN_LEFT, cursor.y, 10, false, COLOR_LIGHT_TEXT);
                cursor.down(6);
            }
            // generated-at timestamp
            cursor.text("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    MARGIN_LEFT, cursor.y, 8, false, COLOR_LIGHT_TEXT);
            cursor.down(4);
            cursor.line(MARGIN_LEFT, cursor.y, PAGE_WIDTH - MARGIN_RIGHT, cursor.y);
            cursor.down(14);

            // ---- Details section (label-value pairs in a card) ----
            if (details != null && !details.isEmpty()) {
                // compute card height based on number of detail rows (2 columns)
                int detailRows = (int) Math.ceil(details.size() / 2.0);
                float cardHeight = 20 + detailRows * 18 + 10;

                cursor.box(MARGIN_LEFT, cursor.y - cardHeight, CONTENT_WIDTH, cardHeight, COLOR_CARD_BG);

                float detailY = cursor.y - 15;
                float col1LabelX = MARGIN_LEFT + 15;
                float col1ValueX = MARGIN_LEFT + 130;
                float col2LabelX = MARGIN_LEFT + CONTENT_WIDTH / 2 + 15;
                float col2ValueX = MARGIN_LEFT + CONTENT_WIDTH / 2 + 130;

                for (int i = 0; i < details.size(); i++) {
                    DetailField field = details.get(i);
                    boolean isLeftColumn = (i % 2 == 0);
                    float labelX = isLeftColumn ? col1LabelX : col2LabelX;
                    float valueX = isLeftColumn ? col1ValueX : col2ValueX;

                    cursor.text(field.label + ":", labelX, detailY, 9, true, COLOR_PRIMARY);
                    cursor.text(safe(field.value), valueX, detailY, 9, false, COLOR_TEXT);

                    if (!isLeftColumn || i == details.size() - 1) {
                        detailY -= 18;
                    }
                }

                cursor.moveTo(cursor.y - cardHeight - 10);
            }

            // ---- Summary Table section ----
            if (tableColumns != null && tableColumns.length > 0) {
                cursor.down(5);
                cursor.text("Summary", MARGIN_LEFT, cursor.y, 12, true, COLOR_PRIMARY);
                cursor.down(18);

                // compute column x-positions (evenly distributed)
                float[] colX = computeColumnPositions(tableColumns.length);

                cursor.tableHeader(tableColumns, colX);

                if (tableRows != null && !tableRows.isEmpty()) {
                    boolean shaded = false;
                    for (String[] row : tableRows) {
                        cursor.ensureRoom(tableColumns, colX, title != null ? title + " (Continued)" : "Summary (Continued)");
                        cursor.tableRow(shaded, colX, row);
                        shaded = !shaded;
                    }
                } else {
                    cursor.text("No items in summary.", MARGIN_LEFT + 10, cursor.y - 10, 8, false, COLOR_LIGHT_TEXT);
                    cursor.down(16);
                }
            }

            cursor.close();
            doc.save(file);
        }
    }

    /**
     * Computes evenly spaced column X positions across the content width.
     */
    private float[] computeColumnPositions(int colCount) {
        float[] positions = new float[colCount];
        float usable = CONTENT_WIDTH - 20; // 10px padding on each side
        float colWidth = usable / colCount;
        for (int i = 0; i < colCount; i++) {
            positions[i] = MARGIN_LEFT + 10 + i * colWidth;
        }
        return positions;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    // ---- Inner cursor class for PDF rendering (mirrors DailyActivityReportExportUtils.PdfCursor) ----

    private static class Cursor {
        private final PDDocument doc;
        private final PDFont regular, bold;
        private PDPage page;
        private PDPageContentStream cs;
        float y;

        Cursor(PDDocument doc, PDFont regular, PDFont bold) throws Exception {
            this.doc = doc;
            this.regular = regular;
            this.bold = bold;
            newPage();
        }

        private void newPage() throws Exception {
            page = new PDPage(new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT));
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = PAGE_HEIGHT - MARGIN_TOP;
        }

        void moveTo(float y) { this.y = y; }
        void down(float amount) { this.y -= amount; }

        void text(String value, float x, float y, float size, boolean isBold, float[] color) throws Exception {
            cs.beginText();
            cs.setFont(isBold ? bold : regular, size);
            cs.setNonStrokingColor(color[0], color[1], color[2]);
            cs.newLineAtOffset(x, y);
            cs.showText(value == null ? "" : value);
            cs.endText();
        }

        void line(float x1, float y1, float x2, float y2) throws Exception {
            cs.setStrokingColor(0.2f, 0.4f, 0.8f);
            cs.setLineWidth(1f);
            cs.moveTo(x1, y1);
            cs.lineTo(x2, y2);
            cs.stroke();
        }

        void box(float x, float y, float w, float h, float[] fill) throws Exception {
            cs.setNonStrokingColor(fill[0], fill[1], fill[2]);
            cs.addRect(x, y, w, h);
            cs.fill();
        }

        void tableHeader(String[] cols, float[] xPositions) throws Exception {
            box(MARGIN_LEFT, y - 14, CONTENT_WIDTH, 18, COLOR_PRIMARY);
            for (int i = 0; i < cols.length; i++) {
                text(cols[i], xPositions[i], y - 10, 9, true, COLOR_WHITE);
            }
            down(16);
        }

        void tableRow(boolean shaded, float[] xPositions, String... values) throws Exception {
            float g = shaded ? 0.96f : 1.0f;
            box(MARGIN_LEFT, y - 14, CONTENT_WIDTH, 16, new float[]{g, g, g});
            for (int i = 0; i < values.length && i < xPositions.length; i++) {
                text(safe(values[i]), xPositions[i], y - 10, 7.5f, false, COLOR_TEXT);
            }
            down(16);
        }

        void ensureRoom(String[] cols, float[] xPositions, String continuedTitle) throws Exception {
            if (y >= MARGIN_BOTTOM) return;
            cs.close();
            newPage();
            text(continuedTitle, MARGIN_LEFT, y, 12, true, COLOR_PRIMARY);
            down(18);
            tableHeader(cols, xPositions);
        }

        void close() throws Exception {
            cs.close();
        }
    }

    // ---- Detail field holder ----

    private static class DetailField {
        final String label;
        final String value;

        DetailField(String label, String value) {
            this.label = label;
            this.value = value;
        }
    }

    // ---- Builder ----

    /**
     * Builder for constructing a FormSummaryPdfExporter.
     * Forms provide their title, detail fields, and table data.
     */
    public static class Builder {
        private String title;
        private String subtitle;
        private final List<DetailField> details = new ArrayList<>();
        private String[] tableColumns;
        private final List<String[]> tableRows = new ArrayList<>();

        private Builder() {}

        /**
         * Sets the document title (displayed at the top of the PDF).
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Sets an optional subtitle (e.g., document number or description).
         */
        public Builder subtitle(String subtitle) {
            this.subtitle = subtitle;
            return this;
        }

        /**
         * Adds a label-value detail field.
         * These are rendered in the details card section of the PDF.
         */
        public Builder addDetail(String label, String value) {
            this.details.add(new DetailField(label, value));
            return this;
        }

        /**
         * Sets the column headers for the summary table.
         */
        public Builder tableColumns(String[] columns) {
            this.tableColumns = columns;
            return this;
        }

        /**
         * Adds a row to the summary table.
         */
        public Builder addTableRow(String[] row) {
            this.tableRows.add(row);
            return this;
        }

        /**
         * Builds the exporter and immediately shows the export dialog.
         *
         * @param parent the parent component for dialog positioning
         */
        public void showExportDialog(Component parent) {
            new FormSummaryPdfExporter(this).showExportDialog(parent);
        }

        /**
         * Builds the exporter and exports to the specified file using BackgroundTask.
         *
         * @param file the destination PDF file
         * @param parent the parent component
         */
        public void exportToFile(File file, Component parent) {
            new FormSummaryPdfExporter(this).exportToFile(file, parent);
        }

        /**
         * Builds the exporter instance without showing a dialog.
         */
        public FormSummaryPdfExporter build() {
            return new FormSummaryPdfExporter(this);
        }
    }
}
