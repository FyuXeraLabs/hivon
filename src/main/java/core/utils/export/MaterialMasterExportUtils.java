/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.utils.export;

import models.dto.MaterialDTO;

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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Export utility for the Material Master (MD31)
 * Supports XLSX via Apache POI and PDF via Apache PDFBox
 *
 * @author Sanod
 */
public class MaterialMasterExportUtils {

    private static final DecimalFormat currencyFmt = new DecimalFormat("#,##0.00");
    private static final DecimalFormat qtyFmt = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Export material master list to an XLSX Excel spreadsheet.
     *
     * @param materials list of MaterialDTO
     * @param file target destination file
     * @throws Exception if export fails
     */
    public static void exportToXLSX(List<MaterialDTO> materials, File file) throws Exception {
        if (materials == null) {
            throw new IllegalArgumentException("Material data is null.");
        }
        file = ensureExtension(file, ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Material Master");
            for (int i = 0; i < 14; i++) {
                sheet.setColumnWidth(i, 18 * 256);
            }
            sheet.setColumnWidth(0, 16 * 256); // Material Code
            sheet.setColumnWidth(1, 30 * 256); // Description
            sheet.setColumnWidth(2, 12 * 256); // Base UOM
            sheet.setColumnWidth(3, 18 * 256); // Category / Group
            sheet.setColumnWidth(4, 18 * 256); // Storage Type
            sheet.setColumnWidth(5, 18 * 256); // Storage Condition

            // Text styles
            CellStyle title   = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 14, true, null);
            CellStyle section = style(wb, IndexedColors.DARK_BLUE.getIndex(), IndexedColors.GREY_25_PERCENT.getIndex(), 11, true, null);
            CellStyle header  = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 10, true, null);
            CellStyle bold    = style(wb, IndexedColors.BLACK.getIndex(), null, 10, true, null);
            CellStyle normal  = style(wb, IndexedColors.BLACK.getIndex(), null, 10, false, null);

            // Numeric styles
            CellStyle boldCurrency   = style(wb, IndexedColors.BLACK.getIndex(), null, 10, true, "#,##0.00");
            CellStyle normalQty      = style(wb, IndexedColors.BLACK.getIndex(), null, 10, false, "#,##0.00");
            CellStyle normalCurrency = style(wb, IndexedColors.BLACK.getIndex(), null, 10, false, "#,##0.00");

            int r = 0;

            // Title block
            row(sheet, r++, c(title, "HIVON WMS - MATERIAL MASTER (MD31)"));
            row(sheet, r++, c(bold, "Generated: " + LocalDateTime.now().format(dtf)));
            r++;

            // Summary section
            long batchManagedCount = materials.stream().filter(MaterialDTO::isBatchManaged).count();
            row(sheet, r++, c(section, "MASTER DATA SUMMARY"));
            row(sheet, r++, c(bold, "Total Materials:"), c(bold, materials.size()),
                            c(bold, "Batch-Managed SKUs:"), c(bold, batchManagedCount),
                            c(bold, "Standard SKUs:"), c(bold, (materials.size() - batchManagedCount)));
            r++;

            // Table headers
            row(sheet, r++, c(section, "MATERIAL MASTER RECORDS"));
            row(sheet, r++,
                c(header, "Material Code"),
                c(header, "Description"),
                c(header, "Base UOM"),
                c(header, "Material Group"),
                c(header, "Storage Type"),
                c(header, "Storage Condition"),
                c(header, "Min Stock"),
                c(header, "Max Stock"),
                c(header, "Reorder Point"),
                c(header, "Unit Cost"),
                c(header, "Batch Managed"),
                c(header, "Unit Weight"),
                c(header, "Volume"),
                c(header, "Last Modified")
            );

