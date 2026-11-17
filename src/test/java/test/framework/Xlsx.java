package test.framework;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Small styling/report helpers shared by the two workbook writers.
 */
final class Xlsx {

    private Xlsx() {
    }

    static CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    static CellStyle passStyle(Workbook wb) {
        return colored(wb, IndexedColors.LIGHT_GREEN);
    }

    static CellStyle failStyle(Workbook wb) {
        CellStyle style = colored(wb, IndexedColors.ROSE);
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.DARK_RED.getIndex());
        style.setFont(font);
        return style;
    }

    static CellStyle skipStyle(Workbook wb) {
        return colored(wb, IndexedColors.LIGHT_YELLOW);
    }

    private static CellStyle colored(Workbook wb, IndexedColors color) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    static void headerRow(Sheet sheet, int rowIndex, String... titles) {
        Workbook wb = sheet.getWorkbook();
        CellStyle style = headerStyle(wb);
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < titles.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(titles[i]);
            cell.setCellStyle(style);
        }
    }

    static void autoWidth(Sheet sheet, int columns, int maxWidth) {
        int[] widths = new int[columns];
        for (Row row : sheet) {
            for (int i = 0; i < columns; i++) {
                Cell cell = row.getCell(i);
                if (cell != null) {
                    widths[i] = Math.max(widths[i], cell.toString().length());
                }
            }
        }
        for (int i = 0; i < columns; i++) {
            int width = Math.min(maxWidth, (widths[i] + 2) * 256);
            sheet.setColumnWidth(i, Math.max(width, 10 * 256));
        }
    }

    static Workbook newWorkbook() {
        return new XSSFWorkbook();
    }
}
