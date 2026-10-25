/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.utils.export;

import models.dto.PerformanceReportDTO;
import models.dto.PerformanceReportDTO.TaskBreakdownItem;
import models.dto.PerformanceReportDTO.WorkerPerformanceItem;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;

/**
 * Export utility for the worker performance report (REP64)
 * XLSX via Apache POI, PDF via Apache PDFBox
 *
 * @author Sanod
 */
public class PerformanceReportExportUtils {

    private static final DecimalFormat qtyFmt = new DecimalFormat("#,##0.00");
    private static final DecimalFormat pctFmt = new DecimalFormat("#,##0.0");

    // XLSX export
    public static void exportToXLSX(PerformanceReportDTO dto, File file) throws Exception {
        if (dto == null) throw new IllegalArgumentException("Report data is null.");
        file = ensureExtension(file, ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Performance Report");
            for (int i = 0; i < 7; i++) sheet.setColumnWidth(i, 22 * 256);

            // text styles
            CellStyle title   = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 14, true, null);
            CellStyle section = style(wb, IndexedColors.DARK_BLUE.getIndex(), IndexedColors.GREY_25_PERCENT.getIndex(), 12, true, null);
            CellStyle header  = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 11, true, null);
            CellStyle bold    = style(wb, IndexedColors.BLACK.getIndex(), null, 11, true, null);
            CellStyle normal  = style(wb, IndexedColors.BLACK.getIndex(), null, 11, false, null);

            // numeric styles
            CellStyle boldNum   = style(wb, IndexedColors.BLACK.getIndex(), null, 11, true, "#,##0");
            CellStyle normalNum = style(wb, IndexedColors.BLACK.getIndex(), null, 11, false, "#,##0");
            CellStyle normalDec = style(wb, IndexedColors.BLACK.getIndex(), null, 11, false, "#,##0.0");
            CellStyle normalPct = style(wb, IndexedColors.BLACK.getIndex(), null, 11, false, "0.0%");

            int r = 0;

            // title block
            row(sheet, r++, c(title, "HIVON WMS - WORKER PERFORMANCE REPORT"));
            row(sheet, r++, c(bold, "From: " + safe(dto.getFromDate())), c(bold, "To: " + safe(dto.getToDate())));
            r++;

            // summary section
            row(sheet, r++, c(section, "KEY PERFORMANCE INDICATORS"));
            row(sheet, r++, c(bold, "Total Tasks Completed:"), c(boldNum, dto.getTotalTasksCompleted()));
            row(sheet, r++, c(bold, "Average Tasks / Hour:"), c(normalDec, dto.getAverageTasksPerHour()));
            row(sheet, r++, c(bold, "Overall Accuracy Rate:"), c(normalPct, dto.getOverallAccuracyRate() / 100.0));
            row(sheet, r++, c(bold, "Average Time / Task:"), c(normal, safe(dto.getAverageTimePerTask())));
            row(sheet, r++, c(bold, "Error Rate:"), c(normalPct, dto.getErrorRate() / 100.0));
            r++;

            // worker performance table
            if (dto.getWorkers() != null && !dto.getWorkers().isEmpty()) {
                row(sheet, r++, c(section, "WORKER PERFORMANCE"));
                row(sheet, r++, c(header, "Worker Name"), c(header, "Tasks Completed"), c(header, "Tasks/Hour"),
                        c(header, "Accuracy %"), c(header, "Avg Time/Task"), c(header, "Errors"), c(header, "Rating"));
                for (WorkerPerformanceItem w : dto.getWorkers()) {
                    row(sheet, r++, c(normal, safe(w.getWorkerName())), c(normalNum, w.getTasksCompleted()),
                            c(normalDec, w.getTasksPerHour()), c(normalPct, w.getAccuracyPct() / 100.0),
                            c(normal, safe(w.getAvgTimePerTask())), c(normalNum, w.getErrors()),
                            c(normal, safe(w.getRating())));
                }
                r++;
            }

