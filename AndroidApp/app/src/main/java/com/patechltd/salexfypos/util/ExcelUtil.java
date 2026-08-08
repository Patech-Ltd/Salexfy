package com.patechltd.salexfypos.util;

import android.content.Context;
import android.net.Uri;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.OutputStream;
import java.util.List;

public class ExcelUtil {

    private ExcelUtil() {
    }

    public interface RowWriter {
        Object[] header();

        Object[] row(Object item, int index);
    }

    public static boolean export(Context context, Uri target, String sheetName, List<?> data, RowWriter writer) {
        Workbook wb = new HSSFWorkbook();
        writeSheet(wb, sheetName, data, writer);
        return writeTo(wb, context, target);
    }

    public static class Section {
        public final String name;
        public final List<?> data;
        public final RowWriter writer;

        public Section(String name, List<?> data, RowWriter writer) {
            this.name = name;
            this.data = data;
            this.writer = writer;
        }
    }

    public static boolean exportMulti(Context context, Uri target, List<Section> sections) {
        Workbook wb = new HSSFWorkbook();
        for (Section s : sections) {
            writeSheet(wb, s.name, s.data, s.writer);
        }
        return writeTo(wb, context, target);
    }

    private static void writeSheet(Workbook wb, String sheetName, List<?> data, RowWriter writer) {
        Sheet sheet = wb.createSheet(sheetName);

        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.DARK_TEAL.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Object[] header = writer.header();
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < header.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(String.valueOf(header[i]));
            cell.setCellStyle(headerStyle);
        }

        int r = 1;
        for (Object item : data) {
            Object[] values = writer.row(item, r - 1);
            if (values == null) continue;
            Row row = sheet.createRow(r++);
            for (int i = 0; i < values.length; i++) {
                Object v = values[i];
                Cell cell = row.createCell(i);
                if (v instanceof Number) {
                    cell.setCellValue(((Number) v).doubleValue());
                } else {
                    cell.setCellValue(v == null ? "" : String.valueOf(v));
                }
            }
        }

        for (int i = 0; i < header.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static boolean writeTo(Workbook wb, Context context, Uri target) {
        try (OutputStream os = context.getContentResolver().openOutputStream(target)) {
            if (os == null) return false;
            wb.write(os);
            wb.close();
            return true;
        } catch (Exception e) {
            AppLogger.e("Excel export failed", e);
            return false;
        }
    }
}
