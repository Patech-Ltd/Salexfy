package com.patechltd.salexfypos.util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class NumberUtil {

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00");
    private static final DecimalFormat QTY = new DecimalFormat("#,##0.###");

    private NumberUtil() {
    }

    public static String money(double value) {
        return MONEY.format(value);
    }

    public static String money(double value, String currency) {
        return currency + " " + MONEY.format(value);
    }

    public static String qty(double value) {
        if (value == Math.rint(value)) {
            return String.format(Locale.US, "%d", (long) value);
        }
        return QTY.format(value);
    }

    public static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public static double parse(String s, double def) {
        if (s == null) return def;
        try {
            return Double.parseDouble(s.trim().replace(",", ""));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    public static String percent(double value) {
        return new DecimalFormat("0.##").format(value) + "%";
    }
}
