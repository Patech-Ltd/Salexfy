package com.patechltd.salexfypos.sync;

import com.patechltd.salexfypos.db.dao.SyncDao;
import com.patechltd.salexfypos.db.entity.SyncChange;

public final class SyncTracker {

    public static final String PRODUCT = "PRODUCT";
    public static final String PRODUCT_BARCODE = "PRODUCT_BARCODE";
    public static final String CATEGORY = "CATEGORY";
    public static final String BRAND = "BRAND";
    public static final String UNIT = "UNIT";
    public static final String SUPPLIER = "SUPPLIER";
    public static final String CUSTOMER = "CUSTOMER";
    public static final String DEBT_PAYMENT = "DEBT_PAYMENT";
    public static final String PURCHASE = "PURCHASE";
    public static final String PURCHASE_ITEM = "PURCHASE_ITEM";
    public static final String SALE = "SALE";
    public static final String SALE_ITEM = "SALE_ITEM";
    public static final String SALE_PAYMENT = "SALE_PAYMENT";
    public static final String STOCK_MOVEMENT = "STOCK_MOVEMENT";
    public static final String STOCK_TAKE = "STOCK_TAKE";
    public static final String USER = "USER";
    public static final String ROLE = "ROLE";
    public static final String EXPENSE = "EXPENSE";

    private static volatile SyncDao syncDao;
    private static final ThreadLocal<Boolean> MUTED = new ThreadLocal<>();

    private SyncTracker() {
    }

    public static void attach(SyncDao dao) {
        syncDao = dao;
    }

    public static void mute() {
        MUTED.set(Boolean.TRUE);
    }

    public static void unmute() {
        MUTED.set(Boolean.FALSE);
    }

    public static void track(String entityType, String recordId, String operation, String payload) {
        if (syncDao == null) return;
        Boolean muted = MUTED.get();
        if (muted != null && muted) return;
        try {
            SyncChange change = new SyncChange();
            change.entityType = entityType;
            change.recordId = recordId;
            change.operation = operation;
            change.payload = payload == null ? "" : payload;
            change.synced = false;
            long now = System.currentTimeMillis();
            change.createdAt = now;
            change.updatedAt = now;
            syncDao.insertChange(change);
        } catch (Throwable ignored) {
        }
    }
}
