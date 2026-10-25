/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.utils.export;

import models.dto.StockAgingReportDTO;
import models.dto.StockAgingReportDTO.StockAgingItem;

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
 * Export utility for the Stock Aging Report (REP63)
 * XLSX via Apache POI, PDF via Apache PDFBox
 *
 * @author Sanod
 */
public class StockAgingReportExportUtils {

    private static final DecimalFormat currencyFmt = new DecimalFormat("#,##0.00");
    private static final DecimalFormat qtyFmt = new DecimalFormat("#,##0.000");

    // XLSX export
    public static void exportToXLSX(StockAgingReportDTO dto, File file) throws Exception {
        if (dto == null) throw new IllegalArgumentException("Report data is null.");
        file = ensureExtension(file, ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Stock Aging Report");
            for (int i = 0; i < 9; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }

            // text styles
            CellStyle title   = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 14, true, null);
            CellStyle section = style(wb, IndexedColors.DARK_BLUE.getIndex(), IndexedColors.GREY_25_PERCENT.getIndex(), 12, true, null);
            CellStyle header  = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 10, true, null);
            CellStyle bold    = style(wb, IndexedColors.BLACK.getIndex(), null, 10, true, null);
            CellStyle normal  = style(wb, IndexedColors.BLACK.getIndex(), null, 10, false, null);

            // numeric styles
            CellStyle boldCurrency   = style(wb, IndexedColors.BLACK.getIndex(), null, 10, true, "#,##0.00");
            CellStyle normalQty      = style(wb, IndexedColors.BLACK.getIndex(), null, 10, false, "#,##0.000");
            CellStyle normalCurrency = style(wb, IndexedColors.BLACK.getIndex(), null, 10, false, "#,##0.00");
            CellStyle normalRate     = style(wb, IndexedColors.BLACK.getIndex(), null, 10, false, "0.00");

            int r = 0;

            // title block
            row(sheet, r++, c(title, "HIVON WMS - STOCK AGING REPORT (REP63)"));
            row(sheet, r++, c(bold, "Generated: " + safe(dto.getGeneratedAt())));
            r++;

            // summary section
            row(sheet, r++, c(section, "ANALYSIS SUMMARY"));
            row(sheet, r++, c(bold, "Total Materials Analyzed:"), c(bold, dto.getTotalMaterialsAnalyzed()));
            row(sheet, r++, c(bold, "Average Age of Inventory:"), c(bold, dto.getAverageAgeOfInventory() + " days"));
            row(sheet, r++, c(bold, "Slow-Moving Materials:"), c(bold, dto.getSlowMovingMaterialsCount()));
            row(sheet, r++, c(bold, "Potentially Obsolete:"), c(bold, dto.getPotentiallyObsoleteCount()));
            row(sheet, r++, c(bold, "High-Value Obsolete (" + dto.getCurrency() + "):"), c(boldCurrency, dto.getHighValueObsolete()));
            r++;

            // results table
            row(sheet, r++, c(section, "STOCK AGING RESULTS"));
            row(sheet, r++,
                c(header, "Material Code"),
                c(header, "Material Name"),
                c(header, "Current Qty"),
                c(header, "Days in Stock"),
                c(header, "Last Movement Date"),
                c(header, "Avg Monthly Usage"),
                c(header, "Days of Supply"),
                c(header, "Turnover Rate"),
                c(header, "Recommendation")
            );

            if (dto.getItems() != null) {
                for (StockAgingItem item : dto.getItems()) {
                    row(sheet, r++,
                        c(normal, safe(item.getMaterialCode())),
                        c(normal, safe(item.getMaterialName())),
                        c(normalQty, item.getCurrentQty()),
                        c(normal, item.getDaysInStock()),
                        c(normal, safe(item.getLastMovementDate())),
                        c(normalRate, item.getAvgMonthlyUsage()),
                        c(normalRate, item.getDaysOfSupply()),
                        c(normalRate, item.getTurnoverRate()),
                        c(normal, safe(item.getRecommendation()))
                    );
                }
            }

