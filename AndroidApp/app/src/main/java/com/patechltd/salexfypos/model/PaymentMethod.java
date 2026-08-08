package com.patechltd.salexfypos.model;

public enum PaymentMethod {
    CASH("Cash"),
    MPESA("M-Pesa"),
    CARD("Card"),
    BANK("Bank Transfer"),
    CREDIT("On Credit");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