            // task breakdown table
            if (dto.getTaskBreakdowns() != null && !dto.getTaskBreakdowns().isEmpty()) {
                row(sheet, r++, c(section, "TASK TYPE BREAKDOWN"));
                row(sheet, r++, c(header, "Task Type"), c(header, "Count"), c(header, "Avg Time"),
                        c(header, "Accuracy %"), c(header, "Total Qty Handled"));
                for (TaskBreakdownItem t : dto.getTaskBreakdowns()) {
                    row(sheet, r++, c(normal, safe(t.getTaskType())), c(normalNum, t.getCount()),
                            c(normal, safe(t.getAvgTime())), c(normalPct, t.getAccuracyPct() / 100.0),
                            c(normalDec, t.getTotalQtyHandled()));
                }
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    private static class CellDef {
        final CellStyle style;
        final Object value;
        CellDef(CellStyle s, Object v) { this.style = s; this.value = v; }
    }

    private static CellDef c(CellStyle s, Object v) { return new CellDef(s, v); }

    private static void row(Sheet sheet, int rowIdx, CellDef... cells) {
        Row row = sheet.createRow(rowIdx);
        for (int i = 0; i < cells.length; i++) {
            Cell cell = row.createCell(i);
            Object v = cells[i].value;
            if (v instanceof Number) {
                cell.setCellValue(((Number) v).doubleValue());
            } else {
                cell.setCellValue(v == null ? "" : v.toString());
            }
            cell.setCellStyle(cells[i].style);
        }
    }

    private static CellStyle style(Workbook wb, short fontColor, Short bg, int size, boolean bold, String fmt) {
        Font font = wb.createFont();
        font.setColor(fontColor);
        font.setFontHeightInPoints((short) size);
        font.setBold(bold);

        CellStyle cs = wb.createCellStyle();
        cs.setFont(font);
        if (bg != null) {
            cs.setFillForegroundColor(bg);
            cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        if (fmt != null) {
            cs.setDataFormat(wb.createDataFormat().getFormat(fmt));
        }
        return cs;
    }

    // PDF export
    public static void exportToPDF(PerformanceReportDTO dto, File file) throws Exception {
        if (dto == null) throw new IllegalArgumentException("Report data is null.");
        file = ensureExtension(file, ".pdf");

        try (PDDocument doc = new PDDocument()) {
            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PdfCursor cur = new PdfCursor(doc, regular, bold);

            // Title
            cur.text("HIVON WMS - WORKER PERFORMANCE REPORT", 50, 545, 16, true, 0.12f, 0.23f, 0.54f);
            cur.text("Date Range: " + safe(dto.getFromDate()) + " to " + safe(dto.getToDate()) + "   |   Generated: " + safe(dto.getGeneratedAt()), 50, 527, 10, false, 0.4f, 0.4f, 0.4f);
            cur.line(50, 517, 792, 517);

            // Summary cards
            cur.card(50, 420, 220, 80, "Productivity Summary",
                    "Total Tasks: " + dto.getTotalTasksCompleted(),
                    "Avg Tasks/Hour: " + pctFmt.format(dto.getAverageTasksPerHour()),
                    "Avg Time/Task: " + safe(dto.getAverageTimePerTask()));

            cur.card(290, 420, 240, 80, "Quality & Accuracy",
                    "Overall Accuracy: " + pctFmt.format(dto.getOverallAccuracyRate()) + "%",
                    "Error Rate: " + pctFmt.format(dto.getErrorRate()) + "%",
                    "Status: " + (dto.getErrorRate() <= 5.0 ? "Optimal" : "Review Recommended"));

            String topWorkerName = (dto.getWorkers() != null && !dto.getWorkers().isEmpty())
                    ? dto.getWorkers().get(0).getWorkerName() : "N/A";
            int topWorkerTasks = (dto.getWorkers() != null && !dto.getWorkers().isEmpty())
                    ? dto.getWorkers().get(0).getTasksCompleted() : 0;
            cur.card(550, 420, 240, 80, "Top Contributor",
                    "Top Worker: " + topWorkerName,
                    "Tasks Completed: " + topWorkerTasks,
                    "Total Workers Tracked: " + (dto.getWorkers() != null ? dto.getWorkers().size() : 0));

            // Worker Performance Table
            cur.moveTo(390);
            cur.sectionTitle("Worker Performance");
            cur.down(18);

            String[] workerCols = {"Worker Name", "Tasks Completed", "Tasks/Hour", "Accuracy %", "Avg Time/Task", "Errors", "Rating"};
            float[] workerX = {55, 230, 340, 430, 520, 630, 710};
            cur.tableHeader(workerCols, workerX);

            if (dto.getWorkers() != null && !dto.getWorkers().isEmpty()) {
                boolean shaded = false;
                for (WorkerPerformanceItem w : dto.getWorkers()) {
                    cur.ensureRoom(workerCols, workerX, "Worker Performance (Continued)");
                    cur.tableRow(shaded, workerX,
                            trunc(w.getWorkerName(), 30),
                            String.valueOf(w.getTasksCompleted()),
                            pctFmt.format(w.getTasksPerHour()),
                            pctFmt.format(w.getAccuracyPct()) + "%",
                            safe(w.getAvgTimePerTask()),
                            String.valueOf(w.getErrors()),
                            safe(w.getRating()));
                    shaded = !shaded;
                }
            } else {
                cur.text("No worker performance records found.", 60, cur.y - 10, 8, false, 0.5f, 0.5f, 0.5f);
                cur.down(16);
            }

            // Task Type Breakdown Table
            cur.down(16);
            cur.ensureRoom(new String[]{"Task Type", "Count", "Avg Time", "Accuracy %", "Total Qty Handled"},
                    new float[]{55, 230, 360, 490, 630}, "Task Type Breakdown");
            cur.sectionTitle("Task Type Breakdown");
            cur.down(18);

            String[] taskCols = {"Task Type", "Count", "Avg Time", "Accuracy %", "Total Qty Handled"};
            float[] taskX = {55, 230, 360, 490, 630};
            cur.tableHeader(taskCols, taskX);

            if (dto.getTaskBreakdowns() != null && !dto.getTaskBreakdowns().isEmpty()) {
                boolean shaded = false;
                for (TaskBreakdownItem t : dto.getTaskBreakdowns()) {
                    cur.ensureRoom(taskCols, taskX, "Task Type Breakdown (Continued)");
                    cur.tableRow(shaded, taskX,
                            safe(t.getTaskType()),
                            String.valueOf(t.getCount()),
                            safe(t.getAvgTime()),
                            pctFmt.format(t.getAccuracyPct()) + "%",
                            qtyFmt.format(t.getTotalQtyHandled()));
                    shaded = !shaded;
                }
            } else {
                cur.text("No task breakdown records found.", 60, cur.y - 10, 8, false, 0.5f, 0.5f, 0.5f);
            }

            cur.close();
            doc.save(file);
        }
    }

    private static String trunc(String str, int maxLen) {
        if (str == null) return "";
        return str.length() <= maxLen ? str : str.substring(0, maxLen - 1) + "..";
    }

    private static class PdfCursor {
        private final PDDocument doc;
        private final PDFont regular, bold;
        private PDPage page;
        private PDPageContentStream cs;
        float y;

        PdfCursor(PDDocument doc, PDFont regular, PDFont bold) throws Exception {
            this.doc = doc;
            this.regular = regular;
            this.bold = bold;
            newPage();
        }

        private void newPage() throws Exception {
            // landscape A4
            page = new PDPage(new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()));
            doc.addPage(page);
            cs = new PDPageContentStream(doc, page);
            y = 545;
        }

        void moveTo(float y) { this.y = y; }
        void down(float amount) { this.y -= amount; }

        void text(String value, float x, float y, float size, boolean isBold, float r, float g, float b) throws Exception {
            cs.beginText();
            cs.setFont(isBold ? bold : regular, size);
            cs.setNonStrokingColor(r, g, b);
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

        void box(float x, float y, float w, float h, float fillR, float fillG, float fillB) throws Exception {
            cs.setNonStrokingColor(fillR, fillG, fillB);
            cs.addRect(x, y, w, h);
            cs.fill();
        }

        void card(float x, float y, float w, float h, String title, String... lines) throws Exception {
            box(x, y, w, h, 0.97f, 0.98f, 1.0f);
            text(title, x + 10, y + h - 15, 11, true, 0.12f, 0.23f, 0.54f);
            float ty = y + h - 35;
            for (String line : lines) {
                text(line, x + 10, ty, 9, false, 0.2f, 0.2f, 0.2f);
                ty -= 15;
            }
        }

        void sectionTitle(String title) throws Exception {
            text(title, 50, y, 12, true, 0.12f, 0.23f, 0.54f);
        }

        private static final float TABLE_WIDTH = 742;

        void tableHeader(String[] cols, float[] xPositions) throws Exception {
            box(50, y - 14, TABLE_WIDTH, 18, 0.12f, 0.23f, 0.54f);
            for (int i = 0; i < cols.length; i++) {
                text(cols[i], xPositions[i], y - 10, 9, true, 1f, 1f, 1f);
            }
            down(16);
        }

        void tableRow(boolean shaded, float[] xPositions, String... values) throws Exception {
            float g = shaded ? 0.96f : 1.0f;
            box(50, y - 14, TABLE_WIDTH, 16, g, g, g);
            for (int i = 0; i < values.length; i++) {
                text(values[i], xPositions[i], y - 10, 7.5f, false, 0.2f, 0.2f, 0.2f);
            }
            down(16);
        }

        void ensureRoom(String[] cols, float[] xPositions, String continuedTitle) throws Exception {
            if (y >= 80) return;
            cs.close();
            newPage();
            sectionTitle(continuedTitle);
            down(18);
            tableHeader(cols, xPositions);
        }

        void close() throws Exception {
            cs.close();
        }
    }

    private static File ensureExtension(File file, String ext) {
        String path = file.getAbsolutePath();
        return path.toLowerCase().endsWith(ext) ? file : new File(path + ext);
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
