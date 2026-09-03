package core.utils;

import models.dto.DailyActivityReportDTO;
import models.dto.DailyActivityReportDTO.ActivityLogItem;
import models.dto.DailyActivityReportDTO.WorkerProductivityItem;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for exporting report data to XLSX (Excel XML / CSV format)
 * and native PDF-1.4 binary format.
 *
 * @author Sanod
 */
public class ReportExportUtils {

    private static final DecimalFormat currencyFmt = new DecimalFormat("#,##0.00");
    private static final DecimalFormat qtyFmt = new DecimalFormat("#,##0");

    /**
     * Exports Daily Activity Report to Excel XLSX (Excel XML Spreadsheet format).
     *
     * @param dto Report DTO
     * @param file Target output file
     * @throws Exception if file writing fails
     */
    public static void exportToXLSX(DailyActivityReportDTO dto, File file) throws Exception {
        if (dto == null) {
            throw new IllegalArgumentException("Report data is null.");
        }

        String filePath = file.getAbsolutePath();
        if (!filePath.toLowerCase().endsWith(".xlsx") && !filePath.toLowerCase().endsWith(".xml")) {
            file = new File(filePath + ".xlsx");
        }

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            writer.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            writer.println("<?mso-application progid=\"Excel.Sheet\"?>");
            writer.println("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"");
            writer.println(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"");
            writer.println(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"");
            writer.println(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">");

            // Styles
            writer.println(" <Styles>");
            writer.println("  <Style ss:ID=\"Default\" ss:Name=\"Normal\"><Font ss:FontName=\"Calibri\" ss:Size=\"11\"/></Style>");
            writer.println("  <Style ss:ID=\"TitleStyle\"><Font ss:FontName=\"Calibri\" ss:Size=\"16\" ss:Bold=\"1\" ss:Color=\"#1E3A8A\"/></Style>");
            writer.println("  <Style ss:ID=\"SectionStyle\"><Font ss:FontName=\"Calibri\" ss:Size=\"13\" ss:Bold=\"1\" ss:Color=\"#1E3A8A\"/><Interior ss:Color=\"#F3F4F6\" ss:Pattern=\"Solid\"/></Style>");
            writer.println("  <Style ss:ID=\"HeaderStyle\"><Font ss:FontName=\"Calibri\" ss:Size=\"11\" ss:Bold=\"1\" ss:Color=\"#FFFFFF\"/><Interior ss:Color=\"#1E3A8A\" ss:Pattern=\"Solid\"/></Style>");
            writer.println("  <Style ss:ID=\"BoldLabel\"><Font ss:FontName=\"Calibri\" ss:Size=\"11\" ss:Bold=\"1\"/></Style>");
            writer.println("  <Style ss:ID=\"CurrencyStyle\"><NumberFormat ss:Format=\"$#,##0.00\"/></Style>");
            writer.println("  <Style ss:ID=\"NumberStyle\"><NumberFormat ss:Format=\"#,##0\"/></Style>");
            writer.println(" </Styles>");

            // Worksheet
            writer.println(" <Worksheet ss:Name=\"Daily Activity Report\">");
            writer.println("  <Table>");

            // Column Widths
            writer.println("   <Column ss:Width=\"150\"/>");
            writer.println("   <Column ss:Width=\"120\"/>");
            writer.println("   <Column ss:Width=\"160\"/>");
            writer.println("   <Column ss:Width=\"140\"/>");
            writer.println("   <Column ss:Width=\"100\"/>");
            writer.println("   <Column ss:Width=\"140\"/>");
            writer.println("   <Column ss:Width=\"100\"/>");

            // Title
            writer.println("   <Row ss:Height=\"25\"><Cell ss:StyleID=\"TitleStyle\"><Data ss:Type=\"String\">HIVON WMS - DAILY ACTIVITY REPORT</Data></Cell></Row>");
            writer.println("   <Row><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Report Date: " + escapeXml(dto.getReportDate()) + "</Data></Cell></Row>");
            writer.println("   <Row></Row>");

            // Receipts Summary
            writer.println("   <Row ss:Height=\"20\"><Cell ss:StyleID=\"SectionStyle\"><Data ss:Type=\"String\">RECEIPTS SUMMARY</Data></Cell></Row>");
            writer.println("   <Row><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Total GR Count</Data></Cell><Cell ss:StyleID=\"NumberStyle\"><Data ss:Type=\"Number\">" + dto.getTotalGRCount() + "</Data></Cell><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">PO Receipts</Data></Cell><Cell ss:StyleID=\"NumberStyle\"><Data ss:Type=\"Number\">" + dto.getReceiptPO() + "</Data></Cell></Row>");
            writer.println("   <Row><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Total Receipt Qty</Data></Cell><Cell ss:StyleID=\"NumberStyle\"><Data ss:Type=\"Number\">" + dto.getTotalReceiptQty() + "</Data></Cell><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Customer Returns</Data></Cell><Cell ss:StyleID=\"NumberStyle\"><Data ss:Type=\"Number\">" + dto.getReceiptCustomerReturns() + "</Data></Cell></Row>");
            writer.println("   <Row><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Total Receipt Value</Data></Cell><Cell ss:StyleID=\"CurrencyStyle\"><Data ss:Type=\"Number\">" + dto.getTotalReceiptValue() + "</Data></Cell><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Transfer In</Data></Cell><Cell ss:StyleID=\"NumberStyle\"><Data ss:Type=\"Number\">" + dto.getReceiptTransferIn() + "</Data></Cell></Row>");
            writer.println("   <Row></Row>");

            // Issues Summary
            writer.println("   <Row ss:Height=\"20\"><Cell ss:StyleID=\"SectionStyle\"><Data ss:Type=\"String\">ISSUES SUMMARY</Data></Cell></Row>");
            writer.println("   <Row><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Total GI Count</Data></Cell><Cell ss:StyleID=\"NumberStyle\"><Data ss:Type=\"Number\">" + dto.getTotalGICount() + "</Data></Cell><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Sales Orders</Data></Cell><Cell ss:StyleID=\"NumberStyle\"><Data ss:Type=\"Number\">" + dto.getSalesOrder() + "</Data></Cell></Row>");
            writer.println("   <Row><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Total Issue Qty</Data></Cell><Cell ss:StyleID=\"NumberStyle\"><Data ss:Type=\"Number\">" + dto.getTotalIssueQty() + "</Data></Cell><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Internal Consumption</Data></Cell><Cell ss:StyleID=\"NumberStyle\"><Data ss:Type=\"Number\">" + dto.getInternalConsumption() + "</Data></Cell></Row>");
            writer.println("   <Row><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Total Issue Value</Data></Cell><Cell ss:StyleID=\"CurrencyStyle\"><Data ss:Type=\"Number\">" + dto.getTotalIssueValue() + "</Data></Cell><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Transfer Out</Data></Cell><Cell ss:StyleID=\"NumberStyle\"><Data ss:Type=\"Number\">" + dto.getTransferOut() + "</Data></Cell></Row>");
            writer.println("   <Row></Row>");

            // Transfers & Adjustments
            writer.println("   <Row ss:Height=\"20\"><Cell ss:StyleID=\"SectionStyle\"><Data ss:Type=\"String\">TRANSFERS &amp; ADJUSTMENTS</Data></Cell></Row>");
            writer.println("   <Row><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Bin-to-Bin Transfers</Data></Cell><Cell ss:StyleID=\"NumberStyle\"><Data ss:Type=\"Number\">" + dto.getBintoBinTransfers() + "</Data></Cell><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Cycle Count Adjustments</Data></Cell><Cell ss:StyleID=\"NumberStyle\"><Data ss:Type=\"Number\">" + dto.getCycleCountAdjustments() + "</Data></Cell></Row>");
            writer.println("   <Row><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Total Transfer Qty</Data></Cell><Cell ss:StyleID=\"NumberStyle\"><Data ss:Type=\"Number\">" + dto.getTotalTransferQty() + "</Data></Cell><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Inventory Adjustments</Data></Cell><Cell ss:StyleID=\"NumberStyle\"><Data ss:Type=\"Number\">" + dto.getInventoryAdjustments() + "</Data></Cell></Row>");
            writer.println("   <Row><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Total Transfer Value</Data></Cell><Cell ss:StyleID=\"CurrencyStyle\"><Data ss:Type=\"Number\">" + dto.getTotalTransferValue() + "</Data></Cell><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Net Adjustment Value</Data></Cell><Cell ss:StyleID=\"CurrencyStyle\"><Data ss:Type=\"Number\">" + dto.getNetAdjustmentValue() + "</Data></Cell></Row>");
            writer.println("   <Row></Row>");

            // Exceptions Summary
            writer.println("   <Row ss:Height=\"20\"><Cell ss:StyleID=\"SectionStyle\"><Data ss:Type=\"String\">EXCEPTION SUMMARY</Data></Cell></Row>");
            writer.println("   <Row><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Short Picks</Data></Cell><Cell ss:StyleID=\"NumberStyle\"><Data ss:Type=\"Number\">" + dto.getShortPicks() + "</Data></Cell><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Damaged Items</Data></Cell><Cell ss:StyleID=\"NumberStyle\"><Data ss:Type=\"Number\">" + dto.getDamagedItems() + "</Data></Cell><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Variances</Data></Cell><Cell ss:StyleID=\"NumberStyle\"><Data ss:Type=\"Number\">" + dto.getVariances() + "</Data></Cell></Row>");
            writer.println("   <Row></Row>");

            // Worker Productivity Table
            writer.println("   <Row ss:Height=\"20\"><Cell ss:StyleID=\"SectionStyle\"><Data ss:Type=\"String\">WORKER PRODUCTIVITY</Data></Cell></Row>");
            writer.println("   <Row><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Total Active Workers: " + dto.getTotalActiveWorkers() + "</Data></Cell><Cell ss:StyleID=\"BoldLabel\"><Data ss:Type=\"String\">Avg Tasks/Worker: " + dto.getAverageTasksPerWorker() + "</Data></Cell></Row>");
            writer.println("   <Row ss:Height=\"20\">");
            writer.println("    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">Worker Name</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">Tasks Completed</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">Quantities Handled</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">Hours Active</Data></Cell>");
            writer.println("   </Row>");

            if (dto.getWorkerList() != null) {
                for (WorkerProductivityItem w : dto.getWorkerList()) {
                    writer.println("   <Row>");
                    writer.println("    <Cell><Data ss:Type=\"String\">" + escapeXml(w.getWorkerName()) + "</Data></Cell>");
                    writer.println("    <Cell ss:StyleID=\"NumberStyle\"><Data ss:Type=\"Number\">" + w.getTasksCompleted() + "</Data></Cell>");
                    writer.println("    <Cell ss:StyleID=\"NumberStyle\"><Data ss:Type=\"Number\">" + w.getQuantitiesHandled() + "</Data></Cell>");
                    writer.println("    <Cell><Data ss:Type=\"Number\">" + w.getHoursActive() + "</Data></Cell>");
                    writer.println("   </Row>");
                }
            }
            writer.println("   <Row></Row>");

            // Activity Log Table
            writer.println("   <Row ss:Height=\"20\"><Cell ss:StyleID=\"SectionStyle\"><Data ss:Type=\"String\">ACTIVITY LOG</Data></Cell></Row>");
            writer.println("   <Row ss:Height=\"20\">");
            writer.println("    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">Time</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">User</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">Activity Type</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">Material</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">Quantity</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">From -> To</Data></Cell>");
            writer.println("    <Cell ss:StyleID=\"HeaderStyle\"><Data ss:Type=\"String\">Status</Data></Cell>");
            writer.println("   </Row>");

            if (dto.getActivityLogList() != null) {
                for (ActivityLogItem log : dto.getActivityLogList()) {
                    writer.println("   <Row>");
                    writer.println("    <Cell><Data ss:Type=\"String\">" + escapeXml(log.getTime()) + "</Data></Cell>");
                    writer.println("    <Cell><Data ss:Type=\"String\">" + escapeXml(log.getUser()) + "</Data></Cell>");
                    writer.println("    <Cell><Data ss:Type=\"String\">" + escapeXml(log.getActivityType()) + "</Data></Cell>");
                    writer.println("    <Cell><Data ss:Type=\"String\">" + escapeXml(log.getMaterial()) + "</Data></Cell>");
                    writer.println("    <Cell ss:StyleID=\"NumberStyle\"><Data ss:Type=\"Number\">" + log.getQuantity() + "</Data></Cell>");
                    writer.println("    <Cell><Data ss:Type=\"String\">" + escapeXml(log.getFromTo()) + "</Data></Cell>");
                    writer.println("    <Cell><Data ss:Type=\"String\">" + escapeXml(log.getStatus()) + "</Data></Cell>");
                    writer.println("   </Row>");
                }
            }

            writer.println("  </Table>");
            writer.println(" </Worksheet>");
            writer.println("</Workbook>");
        }
    }