            try (FileOutputStream out = new FileOutputStream(file)) {
                wb.write(out);
            }
        }
    }

    // PDF export
    public static void exportToPDF(StockAgingReportDTO dto, File file) throws Exception {
        if (dto == null) throw new IllegalArgumentException("Report data is null.");
        file = ensureExtension(file, ".pdf");

        // Landscape A4
        PDRectangle pageSize = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());

        PDFont fBold   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDFont fNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        float margin = 36;
        float pageW  = pageSize.getWidth();
        float pageH  = pageSize.getHeight();

        float[] colW = { 70, 150, 65, 65, 80, 75, 75, 65, 120 };

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            float y = pageH - margin;

            // Title
            text(cs, fBold, 15, margin, y, "HIVON WMS - STOCK AGING REPORT (REP63)");
            y -= 16;
            text(cs, fNormal, 9, margin, y, "Generated: " + safe(dto.getGeneratedAt()));
            y -= 18;

            // Summary box
            cs.setNonStrokingColor(0.95f, 0.95f, 0.98f);
            cs.addRect(margin, y - 26, pageW - 2 * margin, 32);
            cs.fill();
            cs.setNonStrokingColor(0, 0, 0);

            float sumY = y - 4;
            text(cs, fBold, 8.5f, margin + 8, sumY, "Materials Analyzed: " + dto.getTotalMaterialsAnalyzed());
            text(cs, fBold, 8.5f, margin + 140, sumY, "Avg Inventory Age: " + dto.getAverageAgeOfInventory() + " days");
            text(cs, fBold, 8.5f, margin + 300, sumY, "Slow Moving: " + dto.getSlowMovingMaterialsCount());
            text(cs, fBold, 8.5f, margin + 420, sumY, "Obsolete Count: " + dto.getPotentiallyObsoleteCount());
            text(cs, fBold, 8.5f, margin + 550, sumY, "High-Value Obsolete: " + dto.getCurrency() + currencyFmt.format(dto.getHighValueObsolete()));

            y -= 40;

            // Table headers
            String[] headers = { "Material Code", "Material Name", "Current Qty", "Days In Stock", "Last Movement", "Avg Mth Usage", "Days Supply", "Turnover", "Recommendation" };
            y = drawTableRow(cs, fBold, 8.5f, margin, y, colW, headers, true);

            // Table rows
            List<StockAgingItem> items = dto.getItems();
            if (items != null) {
                for (StockAgingItem item : items) {
                    if (y < margin + 25) {
                        cs.close();
                        page = new PDPage(pageSize);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        y = pageH - margin;
                        y = drawTableRow(cs, fBold, 8.5f, margin, y, colW, headers, true);
                    }

                    String[] rowData = {
                        safe(item.getMaterialCode()),
                        safe(item.getMaterialName()),
                        qtyFmt.format(item.getCurrentQty()),
                        String.valueOf(item.getDaysInStock()),
                        safe(item.getLastMovementDate()),
                        currencyFmt.format(item.getAvgMonthlyUsage()),
                        currencyFmt.format(item.getDaysOfSupply()),
                        currencyFmt.format(item.getTurnoverRate()),
                        safe(item.getRecommendation())
                    };
                    y = drawTableRow(cs, fNormal, 8.0f, margin, y, colW, rowData, false);
                }
            }

            cs.close();
            doc.save(file);
        }
    }

    private static float drawTableRow(PDPageContentStream cs, PDFont font, float fontSize,
                                      float startX, float y, float[] colW, String[] data, boolean isHeader) throws Exception {
        float rowHeight = 16f;

        if (isHeader) {
            cs.setNonStrokingColor(0.12f, 0.23f, 0.36f);
            float totalW = 0;
            for (float w : colW) totalW += w;
            cs.addRect(startX, y - 4, totalW, rowHeight);
            cs.fill();
            cs.setNonStrokingColor(1, 1, 1);
        } else {
            cs.setNonStrokingColor(0.92f, 0.92f, 0.92f);
            float totalW = 0;
            for (float w : colW) totalW += w;
            cs.moveTo(startX, y - 4);
            cs.lineTo(startX + totalW, y - 4);
            cs.stroke();
            cs.setNonStrokingColor(0.1f, 0.1f, 0.1f);
        }

        float curX = startX;
        for (int i = 0; i < colW.length && i < data.length; i++) {
            String text = data[i];
            if (text == null) text = "";
            int maxChars = (int) (colW[i] / (fontSize * 0.52f));
            if (text.length() > maxChars && maxChars > 3) {
                text = text.substring(0, maxChars - 3) + "...";
            }
            text(cs, font, fontSize, curX + 3, y, text);
            curX += colW[i];
        }

        cs.setNonStrokingColor(0, 0, 0);
        return y - rowHeight;
    }

    private static void text(PDPageContentStream cs, PDFont font, float size, float x, float y, String s) throws Exception {
        if (s == null || s.isEmpty()) return;
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(s);
        cs.endText();
    }

    private static CellStyle style(Workbook wb, Short fg, Short bg, int pts, boolean b, String fmt) {
        CellStyle cs = wb.createCellStyle();
        Font f = wb.createFont();
        f.setFontName("Segoe UI");
        f.setFontHeightInPoints((short) pts);
        f.setBold(b);
        if (fg != null) f.setColor(fg);
        cs.setFont(f);
        if (bg != null) {
            cs.setFillForegroundColor(bg);
            cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        if (fmt != null) cs.setDataFormat(wb.createDataFormat().getFormat(fmt));
        return cs;
    }

    private static void row(Sheet s, int idx, CellVal... cells) {
        Row r = s.createRow(idx);
        for (int i = 0; i < cells.length; i++) {
            Cell c = r.createCell(i);
            c.setCellStyle(cells[i].style);
            Object v = cells[i].val;
            if (v instanceof Number) c.setCellValue(((Number) v).doubleValue());
            else c.setCellValue(v != null ? v.toString() : "");
        }
    }

    private static CellVal c(CellStyle s, Object v) { return new CellVal(s, v); }
    private static String safe(String s) { return s != null ? s : ""; }

    private static File ensureExtension(File f, String ext) {
        return f.getName().toLowerCase().endsWith(ext) ? f : new File(f.getAbsolutePath() + ext);
    }

    private static class CellVal {
        CellStyle style;
        Object val;
        CellVal(CellStyle s, Object v) { this.style = s; this.val = v; }
    }
}
