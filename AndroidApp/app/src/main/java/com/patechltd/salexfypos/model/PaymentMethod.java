package com.patechltd.salexfypos.model;

import com.patechltd.salexfypos.util.PaymentMethods;

/**
 * Payment method identifiers (stable ids stored on sales / sale_payments)
 * and lookups. The actual user-facing list is configurable in Settings and
 * lives in the payment_methods table; this class only exposes the special
 * system ids and label resolution.
 */
public final class PaymentMethod {

    public static final String CASH = "CASH";
    public static final String MPESA = "MPESA";
    public static final String CREDIT = "CREDIT";

    private PaymentMethod() {
    }

    /** Display label for a stored method id. Falls back to the raw id. */
    public static String labelOf(String id) {
        return PaymentMethods.labelOf(id);
    }

    /** True when the method records a credit balance (on-credit) sale. */
    public static boolean isCredit(String id) {
        return PaymentMethods.isCredit(id);
    }
}