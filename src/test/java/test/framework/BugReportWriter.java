package test.framework;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes the bug workbook (BUG_REPORT*.xlsx) derived from failing test cases.
 */
public final class BugReportWriter {

    private BugReportWriter() {
    }

    public static Path write(RunReport report, Path dir, String stamp) throws IOException {
        Files.createDirectories(dir);
        String name = "BUG_REPORT" + TestReportWriter.labelSuffix(report.runLabel) + "_" + stamp + ".xlsx";
        Path file = dir.resolve(name);

        try (Workbook wb = Xlsx.newWorkbook()) {
            writeSummary(wb, report);
            writeBugs(wb, report);
            writeReproduction(wb, report);

            try (FileOutputStream out = new FileOutputStream(file.toFile())) {
                wb.write(out);
            }
        }
        return file;
    }

    private static void writeSummary(Workbook wb, RunReport report) {
        Sheet sheet = wb.createSheet("Summary");
        Xlsx.headerRow(sheet, 0, "Metric", "Value");

        Map<String, Integer> bySeverity = new LinkedHashMap<>();
        bySeverity.put("High", 0);
        bySeverity.put("Medium", 0);
        bySeverity.put("Low", 0);
        Map<String, Integer> byForm = new LinkedHashMap<>();
        for (Bug bug : report.bugs) {
            bySeverity.merge(bug.severity, 1, Integer::sum);
            byForm.merge(bug.form, 1, Integer::sum);
        }

        int row = 1;
        row = kv(sheet, row, "Run", report.runLabel);
        row = kv(sheet, row, "Run Date", report.runDate);
        row = kv(sheet, row, "Base URL", report.baseUrl);
        row = kv(sheet, row, "Total Bugs", String.valueOf(report.bugs.size()));
        row = kv(sheet, row, "High", String.valueOf(bySeverity.get("High")));
        row = kv(sheet, row, "Medium", String.valueOf(bySeverity.get("Medium")));
        row = kv(sheet, row, "Low", String.valueOf(bySeverity.get("Low")));
        row = kv(sheet, row, "Failing Tests", String.valueOf(report.failed));
        row++;
        row = kv(sheet, row, "Bugs by Form", "");
        for (Map.Entry<String, Integer> entry : byForm.entrySet()) {
            row = kv(sheet, row, "  " + entry.getKey(), String.valueOf(entry.getValue()));
        }
        Xlsx.autoWidth(sheet, 2, 60);
    }

    private static int kv(Sheet sheet, int rowIndex, String key, String value) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(key);
        row.createCell(1).setCellValue(value == null ? "" : value);
        return rowIndex + 1;
    }

    private static void writeBugs(Workbook wb, RunReport report) {
        Sheet sheet = wb.createSheet("Bugs");
        Xlsx.headerRow(sheet, 0, "Bug ID", "Severity", "Form", "Layer",
                "Test Case", "Class", "Message");

        CellStyle high = Xlsx.failStyle(wb);
        int rowIndex = 1;
        for (Bug bug : report.bugs) {
            TestResult first = bug.first();
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(bug.id);
            Cell severity = row.createCell(1);
            severity.setCellValue(bug.severity);
            if ("High".equals(bug.severity)) {
                severity.setCellStyle(high);
            }
            row.createCell(2).setCellValue(bug.form);
            row.createCell(3).setCellValue(bug.layer);
            row.createCell(4).setCellValue(first == null ? "" : first.displayName);
            row.createCell(5).setCellValue(first == null ? "" : TestReportWriter.shortName(first.className));
            row.createCell(6).setCellValue(first == null || first.message == null ? "" : first.message);
        }
        Xlsx.autoWidth(sheet, 7, 80);
    }

    private static void writeReproduction(Workbook wb, RunReport report) {
        Sheet sheet = wb.createSheet("Reproduction");
        Xlsx.headerRow(sheet, 0, "Bug ID", "Test ID", "Test", "Status",
                "Duration (ms)", "Stack Trace");

        int rowIndex = 1;
        for (Bug bug : report.bugs) {
            for (TestResult tr : bug.cases) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(bug.id);
                row.createCell(1).setCellValue(tr.id);
                row.createCell(2).setCellValue(tr.displayName);
                row.createCell(3).setCellValue(tr.status);
                row.createCell(4).setCellValue(tr.durationMs);
                row.createCell(5).setCellValue(tr.stackTrace == null ? "" : tr.stackTrace);
            }
        }
        Xlsx.autoWidth(sheet, 6, 100);
    }
}
