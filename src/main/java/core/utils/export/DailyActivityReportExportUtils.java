/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.utils.export;

import models.dto.DailyActivityReportDTO;
import models.dto.DailyActivityReportDTO.ActivityLogItem;
import models.dto.DailyActivityReportDTO.WorkerProductivityItem;

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
 * export utility for the daily activity report
 * xlsx via apache poi, pdf via apache pdfbox
 *
 * @author Sanod
 */
public class DailyActivityReportExportUtils {

    private static final DecimalFormat currencyFmt = new DecimalFormat("#,##0.00");
    private static final DecimalFormat qtyFmt = new DecimalFormat("#,##0");

    // xlsx export
    public static void exportToXLSX(DailyActivityReportDTO dto, File file) throws Exception {
        if (dto == null) throw new IllegalArgumentException("Report data is null.");
        file = ensureExtension(file, ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Daily Activity Report");
            for (int i = 0; i < 8; i++) sheet.setColumnWidth(i, 20 * 256);

            // text styles
            CellStyle title   = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 14, true, null);
            CellStyle section = style(wb, IndexedColors.DARK_BLUE.getIndex(), IndexedColors.GREY_25_PERCENT.getIndex(), 12, true, null);
            CellStyle header  = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 11, true, null);
            CellStyle bold    = style(wb, IndexedColors.BLACK.getIndex(), null, 11, true, null);
            CellStyle normal  = style(wb, IndexedColors.BLACK.getIndex(), null, 11, false, null);

            // numeric styles
            CellStyle boldQty      = style(wb, IndexedColors.BLACK.getIndex(), null, 11, true, "#,##0");
            CellStyle boldCurrency = style(wb, IndexedColors.BLACK.getIndex(), null, 11, true, "#,##0.00");
            CellStyle normalQty    = style(wb, IndexedColors.BLACK.getIndex(), null, 11, false, "#,##0");
            CellStyle normalHours  = style(wb, IndexedColors.BLACK.getIndex(), null, 11, false, "#,##0.0");

            int r = 0;

            // title block
            row(sheet, r++, c(title, "HIVON WMS - DAILY ACTIVITY REPORT"));
            row(sheet, r++, c(bold, "Report Date: " + safe(dto.getReportDate())));
            r++;

            // receipts summary
            row(sheet, r++, c(section, "RECEIPTS SUMMARY"));
            row(sheet, r++, c(bold, "Total GR Count"), c(boldQty, dto.getTotalGRCount()), c(bold, "PO Receipts"), c(boldQty, dto.getReceiptPO()));
            row(sheet, r++, c(bold, "Total Receipt Qty"), c(boldQty, dto.getTotalReceiptQty()), c(bold, "Customer Returns"), c(boldQty, dto.getReceiptCustomerReturns()));
            row(sheet, r++, c(bold, "Total Receipt Value"), c(boldCurrency, dto.getTotalReceiptValue()), c(bold, "Transfer In"), c(boldQty, dto.getReceiptTransferIn()));
            r++;

            // issues summary
            row(sheet, r++, c(section, "ISSUES SUMMARY"));
            row(sheet, r++, c(bold, "Total GI Count"), c(boldQty, dto.getTotalGICount()), c(bold, "Sales Orders"), c(boldQty, dto.getSalesOrder()));
            row(sheet, r++, c(bold, "Total Issue Qty"), c(boldQty, dto.getTotalIssueQty()), c(bold, "Internal Consumption"), c(boldQty, dto.getInternalConsumption()));
            row(sheet, r++, c(bold, "Total Issue Value"), c(boldCurrency, dto.getTotalIssueValue()), c(bold, "Transfer Out"), c(boldQty, dto.getTransferOut()));
            r++;

