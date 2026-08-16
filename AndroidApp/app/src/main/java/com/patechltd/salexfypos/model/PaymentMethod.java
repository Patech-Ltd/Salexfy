package com.patechltd.salexfypos.model;

public enum PaymentMethod {
    CASH("Cash"),
    MPESA("M-Pesa"),
    AIRTEL_MONEY("Airtel Money"),
    TIGO_PESA("Tigo Pesa"),
    MTN_MOMO("MTN MoMo"),
    ORANGE_MONEY("Orange Money"),
    HALOPESA("Halopesa"),
    CARD("Card"),
    BANK("Bank Transfer"),
    CHEQUE("Cheque"),
    MOBILE_PAY("Google Pay / Apple Pay"),
    PAYPAL("PayPal"),
    CRYPTO("Crypto"),
    VOUCHER("Gift Voucher"),
    CREDIT("On Credit");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static String labelOf(String name) {
        if (name == null || name.isEmpty()) return "Cash";
        for (PaymentMethod m : values()) {
            if (m.name().equals(name)) return m.label;
        }
        return name;
    }
}
