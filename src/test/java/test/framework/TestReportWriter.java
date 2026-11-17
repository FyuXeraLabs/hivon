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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes the test execution workbook (TEST_REPORT*.xlsx).
 */
public final class TestReportWriter {

    private TestReportWriter() {
    }

    public static Path write(RunReport report, Path dir, String stamp) throws IOException {
        Files.createDirectories(dir);
        String name = "TEST_REPORT" + labelSuffix(report.runLabel) + "_" + stamp + ".xlsx";
        Path file = dir.resolve(name);

        try (Workbook wb = Xlsx.newWorkbook()) {
            writeSummary(wb, report);
            writeGrouped(wb, "By Form", "Form", report, true);
            writeGrouped(wb, "By Layer", "Layer", report, false);
            writeCases(wb, report);
            writeNotes(wb, report);

            try (FileOutputStream out = new FileOutputStream(file.toFile())) {
                wb.write(out);
            }
        }
        return file;
    }

    private static void writeSummary(Workbook wb, RunReport report) {
        Sheet sheet = wb.createSheet("Summary");
        Xlsx.headerRow(sheet, 0, "Metric", "Value");
        int[] row = {1};
        addKv(sheet, row[0]++, "Run", report.runLabel);
        addKv(sheet, row[0]++, "Run Date", report.runDate);
        addKv(sheet, row[0]++, "Base URL", report.baseUrl);
        addKv(sheet, row[0]++, "Authenticated As", report.username);
        addKv(sheet, row[0]++, "Duration (ms)", String.valueOf(report.durationMs));
        addKv(sheet, row[0]++, "Total Tests", String.valueOf(report.total()));
        addKv(sheet, row[0]++, "Passed", String.valueOf(report.passed));
        addKv(sheet, row[0]++, "Failed", String.valueOf(report.failed));
        addKv(sheet, row[0]++, "Skipped", String.valueOf(report.skipped));
        addKv(sheet, row[0]++, "Pass Rate", passRate(report.passed, report.total()));
        addKv(sheet, row[0]++, "Open Bugs", String.valueOf(report.bugs.size()));
        Xlsx.autoWidth(sheet, 2, 60);
    }

    private static void addKv(Sheet sheet, int rowIndex, String key, String value) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(key);
        row.createCell(1).setCellValue(value == null ? "" : value);
    }

    private static void writeGrouped(Workbook wb, String sheetName, String keyTitle,
                                     RunReport report, boolean byForm) {
        Sheet sheet = wb.createSheet(sheetName);
        Xlsx.headerRow(sheet, 0, keyTitle, "Total", "Passed", "Failed", "Skipped", "Pass Rate");

        Map<String, int[]> groups = new LinkedHashMap<>();
        for (TestResult tr : report.results) {
            String key = byForm ? tr.form : tr.layer;
            int[] counts = groups.computeIfAbsent(key, k -> new int[4]);
            counts[0]++;
            if (tr.passed()) {
                counts[1]++;
            } else if (tr.skipped()) {
                counts[3]++;
            } else {
                counts[2]++;
            }
        }

        int rowIndex = 1;
        for (Map.Entry<String, int[]> entry : groups.entrySet()) {
            int[] c = entry.getValue();
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(c[0]);
            row.createCell(2).setCellValue(c[1]);
            row.createCell(3).setCellValue(c[2]);
            row.createCell(4).setCellValue(c[3]);
            row.createCell(5).setCellValue(passRate(c[1], c[0]));
        }
        Xlsx.autoWidth(sheet, 6, 40);
    }

    private static void writeCases(Workbook wb, RunReport report) {
        Sheet sheet = wb.createSheet("Test Cases");
        Xlsx.headerRow(sheet, 0, "ID", "Form", "Layer", "Class", "Test",
                "Status", "Duration (ms)", "Message");

        CellStyle pass = Xlsx.passStyle(wb);
        CellStyle fail = Xlsx.failStyle(wb);
        CellStyle skip = Xlsx.skipStyle(wb);

        int rowIndex = 1;
        for (TestResult tr : report.results) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(tr.id);
            row.createCell(1).setCellValue(tr.form);
            row.createCell(2).setCellValue(tr.layer);
            row.createCell(3).setCellValue(shortName(tr.className));
            row.createCell(4).setCellValue(tr.displayName);
            Cell status = row.createCell(5);
            status.setCellValue(tr.status);
            status.setCellStyle(tr.passed() ? pass : (tr.skipped() ? skip : fail));
            row.createCell(6).setCellValue(tr.durationMs);
            row.createCell(7).setCellValue(tr.message == null ? "" : tr.message);
        }
        Xlsx.autoWidth(sheet, 8, 70);
    }

    private static void writeNotes(Workbook wb, RunReport report) {
        Sheet sheet = wb.createSheet("Notes");
        Xlsx.headerRow(sheet, 0, "Setup / Environment Notes");
        List<String> notes = new ArrayList<>(report.setupNotes);
        notes.add("Failures are surfaced as bugs in the paired BUG_REPORT workbook.");
        notes.add("Severity: 5xx/timeout/HeadlessException/NullPointer = High; other failures = Medium.");
        int rowIndex = 1;
        for (String note : notes) {
            sheet.createRow(rowIndex++).createCell(0).setCellValue(note);
        }
        Xlsx.autoWidth(sheet, 1, 120);
    }

    static String passRate(int passed, int total) {
        if (total == 0) {
            return "n/a";
        }
        return String.format("%.1f%%", (passed * 100.0) / total);
    }

    static String shortName(String className) {
        if (className == null) {
            return "";
        }
        int idx = className.lastIndexOf('.');
        return idx < 0 ? className : className.substring(idx + 1);
    }

    static String labelSuffix(String label) {
        if (label == null || label.isEmpty() || "ALL".equals(label)) {
            return "";
        }
        return "_" + label.replaceAll("[^A-Za-z0-9_]", "");
    }
}
