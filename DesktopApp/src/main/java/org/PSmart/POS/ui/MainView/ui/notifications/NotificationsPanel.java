package org.PSmart.POS.ui.MainView.ui.notifications;

import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.StockFeedItem;
import org.PSmart.POS.Repositories.SaleRepository;
import org.PSmart.POS.Utils.Palette;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Professional notification centre: daily sales summary + live stock alerts
 * (low stock vs reorder level, expired batches, expiring soon). Built on null
 * layout so bounds are set explicitly for full control.
 */
public class NotificationsPanel extends JPanel {

    private JPanel cards;
    private JLabel alertCountLabel;
    private java.util.List<Alert> alerts = new java.util.ArrayList<>();

    public NotificationsPanel() {
        super();
        draw();
    }

    private void draw() {
        setLayout(null);
        setBackground(Palette.BG);

        buildHeader();
        buildAlerts();

        revalidate();
        repaint();
    }

    private void buildHeader() {
        JPanel header = new JPanel(null);
        header.setBackground(Palette.HEADER);
        header.setBounds(0, 0, getWidth(), 60);

        JLabel title = new JLabel("Notifications Centre");
        title.setFont(Palette.courier(22));
        title.setForeground(Color.WHITE);
        title.setBounds(20, 0, 320, 60);

        alertCountLabel = new JLabel("", SwingConstants.RIGHT);
        alertCountLabel.setFont(Palette.courier(15));
        alertCountLabel.setForeground(new Color(255, 193, 7));
        alertCountLabel.setBounds(getWidth() - 360, 0, 340, 60);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setFont(Palette.courier(13));
        refreshBtn.setBackground(Palette.BLUE_BTN);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setBorderPainted(false);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.setBounds(getWidth() - 110, 14, 90, 32);
        refreshBtn.addActionListener(e -> {
            removeAll();
            draw();
        });

        header.add(title);
        header.add(alertCountLabel);
        header.add(refreshBtn);
        add(header);
    }

    private void buildAlerts() {
        cards = new JPanel(new GridLayout(0, 1, 0, 10));
        cards.setBackground(Palette.BG);
        cards.setBounds(20, 74, getWidth() - 40, getHeight() - 94);

        alerts.clear();

        SaleRepository saleRepo = DatabaseUtils.getInstance().getSaleRepository();

        alerts.add(new Alert("Daily Revenue",
                "Today's Sales: KES " + org.PSmart.POS.Utils.MyUtils.formatCurrency(
                        String.valueOf(saleRepo.getTotalSalesToday())),
                Palette.GREEN, "Total value of sales made today."));

        alerts.add(new Alert("Transactions Today",
                saleRepo.getSalesCountToday() + " transactions recorded",
                Palette.BLUE_ACTIVE, "Number of completed sales today."));

        collectStockAlerts();

        if (alerts.isEmpty()) {
            alerts.add(new Alert("All Clear",
                    "No low-stock or expiry issues to address.",
                    Palette.GREEN, "Inventory is healthy."));
        }

        for (Alert a : alerts) cards.add(buildAlertCard(a));

        int open = 0;
        for (Alert a : alerts) if (a.level > 0) open++;
        alertCountLabel.setText(open + " actionable alerts");

        cards.revalidate();
        cards.repaint();
        add(cards);
    }

