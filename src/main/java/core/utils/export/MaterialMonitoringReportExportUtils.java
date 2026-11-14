/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.utils.export;

import models.dto.MaterialMonitoringReportDTO;
import models.dto.MaterialMonitoringReportDTO.MaterialMonitoringItem;

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
 * Export utility for Material Monitoring & Batch Traceability Report (REP67)
 * XLSX via Apache POI, PDF via Apache PDFBox
 *
 * @author Sanod
 */
public class MaterialMonitoringReportExportUtils {

    private static final DecimalFormat currencyFmt = new DecimalFormat("#,##0.00");
    private static final DecimalFormat qtyFmt = new DecimalFormat("#,##0.000");
    private static final DecimalFormat pctFmt = new DecimalFormat("0.0'%'");

    /**
     * Export report data to XLSX Excel file
     */
    public static void exportToXLSX(MaterialMonitoringReportDTO dto, File file) throws Exception {
        if (dto == null) throw new IllegalArgumentException("Report data is null.");
        file = ensureExtension(file, ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Material Monitoring Report");
            for (int i = 0; i < 9; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }

            // Styles
            CellStyle title   = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 14, true, null);
            CellStyle section = style(wb, IndexedColors.DARK_BLUE.getIndex(), IndexedColors.GREY_25_PERCENT.getIndex(), 12, true, null);
            CellStyle header  = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 10, true, null);
            CellStyle bold    = style(wb, IndexedColors.BLACK.getIndex(), null, 10, true, null);
            CellStyle normal  = style(wb, IndexedColors.BLACK.getIndex(), null, 10, false, null);

            CellStyle boldCurrency   = style(wb, IndexedColors.BLACK.getIndex(), null, 10, true, "#,##0.00");
            CellStyle normalQty      = style(wb, IndexedColors.BLACK.getIndex(), null, 10, false, "#,##0.000");
            CellStyle normalCurrency = style(wb, IndexedColors.BLACK.getIndex(), null, 10, false, "#,##0.00");
            CellStyle normalPct      = style(wb, IndexedColors.BLACK.getIndex(), null, 10, false, "0.0%");

            int r = 0;

            // Title block
            row(sheet, r++, c(title, "HIVON WMS - MATERIAL MONITORING & BATCH TRACEABILITY REPORT (REP67)"));
            row(sheet, r++, c(bold, "Generated: " + safe(dto.getGeneratedAt())));
            r++;

            // Summary KPI section
            row(sheet, r++, c(section, "MONITORING SUMMARY"));
            row(sheet, r++, c(bold, "Active Inventory SKUs:"), c(bold, dto.getActiveInventorySkus()));
            row(sheet, r++, c(bold, "Total Quantity in Bins:"), c(normalQty, dto.getTotalQuantity()));
            row(sheet, r++, c(bold, "Total Inventory Value (" + dto.getCurrency() + "):"), c(boldCurrency, dto.getTotalInventoryValue()));
            row(sheet, r++, c(bold, "Expiring / Expired Items:"), c(bold, dto.getExpiringCount()));
            row(sheet, r++, c(bold, "Avg Bin Capacity Usage:"), c(bold, String.format("%.1f%%", dto.getAvgBinCapacityUsage())));
            r++;

            // Table headers
            row(sheet, r++, c(section, "MATERIAL BATCH & BIN DETAILS"));
            row(sheet, r++,
                c(header, "Material Code"),
                c(header, "Batch Number"),
                c(header, "Storage Bin"),
                c(header, "Qty in Bin"),
                c(header, "% Bin"),
                c(header, "Man. Date"),
                c(header, "Expire Date"),
                c(header, "Batch Cost"),
                c(header, "Line /Batch Value")
            );

