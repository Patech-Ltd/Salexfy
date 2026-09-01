package com.patechltd.salexfypos.util;

import android.content.Context;
import android.net.Uri;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ExcelUtil {

    private ExcelUtil() {
    }

    public interface RowWriter {
        Object[] header();

        Object[] row(Object item, int index);
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

    public static boolean export(Context context, Uri target, String sheetName, List<?> data, RowWriter writer) {
        return exportMulti(context, target, Collections.singletonList(new Section(sheetName, data, writer)));
    }

    public static boolean exportMulti(Context context, Uri target, List<Section> sections) {
        try (OutputStream out = context.getContentResolver().openOutputStream(target)) {
            if (out == null) return false;
            writeXlsx(out, sections);
            return true;
        } catch (Exception e) {
            AppLogger.e("Excel export failed", e);
            return false;
        }
    }

    private static void writeXlsx(OutputStream out, List<Section> sections) throws Exception {
        ZipOutputStream zip = new ZipOutputStream(out);

        zip.putNextEntry(new ZipEntry("[Content_Types].xml"));
        zip.write(contentTypes(sections.size()));
        zip.closeEntry();

        zip.putNextEntry(new ZipEntry("_rels/.rels"));
        zip.write(rootRels());
        zip.closeEntry();

        zip.putNextEntry(new ZipEntry("xl/workbook.xml"));
        zip.write(workbookXml(sections));
        zip.closeEntry();

        zip.putNextEntry(new ZipEntry("xl/_rels/workbook.xml.rels"));
        zip.write(workbookRels(sections.size()));
        zip.closeEntry();

        for (int i = 0; i < sections.size(); i++) {
            zip.putNextEntry(new ZipEntry("xl/worksheets/sheet" + (i + 1) + ".xml"));
            zip.write(sheetXml(sections.get(i)));
            zip.closeEntry();
        }

        zip.finish();
    }

    private static byte[] contentTypes(int count) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
                .append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
                .append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                .append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
        for (int i = 1; i <= count; i++) {
            sb.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        sb.append("</Types>");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] rootRels() {
        return ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
                + "</Relationships>")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] workbookXml(List<Section> sections) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
        for (int i = 0; i < sections.size(); i++) {
            sb.append("<sheet name=\"").append(xmlEscape(sanitizeSheetName(sections.get(i).name)))
                    .append("\" sheetId=\"").append(i + 1)
                    .append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        sb.append("</sheets></workbook>");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] workbookRels(int count) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
        for (int i = 1; i <= count; i++) {
            sb.append("<Relationship Id=\"rId").append(i)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(i).append(".xml\"/>");
        }
        sb.append("</Relationships>");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] sheetXml(Section section) {
        RowWriter writer = section.writer;
        List<?> data = section.data;
        StringBuilder sb = new StringBuilder(data.size() * 48 + 256);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
                .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");

        Object[] header = writer.header();
        if (header != null && header.length > 0) {
            appendRow(sb, header, 1);
        }

        int r = 2;
        int index = 0;
        for (Object item : data) {
            Object[] values = writer.row(item, index++);
            if (values == null || values.length == 0) continue;
            appendRow(sb, values, r++);
        }

        sb.append("</sheetData></worksheet>");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendRow(StringBuilder sb, Object[] values, int row) {
        sb.append("<row r=\"").append(row).append("\">");
        for (int i = 0; i < values.length; i++) {
            String ref = colName(i) + row;
            Object v = values[i];
            if (v instanceof Number) {
                sb.append("<c r=\"").append(ref).append("\"><v>").append(number((Number) v)).append("</v></c>");
            } else {
                String s = v == null ? "" : String.valueOf(v);
                sb.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
                        .append(xmlEscape(s)).append("</t></is></c>");
            }
        }
        sb.append("</row>");
    }

    private static String number(Number n) {
        double d = n.doubleValue();
        if (d == Math.rint(d) && Math.abs(d) < 1e15) {
            return String.valueOf((long) d);
        }
        return String.valueOf(d);
    }

    private static String colName(int index) {
        StringBuilder sb = new StringBuilder();
        index++;
        while (index > 0) {
            int rem = (index - 1) % 26;
            sb.insert(0, (char) ('A' + rem));
            index = (index - 1) / 26;
        }
        return sb.toString();
    }

    private static String xmlEscape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&apos;");
                    break;
                default:
                    if (c >= 0x20 || c == '\t' || c == '\n' || c == '\r') {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private static String sanitizeSheetName(String name) {
        if (name == null) return "Sheet";
        String s = name.replaceAll("[\\\\/*?:\\[\\]]", "").trim();
        if (s.isEmpty()) s = "Sheet";
        if (s.length() > 31) s = s.substring(0, 31);
        return s;
    }
}