            // transfers & adjustments
            row(sheet, r++, c(section, "TRANSFERS & ADJUSTMENTS"));
            row(sheet, r++, c(bold, "Bin-to-Bin Transfers"), c(boldQty, dto.getBintoBinTransfers()), c(bold, "Cycle Count Adjustments"), c(boldQty, dto.getCycleCountAdjustments()));
            row(sheet, r++, c(bold, "Total Transfer Qty"), c(boldQty, dto.getTotalTransferQty()), c(bold, "Inventory Adjustments"), c(boldQty, dto.getInventoryAdjustments()));
            row(sheet, r++, c(bold, "Total Transfer Value"), c(boldCurrency, dto.getTotalTransferValue()), c(bold, "Net Adjustment Value"), c(boldCurrency, dto.getNetAdjustmentValue()));
            r++;

            // exceptions
            row(sheet, r++, c(section, "EXCEPTION SUMMARY"));
            row(sheet, r++, c(bold, "Short Picks"), c(boldQty, dto.getShortPicks()), c(bold, "Damaged Items"), c(boldQty, dto.getDamagedItems()), c(bold, "Variances"), c(boldQty, dto.getVariances()));
            r++;

            // worker productivity
            row(sheet, r++, c(section, "WORKER PRODUCTIVITY"));
            row(sheet, r++, c(bold, "Total Active Workers"), c(boldQty, dto.getTotalActiveWorkers()), c(bold, "Avg Tasks/Worker"), c(boldQty, dto.getAverageTasksPerWorker()));
            row(sheet, r++, c(header, "Worker Name"), c(header, "Tasks Completed"), c(header, "Quantities Handled"), c(header, "Hours Active"));
            if (dto.getWorkerList() != null) {
                for (WorkerProductivityItem w : dto.getWorkerList()) {
                    row(sheet, r++, c(normal, safe(w.getWorkerName())), c(normalQty, w.getTasksCompleted()),
                            c(normalQty, w.getQuantitiesHandled()), c(normalHours, w.getHoursActive()));
                }
            }
            r++;

