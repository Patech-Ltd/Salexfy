package org.PSmart.POS.Utils.Components.forms;

import org.PSmart.POS.Entities.RoleType;
import org.PSmart.POS.Entities.User;
import org.PSmart.POS.Utils.Components.PTextField;
import org.PSmart.POS.Utils.SupervisorApproval;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.BiFunction;

/** Shared pieces of the transaction detail forms: permission check, two-step cancel, status lines. */
public final class TxnUi {

    private TxnUi() {}

    /**
     * Permission check for an edit/cancel. Returns the name to record ("Grace" or "Grace (approved by Mary)"),
     * or null after showing an inline message when nobody approved.
     */
    public static String authorise(FormFrame f, User user, RoleType area, String verb) {
        User approver = SupervisorApproval.require(f, user, area, verb);
        if (approver == null) {
            f.error("You need a supervisor's approval to " + verb + ".");
            return null;
        }
        return SupervisorApproval.credit(user, approver);
    }

    /**
     * Adds the "Cancel ..." section + red button. First press opens the reason box, second press confirms.
     * {@code run} gets (reason, recordedName) and returns null on success or an error message.
     */
    public static JButton cancelFlow(FormFrame f, User user, RoleType area, String noun, String warning,
                                     BiFunction<String, String, String> run, Runnable done) {
        FormFrame.Section sec = f.section("Cancel this " + noun, false);
        PTextField reason = sec.field("Reason for cancelling (required)", "");
        f.errorLine();
        final boolean[] armed = {false};
        final JButton[] btn = new JButton[1];
        btn[0] = f.danger("Cancel " + cap(noun), () -> {
            if (!armed[0]) {
                armed[0] = true;
                sec.setOpen(true);
                btn[0].setText("Confirm Cancel");
                f.error(warning + " Enter the reason, then press Confirm Cancel.");
                reason.getField().requestFocusInWindow();
                return;
            }
            if (reason.getText().trim().length() < 3) { f.error("Enter a reason for cancelling (at least 3 letters)."); return; }
            String by = authorise(f, user, area, "cancel this " + noun);
            if (by == null) return;
            String err = run.apply(reason.getText().trim(), by);
            if (err != null) { f.error(err); return; }
            Ui.toast(cap(noun) + " cancelled");
            f.dispose();
            if (done != null) done.run();
        });
        return btn[0];
    }

    /** Red line "CANCELLED date by X - reason" for a cancelled record. */
    public static void cancelledBanner(FormFrame f, Long at, String by, String reason) {
        JLabel l = f.info("CANCELLED", (at == null ? "" : new SimpleDateFormat("dd-MMM-yyyy HH:mm").format(new Date(at)))
                + (by == null ? "" : " by " + by) + (reason == null ? "" : " - " + reason));
        l.setForeground(Color.RED);
    }

    public static void editedLine(FormFrame f, Long at, String by) {
        if (at == null) return;
        f.note("Last edited " + new SimpleDateFormat("dd-MMM-yyyy HH:mm").format(new Date(at)) + (by == null ? "" : " by " + by));
    }

    public static String money(Double v) { return String.format("%,.2f", v == null ? 0.0 : v); }

    public static String when(Long t) {
        return t == null || t == 0 ? "-" : new SimpleDateFormat("dd-MMM-yyyy HH:mm").format(new Date(t));
    }

    private static String cap(String s) { return s.substring(0, 1).toUpperCase() + s.substring(1); }
}
