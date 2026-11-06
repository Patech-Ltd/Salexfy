package org.PSmart.POS.Utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Creates portable JSON snapshots of the whole database using the JPA layer,
 * so backups work without relying on pg_dump or a running Postgres shell.
 * Writes into a local "backups" folder and rotates old backups.
 */
public final class BackupService {

    public static final String BACKUP_DIR = "backups";

    private BackupService() {}

    public static File createBackup() throws Exception {
        File dir = new File(BACKUP_DIR);
        if (!dir.exists()) dir.mkdirs();

        Snapshot snap = collect();

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        String stamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        File out = new File(dir, "backup_" + stamp + ".json");
        mapper.writeValue(out, snap);

        return out;
    }

    public static void createBackupAndRotate(int keepCount) throws Exception {
        File out = createBackup();
        rotate(keepCount);
        System.out.println("Backup created: " + out.getAbsolutePath());
    }

    private static void rotate(int keepCount) throws Exception {
        if (keepCount <= 0) keepCount = 10;
        File dir = new File(BACKUP_DIR);
        File[] backups = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (backups == null || backups.length <= keepCount) return;
        Arrays.sort(backups, Comparator.comparingLong(File::lastModified).reversed());
        for (int i = keepCount; i < backups.length; i++) {
            backups[i].delete();
        }
    }

    public static File latestBackup() {
        File dir = new File(BACKUP_DIR);
        File[] backups = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (backups == null || backups.length == 0) return null;
        Arrays.sort(backups, Comparator.comparingLong(File::lastModified).reversed());
        return backups[0];
    }

    public static List<File> listBackups() {
        File dir = new File(BACKUP_DIR);
        File[] backups = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (backups == null) return new ArrayList<>();
        Arrays.sort(backups, Comparator.comparingLong(File::lastModified).reversed());
        return new ArrayList<>(Arrays.asList(backups));
    }

    // ------------------------------------------------------------------
    // Snapshot data collection (safe, null-tolerant)
    // ------------------------------------------------------------------

    public static class Snapshot {
        public String createdAt;
        public List<Item> items = new ArrayList<>();
        public List<ItemCategory> categories = new ArrayList<>();
        public List<User> users = new ArrayList<>();
        public List<Customer> customers = new ArrayList<>();
        public List<Sale> sales = new ArrayList<>();
        public List<SaleItem> saleItems = new ArrayList<>();
        public List<Purchase> purchases = new ArrayList<>();
        public List<PurchaseItem> purchaseItems = new ArrayList<>();
        public List<Expense> expenses = new ArrayList<>();
        public List<ExpenseItem> expenseItems = new ArrayList<>();
        public List<StockFeed> stockFeeds = new ArrayList<>();
        public List<StockFeedItem> stockFeedItems = new ArrayList<>();
        public StoreConfig config;
    }

    private static Snapshot collect() {
        Snapshot s = new Snapshot();
        s.createdAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        s.config = DatabaseUtils.getInstance().getStoreConfigRepository().getStoreConfig();

        safeAdd(s.items, "FROM Item i WHERE i.deleted=false");
        safeAdd(s.categories, "FROM ItemCategory c WHERE c.deleted=false");
        safeAdd(s.users, "FROM User u WHERE u.deleted=false");
        safeAdd(s.customers, "FROM Customer c WHERE c.deleted=false");
        safeAdd(s.sales, "FROM Sale s WHERE s.deleted=false");
        safeAdd(s.saleItems, "FROM SaleItem s WHERE s.deleted=false");
        safeAdd(s.purchases, "FROM Purchase p WHERE p.deleted=false");
        safeAdd(s.purchaseItems, "FROM PurchaseItem p WHERE p.deleted=false");
        safeAdd(s.expenses, "FROM Expense e WHERE e.deleted=false");
        safeAdd(s.expenseItems, "FROM ExpenseItem e WHERE e.deleted=false");
        safeAdd(s.stockFeeds, "FROM StockFeed s WHERE s.deleted=false");
        safeAdd(s.stockFeedItems, "FROM StockFeedItem s WHERE s.deleted=false");
        return s;
    }

    private static <T> void safeAdd(List<T> target, String jpql) {
        try {
            List<T> res = org.PSmart.POS.TestJPA.getEntityManager().createQuery(jpql).getResultList();
            for (T t : res) {
                // Break bidirectional cycles that would break JSON serialization.
                if (t instanceof Sale) {
                    Sale sale = (Sale) t;
                    sale.setItems(null);
                } else if (t instanceof SaleItem) {
                    ((SaleItem) t).setSale(null);
                } else if (t instanceof Purchase) {
                    ((Purchase) t).setItems(null);
                } else if (t instanceof PurchaseItem) {
                    ((PurchaseItem) t).setPurchase(null);
                } else if (t instanceof Expense) {
                    ((Expense) t).setItems(null);
                } else if (t instanceof ExpenseItem) {
                    ((ExpenseItem) t).setExpense(null);
                }
            }
            target.addAll(res);
        } catch (Exception ex) {
            // table may not exist yet on first run — skip
        }
    }
}