/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package core.utils.export;

import models.dto.UtilizationReportDTO;
import models.dto.UtilizationReportDTO.BinUtilizationItem;
import models.dto.UtilizationReportDTO.TopSpaceConsumerItem;
import models.dto.UtilizationReportDTO.ZoneUtilizationItem;

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
 * Export utility for the Warehouse Utilization report (REP65)
 * XLSX via Apache POI, PDF via Apache PDFBox
 *
 * @author Sanod
 */
public class UtilizationReportExportUtils {

    private static final DecimalFormat currencyFmt = new DecimalFormat("#,##0.00");
    private static final DecimalFormat qtyFmt = new DecimalFormat("#,##0.000");
    private static final DecimalFormat pctFmt = new DecimalFormat("#,##0.0");

    // XLSX export
    public static void exportToXLSX(UtilizationReportDTO dto, File file) throws Exception {
        if (dto == null) throw new IllegalArgumentException("Report data is null.");
        file = ensureExtension(file, ".xlsx");

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Utilization Report");
            for (int i = 0; i < 7; i++) sheet.setColumnWidth(i, 22 * 256);

            // text styles
            CellStyle title   = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 14, true, null);
            CellStyle section = style(wb, IndexedColors.DARK_BLUE.getIndex(), IndexedColors.GREY_25_PERCENT.getIndex(), 12, true, null);
            CellStyle header  = style(wb, IndexedColors.WHITE.getIndex(), IndexedColors.DARK_BLUE.getIndex(), 11, true, null);
            CellStyle bold    = style(wb, IndexedColors.BLACK.getIndex(), null, 11, true, null);
            CellStyle normal  = style(wb, IndexedColors.BLACK.getIndex(), null, 11, false, null);

            // numeric styles
            CellStyle boldNum       = style(wb, IndexedColors.BLACK.getIndex(), null, 11, true, "#,##0");
            CellStyle boldQty       = style(wb, IndexedColors.BLACK.getIndex(), null, 11, true, "#,##0.000");
            CellStyle boldCurrency  = style(wb, IndexedColors.BLACK.getIndex(), null, 11, true, "#,##0.00");
            CellStyle normalNum     = style(wb, IndexedColors.BLACK.getIndex(), null, 11, false, "#,##0");
            CellStyle normalQty     = style(wb, IndexedColors.BLACK.getIndex(), null, 11, false, "#,##0.000");
            CellStyle normalPct     = style(wb, IndexedColors.BLACK.getIndex(), null, 11, false, "0.0%");

            int r = 0;

            // title block
            row(sheet, r++, c(title, "HIVON WMS - WAREHOUSE UTILIZATION REPORT"));
            row(sheet, r++, c(bold, "Generated: " + safe(dto.getGeneratedAt())));
            r++;

            // summary section
            row(sheet, r++, c(section, "UTILIZATION SUMMARY"));
            row(sheet, r++, c(bold, "Total Warehouse Capacity:"), c(boldQty, dto.getTotalWarehouseCapacity()));
            row(sheet, r++, c(bold, "Current Stock Value:"), c(boldCurrency, dto.getCurrentStockValue()));
            row(sheet, r++, c(bold, "Overall Utilization:"), c(normalPct, dto.getOverallUtilizationPct() / 100.0));
            row(sheet, r++, c(bold, "Empty Bins:"), c(boldNum, dto.getEmptyBins()));
            row(sheet, r++, c(bold, "Fully Occupied Bins:"), c(boldNum, dto.getFullyOccupiedBins()));
            row(sheet, r++, c(bold, "Average Bin Utilization:"), c(normalPct, dto.getAverageBinUtilization() / 100.0));
            r++;

            // bin utilization table
            if (dto.getBinUtilizationList() != null && !dto.getBinUtilizationList().isEmpty()) {
                row(sheet, r++, c(section, "BIN UTILIZATION"));
                row(sheet, r++, c(header, "Bin Code"), c(header, "Zone"), c(header, "Current Items"),
                        c(header, "Current Qty"), c(header, "Max Capacity"), c(header, "Utilization %"), c(header, "Status"));
                for (BinUtilizationItem b : dto.getBinUtilizationList()) {
                    row(sheet, r++, c(normal, safe(b.getBinCode())), c(normal, safe(b.getZone())),
                            c(normalNum, b.getCurrentItems()), c(normalQty, b.getCurrentQty()),
                            c(normalQty, b.getMaxCapacity()), c(normalPct, b.getUtilizationPct() / 100.0),
                            c(normal, safe(b.getStatus())));
                }
                r++;
            }

            // zone utilization table
            if (dto.getZoneUtilizationList() != null && !dto.getZoneUtilizationList().isEmpty()) {
                row(sheet, r++, c(section, "ZONE UTILIZATION"));
                row(sheet, r++, c(header, "Zone Name"), c(header, "Total Bins"), c(header, "Total Capacity"),
                        c(header, "Current Qty"), c(header, "Utilization %"));
                for (ZoneUtilizationItem z : dto.getZoneUtilizationList()) {
                    row(sheet, r++, c(normal, safe(z.getZoneName())), c(normalNum, z.getTotalBins()),
                            c(normalQty, z.getTotalCapacity()), c(normalQty, z.getCurrentQty()),
                            c(normalPct, z.getUtilizationPct() / 100.0));
                }
                r++;
            }

