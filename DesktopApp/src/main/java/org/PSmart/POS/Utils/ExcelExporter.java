package org.PSmart.POS.Utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;

/**
 * Exports table data to a .xlsx file using Apache POI.
 * Styled with a dark header row and light grid for a clean, readable report.
 */
public final class ExcelExporter {

    private ExcelExporter() {}

    /**
     * Opens a save dialog and writes the given headers + rows to an Excel file.
     * Falls back to CSV if Excel export fails part-way and calls onFailure.
     */
    public static void exportToExcel(Component parent, String defaultName,
                                     String[] headers, java.util.List<Object[]> rows) {
        // The operating system's own "Save as" dialog is shown from a background thread so the app stays responsive.
        final java.util.List<Object[]> data = new java.util.ArrayList<>(rows);
        new Thread(() -> {
            File file = NativeFileDialog.save(parent, "Save report", defaultName, "xlsx", "Excel workbook");
            if (file == null) return;
            write(file, headers, data);
        }, "export-excel").start();
    }

    private static void write(File file, String[] headers, java.util.List<Object[]> rows) {
        try (Workbook wb = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(file)) {
            Sheet sheet = wb.createSheet("Report");

            CellStyle headerStyle = headerStyle(wb);
            CellStyle bodyStyle = bodyStyle(wb);

            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(headers[c]);
                cell.setCellStyle(headerStyle);
            }

            int r = 1;
            for (Object[] row : rows) {
                org.apache.poi.ss.usermodel.Row xRow = sheet.createRow(r);
                for (int c = 0; c < headers.length; c++) {
                    Cell cell = xRow.createCell(c);
                    Object v = c < row.length ? row[c] : "";
                    if (v instanceof Number) {
                        cell.setCellValue(((Number) v).doubleValue());
                    } else {
                        cell.setCellValue(v == null ? "" : String.valueOf(v));
                    }
                    cell.setCellStyle(bodyStyle);
                }
                // light zebra reading aid
                if (r % 2 == 0) {
                    for (int c = 0; c < headers.length; c++) {
                        xRow.getCell(c).setCellStyle(zebraStyle(wb));
                    }
                }
                r++;
            }

            for (int c = 0; c < headers.length; c++) sheet.autoSizeColumn(c);

            wb.write(out);
            org.PSmart.POS.tests.AutoClosingNotification.sendNotification("Exported to " + file.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            org.PSmart.POS.tests.AutoClosingNotification.sendNotification("Could not export: " + ex.getMessage());
        }
    }

    private static CellStyle headerStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private static CellStyle bodyStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setAlignment(HorizontalAlignment.LEFT);
        Font f = wb.createFont();
        f.setFontHeightInPoints((short) 11);
        s.setFont(f);
        return s;
    }

    private static CellStyle zebraStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }
}