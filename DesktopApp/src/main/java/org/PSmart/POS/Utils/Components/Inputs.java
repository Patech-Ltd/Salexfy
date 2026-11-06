package org.PSmart.POS.Utils.Components;

import org.jdatepicker.JDatePicker;
import org.jdatepicker.UtilDateModel;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * Input helpers: number boxes that only accept digits (and one decimal point), and a calendar date picker.
 */
public final class Inputs {

    private Inputs() {}

    private static final class NumberFilter extends DocumentFilter {
        private final boolean decimal;

        NumberFilter(boolean decimal) { this.decimal = decimal; }

        private String clean(String in) {
            if (in == null) return "";
            return in.replace(",", "").replace(" ", "");
        }

        private boolean ok(String s) {
            return decimal ? s.matches("\\d*\\.?\\d*") : s.matches("\\d*");
        }

        @Override public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
            replace(fb, offset, 0, text, attr);
        }

        @Override public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
            String before = cur.substring(0, offset), after = cur.substring(offset + length);
            // keep only the characters that make sense (so pasting "1,200.50 KES" still gives 1200.50)
            StringBuilder keep = new StringBuilder();
            boolean hasDot = before.indexOf('.') >= 0 || after.indexOf('.') >= 0;
            for (char ch : clean(text).toCharArray()) {
                if (Character.isDigit(ch)) keep.append(ch);
                else if (decimal && ch == '.' && !hasDot) { keep.append(ch); hasDot = true; }
            }
            if (keep.length() == 0 && text != null && !text.isEmpty()) { Toolkit.getDefaultToolkit().beep(); return; }
            super.replace(fb, offset, length, keep.toString(), attrs);
        }
    }

    /** Only digits and one decimal point (commas pasted in are dropped). */
    public static <T extends JTextField> T decimal(T field) {
        ((PlainDocument) field.getDocument()).setDocumentFilter(new NumberFilter(true));
        return field;
    }

    /** Only whole numbers. */
    public static <T extends JTextField> T integer(T field) {
        ((PlainDocument) field.getDocument()).setDocumentFilter(new NumberFilter(false));
        return field;
    }

    public static PTextField decimal(PTextField f) { decimal(f.getField()); return f; }

    public static PTextField integer(PTextField f) { integer(f.getField()); return f; }

    /** Table editor for decimal cells. */
    public static TableCellEditor decimalCellEditor(int fontSize) {
        JTextField f = new JTextField();
        f.setHorizontalAlignment(JTextField.RIGHT);
        f.setFont(new Font("Courier", Font.PLAIN, fontSize));
        decimal(f);
        DefaultCellEditor e = new DefaultCellEditor(f);
        e.setClickCountToStart(1);
        return e;
    }

    // ------------------------------------------------------------------ dates

    /** Calendar date picker (jdatepicker). Empty when {@code initial} is null. */
    public static JDatePicker datePicker(LocalDate initial) {
        UtilDateModel model = new UtilDateModel();
        if (initial != null) {
            model.setValue(Date.from(initial.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        }
        JDatePicker p = new JDatePicker(model, "dd-MMM-yyyy");
        p.setShowYearButtons(true);
        p.getFormattedTextField().setFont(new Font("Courier", Font.PLAIN, 15));
        return p;
    }

    /** Selected day, or null when empty. */
    public static LocalDate dateOf(JDatePicker p) {
        Object v = p.getModel().getValue();
        if (!(v instanceof Date)) return null;
        return ((Date) v).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static void setDate(JDatePicker p, LocalDate d) {
        @SuppressWarnings("unchecked")
        org.jdatepicker.DateModel<Date> m = (org.jdatepicker.DateModel<Date>) p.getModel();
        m.setValue(d == null ? null : Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    /** Table cell editor that opens a calendar; the cell value is "dd-MM-yyyy" or "" (cleared). */
    public static TableCellEditor dateCellEditor() {
        return new TableCellEditorImpl(datePicker(null), java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"));
    }

    private static final class TableCellEditorImpl extends AbstractCellEditor implements TableCellEditor {
        private final JDatePicker picker;
        private final java.time.format.DateTimeFormatter fmt;

        TableCellEditorImpl(JDatePicker picker, java.time.format.DateTimeFormatter fmt) {
            this.picker = picker;
            this.fmt = fmt;
            picker.addActionListener(e -> stopCellEditing());
        }

        @Override public Object getCellEditorValue() {
            LocalDate d = dateOf(picker);
            return d == null ? "" : d.format(fmt);
        }

        @Override public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int r, int c) {
            LocalDate d = null;
            try { if (v != null && !v.toString().isBlank()) d = LocalDate.parse(v.toString().trim(), fmt); } catch (Exception ignored) { }
            setDate(picker, d);
            return picker;
        }
    }
}