    /**
     * Exports Daily Activity Report to native binary PDF (PDF 1.4 format).
     *
     * @param dto Report DTO
     * @param file Target output file
     * @throws Exception if PDF generation fails
     */
    public static void exportToPDF(DailyActivityReportDTO dto, File file) throws Exception {
        if (dto == null) {
            throw new IllegalArgumentException("Report data is null.");
        }

        String filePath = file.getAbsolutePath();
        if (!filePath.toLowerCase().endsWith(".pdf")) {
            file = new File(filePath + ".pdf");
        }

        SimplePdfBuilder pdf = new SimplePdfBuilder();
        pdf.generateDailyActivityPdf(dto, file);
    }

    private static String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }

    /**
     * Native PDF 1.4 Generator Class.
     * Constructs valid binary PDF documents without external dependencies.
     */
    private static class SimplePdfBuilder {

        public void generateDailyActivityPdf(DailyActivityReportDTO dto, File file) throws Exception {
            List<String> pageContentStreams = new ArrayList<>();

            // Build page 1 content stream
            ByteArrayOutputStream page1 = new ByteArrayOutputStream();
            PdfStreamWriter w = new PdfStreamWriter(page1);

            // Title
            w.drawText("HIVON WMS - DAILY ACTIVITY REPORT", 50, 790, 16, true, 0.12f, 0.23f, 0.54f);
            w.drawText("Report Date: " + dto.getReportDate(), 50, 772, 10, false, 0.4f, 0.4f, 0.4f);
            w.drawLine(50, 762, 545, 762, 1.0f, 0.2f, 0.4f, 0.8f);

            // Summary Card 1: Receipts Summary
            w.drawBox(50, 640, 240, 110, 0.97f, 0.98f, 1.0f, 0.8f, 0.85f, 0.95f);
            w.drawText("Receipts Summary", 60, 735, 11, true, 0.12f, 0.23f, 0.54f);
            w.drawText("Total GR Count: " + dto.getTotalGRCount(), 60, 715, 9, false, 0.2f, 0.2f, 0.2f);
            w.drawText("Total Receipt Qty: " + qtyFmt.format(dto.getTotalReceiptQty()), 60, 700, 9, false, 0.2f, 0.2f, 0.2f);
            w.drawText("Total Receipt Value: $" + currencyFmt.format(dto.getTotalReceiptValue()), 60, 685, 9, true, 0.2f, 0.2f, 0.2f);
            w.drawText("PO: " + dto.getReceiptPO() + "  | Customer Returns: " + dto.getReceiptCustomerReturns() + "  | Transfer In: " + dto.getReceiptTransferIn(), 60, 665, 8, false, 0.4f, 0.4f, 0.4f);

            // Summary Card 2: Issues Summary
            w.drawBox(305, 640, 240, 110, 0.97f, 0.98f, 1.0f, 0.8f, 0.85f, 0.95f);
            w.drawText("Issues Summary", 315, 735, 11, true, 0.12f, 0.23f, 0.54f);
            w.drawText("Total GI Count: " + dto.getTotalGICount(), 315, 715, 9, false, 0.2f, 0.2f, 0.2f);
            w.drawText("Total Issue Qty: " + qtyFmt.format(dto.getTotalIssueQty()), 315, 700, 9, false, 0.2f, 0.2f, 0.2f);
            w.drawText("Total Issue Value: $" + currencyFmt.format(dto.getTotalIssueValue()), 315, 685, 9, true, 0.2f, 0.2f, 0.2f);
            w.drawText("Sales Order: " + dto.getSalesOrder() + " | Internal Consumption: " + dto.getInternalConsumption() + " | Transfer Out: " + dto.getTransferOut(), 315, 665, 8, false, 0.4f, 0.4f, 0.4f);

            // Summary Card 3: Transfers & Adjustments
            w.drawBox(50, 515, 240, 115, 0.97f, 0.98f, 1.0f, 0.8f, 0.85f, 0.95f);
            w.drawText("Transfers & Adjustments", 60, 615, 11, true, 0.12f, 0.23f, 0.54f);
            w.drawText("Bin-to-Bin Transfers: " + dto.getBintoBinTransfers(), 60, 595, 9, false, 0.2f, 0.2f, 0.2f);
            w.drawText("Total Transfer Qty: " + qtyFmt.format(dto.getTotalTransferQty()), 60, 580, 9, false, 0.2f, 0.2f, 0.2f);
            w.drawText("Total Transfer Value: $" + currencyFmt.format(dto.getTotalTransferValue()), 60, 565, 9, false, 0.2f, 0.2f, 0.2f);
            w.drawText("Cycle Count Adj: " + dto.getCycleCountAdjustments() + " | Inventory Adj: " + dto.getInventoryAdjustments(), 60, 550, 8, false, 0.4f, 0.4f, 0.4f);
            w.drawText("Net Adjustment Value: $" + currencyFmt.format(dto.getNetAdjustmentValue()), 60, 535, 9, true, 0.2f, 0.2f, 0.2f);

            // Summary Card 4: Exception Summary
            w.drawBox(305, 515, 240, 115, 0.97f, 0.98f, 1.0f, 0.8f, 0.85f, 0.95f);
            w.drawText("Exception Summary", 315, 615, 11, true, 0.12f, 0.23f, 0.54f);
            w.drawText("Short Picks: " + dto.getShortPicks(), 315, 595, 9, false, 0.2f, 0.2f, 0.2f);
            w.drawText("Damaged Items: " + dto.getDamagedItems(), 315, 580, 9, false, 0.2f, 0.2f, 0.2f);
            w.drawText("Variances: " + dto.getVariances(), 315, 565, 9, false, 0.2f, 0.2f, 0.2f);

            // Worker Productivity Section
            float y = 490;
            w.drawText("Worker Productivity", 50, y, 12, true, 0.12f, 0.23f, 0.54f);
            y -= 14;
            w.drawText("Active Workers: " + dto.getTotalActiveWorkers() + "   |   Avg Tasks per Worker: " + dto.getAverageTasksPerWorker(), 50, y, 9, false, 0.4f, 0.4f, 0.4f);
            y -= 18;

            // Worker Table Header
            w.drawBox(50, y - 14, 495, 18, 0.12f, 0.23f, 0.54f, 0.12f, 0.23f, 0.54f);
            w.drawText("Worker Name", 60, y - 10, 9, true, 1.0f, 1.0f, 1.0f);
            w.drawText("Tasks Completed", 220, y - 10, 9, true, 1.0f, 1.0f, 1.0f);
            w.drawText("Quantities Handled", 340, y - 10, 9, true, 1.0f, 1.0f, 1.0f);
            w.drawText("Hours Active", 460, y - 10, 9, true, 1.0f, 1.0f, 1.0f);
            y -= 16;

            if (dto.getWorkerList() != null && !dto.getWorkerList().isEmpty()) {
                int rowIdx = 0;
                for (WorkerProductivityItem wp : dto.getWorkerList()) {
                    float bgG = (rowIdx % 2 == 0) ? 0.96f : 1.0f;
                    w.drawBox(50, y - 14, 495, 16, bgG, bgG, bgG, 0.85f, 0.85f, 0.85f);
                    w.drawText(wp.getWorkerName(), 60, y - 10, 8, false, 0.2f, 0.2f, 0.2f);
                    w.drawText(String.valueOf(wp.getTasksCompleted()), 220, y - 10, 8, false, 0.2f, 0.2f, 0.2f);
                    w.drawText(qtyFmt.format(wp.getQuantitiesHandled()), 340, y - 10, 8, false, 0.2f, 0.2f, 0.2f);
                    w.drawText(String.valueOf(wp.getHoursActive()), 460, y - 10, 8, false, 0.2f, 0.2f, 0.2f);
                    y -= 16;
                    rowIdx++;
                }
            } else {
                w.drawText("No worker activity recorded for date.", 60, y - 10, 8, false, 0.5f, 0.5f, 0.5f);
                y -= 16;
            }

            y -= 15;

            // Activity Log Section
            w.drawText("Activity Log", 50, y, 12, true, 0.12f, 0.23f, 0.54f);
            y -= 18;

            // Activity Log Table Header
            w.drawBox(50, y - 14, 495, 18, 0.12f, 0.23f, 0.54f, 0.12f, 0.23f, 0.54f);
            w.drawText("Time", 55, y - 10, 8, true, 1.0f, 1.0f, 1.0f);
            w.drawText("User", 110, y - 10, 8, true, 1.0f, 1.0f, 1.0f);
            w.drawText("Activity Type", 170, y - 10, 8, true, 1.0f, 1.0f, 1.0f);
            w.drawText("Material", 280, y - 10, 8, true, 1.0f, 1.0f, 1.0f);
            w.drawText("Quantity", 345, y - 10, 8, true, 1.0f, 1.0f, 1.0f);
            w.drawText("From -> To", 410, y - 10, 8, true, 1.0f, 1.0f, 1.0f);
            w.drawText("Status", 495, y - 10, 8, true, 1.0f, 1.0f, 1.0f);
            y -= 16;

            if (dto.getActivityLogList() != null && !dto.getActivityLogList().isEmpty()) {
                int rowIdx = 0;
                for (ActivityLogItem log : dto.getActivityLogList()) {
                    if (y < 50) {
                        // Close current page and start page 2
                        pageContentStreams.add(page1.toString(StandardCharsets.UTF_8));
                        page1 = new ByteArrayOutputStream();
                        w = new PdfStreamWriter(page1);
                        y = 790;
                        w.drawText("Activity Log (Continued)", 50, y, 12, true, 0.12f, 0.23f, 0.54f);
                        y -= 18;
                        w.drawBox(50, y - 14, 495, 18, 0.12f, 0.23f, 0.54f, 0.12f, 0.23f, 0.54f);
                        w.drawText("Time", 55, y - 10, 8, true, 1.0f, 1.0f, 1.0f);
                        w.drawText("User", 110, y - 10, 8, true, 1.0f, 1.0f, 1.0f);
                        w.drawText("Activity Type", 170, y - 10, 8, true, 1.0f, 1.0f, 1.0f);
                        w.drawText("Material", 280, y - 10, 8, true, 1.0f, 1.0f, 1.0f);
                        w.drawText("Quantity", 345, y - 10, 8, true, 1.0f, 1.0f, 1.0f);
                        w.drawText("From -> To", 410, y - 10, 8, true, 1.0f, 1.0f, 1.0f);
                        w.drawText("Status", 495, y - 10, 8, true, 1.0f, 1.0f, 1.0f);
                        y -= 16;
                    }

                    float bgG = (rowIdx % 2 == 0) ? 0.96f : 1.0f;
                    w.drawBox(50, y - 14, 495, 16, bgG, bgG, bgG, 0.85f, 0.85f, 0.85f);
                    w.drawText(log.getTime(), 55, y - 10, 7.5f, false, 0.2f, 0.2f, 0.2f);
                    w.drawText(log.getUser(), 110, y - 10, 7.5f, false, 0.2f, 0.2f, 0.2f);
                    w.drawText(truncateText(log.getActivityType(), 22), 170, y - 10, 7.5f, false, 0.2f, 0.2f, 0.2f);
                    w.drawText(truncateText(log.getMaterial(), 14), 280, y - 10, 7.5f, false, 0.2f, 0.2f, 0.2f);
                    w.drawText(qtyFmt.format(log.getQuantity()), 345, y - 10, 7.5f, false, 0.2f, 0.2f, 0.2f);
                    w.drawText(truncateText(log.getFromTo(), 16), 410, y - 10, 7.5f, false, 0.2f, 0.2f, 0.2f);
                    w.drawText(log.getStatus(), 495, y - 10, 7.5f, false, 0.2f, 0.2f, 0.2f);
                    y -= 16;
                    rowIdx++;
                }
            } else {
                w.drawText("No activity log entries found for date.", 60, y - 10, 8, false, 0.5f, 0.5f, 0.5f);
            }

            pageContentStreams.add(page1.toString(StandardCharsets.UTF_8));

            // Write final PDF file structure
            writePdfFile(pageContentStreams, file);
        }

        private String truncateText(String str, int maxLen) {
            if (str == null) return "";
            if (str.length() <= maxLen) return str;
            return str.substring(0, maxLen - 1) + "..";
        }

        private void writePdfFile(List<String> pagesContents, File outFile) throws Exception {
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                List<Long> offsets = new ArrayList<>();

                // PDF Header
                String header = "%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n";
                fos.write(header.getBytes(StandardCharsets.ISO_8859_1));
                long currentOffset = header.getBytes(StandardCharsets.ISO_8859_1).length;

                // Object 1: Catalog
                offsets.add(currentOffset);
                String obj1 = "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n";
                byte[] b1 = obj1.getBytes(StandardCharsets.ISO_8859_1);
                fos.write(b1);
                currentOffset += b1.length;

                int totalPages = pagesContents.size();

                // Object 2: Pages
                offsets.add(currentOffset);
                StringBuilder kids = new StringBuilder();
                for (int i = 0; i < totalPages; i++) {
                    kids.append((3 + i * 2)).append(" 0 R ");
                }
                String obj2 = "2 0 obj\n<< /Type /Pages /Kids [" + kids.toString().trim() + "] /Count " + totalPages + " >>\nendobj\n";
                byte[] b2 = obj2.getBytes(StandardCharsets.ISO_8859_1);
                fos.write(b2);
                currentOffset += b2.length;

                int fontObjRef = 3 + totalPages * 2;
                int fontBoldObjRef = fontObjRef + 1;

                // Objects for each page (Page Obj & Content Stream Obj)
                for (int i = 0; i < totalPages; i++) {
                    int pageObjNum = 3 + i * 2;
                    int streamObjNum = pageObjNum + 1;
                    String streamText = pagesContents.get(i);
                    byte[] streamBytes = streamText.getBytes(StandardCharsets.ISO_8859_1);

                    // Page Obj
                    offsets.add(currentOffset);
                    String pageObj = pageObjNum + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 " + fontObjRef + " 0 R /F2 " + fontBoldObjRef + " 0 R >> >> /Contents " + streamObjNum + " 0 R >>\nendobj\n";
                    byte[] bPage = pageObj.getBytes(StandardCharsets.ISO_8859_1);
                    fos.write(bPage);
                    currentOffset += bPage.length;

                    // Content Stream Obj
                    offsets.add(currentOffset);
                    String streamObjHeader = streamObjNum + " 0 obj\n<< /Length " + streamBytes.length + " >>\nstream\n";
                    byte[] bStreamHeader = streamObjHeader.getBytes(StandardCharsets.ISO_8859_1);
                    fos.write(bStreamHeader);
                    currentOffset += bStreamHeader.length;

                    fos.write(streamBytes);
                    currentOffset += streamBytes.length;

                    String streamObjFooter = "\nendstream\nendobj\n";
                    byte[] bStreamFooter = streamObjFooter.getBytes(StandardCharsets.ISO_8859_1);
                    fos.write(bStreamFooter);
                    currentOffset += bStreamFooter.length;
                }

                // Font F1 (Helvetica)
                offsets.add(currentOffset);
                String objFont = fontObjRef + " 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n";
                byte[] bFont = objFont.getBytes(StandardCharsets.ISO_8859_1);
                fos.write(bFont);
                currentOffset += bFont.length;

                // Font F2 (Helvetica-Bold)
                offsets.add(currentOffset);
                String objFontBold = fontBoldObjRef + " 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj\n";
                byte[] bFontBold = objFontBold.getBytes(StandardCharsets.ISO_8859_1);
                fos.write(bFontBold);
                currentOffset += bFontBold.length;

                // Xref table
                long startXref = currentOffset;
                int totalObjects = offsets.size() + 1;
                StringBuilder xref = new StringBuilder();
                xref.append("xref\n0 ").append(totalObjects).append("\n0000000000 65535 f \n");
                for (Long offset : offsets) {
                    xref.append(String.format("%010d 00000 n \n", offset));
                }
                xref.append("trailer\n<< /Size ").append(totalObjects).append(" /Root 1 0 R >>\n");
                xref.append("startxref\n").append(startXref).append("\n%%EOF\n");

                byte[] bXref = xref.toString().getBytes(StandardCharsets.ISO_8859_1);
                fos.write(bXref);
            }
        }

        private static class PdfStreamWriter {
            private final ByteArrayOutputStream out;

            public PdfStreamWriter(ByteArrayOutputStream out) {
                this.out = out;
            }

            public void drawText(String text, float x, float y, float fontSize, boolean isBold, float r, float g, float b) throws Exception {
                String fontRef = isBold ? "/F2" : "/F1";
                String escapedText = escapePdfString(text);
                String cmd = String.format(java.util.Locale.US, "BT %s %.1f Tf %.2f %.2f %.2f rg %.2f %.2f Td (%s) Tj ET\n", fontRef, fontSize, r, g, b, x, y, escapedText);
                out.write(cmd.getBytes(StandardCharsets.ISO_8859_1));
            }

            public void drawLine(float x1, float y1, float x2, float y2, float lineWidth, float r, float g, float b) throws Exception {
                String cmd = String.format(java.util.Locale.US, "%.2f %.2f %.2f RG %.1f w %.2f %.2f m %.2f %.2f l S\n", r, g, b, lineWidth, x1, y1, x2, y2);
                out.write(cmd.getBytes(StandardCharsets.ISO_8859_1));
            }

            public void drawBox(float x, float y, float width, float height, float fillR, float fillG, float fillB, float strokeR, float strokeG, float strokeB) throws Exception {
                String cmd = String.format(java.util.Locale.US, "%.2f %.2f %.2f rg %.2f %.2f %.2f RG 0.7 w %.2f %.2f %.2f %.2f re B\n", fillR, fillG, fillB, strokeR, strokeG, strokeB, x, y, width, height);
                out.write(cmd.getBytes(StandardCharsets.ISO_8859_1));
            }

            private String escapePdfString(String str) {
                if (str == null) return "";
                return str.replace("\\", "\\\\")
                          .replace("(", "\\(")
                          .replace(")", "\\)")
                          .replace("\r", "")
                          .replace("\n", " ");
            }
        }
    }
}
