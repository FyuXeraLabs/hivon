/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.utils.export;

import models.dto.InventoryValuationReportDTO;
import models.dto.InventoryValuationReportDTO.CategoryBreakdownItem;
import models.dto.InventoryValuationReportDTO.ValuationItem;
import models.dto.InventoryValuationReportDTO.WarehouseBreakdownItem;

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
 * Export utility for the inventory valuation report
 * XLSX via Apache POI, PDF via Apache PDFBox
 *
 * @author Sanod
 */
public class InventoryValuationReportExportUtils {

    private static final DecimalFormat currencyFmt = new DecimalFormat("#,##0.00");
    private static final DecimalFormat qtyFmt = new DecimalFormat("#,##0.000");

    // XLSX export
    public static void exportToXLSX(InventoryValuationReportDTO dto, File file) throws Exception {
        if (dto == null) throw new IllegalArgumentException("Report data is null.");
        file = ensureExtension(file, ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Inventory Valuation Report");
            for (int i = 0; i < 7; i++) sheet.setColumnWidth(i, 22 * 256);

            // text styles
            CellStyle title   = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 14, true, null);
            CellStyle section = style(wb, IndexedColors.DARK_BLUE.getIndex(), IndexedColors.GREY_25_PERCENT.getIndex(), 12, true, null);
            CellStyle header  = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 11, true, null);
            CellStyle bold    = style(wb, IndexedColors.BLACK.getIndex(), null, 11, true, null);
            CellStyle normal  = style(wb, IndexedColors.BLACK.getIndex(), null, 11, false, null);

            // numeric styles
            CellStyle boldQty      = style(wb, IndexedColors.BLACK.getIndex(), null, 11, true, "#,##0.000");
            CellStyle boldCurrency = style(wb, IndexedColors.BLACK.getIndex(), null, 11, true, "#,##0.00");
            CellStyle normalQty    = style(wb, IndexedColors.BLACK.getIndex(), null, 11, false, "#,##0.000");
            CellStyle normalCurrency = style(wb, IndexedColors.BLACK.getIndex(), null, 11, false, "#,##0.00");
            CellStyle normalPct    = style(wb, IndexedColors.BLACK.getIndex(), null, 11, false, "0.00%");

            int r = 0;

            // title block
            row(sheet, r++, c(title, "HIVON WMS - INVENTORY VALUATION REPORT"));
            row(sheet, r++, c(bold, "Valuation Date: " + safe(dto.getValuationDate())), c(bold, "Valuation Method: " + safe(dto.getValuationMethod())));
            r++;

            // summary section
            row(sheet, r++, c(section, "VALUATION SUMMARY"));
            row(sheet, r++, c(bold, "Total Materials in Inventory:"), c(bold, dto.getTotalMaterialsCount()));
            row(sheet, r++, c(bold, "Total Quantity:"), c(boldQty, dto.getTotalQuantity()));
            row(sheet, r++, c(bold, "Total Inventory Value:"), c(boldCurrency, dto.getTotalValue()));
            r++;

            // category breakdown
            if (dto.getByCategoryList() != null && !dto.getByCategoryList().isEmpty()) {
                row(sheet, r++, c(section, "BY CATEGORY BREAKDOWN"));
                row(sheet, r++, c(header, "Category"), c(header, "Quantity"), c(header, "Value (" + dto.getCurrency() + ")"), c(header, "% of Total"));
                for (CategoryBreakdownItem cat : dto.getByCategoryList()) {
                    row(sheet, r++, c(normal, safe(cat.getCategoryName())), c(normalQty, cat.getQuantity()),
                            c(normalCurrency, cat.getValue()), c(normalPct, cat.getPercentage() / 100.0));
                }
                r++;
            }

            // warehouse breakdown
            if (dto.getByWarehouseList() != null && !dto.getByWarehouseList().isEmpty()) {
                row(sheet, r++, c(section, "BY WAREHOUSE BREAKDOWN"));
                row(sheet, r++, c(header, "Warehouse Code"), c(header, "Warehouse Name"), c(header, "Quantity"), c(header, "Value (" + dto.getCurrency() + ")"), c(header, "% of Total"));
                for (WarehouseBreakdownItem wh : dto.getByWarehouseList()) {
                    row(sheet, r++, c(normal, safe(wh.getWarehouseCode())), c(normal, safe(wh.getWarehouseName())),
                            c(normalQty, wh.getQuantity()), c(normalCurrency, wh.getValue()), c(normalPct, wh.getPercentage() / 100.0));
                }
                r++;
            }