            // activity log
            row(sheet, r++, c(section, "ACTIVITY LOG"));
            row(sheet, r++, c(header, "Time"), c(header, "User"), c(header, "Activity Type"), c(header, "Material"),
                    c(header, "Quantity"), c(header, "From"), c(header, "To"), c(header, "Status"));
            if (dto.getActivityLogList() != null) {
                for (ActivityLogItem log : dto.getActivityLogList()) {
                    row(sheet, r++, c(normal, safe(log.getTime())), c(normal, safe(log.getUser())), c(normal, safe(log.getActivityType())),
                            c(normal, safe(log.getMaterial())), c(normalQty, log.getQuantity()), c(normal, safe(log.getFrom())), c(normal, safe(log.getTo())), c(normal, safe(log.getStatus())));
                }
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    private static C c(CellStyle style, Object value) { return new C(style, value); }

    private static class C {
        final CellStyle style;
        final Object value;
        C(CellStyle style, Object value) { this.style = style; this.value = value; }
    }

    private static void row(Sheet sheet, int rowIdx, C... cells) {
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

    // pdf export

    public static void exportToPDF(DailyActivityReportDTO dto, File file) throws Exception {
        if (dto == null) throw new IllegalArgumentException("Report data is null.");
        file = ensureExtension(file, ".pdf");

        try (PDDocument doc = new PDDocument()) {
            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PdfCursor cur = new PdfCursor(doc, regular, bold);

            // title
            cur.text("HIVON WMS - DAILY ACTIVITY REPORT", 50, 545, 16, true, 0.12f, 0.23f, 0.54f);
            cur.text("Report Date: " + safe(dto.getReportDate()), 50, 527, 10, false, 0.4f, 0.4f, 0.4f);
            cur.line(50, 517, 792, 517);

            // summary cards
            cur.card(50, 400, 178, 100, "Receipts Summary",
                    "Total GR Count: " + dto.getTotalGRCount(),
                    "Total Receipt Qty: " + qtyFmt.format(dto.getTotalReceiptQty()),
                    "Total Receipt Value: $" + currencyFmt.format(dto.getTotalReceiptValue()),
                    "PO: " + dto.getReceiptPO() + " | Returns: " + dto.getReceiptCustomerReturns() + " | Transfer In: " + dto.getReceiptTransferIn());

            cur.card(238, 400, 178, 100, "Issues Summary",
                    "Total GI Count: " + dto.getTotalGICount(),
                    "Total Issue Qty: " + qtyFmt.format(dto.getTotalIssueQty()),
                    "Total Issue Value: $" + currencyFmt.format(dto.getTotalIssueValue()),
                    "Sales Order: " + dto.getSalesOrder() + " | Internal: " + dto.getInternalConsumption() + " | Transfer Out: " + dto.getTransferOut());

            cur.card(426, 400, 178, 100, "Transfers & Adjustments",
                    "Bin-to-Bin Transfers: " + dto.getBintoBinTransfers(),
                    "Total Transfer Qty: " + qtyFmt.format(dto.getTotalTransferQty()),
                    "Total Transfer Value: $" + currencyFmt.format(dto.getTotalTransferValue()),
                    "Net Adjustment Value: $" + currencyFmt.format(dto.getNetAdjustmentValue()));

            cur.card(614, 400, 178, 100, "Exception Summary",
                    "Short Picks: " + dto.getShortPicks(),
                    "Damaged Items: " + dto.getDamagedItems(),
                    "Variances: " + dto.getVariances());

            // worker productivity tbl
            cur.moveTo(370);
            cur.sectionTitle("Worker Productivity");
            cur.down(14);
            cur.text("Active Workers: " + dto.getTotalActiveWorkers() + "   Avg Tasks/Worker: " + dto.getAverageTasksPerWorker(),
                    50, cur.y, 9, false, 0.4f, 0.4f, 0.4f);
            cur.down(18);

            String[] workerCols = {"Worker Name", "Tasks Completed", "Quantities Handled", "Hours Active"};
            float[] workerX = {60, 320, 480, 640};
            cur.tableHeader(workerCols, workerX);

            if (dto.getWorkerList() != null && !dto.getWorkerList().isEmpty()) {
                boolean shaded = false;
                for (WorkerProductivityItem w : dto.getWorkerList()) {
                    cur.tableRow(shaded, workerX,
                            w.getWorkerName(), String.valueOf(w.getTasksCompleted()),
                            qtyFmt.format(w.getQuantitiesHandled()), String.valueOf(w.getHoursActive()));
                    shaded = !shaded;
                }
            } else {
                cur.text("No worker activity recorded for date.", 60, cur.y - 10, 8, false, 0.5f, 0.5f, 0.5f);
                cur.down(16);
            }

            // activity log tbl
            cur.down(15);
            cur.sectionTitle("Activity Log");
            cur.down(18);

            String[] logCols = {"Time", "User", "Activity Type", "Material", "Quantity", "From", "To", "Status"};
            float[] logX = {55, 110, 185, 320, 450, 515, 600, 695};
            cur.tableHeader(logCols, logX);

            if (dto.getActivityLogList() != null && !dto.getActivityLogList().isEmpty()) {
                boolean shaded = false;
                for (ActivityLogItem log : dto.getActivityLogList()) {
                    cur.ensureRoom(logCols, logX, "Activity Log (Continued)");
                    cur.tableRow(shaded, logX,
                            log.getTime(), log.getUser(), trunc(log.getActivityType(), 25),
                            trunc(log.getMaterial(), 20), qtyFmt.format(log.getQuantity()),
                            trunc(log.getFrom(), 15), trunc(log.getTo(), 15), log.getStatus());
                    shaded = !shaded;
                }
            } else {
                cur.text("No activity log entries found for date.", 60, cur.y - 10, 8, false, 0.5f, 0.5f, 0.5f);
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
            // landscape
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

        // dark header bar with column labels
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
            if (y >= 50) return;
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

    // shared helpers

    private static File ensureExtension(File file, String ext) {
        String path = file.getAbsolutePath();
        return path.toLowerCase().endsWith(ext) ? file : new File(path + ext);
    }

    private static String safe(String s) { return s == null ? "" : s; }
}