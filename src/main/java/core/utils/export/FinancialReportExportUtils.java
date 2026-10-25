/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.utils.export;

import models.dto.FinancialReportDTO;

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
import java.util.List;

/**
 * Export utility for the Financial Report (REP66)
 * XLSX via Apache POI, PDF via Apache PDFBox
 *
 * @author Sanod
 */
public class FinancialReportExportUtils {

    private static final DecimalFormat currencyFmt = new DecimalFormat("#,##0.00");

    // XLSX export
    public static void exportToXLSX(FinancialReportDTO dto, File file) throws Exception {
        if (dto == null) throw new IllegalArgumentException("Report data is null.");
        file = ensureExtension(file, ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Financial Report");
            int colCount = Math.max(7, dto.getColumns() != null ? dto.getColumns().size() : 7);
            for (int i = 0; i < colCount; i++) sheet.setColumnWidth(i, 22 * 256);

            // text styles
            CellStyle title   = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 14, true, null);
            CellStyle section = style(wb, IndexedColors.DARK_BLUE.getIndex(), IndexedColors.GREY_25_PERCENT.getIndex(), 12, true, null);
            CellStyle header  = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 11, true, null);
            CellStyle bold    = style(wb, IndexedColors.BLACK.getIndex(), null, 11, true, null);
            CellStyle normal  = style(wb, IndexedColors.BLACK.getIndex(), null, 11, false, null);

            // numeric styles
            CellStyle boldCurrency   = style(wb, IndexedColors.BLACK.getIndex(), null, 11, true, "#,##0.00");
            CellStyle normalCurrency = style(wb, IndexedColors.BLACK.getIndex(), null, 11, false, "#,##0.00");

            int r = 0;

            // title block
            row(sheet, r++, c(title, "HIVON WMS - FINANCIAL REPORT (" + safe(dto.getReportType()).toUpperCase() + ")"));
            row(sheet, r++, c(bold, "From: " + safe(dto.getFromDate())), c(bold, "To: " + safe(dto.getToDate())),
                    c(bold, "Method: " + safe(dto.getValuationMethod())));
            r++;

            // summary section
            row(sheet, r++, c(section, "FINANCIAL SUMMARY"));
            row(sheet, r++, c(bold, "Beginning Balance:"), c(boldCurrency, dto.getBeginningBalance()));
            row(sheet, r++, c(bold, "Additions (Inbound):"), c(boldCurrency, dto.getAdditions()));
            row(sheet, r++, c(bold, "Reductions (Outbound/Scrap):"), c(boldCurrency, dto.getReductions()));
            row(sheet, r++, c(bold, "Adjustments (Net):"), c(boldCurrency, dto.getAdjustments()));
            row(sheet, r++, c(bold, "Ending Balance:"), c(boldCurrency, dto.getEndingBalance()));
            row(sheet, r++, c(bold, "Impact on P&L:"), c(boldCurrency, dto.getImpactPL()));
            r++;

            // report table
            if (dto.getColumns() != null && !dto.getColumns().isEmpty()) {
                row(sheet, r++, c(section, safe(dto.getReportTitle()).toUpperCase()));

                Row headerRow = sheet.createRow(r++);
                for (int i = 0; i < dto.getColumns().size(); i++) {
                    Cell cell = headerRow.createCell(i);
                    cell.setCellValue(dto.getColumns().get(i));
                    cell.setCellStyle(header);
                }

                if (dto.getRowData() != null) {
                    for (Object[] rowValues : dto.getRowData()) {
                        Row dataRow = sheet.createRow(r++);
                        for (int i = 0; i < rowValues.length; i++) {
                            Cell cell = dataRow.createCell(i);
                            Object v = rowValues[i];
                            if (v instanceof Number) {
                                cell.setCellValue(((Number) v).doubleValue());
                                cell.setCellStyle(normalCurrency);
                            } else {
                                cell.setCellValue(v == null ? "" : v.toString());
                                cell.setCellStyle(normal);
                            }
                        }
                    }
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
    public static void exportToPDF(FinancialReportDTO dto, File file) throws Exception {
        if (dto == null) throw new IllegalArgumentException("Report data is null.");
        file = ensureExtension(file, ".pdf");

        try (PDDocument doc = new PDDocument()) {
            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PdfCursor cur = new PdfCursor(doc, regular, bold);

            String curSym = dto.getCurrency() != null ? dto.getCurrency() : "$";

            // Title
            cur.text("HIVON WMS - FINANCIAL REPORT", 50, 545, 16, true, 0.12f, 0.23f, 0.54f);
            cur.text("Report: " + safe(dto.getReportTitle()) + "   |   Range: " + safe(dto.getFromDate()) + " to " + safe(dto.getToDate()) + "   |   Method: " + safe(dto.getValuationMethod()), 50, 527, 10, false, 0.4f, 0.4f, 0.4f);
            cur.line(50, 517, 792, 517);

            // Summary cards
            cur.card(50, 420, 220, 80, "Balance Overview",
                    "Beginning: " + curSym + " " + currencyFmt.format(dto.getBeginningBalance()),
                    "Ending: " + curSym + " " + currencyFmt.format(dto.getEndingBalance()),
                    "Impact on P&L: " + curSym + " " + currencyFmt.format(dto.getImpactPL()));

            cur.card(290, 420, 240, 80, "Periodic Flow",
                    "Additions (Inbound): " + curSym + " " + currencyFmt.format(dto.getAdditions()),
                    "Reductions (Outbound): " + curSym + " " + currencyFmt.format(dto.getReductions()),
                    "Adjustments (Net): " + curSym + " " + currencyFmt.format(dto.getAdjustments()));

            cur.card(550, 420, 240, 80, "Parameters & Scope",
                    "Report Type: " + safe(dto.getReportType()),
                    "Valuation Method: " + safe(dto.getValuationMethod()),
                    "Total Records: " + (dto.getRowData() != null ? dto.getRowData().size() : 0));

            // Details Table
            cur.moveTo(390);
            cur.sectionTitle(safe(dto.getReportTitle()));
            cur.down(18);

            List<String> colList = dto.getColumns();
            if (colList != null && !colList.isEmpty()) {
                String[] colArr = colList.toArray(new String[0]);
                float[] xPos = computeColumnPositions(colArr.length);
                cur.tableHeader(colArr, xPos);

                if (dto.getRowData() != null && !dto.getRowData().isEmpty()) {
                    boolean shaded = false;
                    for (Object[] rowVals : dto.getRowData()) {
                        cur.ensureRoom(colArr, xPos, safe(dto.getReportTitle()) + " (Continued)");
                        String[] strVals = new String[rowVals.length];
                        for (int i = 0; i < rowVals.length; i++) {
                            strVals[i] = rowVals[i] == null ? "" : trunc(rowVals[i].toString(), 28);
                        }
                        cur.tableRow(shaded, xPos, strVals);
                        shaded = !shaded;
                    }
                } else {
                    cur.text("No records found for this period.", 60, cur.y - 10, 8, false, 0.5f, 0.5f, 0.5f);
                }
            }

            cur.close();
            doc.save(file);
        }
    }

    private static float[] computeColumnPositions(int count) {
        float startX = 55;
        float totalWidth = 730;
        float step = totalWidth / Math.max(1, count);
        float[] x = new float[count];
        for (int i = 0; i < count; i++) {
            x[i] = startX + (i * step);
        }
        return x;
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
                text(cols[i], xPositions[i], y - 10, 8.5f, true, 1f, 1f, 1f);
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