            // valuation results table
            row(sheet, r++, c(section, "VALUATION RESULTS"));
            row(sheet, r++, c(header, "Material Code"), c(header, "Material Name"), c(header, "Material Group"),
                    c(header, "Quantity"), c(header, "Unit Cost"), c(header, "Total Value"));

            if (dto.getItems() != null) {
                for (ValuationItem item : dto.getItems()) {
                    row(sheet, r++, c(normal, safe(item.getMaterialCode())), c(normal, safe(item.getMaterialName())),
                            c(normal, safe(item.getMaterialGroup())), c(normalQty, item.getQuantity()),
                            c(normalCurrency, item.getUnitCost()), c(normalCurrency, item.getTotalValue()));
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

    // PDF export
    public static void exportToPDF(InventoryValuationReportDTO dto, File file) throws Exception {
        if (dto == null) throw new IllegalArgumentException("Report data is null.");
        file = ensureExtension(file, ".pdf");

        try (PDDocument doc = new PDDocument()) {
            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PdfCursor cur = new PdfCursor(doc, regular, bold);

            // Title
            cur.text("HIVON WMS - INVENTORY VALUATION REPORT", 50, 545, 16, true, 0.12f, 0.23f, 0.54f);
            cur.text("Valuation Date: " + safe(dto.getValuationDate()) + "   |   Method: " + safe(dto.getValuationMethod()), 50, 527, 10, false, 0.4f, 0.4f, 0.4f);
            cur.line(50, 517, 792, 517);

            // Summary cards
            cur.card(50, 420, 220, 80, "Inventory Summary",
                    "Total Materials: " + dto.getTotalMaterialsCount(),
                    "Total Quantity: " + qtyFmt.format(dto.getTotalQuantity()),
                    "Total Value: " + dto.getCurrency() + " " + currencyFmt.format(dto.getTotalValue()));

            cur.card(290, 420, 240, 80, "Categories Breakdown",
                    "Total Categories: " + (dto.getByCategoryList() != null ? dto.getByCategoryList().size() : 0),
                    "Top Category: " + (dto.getByCategoryList() != null && !dto.getByCategoryList().isEmpty() ? dto.getByCategoryList().get(0).getCategoryName() : "N/A"),
                    "Top Category Share: " + (dto.getByCategoryList() != null && !dto.getByCategoryList().isEmpty() ? dto.getByCategoryList().get(0).getPercentage() + "%" : "0%"));

            cur.card(550, 420, 240, 80, "Warehouses Breakdown",
                    "Total Warehouses: " + (dto.getByWarehouseList() != null ? dto.getByWarehouseList().size() : 0),
                    "Top Warehouse: " + (dto.getByWarehouseList() != null && !dto.getByWarehouseList().isEmpty() ? dto.getByWarehouseList().get(0).getWarehouseName() : "N/A"),
                    "Top Warehouse Share: " + (dto.getByWarehouseList() != null && !dto.getByWarehouseList().isEmpty() ? dto.getByWarehouseList().get(0).getPercentage() + "%" : "0%"));

            // Valuation Results Table
            cur.moveTo(390);
            cur.sectionTitle("Valuation Results");
            cur.down(18);

            String[] tableCols = {"Material Code", "Material Name", "Group", "Quantity", "UOM", "Unit Cost", "Total Value"};
            float[] tableX = {55, 150, 360, 480, 560, 620, 710};
            cur.tableHeader(tableCols, tableX);

            if (dto.getItems() != null && !dto.getItems().isEmpty()) {
                boolean shaded = false;
                for (ValuationItem item : dto.getItems()) {
                    cur.ensureRoom(tableCols, tableX, "Valuation Results (Continued)");
                    cur.tableRow(shaded, tableX,
                            trunc(item.getMaterialCode(), 15),
                            trunc(item.getMaterialName(), 30),
                            trunc(item.getMaterialGroup(), 18),
                            qtyFmt.format(item.getQuantity()),
                            safe(item.getUom()),
                            dto.getCurrency() + " " + currencyFmt.format(item.getUnitCost()),
                            dto.getCurrency() + " " + currencyFmt.format(item.getTotalValue()));
                    shaded = !shaded;
                }
            } else {
                cur.text("No inventory valuation items found.", 60, cur.y - 10, 8, false, 0.5f, 0.5f, 0.5f);
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

    private static File ensureExtension(File file, String ext) {
        String path = file.getAbsolutePath();
        return path.toLowerCase().endsWith(ext) ? file : new File(path + ext);
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
