package com.patechltd.salexfypos.model;

public enum SaleStatus {
    DRAFT("Draft"),
    HELD("Held"),
    COMPLETE("Complete"),
    VOID("Void");

    private final String label;

    SaleStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
