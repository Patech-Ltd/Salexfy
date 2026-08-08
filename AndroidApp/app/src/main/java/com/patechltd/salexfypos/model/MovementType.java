package com.patechltd.salexfypos.model;

public enum MovementType {
    PURCHASE("Purchase"),
    SALE("Sale"),
    SALE_VOID("Sale Voided"),
    ADJUSTMENT("Adjustment"),
    STOCK_TAKE("Stock Take"),
    RETURN("Return"),
    OPENING_STOCK("Opening Stock");

    private final String label;

    MovementType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
