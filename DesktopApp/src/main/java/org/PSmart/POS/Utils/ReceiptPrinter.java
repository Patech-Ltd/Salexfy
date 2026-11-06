package org.PSmart.POS.Utils;

import org.PSmart.POS.Entities.Purchase;
import org.PSmart.POS.Entities.Sale;
import org.PSmart.POS.Entities.StoreConfig;
import org.PSmart.POS.Utils.receipt.Receipt;
import org.PSmart.POS.Utils.receipt.ReceiptDocs;
import org.PSmart.POS.tests.AutoClosingNotification;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.PrinterJob;

/**
 * Printing front door. Printing is silent (no dialog at the till): it goes to the printer chosen in
 * Settings > Receipt &amp; Printer, or the system default. Every document can also be previewed first.
 */
public class ReceiptPrinter {

    // ---- thermal receipt (kept signature used across the app)
    public static void printReceipt(Sale sale, StoreConfig config) {
        print(ReceiptDocs.saleReceipt(sale, config), config);
    }

    public static void printInvoice(Sale sale, StoreConfig config) {
        print(ReceiptDocs.saleInvoice(sale, config), config);
    }

    public static void printPurchase(Purchase purchase, StoreConfig config) {
        print(ReceiptDocs.purchaseInvoice(purchase, config), config);
    }

    public static void previewReceipt(Component parent, Sale sale, StoreConfig config) {
        preview(parent, ReceiptDocs.saleReceipt(sale, config), config);
    }

    public static void previewInvoice(Component parent, Sale sale, StoreConfig config) {
        preview(parent, ReceiptDocs.saleInvoice(sale, config), config);
    }

    public static void previewPurchase(Component parent, Purchase purchase, StoreConfig config) {
        preview(parent, ReceiptDocs.purchaseInvoice(purchase, config), config);
    }

    // ---- core
    /** Prints silently on a background thread. Falls back to a toast if no printer is available. */
    public static void print(Receipt doc, StoreConfig config) {
        new Thread(() -> {
            try {
                PrinterJob job = PrinterJob.getPrinterJob();
                PrintService svc = chosenService(config);
                if (svc == null && PrintServiceLookup.lookupPrintServices(null, null).length == 0) {
                    AutoClosingNotification.sendNotification("No printer found - use Preview, or install a printer");
                    return;
                }
                if (svc != null) job.setPrintService(svc);
                job.setJobName(doc.title());
                job.setCopies(1);          // CUPS/lpr rejects "copies 0", which is what Java sends by default
                job.setPrintable(doc, doc.pageFormat(job.defaultPage()));
                job.print();
            } catch (Exception e) {
                e.printStackTrace();
                AutoClosingNotification.sendNotification("Print failed: " + e.getMessage());
            }
        }, "receipt-print").start();
    }

    private static PrintService chosenService(StoreConfig config) {
        if (config == null || config.getDefaultPrinter() == null) return null;
        String want = config.getDefaultPrinter().trim();
        if (want.isEmpty() || want.startsWith("(")) return null;
        for (PrintService s : PrinterJob.lookupPrintServices()) {
            if (s.getName().equalsIgnoreCase(want)) return s;
        }
        return null;
    }

    /** On-screen preview with Print / Close. */
    public static void preview(Component parent, Receipt doc, StoreConfig config) {
        Runnable show = () -> {
            BufferedImage img = doc.toImage(24);
            double scale = doc.paper() == Receipt.Paper.A4 ? 0.52 : 0.8;
            int w = (int) (img.getWidth() * scale), h = (int) (img.getHeight() * scale);
            Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);

            JFrame f = new JFrame("Preview - " + doc.title());
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            f.setLayout(new BorderLayout());
            JLabel page = new JLabel(new ImageIcon(scaled));
            JPanel holder = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 16));
            holder.setBackground(new Color(120, 124, 130));
            holder.add(page);
            JScrollPane sp = new JScrollPane(holder);
            sp.getVerticalScrollBar().setUnitIncrement(24);
            f.add(sp, BorderLayout.CENTER);

            JPanel bar = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 10));
            bar.setBackground(Color.WHITE);
            JButton print = MyUtils.createButton("Print", Color.BLUE);
            print.setForeground(Color.WHITE);
            print.setFont(MyUtils.courierFont(18));
            print.setPreferredSize(new Dimension(180, 42));
            print.addActionListener(e -> print(doc, config));
            JButton close = MyUtils.createButton("Close", new Color(52, 58, 64));
            close.setForeground(Color.WHITE);
            close.setFont(MyUtils.courierFont(18));
            close.setPreferredSize(new Dimension(180, 42));
            close.addActionListener(e -> f.dispose());
            bar.add(close);
            bar.add(print);
            f.add(bar, BorderLayout.SOUTH);

            Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            f.setSize(Math.min(w + 80, screen.width - 100), Math.min(h + 160, screen.height - 100));
            f.setLocationRelativeTo(parent);
            f.setVisible(true);
        };
        if (SwingUtilities.isEventDispatchThread()) show.run(); else SwingUtilities.invokeLater(show);
    }
}