            // Table rows
            if (dto.getItems() != null) {
                for (MaterialMonitoringItem item : dto.getItems()) {
                    row(sheet, r++,
                        c(normal, safe(item.getMaterialCode())),
                        c(normal, safe(item.getBatchNumber())),
                        c(normal, safe(item.getStorageBin())),
                        c(normalQty, item.getQuantity()),
                        c(normalPct, item.getBinCapacityPct() / 100.0),
                        c(normal, safe(item.getManufactureDate())),
                        c(normal, safe(item.getExpiryDate())),
                        c(normalCurrency, item.getBatchCost()),
                        c(normalCurrency, item.getLineValue())
                    );
                }
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    /**
     * Export report data to PDF document
     */
    public static void exportToPDF(MaterialMonitoringReportDTO dto, File file) throws Exception {
        if (dto == null) throw new IllegalArgumentException("Report data is null.");
        file = ensureExtension(file, ".pdf");

        // Use Landscape A4 for wide table presentation
        PDRectangle pageSize = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
        float pageW = pageSize.getWidth();
        float pageH = pageSize.getHeight();
        float margin = 36f;
        float[] colW = { 80f, 130f, 80f, 70f, 50f, 75f, 75f, 70f, 90f };

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(pageSize);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            PDFont fTitle = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont fBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont fNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            float y = pageH - margin;

            // Title Header Banner
            cs.setNonStrokingColor(0.12f, 0.23f, 0.36f); // Dark Slate Blue
            cs.addRect(margin, y - 24, pageW - 2 * margin, 28);
            cs.fill();

            cs.setNonStrokingColor(1, 1, 1);
            text(cs, fTitle, 13f, margin + 10, y - 16, "HIVON WMS - MATERIAL MONITORING REPORT (REP67)");

            y -= 38;
            cs.setNonStrokingColor(0.3f, 0.3f, 0.3f);
            text(cs, fNormal, 8.5f, margin, y, "Generated: " + safe(dto.getGeneratedAt()));

            y -= 22;

            // Summary Card Box
            cs.setNonStrokingColor(0.95f, 0.96f, 0.98f);
            cs.addRect(margin, y - 20, pageW - 2 * margin, 24);
            cs.fill();
            cs.setStrokingColor(0.85f, 0.88f, 0.92f);
            cs.addRect(margin, y - 20, pageW - 2 * margin, 24);
            cs.stroke();

            cs.setNonStrokingColor(0.15f, 0.2f, 0.25f);
            float sumY = y - 14;
            text(cs, fBold, 8.0f, margin + 8, sumY, "SKUs: " + dto.getActiveInventorySkus());
            text(cs, fBold, 8.0f, margin + 110, sumY, "Total Qty: " + qtyFmt.format(dto.getTotalQuantity()));
            text(cs, fBold, 8.0f, margin + 260, sumY, "Total Value: " + dto.getCurrency() + currencyFmt.format(dto.getTotalInventoryValue()));
            text(cs, fBold, 8.0f, margin + 450, sumY, "Expiring/Expired: " + dto.getExpiringCount());
            text(cs, fBold, 8.0f, margin + 590, sumY, "Avg Bin Capacity: " + String.format("%.1f%%", dto.getAvgBinCapacityUsage()));

            y -= 34;

            // Table headers
            String[] headers = {
                "Material Code", "Batch Number", "Storage Bin", "Qty in Bin", "% Bin", "Man. Date", "Expire Date", "Batch Cost", "Line Value"
            };
            y = drawTableRow(cs, fBold, 8.5f, margin, y, colW, headers, true, false, false);

            // Table rows
            List<MaterialMonitoringItem> items = dto.getItems();
            if (items != null) {
                for (MaterialMonitoringItem item : items) {
                    if (y < margin + 25) {
                        cs.close();
                        page = new PDPage(pageSize);
                        doc.addPage(page);
                        cs = new PDPageContentStream(doc, page);
                        y = pageH - margin;
                        y = drawTableRow(cs, fBold, 8.5f, margin, y, colW, headers, true, false, false);
                    }

                    String[] rowData = {
                        safe(item.getMaterialCode()),
                        safe(item.getBatchNumber()),
                        safe(item.getStorageBin()),
                        qtyFmt.format(item.getQuantity()),
                        String.format("%.1f%%", item.getBinCapacityPct()),
                        safe(item.getManufactureDate()),
                        safe(item.getExpiryDate()),
                        dto.getCurrency() + currencyFmt.format(item.getBatchCost()),
                        dto.getCurrency() + currencyFmt.format(item.getLineValue())
                    };

                    y = drawTableRow(cs, fNormal, 8.0f, margin, y, colW, rowData, false, item.isExpired(), item.isExpiringSoon());
                }
            }

            cs.close();
            doc.save(file);
        }
    }

    private static float drawTableRow(PDPageContentStream cs, PDFont font, float fontSize,
                                      float startX, float y, float[] colW, String[] data,
                                      boolean isHeader, boolean isExpired, boolean isExpiringSoon) throws Exception {
        float rowHeight = 16f;

        if (isHeader) {
            cs.setNonStrokingColor(0.12f, 0.23f, 0.36f);
            float totalW = 0;
            for (float w : colW) totalW += w;
            cs.addRect(startX, y - 4, totalW, rowHeight);
            cs.fill();
            cs.setNonStrokingColor(1, 1, 1);
        } else {
            float totalW = 0;
            for (float w : colW) totalW += w;

            // Highlight expiring or expired rows slightly
            if (isExpired) {
                cs.setNonStrokingColor(0.99f, 0.92f, 0.92f); // soft red
                cs.addRect(startX, y - 4, totalW, rowHeight);
                cs.fill();
            } else if (isExpiringSoon) {
                cs.setNonStrokingColor(0.99f, 0.97f, 0.88f); // soft amber
                cs.addRect(startX, y - 4, totalW, rowHeight);
                cs.fill();
            }

            cs.setStrokingColor(0.92f, 0.92f, 0.92f);
            cs.moveTo(startX, y - 4);
            cs.lineTo(startX + totalW, y - 4);
            cs.stroke();

            if (isExpired) {
                cs.setNonStrokingColor(0.7f, 0.1f, 0.1f);
            } else if (isExpiringSoon) {
                cs.setNonStrokingColor(0.6f, 0.35f, 0.05f);
            } else {
                cs.setNonStrokingColor(0.1f, 0.1f, 0.1f);
            }
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
        if (fmt != null) {
            DataFormat df = wb.createDataFormat();
            cs.setDataFormat(df.getFormat(fmt));
        }
        return cs;
    }

    private static void row(Sheet sheet, int r, Object... cells) {
        Row row = sheet.createRow(r);
        for (int i = 0; i < cells.length; i++) {
            Cell c = row.createCell(i);
            Object obj = cells[i];
            if (obj instanceof Holder) {
                Holder h = (Holder) obj;
                if (h.cs != null) c.setCellStyle(h.cs);
                if (h.val instanceof Number) {
                    c.setCellValue(((Number) h.val).doubleValue());
                } else if (h.val instanceof Boolean) {
                    c.setCellValue((Boolean) h.val);
                } else {
                    c.setCellValue(String.valueOf(h.val));
                }
            } else if (obj != null) {
                c.setCellValue(String.valueOf(obj));
            }
        }
    }

    private static Holder c(CellStyle cs, Object val) {
        return new Holder(cs, val);
    }

    private static class Holder {
        CellStyle cs;
        Object val;
        Holder(CellStyle cs, Object val) { this.cs = cs; this.val = val; }
    }

    private static File ensureExtension(File f, String ext) {
        if (!f.getName().toLowerCase().endsWith(ext.toLowerCase())) {
            return new File(f.getParentFile(), f.getName() + ext);
        }
        return f;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