            // top space consumers table
            if (dto.getTopSpaceConsumersList() != null && !dto.getTopSpaceConsumersList().isEmpty()) {
                row(sheet, r++, c(section, "TOP SPACE CONSUMERS"));
                row(sheet, r++, c(header, "Material Code"), c(header, "Material Name"), c(header, "Bins Used"),
                        c(header, "Volume/Qty"), c(header, "Space %"));
                for (TopSpaceConsumerItem t : dto.getTopSpaceConsumersList()) {
                    row(sheet, r++, c(normal, safe(t.getMaterialCode())), c(normal, safe(t.getMaterialName())),
                            c(normalNum, t.getBinsUsed()), c(normalQty, t.getVolumeQty()),
                            c(normal, safe(t.getSpacePct())));
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
    public static void exportToPDF(UtilizationReportDTO dto, File file) throws Exception {
        if (dto == null) throw new IllegalArgumentException("Report data is null.");
        file = ensureExtension(file, ".pdf");

        try (PDDocument doc = new PDDocument()) {
            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PdfCursor cur = new PdfCursor(doc, regular, bold);

            // Title
            cur.text("HIVON WMS - WAREHOUSE UTILIZATION REPORT", 50, 545, 16, true, 0.12f, 0.23f, 0.54f);
            cur.text("Generated: " + safe(dto.getGeneratedAt()) + "   |   Overall Space Utilization: " + pctFmt.format(dto.getOverallUtilizationPct()) + "%", 50, 527, 10, false, 0.4f, 0.4f, 0.4f);
            cur.line(50, 517, 792, 517);

            // Summary cards
            cur.card(50, 420, 220, 80, "Warehouse Capacity",
                    "Total Capacity: " + qtyFmt.format(dto.getTotalWarehouseCapacity()),
                    "Stock Value: " + dto.getCurrency() + " " + currencyFmt.format(dto.getCurrentStockValue()),
                    "Overall Utilization: " + pctFmt.format(dto.getOverallUtilizationPct()) + "%");

            cur.card(290, 420, 240, 80, "Bin Occupancy",
                    "Empty Bins: " + dto.getEmptyBins(),
                    "Fully Occupied Bins: " + dto.getFullyOccupiedBins(),
                    "Avg Bin Utilization: " + pctFmt.format(dto.getAverageBinUtilization()) + "%");

            String topZoneName = (dto.getZoneUtilizationList() != null && !dto.getZoneUtilizationList().isEmpty())
                    ? dto.getZoneUtilizationList().get(0).getZoneName() : "N/A";
            String topConsumerName = (dto.getTopSpaceConsumersList() != null && !dto.getTopSpaceConsumersList().isEmpty())
                    ? dto.getTopSpaceConsumersList().get(0).getMaterialName() : "N/A";
            cur.card(550, 420, 240, 80, "Space Distribution",
                    "Zones Tracked: " + (dto.getZoneUtilizationList() != null ? dto.getZoneUtilizationList().size() : 0),
                    "Primary Zone: " + topZoneName,
                    "Top Consumer: " + trunc(topConsumerName, 20));

            // Bin Utilization Table
            cur.moveTo(390);
            cur.sectionTitle("Bin Utilization");
            cur.down(18);

            String[] binCols = {"Bin Code", "Zone", "Current Items", "Current Qty", "Max Capacity", "Utilization %", "Status"};
            float[] binX = {55, 170, 260, 360, 470, 580, 680};
            cur.tableHeader(binCols, binX);

            if (dto.getBinUtilizationList() != null && !dto.getBinUtilizationList().isEmpty()) {
                boolean shaded = false;
                for (BinUtilizationItem b : dto.getBinUtilizationList()) {
                    cur.ensureRoom(binCols, binX, "Bin Utilization (Continued)");
                    cur.tableRow(shaded, binX,
                            safe(b.getBinCode()),
                            safe(b.getZone()),
                            String.valueOf(b.getCurrentItems()),
                            qtyFmt.format(b.getCurrentQty()),
                            qtyFmt.format(b.getMaxCapacity()),
                            pctFmt.format(b.getUtilizationPct()) + "%",
                            safe(b.getStatus()));
                    shaded = !shaded;
                }
            } else {
                cur.text("No bin utilization records found.", 60, cur.y - 10, 8, false, 0.5f, 0.5f, 0.5f);
                cur.down(16);
            }

            // Top Space Consumers Table
            cur.down(16);
            cur.ensureRoom(new String[]{"Material Code", "Material Name", "Bins Used", "Volume/Qty", "Space %"},
                    new float[]{55, 200, 450, 560, 680}, "Top Space Consumers");
            cur.sectionTitle("Top Space Consumers");
            cur.down(18);

            String[] topCols = {"Material Code", "Material Name", "Bins Used", "Volume/Qty", "Space %"};
            float[] topX = {55, 200, 450, 560, 680};
            cur.tableHeader(topCols, topX);

            if (dto.getTopSpaceConsumersList() != null && !dto.getTopSpaceConsumersList().isEmpty()) {
                boolean shaded = false;
                for (TopSpaceConsumerItem t : dto.getTopSpaceConsumersList()) {
                    cur.ensureRoom(topCols, topX, "Top Space Consumers (Continued)");
                    cur.tableRow(shaded, topX,
                            safe(t.getMaterialCode()),
                            trunc(t.getMaterialName(), 35),
                            String.valueOf(t.getBinsUsed()),
                            qtyFmt.format(t.getVolumeQty()),
                            safe(t.getSpacePct()));
                    shaded = !shaded;
                }
            } else {
                cur.text("No top space consumer records found.", 60, cur.y - 10, 8, false, 0.5f, 0.5f, 0.5f);
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
