package org.PSmart.POS.ui.MainView.ui.stock.current;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.RoleType;
import org.PSmart.POS.Entities.StockMovement;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Repositories.TxnService;
import org.PSmart.POS.Utils.Components.forms.FormFrame;
import org.PSmart.POS.Utils.Components.forms.TxnUi;

import java.util.UUID;

/** One stock adjustment / stock-take line: details, and cancel it (puts the change back). */
public final class StockMovementViewForm {

    private StockMovementViewForm() {}

    public static void open(UUID id, User user, Runnable onChanged) {
        FormFrame f = new FormFrame("Stock entry", "Stock entry", 620);
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        StockMovement m;
        String itemName = "";
        try {
            m = em.find(StockMovement.class, id);
            if (m != null && m.getItem() != null) itemName = m.getItem().getName();
        } finally { em.close(); }
        if (m == null) {
            f.note("This entry could not be found.");
            f.secondary("Close", f::dispose);
            f.open();
            return;
        }
        double d = m.getQuantityDelta() == null ? 0 : m.getQuantityDelta();
        f.info("Type", String.valueOf(m.getType()).replace("_", " ").toLowerCase());
        f.info("Product", itemName);
        f.info("Date", TxnUi.when(m.getCreatedAt()));
        f.info("Change", (d > 0 ? "+" : "") + trim(d) + "   (before " + trim(m.getQuantityBefore()) + ", after " + trim(m.getQuantityAfter()) + ")");
        f.info("Reason", m.getReason() == null ? "-" : m.getReason());
        f.info("By", m.getCreatedByName() == null ? "-" : m.getCreatedByName());
        if (m.getReference() != null) f.info("Stock-take ref", m.getReference());
        if (m.getNotes() != null) f.info("Notes", m.getNotes());
        if (m.isDeleted()) {
            f.note("This entry has been cancelled.");
            f.secondary("Close", f::dispose);
            f.open();
            return;
        }
        TxnUi.cancelFlow(f, user, RoleType.STOCK, "entry", "The stock count goes back to what it was before this entry.",
                (reason, by) -> TxnService.cancelMovement(id, reason, by), onChanged);
        f.secondary("Close", f::dispose);
        f.open();
    }

    private static String trim(Double v) {
        if (v == null) return "-";
        return v == Math.floor(v) ? String.format("%.0f", v) : String.format("%.2f", v);
    }
}
