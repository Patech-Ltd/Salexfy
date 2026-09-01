package com.patechltd.salexfypos.util;

import com.patechltd.salexfypos.db.entity.PaymentMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread-safe shared cache of the configured payment methods (from the
 * payment_methods table). Backed by static defaults so labels resolve
 * correctly even before the database has been loaded.
 */
public final class PaymentMethods {

    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();
    private static final List<PaymentMethod> DEFAULT_LIST = new ArrayList<>();

    static {
        addDefault("CASH", "Cash", false);
        addDefault("MPESA", "M-Pesa", false);
        addDefault("CREDIT", "On Credit", true);
        addDefault("AIRTEL", "Airtel Money", false);
        addDefault("TIGO", "Tigo Pesa", false);
        addDefault("MTN", "MTN MoMo", false);
        addDefault("ORANGE", "Orange Money", false);
        addDefault("HALOPESA", "Halopesa", false);
        addDefault("CARD", "Card", false);
        addDefault("BANK", "Bank Transfer", false);
        addDefault("CHEQUE", "Cheque", false);
        addDefault("VOUCHER", "Gift Voucher", false);
    }

    private static void addDefault(String id, String label, boolean credit) {
        DEFAULTS.put(id, label);
        PaymentMethod m = new PaymentMethod();
        m.id = id;
        m.name = label;
        m.isCredit = credit;
        m.isSystem = "CASH".equals(id) || "CREDIT".equals(id);
        m.active = "CASH".equals(id) || "MPESA".equals(id) || "CREDIT".equals(id);
        m.sortOrder = DEFAULT_LIST.size();
        DEFAULT_LIST.add(m);
    }

    private static volatile Map<String, PaymentMethod> CACHE = new HashMap<>();
    private static volatile List<PaymentMethod> ACTIVE_LIST = DEFAULT_LIST;

    private PaymentMethods() {
    }

    /** Replace the cached list with fresh values from the database. */
    public static void refresh(List<PaymentMethod> all) {
        if (all == null) return;
        if (all.isEmpty()) {
            CACHE = new HashMap<>();
            ACTIVE_LIST = DEFAULT_LIST;
            return;
        }
        Map<String, PaymentMethod> next = new HashMap<>();
        List<PaymentMethod> active = new ArrayList<>();
        for (PaymentMethod m : all) {
            if (m.id == null || m.id.isEmpty()) continue;
            next.put(m.id, m);
            if (m.active) active.add(m);
        }
        if (active.isEmpty()) {
            for (PaymentMethod m : all) active.add(m);
        }
        CACHE = next;
        ACTIVE_LIST = active;
    }

    /** Display label for a stored method id (falls back to defaults / the id). */
    public static String labelOf(String id) {
        if (id == null || id.isEmpty()) return "";
        PaymentMethod m = CACHE.get(id);
        if (m != null && m.name != null && !m.name.isEmpty()) return m.name;
        String d = DEFAULTS.get(id);
        return d != null ? d : id;
    }

    public static boolean isCredit(String id) {
        if (id == null) return false;
        PaymentMethod m = CACHE.get(id);
        if (m != null) return m.isCredit;
        PaymentMethod def = findDefault(id);
        return def != null && def.isCredit;
    }

    public static PaymentMethod find(String id) {
        if (id == null) return null;
        PaymentMethod m = CACHE.get(id);
        if (m != null) return m;
        return findDefault(id);
    }

    private static PaymentMethod findDefault(String id) {
        for (PaymentMethod m : DEFAULT_LIST) {
            if (m.id.equals(id)) return m;
        }
        return null;
    }

    /** Active methods to show in payment screens (cached copy). */
    public static List<PaymentMethod> active() {
        List<PaymentMethod> copy = new ArrayList<>(ACTIVE_LIST);
        if (copy.isEmpty()) copy = DEFAULT_LIST;
        return Collections.unmodifiableList(copy);
    }
}