            // Table rows
            for (MaterialDTO m : materials) {
                row(sheet, r++,
                    c(normal, safe(m.getMaterialCode())),
                    c(normal, safe(m.getMaterialDescription())),
                    c(normal, safe(m.getBaseUom())),
                    c(normal, safe(m.getMaterialGroup())),
                    c(normal, safe(m.getStorageType())),
                    c(normal, safe(m.getStorageCondition())),
                    c(normalQty, m.getMinStockLevel()),
                    c(normalQty, m.getMaxStockLevel()),
                    c(normalQty, m.getReorderPoint()),
                    c(normalCurrency, m.getUnitCost()),
                    c(normal, m.isBatchManaged() ? "Yes" : "No"),
                    c(normalQty, m.getWeight()),
                    c(normalQty, m.getVolume()),
                    c(normal, m.getModifiedDate() != null ? m.getModifiedDate().format(dtf) : "")
                );
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                wb.write(fos);
            }
        }
    }

    /**
     * Export material master list to a PDF document.
     *
     * @param materials list of MaterialDTO
     * @param file target destination file
     * @throws Exception if export fails
     */
    public static void exportToPDF(List<MaterialDTO> materials, File file) throws Exception {
        if (materials == null) {
            throw new IllegalArgumentException("Material data is null.");
        }
        file = ensureExtension(file, ".pdf");

        // Landscape A4 for wide table presentation
        PDRectangle pageSize = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
        float pageW = pageSize.getWidth();
        float pageH = pageSize.getHeight();
        float margin = 36f;

        // Total content width = 841.89 - 72 = 769.89
        float[] colW = { 75f, 150f, 45f, 75f, 70f, 70f, 45f, 45f, 45f, 60f, 45f, 44f };

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
            text(cs, fTitle, 13f, margin + 10, y - 16, "HIVON WMS - MATERIAL MASTER (MD31)");

            y -= 38;
            cs.setNonStrokingColor(0.3f, 0.3f, 0.3f);
            text(cs, fNormal, 8.5f, margin, y, "Generated: " + LocalDateTime.now().format(dtf));

            y -= 22;

            // Summary Card Box
            long batchManagedCount = materials.stream().filter(MaterialDTO::isBatchManaged).count();
            cs.setNonStrokingColor(0.95f, 0.96f, 0.98f);
            cs.addRect(margin, y - 20, pageW - 2 * margin, 24);
            cs.fill();
            cs.setStrokingColor(0.85f, 0.88f, 0.92f);
            cs.addRect(margin, y - 20, pageW - 2 * margin, 24);
            cs.stroke();

            cs.setNonStrokingColor(0.15f, 0.2f, 0.25f);
            float sumY = y - 14;
            text(cs, fBold, 8.5f, margin + 12, sumY, "Total Materials: " + materials.size());
            text(cs, fBold, 8.5f, margin + 200, sumY, "Batch Managed SKUs: " + batchManagedCount);
            text(cs, fBold, 8.5f, margin + 400, sumY, "Standard SKUs: " + (materials.size() - batchManagedCount));

            y -= 34;

            // Table headers
            String[] headers = {
                "Material Code", "Description", "UOM", "Group", "Storage Type", "Storage Cond",
                "Min Stk", "Max Stk", "Reorder", "Unit Cost", "Batch", "Weight"
            };
            y = drawTableRow(cs, fBold, 8.0f, margin, y, colW, headers, true);

            // Table rows
            for (MaterialDTO m : materials) {
                if (y < margin + 25) {
                    cs.close();
                    page = new PDPage(pageSize);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = pageH - margin;
                    y = drawTableRow(cs, fBold, 8.0f, margin, y, colW, headers, true);
                }

                String[] rowData = {
                    safe(m.getMaterialCode()),
                    safe(m.getMaterialDescription()),
                    safe(m.getBaseUom()),
                    safe(m.getMaterialGroup()),
                    safe(m.getStorageType()),
                    safe(m.getStorageCondition()),
                    m.getMinStockLevel() != null ? qtyFmt.format(m.getMinStockLevel()) : "-",
                    m.getMaxStockLevel() != null ? qtyFmt.format(m.getMaxStockLevel()) : "-",
                    m.getReorderPoint() != null ? qtyFmt.format(m.getReorderPoint()) : "-",
                    m.getUnitCost() != null ? currencyFmt.format(m.getUnitCost()) : "-",
                    m.isBatchManaged() ? "Yes" : "No",
                    m.getWeight() != null ? qtyFmt.format(m.getWeight()) : "-"
                };

                y = drawTableRow(cs, fNormal, 7.5f, margin, y, colW, rowData, false);
            }

            cs.close();
            doc.save(file);
        }
    }

    private static float drawTableRow(PDPageContentStream cs, PDFont font, float fontSize,
                                      float startX, float y, float[] colW, String[] data,
                                      boolean isHeader) throws Exception {
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

            cs.setStrokingColor(0.92f, 0.92f, 0.92f);
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
            if (v instanceof Number) {
                c.setCellValue(((Number) v).doubleValue());
            } else {
                c.setCellValue(v != null ? v.toString() : "");
            }
        }
    }

    private static CellVal c(CellStyle s, Object v) {
        return new CellVal(s, v);
    }

    private static String safe(String s) {
        return s != null ? s : "";
    }

    private static File ensureExtension(File f, String ext) {
        return f.getName().toLowerCase().endsWith(ext) ? f : new File(f.getAbsolutePath() + ext);
    }

    private static class CellVal {
        CellStyle style;
        Object val;
        CellVal(CellStyle s, Object v) {
            this.style = s;
            this.val = v;
        }
    }
}
