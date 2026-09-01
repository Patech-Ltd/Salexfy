package com.patechltd.salexfypos.util;

/**
 * Turnover tax (TOT) helpers. TOT is charged as a flat percentage of total
 * sales (turnover) — never on profit.
 */
public final class TaxUtil {

    public static final double TOT_RATE = 1.5;

    private TaxUtil() {
    }

    public static double tot(double totalSales) {
        return NumberUtil.round2(totalSales * TOT_RATE / 100.0);
    }
}