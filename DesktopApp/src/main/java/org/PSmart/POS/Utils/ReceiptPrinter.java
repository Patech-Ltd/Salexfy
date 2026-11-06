package org.PSmart.POS.Utils;

import org.PSmart.POS.Entities.*;

import java.awt.*;
import java.awt.print.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ReceiptPrinter {

    public static void printReceipt(Sale sale, StoreConfig config) {
        PrinterJob printerJob = PrinterJob.getPrinterJob();
        printerJob.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            Graphics2D g2d = (Graphics2D) graphics;
            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());

            int y = 0;
            int pw = (int) pageFormat.getImageableWidth();
            Font plain = new Font("Monospaced", Font.PLAIN, 10);
            Font bold = new Font("Monospaced", Font.BOLD, 10);
            Font title = new Font("Monospaced", Font.BOLD, 14);
            Font small = new Font("Monospaced", Font.PLAIN, 8);

            String storeName = config != null ? config.getStoreName() : "PSMART POS";
            g2d.setFont(title);
            y = drawCentered(g2d, storeName, y, pw, title);
            g2d.setFont(plain);

            if (config != null) {
                if (config.getStoreAddress() != null && !config.getStoreAddress().isEmpty()) {
                    y = drawCentered(g2d, config.getStoreAddress(), y, pw, plain);
                }
                if (config.getStorePhone() != null && !config.getStorePhone().isEmpty()) {
                    y = drawCentered(g2d, config.getStorePhone(), y, pw, plain);
                }
            }

            y = drawLine(g2d, y, pw);
            y = drawCentered(g2d, "RECEIPT", y, pw, bold);
            y = drawLine(g2d, y, pw);

            g2d.setFont(plain);
            y = drawLeftRight(g2d, "Receipt:", sale.getReceiptNumber(), y);
            String dateStr = new SimpleDateFormat("dd-MMM-yyyy HH:mm").format(new Date(sale.getCreatedAt()));
            y = drawLeftRight(g2d, "Date:", dateStr, y);
            if (sale.getCashier() != null) {
                y = drawLeftRight(g2d, "Cashier:", sale.getCashier().getFullname(), y);
            }
            if (sale.getCustomer() != null) {
                y = drawLeftRight(g2d, "Customer:", sale.getCustomer().getFullname(), y);
            }
            y = drawLeftRight(g2d, "Payment:", sale.getPaymentMethod(), y);

            y = drawLine(g2d, y, pw);

            g2d.setFont(bold);
            y = drawLeftRight(g2d, "Item", "Amount", y);
            g2d.setFont(plain);
            y = drawLine(g2d, y, pw);

            if (sale.getItems() != null) {
                for (SaleItem item : sale.getItems()) {
                    String name = item.getItem().getName();
                    if (name.length() > 18) name = name.substring(0, 16) + "..";
                    String qty = String.format("x%.0f", item.getQuantity());
                    String total = MyUtils.formatCurrency(String.valueOf(item.getTotal()));
                    y = drawLeftRight(g2d, name, qty + " " + total, y);
                }
            }

            y = drawLine(g2d, y, pw);
            y = drawLeftRight(g2d, "Subtotal:", "KES " + MyUtils.formatCurrency(String.valueOf(sale.getSubtotal())), y);
            if (sale.getVatAmount() != null && sale.getVatAmount() > 0) {
                String taxName = "Tax:";
                if (config != null && config.getTaxName() != null) taxName = config.getTaxName() + ":";
                y = drawLeftRight(g2d, taxName, "KES " + MyUtils.formatCurrency(String.valueOf(sale.getVatAmount())), y);
            }
            if (sale.getDiscount() != null && sale.getDiscount() > 0) {
                y = drawLeftRight(g2d, "Discount:", "-KES " + MyUtils.formatCurrency(String.valueOf(sale.getDiscount())), y);
            }
            g2d.setFont(bold);
            y = drawLeftRight(g2d, "TOTAL:", "KES " + MyUtils.formatCurrency(String.valueOf(sale.getTotal())), y);
            g2d.setFont(plain);
            y = drawLeftRight(g2d, "Paid:", "KES " + MyUtils.formatCurrency(String.valueOf(sale.getAmountPaid())), y);
            if (sale.getChange() != null && sale.getChange() > 0) {
                y = drawLeftRight(g2d, "Change:", "KES " + MyUtils.formatCurrency(String.valueOf(sale.getChange())), y);
            }

            y = drawLine(g2d, y, pw);

            if (config != null && config.getReceiptFooter() != null && !config.getReceiptFooter().isEmpty()) {
                y = drawCentered(g2d, config.getReceiptFooter(), y, pw, small);
            }
            y = drawCentered(g2d, new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss").format(new Date()), y, pw, small);

            return Printable.PAGE_EXISTS;
        });

        if (printerJob.printDialog()) {
            new Thread(() -> {
                try {
                    printerJob.print();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private static int drawCentered(Graphics2D g2d, String text, int y, int pw, Font font) {
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int x = (pw - fm.stringWidth(text)) / 2;
        g2d.drawString(text, x, y + fm.getAscent());
        return y + fm.getHeight();
    }

    private static int drawLeftRight(Graphics2D g2d, String left, String right, int y) {
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(left, 10, y + fm.getAscent());
        int rw = fm.stringWidth(right);
        g2d.drawString(right, (int) g2d.getClipBounds().getWidth() - rw - 10, y + fm.getAscent());
        return y + fm.getHeight();
    }

    private static int drawLine(Graphics2D g2d, int y, int pw) {
        g2d.drawLine(10, y + 2, pw - 10, y + 2);
        return y + 6;
    }
}
