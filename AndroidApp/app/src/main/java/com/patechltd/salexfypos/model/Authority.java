package com.patechltd.salexfypos.model;

public enum Authority {
    DASHBOARD_VIEW("View dashboard"),
    SELL_VIEW("Use the selling screen"),
    SALE_HOLD("Hold & recall transactions"),
    SALE_VOID("Void completed sales"),
    PRODUCT_VIEW("View products"),
    PRODUCT_EDIT("Add / edit / delete products"),
    CATEGORY_EDIT("Manage categories & brands"),
    STOCK_VIEW("View stock levels"),
    STOCK_TAKE("Perform stock taking"),
    PURCHASE_VIEW("View purchases"),
    PURCHASE_EDIT("Add / edit purchases"),
    SUPPLIER_VIEW("View suppliers"),
    SUPPLIER_EDIT("Add / edit suppliers"),
    CUSTOMER_VIEW("View customers / debtors"),
    CUSTOMER_EDIT("Add / edit customers"),
    CUSTOMER_PAYMENT("Record customer payments"),
    REPORT_VIEW("View reports & export Excel"),
    USER_VIEW("View users"),
    USER_EDIT("Manage users & roles"),
    SETTINGS_EDIT("Change settings"),
    BACKUP_MANAGE("Manage backups & restore"),
    DEBUG_VIEW("View crash logs / debug mode"),
    COMMISSION_VIEW("View commission report");

    private final String label;

    Authority(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
