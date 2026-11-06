package org.PSmart.POS.ui.MainView.ui.notifications;

import org.PSmart.POS.Utils.AlertService;
import org.PSmart.POS.Utils.AlertService.Alert;
import org.PSmart.POS.Utils.AlertService.Level;
import org.PSmart.POS.Utils.BackupService;
import org.PSmart.POS.Utils.Components.forms.Ui;
import org.PSmart.POS.Utils.MyUtils;
import org.PSmart.POS.ui.MainView.DashBoard;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/** Notifications: what needs attention (stock, expiry, backups, security, who owes whom), most urgent first. */
public class NotificationsPanel extends JPanel {

    private static final Color RED = new Color(200, 30, 45), AMBER = new Color(214, 137, 16), BLUE = new Color(25, 118, 211), GREEN = new Color(30, 110, 50);

    private final DashBoard dash;
    private final JPanel list = new JPanel();
    private final JLabel summary = new JLabel(" ");
    private final JLabel status = new JLabel(" ");

    public NotificationsPanel() { this(null); }

    public NotificationsPanel(DashBoard dash) {
        super(new BorderLayout());
        this.dash = dash;
        setBackground(Ui.PAGE);

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Ui.SLATE);
        bar.setPreferredSize(new Dimension(0, 60));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 16));
        JLabel title = new JLabel("Notifications");
        title.setFont(MyUtils.courierFont(22));
        title.setForeground(Color.WHITE);
        bar.add(title, BorderLayout.WEST);
        summary.setFont(new Font("Courier", Font.BOLD, 15));
        summary.setForeground(Color.WHITE);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 12));
        right.setOpaque(false);
        right.add(summary);
        JButton refresh = Ui.primary("Refresh");
        refresh.addActionListener(e -> load());
        right.add(refresh);
        bar.add(right, BorderLayout.EAST);
        add(bar, BorderLayout.NORTH);

        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(Ui.PAGE);
        list.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        JScrollPane sp = new JScrollPane(list);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getViewport().setBackground(Ui.PAGE);
        add(sp, BorderLayout.CENTER);

        status.setFont(new Font("Courier", Font.BOLD, 13));
        status.setBorder(BorderFactory.createEmptyBorder(4, 20, 6, 20));
        add(status, BorderLayout.SOUTH);
        load();
    }

    private void load() {
        status.setForeground(Ui.SLATE);
        status.setText("Checking your shop...");
        new SwingWorker<List<Alert>, Void>() {
            @Override protected List<Alert> doInBackground() { return AlertService.compute(); }
            @Override protected void done() {
                try {
                    render(get());
                    status.setText("Updated " + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
                } catch (Exception ex) {
                    status.setForeground(Color.RED);
                    status.setText("Could not check: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void render(List<Alert> alerts) {
        list.removeAll();
        int crit = 0, warn = 0;
        for (Alert a : alerts) {
            if (a.level == Level.CRITICAL) crit++;
            if (a.level == Level.WARNING) warn++;
            list.add(card(a));
            list.add(Box.createVerticalStrut(10));
        }
        summary.setText(crit + " urgent   " + warn + " to review");
        list.revalidate();
        list.repaint();
    }

    private Color colorOf(Level l) {
        switch (l) {
            case CRITICAL: return RED;
            case WARNING: return AMBER;
            case OK: return GREEN;
            default: return BLUE;
        }
    }

    private JPanel card(Alert a) {
        Color c = colorOf(a.level);
        JPanel card = new JPanel(new BorderLayout(14, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 6, 0, 0, c),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel tag = new JLabel(a.level == Level.OK ? "ALL CLEAR" : a.category.toUpperCase() + (a.level == Level.CRITICAL ? "  -  URGENT" : a.level == Level.WARNING ? "  -  REVIEW" : ""));
        tag.setFont(new Font("Courier", Font.BOLD, 11));
        tag.setForeground(c);
        JLabel t = new JLabel(a.title);
        t.setFont(new Font("Courier", Font.BOLD, 17));
        t.setForeground(new Color(40, 40, 40));
        JLabel d = new JLabel("<html>" + a.detail.replace("&", "&amp;").replace("<", "&lt;") + "</html>");
        d.setFont(new Font("Courier", Font.PLAIN, 13));
        d.setForeground(new Color(90, 96, 104));
        text.add(tag);
        text.add(Box.createVerticalStrut(2));
        text.add(t);
        text.add(Box.createVerticalStrut(3));
        text.add(d);
        card.add(text, BorderLayout.CENTER);

        if (a.actionLabel != null) {
            JButton go = Ui.button(a.actionLabel, a.level == Level.CRITICAL ? RED : Ui.BLUE);
            go.setPreferredSize(new Dimension(170, 38));
            go.addActionListener(e -> act(a));
            JPanel east = new JPanel(new GridBagLayout());
            east.setOpaque(false);
            east.add(go);
            card.add(east, BorderLayout.EAST);
        }
        return card;
    }

    private void act(Alert a) {
        if (a.backupNow) {
            status.setForeground(Ui.SLATE);
            status.setText("Backing up...");
            new SwingWorker<Void, Void>() {
                @Override protected Void doInBackground() throws Exception {
                    org.PSmart.POS.Entities.StoreConfig cfg = org.PSmart.POS.Database.DatabaseUtils.getInstance().getStoreConfigRepository().getStoreConfig();
                    int keep = cfg != null && cfg.getBackupKeepCount() != null ? cfg.getBackupKeepCount() : 10;
                    BackupService.createBackupAndRotate(keep);
                    if (cfg != null) {
                        cfg.setLastBackupAt(System.currentTimeMillis());
                        org.PSmart.POS.Database.DatabaseUtils.getInstance().getStoreConfigRepository().saveOrUpdate(cfg);
                    }
                    return null;
                }
                @Override protected void done() {
                    try { get(); Ui.toast("Backup created"); } catch (Exception ex) { Ui.toast("Backup failed: " + ex.getMessage()); }
                    load();
                }
            }.execute();
        } else if (a.tab >= 0 && dash != null) {
            dash.openTab(a.tab);
        }
    }
}