    private void collectStockAlerts() {
        long now = System.currentTimeMillis();
        long soonWindow = 30L * 24 * 60 * 60 * 1000; // 30 days

        List<StockFeedItem> feeds;
        try {
            feeds = loadFeeds();
        } catch (Exception ex) {
            return;
        }

        java.util.Map<java.util.UUID, Double> qtyByItem = new java.util.HashMap<>();
        java.util.Map<java.util.UUID, String> nameByItem = new java.util.HashMap<>();
        java.util.Map<java.util.UUID, Double> reorderByItem = new java.util.HashMap<>();

        java.util.List<StockFeedItem> expired = new java.util.ArrayList<>();
        java.util.List<StockFeedItem> expiringSoon = new java.util.ArrayList<>();

        for (StockFeedItem feed : feeds) {
            if (feed.getItem() == null) continue;
            if (feed.getExpiry() == null) { // only count non-expired batches as "available"
                qtyByItem.merge(feed.getItem().getUid(), feed.getQuantity() != null ? feed.getQuantity() : 0, Double::sum);
            } else if (feed.getExpiry() < now) {
                expired.add(feed);
            } else if (feed.getExpiry() <= now + soonWindow) {
                expiringSoon.add(feed);
            }
            nameByItem.putIfAbsent(feed.getItem().getUid(), feed.getItem().getName());
            if (feed.getItem().getReorderLevel() != null)
                reorderByItem.putIfAbsent(feed.getItem().getUid(), feed.getItem().getReorderLevel());
        }

        for (java.util.Map.Entry<java.util.UUID, Double> e : qtyByItem.entrySet()) {
            Double reorder = reorderByItem.get(e.getKey());
            if (reorder != null && reorder > 0 && e.getValue() < reorder) {
                String name = nameByItem.get(e.getKey());
                alerts.add(new Alert("Low Stock: " + name,
                        String.format("On hand: %.0f  —  Reorder level: %.0f", e.getValue(), reorder),
                        Palette.AMBER, "Restock this item to avoid stock-out."));
            }
        }

        for (StockFeedItem f : trimmed(expired, 20)) {
            String name = f.getItem().getName();
            alerts.add(new Alert("Expired Batch: " + name,
                    "A received stock batch has expired.",
                    Palette.RED, "Remove this product from sale immediately."));
        }
        if (expired.size() > 20) {
            alerts.add(new Alert("Expired Stock",
                    (expired.size() - 20) + " more expired batches.",
                    Palette.RED, "Review all expiry dates."));
        }

        for (StockFeedItem f : trimmed(expiringSoon, 10)) {
            String name = f.getItem().getName();
            alerts.add(new Alert("Expiring Soon: " + name,
                    "A batch expires within 30 days (" + daysLeft(f.getExpiry()) + " days).",
                    Palette.TEAL, "Consider a discount or promotion."));
        }
    }

    private int daysLeft(long expiry) {
        return Math.max(0, (int) ((expiry - System.currentTimeMillis()) / (24L * 60 * 60 * 1000)));
    }

    private List<StockFeedItem> trimmed(List<StockFeedItem> list, int max) {
        return list.size() > max ? list.subList(0, max) : list;
    }

    /** Load all non-deleted stock feed items (goods received batches). */
    private List<StockFeedItem> loadFeeds() {
        jakarta.persistence.EntityManager em =
                org.PSmart.POS.TestJPA.getEntityManager();
        return em.createQuery(
                "FROM StockFeedItem f WHERE f.deleted=false", StockFeedItem.class)
                .getResultList();
    }

    private JPanel buildAlertCard(Alert a) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Palette.BORDER_CARD),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));

        JPanel accent = new JPanel();
        accent.setBackground(a.levelColor);
        accent.setPreferredSize(new Dimension(5, 0));
        card.add(accent, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        JLabel title = new JLabel(a.title);
        title.setFont(Palette.courier(15));
        JLabel msg = new JLabel(a.message);
        msg.setFont(Palette.courierPlain(13));
        msg.setForeground(Palette.GRAY_SOFT);
        text.add(title);
        text.add(Box.createVerticalStrut(4));
        text.add(msg);
        card.add(text, BorderLayout.CENTER);

        JLabel tag = new JLabel(a.tag);
        tag.setFont(Palette.courier(12));
        tag.setForeground(a.levelColor);
        tag.setPreferredSize(new Dimension(70, 40));
        tag.setHorizontalAlignment(SwingConstants.CENTER);
        tag.setOpaque(true);
        tag.setBackground(Color.WHITE);
        card.add(tag, BorderLayout.EAST);

        return card;
    }

    private static class Alert {
        String title, message, tag;
        Color levelColor;
        int level; // 0 info, 1 warning, 2 critical

        Alert(String title, String message, Color color, String tag) {
            this.title = title;
            this.message = message;
            this.levelColor = color;
            this.tag = tag.toUpperCase();
            this.level = color.equals(Palette.GREEN) ? 0
                    : color.equals(Palette.AMBER) || color.equals(Palette.TEAL) ? 1 : 2;
        }
    }
}