package com.patechltd.salexfypos.db;

import android.content.Context;

import androidx.room.Transaction;

import com.patechltd.salexfypos.db.dao.AdminDao;
import com.patechltd.salexfypos.db.dao.CrashDao;
import com.patechltd.salexfypos.db.dao.DirectoryDao;
import com.patechltd.salexfypos.db.dao.ExpenseDao;
import com.patechltd.salexfypos.db.dao.ProductDao;
import com.patechltd.salexfypos.db.dao.PurchaseDao;
import com.patechltd.salexfypos.db.dao.SaleDao;
import com.patechltd.salexfypos.db.dao.StockDao;
import com.patechltd.salexfypos.db.dao.SupplierDao;
import com.patechltd.salexfypos.db.dao.SyncDao;
import com.patechltd.salexfypos.db.entity.DebtPayment;
import com.patechltd.salexfypos.db.entity.Expense;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.db.entity.Purchase;
import com.patechltd.salexfypos.db.entity.PurchaseItem;
import com.patechltd.salexfypos.db.entity.Sale;
import com.patechltd.salexfypos.db.entity.SaleItem;
import com.patechltd.salexfypos.db.entity.SalePayment;
import com.patechltd.salexfypos.db.entity.StockMovement;
import com.patechltd.salexfypos.db.entity.StockTake;
import com.patechltd.salexfypos.db.entity.StockTakeItem;
import com.patechltd.salexfypos.db.entity.SupplierPayment;
import com.patechltd.salexfypos.model.MovementType;
import com.patechltd.salexfypos.sync.SyncTracker;
import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Repository {

    private static volatile Repository instance;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final java.util.concurrent.Executor mainExecutor = new java.util.concurrent.Executor() {
        private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            handler.post(command);
        }
    };

    public final AppDatabase db;
    public final ProductDao products;
    public final DirectoryDao directory;
    public final SupplierDao suppliers;
    public final PurchaseDao purchases;
    public final SaleDao sales;
    public final StockDao stock;
    public final AdminDao admin;
    public final CrashDao crash;
    public final ExpenseDao expenses;
    public final SyncDao sync;
    public final com.patechltd.salexfypos.db.dao.PaymentMethodDao paymentMethods;

    private Repository(Context context) {
        db = AppDatabase.getInstance(context);
        products = db.productDao();
        directory = db.directoryDao();
        suppliers = db.supplierDao();
        purchases = db.purchaseDao();
        sales = db.saleDao();
        stock = db.stockDao();
        admin = db.adminDao();
        crash = db.crashDao();
        expenses = db.expenseDao();
        sync = db.syncDao();
        paymentMethods = db.paymentMethodDao();
        SyncTracker.attach(sync);
        run(this::refreshPaymentMethods);
    }

    public static Repository get(Context context) {
        if (instance == null) {
            synchronized (Repository.class) {
                if (instance == null) {
                    instance = new Repository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public static void reset() {
        synchronized (Repository.class) {
            instance = null;
            AppDatabase.destroyInstance();
        }
    }

    public void run(Runnable task) {
        executor.execute(task);
    }

    /**
     * Runs a blocking callable on the background thread pool and returns a
     * CompletableFuture. Never call UI views inside the callable.
     */
    public <T> CompletableFuture<T> io(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new java.util.concurrent.CompletionException(e);
            }
        }, executor);
    }

    /** Runs a task on the background thread pool, no result. */
    public CompletableFuture<Void> io(Runnable task) {
        return CompletableFuture.runAsync(task, executor);
    }

    /** Executes the given runnable on the main (UI) thread. */
    public void onMain(Runnable task) {
        mainExecutor.execute(task);
    }

    /** Runs the consumer on the main thread with the future's result. Returns a future that completes when done. */
    public <T> CompletableFuture<T> thenOnMain(CompletableFuture<T> future,
                                               java.util.function.Consumer<T> onResult) {
        return future.whenCompleteAsync((value, error) -> {
            if (error != null) return;
            try {
                onResult.accept(value);
            } catch (Exception ignored) {
            }
        }, mainExecutor);
    }

    public synchronized String nextSaleNo() {
        String max = sales.maxSaleNo();
        int next = 1;
        if (max != null && !max.isEmpty()) {
            try {
                next = Integer.parseInt(max.replaceAll("\\D", "")) + 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return String.format("%08d", next);
    }

    /** Reloads the payment-method list into the shared cache (background). */
    public void refreshPaymentMethods() {
        try {
            List<com.patechltd.salexfypos.db.entity.PaymentMethod> all = paymentMethods.getAll();
            if (all.isEmpty()) {
                paymentMethods.insert(seedMethod("CASH", "Cash", false, true, true, 0));
                paymentMethods.insert(seedMethod("MPESA", "M-Pesa", false, false, true, 1));
                paymentMethods.insert(seedMethod("CREDIT", "On Credit", true, true, true, 2));
                paymentMethods.insert(seedMethod("AIRTEL", "Airtel Money", false, false, false, 3));
                paymentMethods.insert(seedMethod("TIGO", "Tigo Pesa", false, false, false, 4));
                paymentMethods.insert(seedMethod("MTN", "MTN MoMo", false, false, false, 5));
                paymentMethods.insert(seedMethod("ORANGE", "Orange Money", false, false, false, 6));
                paymentMethods.insert(seedMethod("HALOPESA", "Halopesa", false, false, false, 7));
                paymentMethods.insert(seedMethod("CARD", "Card", false, false, false, 8));
                paymentMethods.insert(seedMethod("BANK", "Bank Transfer", false, false, false, 9));
                paymentMethods.insert(seedMethod("CHEQUE", "Cheque", false, false, false, 10));
                paymentMethods.insert(seedMethod("VOUCHER", "Gift Voucher", false, false, false, 11));
                all = paymentMethods.getAll();
            }
            com.patechltd.salexfypos.util.PaymentMethods.refresh(all);
        } catch (Exception e) {
            AppLogger.e("Refresh payment methods failed", e);
        }
    }

    private com.patechltd.salexfypos.db.entity.PaymentMethod seedMethod(String id, String name,
            boolean credit, boolean system, boolean active, int sortOrder) {
        com.patechltd.salexfypos.db.entity.PaymentMethod m =
                new com.patechltd.salexfypos.db.entity.PaymentMethod();
        m.id = id;
        m.name = name;
        m.isCredit = credit;
        m.isSystem = system;
        m.active = active;
        m.sortOrder = sortOrder;
        return m;
    }

    public void savePaymentMethod(com.patechltd.salexfypos.db.entity.PaymentMethod method) {
        paymentMethods.insert(method);
        refreshPaymentMethods();
    }

    public void updatePaymentMethod(com.patechltd.salexfypos.db.entity.PaymentMethod method) {
        paymentMethods.update(method);
        refreshPaymentMethods();
    }

    public void deletePaymentMethod(com.patechltd.salexfypos.db.entity.PaymentMethod method) {
        paymentMethods.delete(method);
        refreshPaymentMethods();
    }

    public double outstandingDebt(String customerId) {
        if (customerId == null || customerId.isEmpty()) return 0;
        double debt = suppliers.customerDebt(customerId);
        double paid = 0;
        for (DebtPayment payment : suppliers.getPayments(customerId)) paid += payment.amount;
        return Math.max(0, Math.round((debt - paid) * 100.0) / 100.0);
    }

    public synchronized String nextInvoiceNo() {
        String max = purchases.maxPurchaseInvoice();
        int next = 1;
        if (max != null && !max.isEmpty()) {
            try {
                next = Integer.parseInt(max.replaceAll("\\D", "")) + 1;
            } catch (NumberFormatException ignored) {
            }
        }
        return "INV-" + String.format("%06d", next);
    }

    @Transaction
    public void completeSale(Sale sale, List<SaleItem> items) {
        if (sale.uid == null) sale.uid = UUID.randomUUID().toString();
        if (sales.getSale(sale.uid) != null) {
            sales.updateSale(sale);
        } else {
            sales.insertSale(sale);
        }
        sales.deleteItemsForSale(sale.uid);
        for (SaleItem item : items) {
            if (item.uid == null) item.uid = UUID.randomUUID().toString();
            item.saleId = sale.uid;
            sales.insertSaleItem(item);
            adjustStock(sale.uid, item.productId, -item.stockQty, MovementType.SALE, item.unitLabel,
                    sale.cashierId, null, sale.saleDate);
        }
        sale.status = "COMPLETE";
        sales.updateSale(sale);
    }

    @Transaction
    public void completeSale(Sale sale, List<SaleItem> items, List<SalePayment> payments) {
        completeSale(sale, items);
        sales.deletePaymentsForSale(sale.uid);
        if (payments == null) return;
        for (SalePayment payment : payments) {
            if (payment.uid == null) payment.uid = UUID.randomUUID().toString();
            payment.saleId = sale.uid;
            sales.insertPayment(payment);
        }
    }

    @Transaction
    public void saveDraft(Sale sale, List<SaleItem> items) {
        if (sale.uid == null) sale.uid = UUID.randomUUID().toString();
        if (sales.getSale(sale.uid) != null) {
            sales.updateSale(sale);
        } else {
            sales.insertSale(sale);
        }
        sales.deleteItemsForSale(sale.uid);
        for (SaleItem item : items) {
            if (item.uid == null) item.uid = UUID.randomUUID().toString();
            item.saleId = sale.uid;
            sales.insertSaleItem(item);
        }
    }

    @Transaction
    public void holdSale(Sale sale, List<SaleItem> items) {
        sale.status = "HELD";
        saveDraft(sale, items);
    }

    @Transaction
    public void voidSale(Sale sale) {
        if ("VOID".equals(sale.status)) return;
        List<SaleItem> items = sales.getItems(sale.uid);
        for (SaleItem item : items) {
            adjustStock(sale.uid, item.productId, item.stockQty, MovementType.SALE_VOID, item.unitLabel,
                    sale.cashierId, "Voided sale " + sale.saleNo, System.currentTimeMillis());
        }
        sale.status = "VOID";
        sales.updateSale(sale);
    }

    @Transaction
    public void deleteDraft(Sale sale) {
        sale.status = "VOID";
        sales.updateSale(sale);
        sales.deleteItemsForSale(sale.uid);
    }

    @Transaction
    public void savePurchase(Purchase purchase, List<PurchaseItem> items, boolean updateCost) {
        if (purchase.uid == null) {
            purchase.uid = UUID.randomUUID().toString();
            purchases.insertPurchase(purchase);
        } else {
            purchases.updatePurchase(purchase);
        }
        purchases.deleteItemsForPurchase(purchase.uid);
        for (PurchaseItem item : items) {
            if (item.uid == null) item.uid = UUID.randomUUID().toString();
            item.purchaseId = purchase.uid;
            purchases.insertPurchaseItem(item);
            if (updateCost) {
                updateProductBuyingPrice(item);
            }
            adjustStock(purchase.uid, item.productId, item.stockQty, MovementType.PURCHASE, item.unitLabel,
                    purchase.createdBy, "Purchase " + purchase.invoiceNo, purchase.purchaseDate);
        }
    }

    /** Refresh a product's buying price from the purchase line, weighted with existing stock. */
    private void updateProductBuyingPrice(PurchaseItem item) {
        if (item.productId == null) return;
        Product p = products.getById(item.productId);
        if (p == null) return;
        double current = stock.currentQty(item.productId);
        double added = Math.max(0, item.stockQty);
        double newQty = current + added;
        double costPerRetail = (item.stockQty > 0 && item.qty > 0)
                ? (item.unitPrice * item.qty / item.stockQty)
                : item.unitPrice;
        double newCost;
        if (newQty > 0 && added > 0) {
            newCost = ((current * p.costPrice) + (added * costPerRetail)) / newQty;
        } else {
            newCost = costPerRetail;
        }
        if (newCost < 0) newCost = 0;
        p.costPrice = Math.round(newCost * 100.0) / 100.0;
        p.updatedAt = System.currentTimeMillis();
        products.update(p);
    }

    @Transaction
    public void deletePurchase(Purchase purchase) {
        stock.deleteMovementsForRef(purchase.uid, MovementType.PURCHASE.name());
        purchases.deletePurchase(purchase);
    }

    @Transaction
    public void removeDeletedSaleItem(String itemId) {
        String saleId = sales.getSaleIdForItem(itemId);
        if (saleId == null) return;

        Sale sale = sales.getSale(saleId);
        if (sale == null) return;

        SaleItem item = null;
        List<SaleItem> items = sales.getItems(saleId);
        for (SaleItem si : items) {
            if (si.uid.equals(itemId)) {
                item = si;
                break;
            }
        }
        if (item == null) return;

        sales.rawDeleteItem(itemId);
        adjustStock(sale.uid, item.productId, item.stockQty, MovementType.SALE_VOID,
                item.unitLabel, sale.cashierId, "Removed deleted-item line", System.currentTimeMillis());

        List<SaleItem> remaining = sales.getItems(sale.uid);
        double newSubtotal = 0;
        for (SaleItem r : remaining) newSubtotal += r.lineTotal;
        double ratio = sale.subtotal > 0 ? newSubtotal / sale.subtotal : 0;
        sale.subtotal = Math.round(newSubtotal * 100.0) / 100.0;
        sale.taxAmount = Math.round(sale.taxAmount * ratio * 100.0) / 100.0;
        sale.discount = Math.round(sale.discount * ratio * 100.0) / 100.0;
        sale.total = Math.round((sale.subtotal + sale.taxAmount - sale.discount) * 100.0) / 100.0;
        sale.paidAmount = Math.min(sale.paidAmount, sale.total);
        sale.changeAmount = Math.max(0, sale.paidAmount - sale.total);
        sales.updateSale(sale);
    }

    @Transaction
    public void updatePurchase(Purchase purchase, List<PurchaseItem> items, boolean updateCost) {
        if (purchase.uid == null) return;
        stock.deleteMovementsForRef(purchase.uid, MovementType.PURCHASE.name());
        purchases.deleteItemsForPurchase(purchase.uid);
        savePurchase(purchase, items, updateCost);
    }

    @Transaction
    public SupplierPayment recordSupplierPayment(String purchaseId, double amount, String method,
                                                String notes, String userId) {
        Purchase purchase = purchases.getPurchase(purchaseId);
        if (purchase == null || amount <= 0) return null;
        double oldPaid = purchase.paidAmount;
        double balance = purchase.total - oldPaid;
        double applied = Math.min(amount, Math.max(0, balance));
        applied = Math.round(applied * 100.0) / 100.0;
        if (applied < 0.005) return null;
        purchase.paidAmount = Math.round((oldPaid + applied) * 100.0) / 100.0;
        purchases.updatePurchase(purchase);
        SupplierPayment payment = new SupplierPayment();
        payment.uid = UUID.randomUUID().toString();
        payment.purchaseId = purchaseId;
        payment.amount = applied;
        payment.paymentDate = System.currentTimeMillis();
        payment.method = method;
        payment.notes = notes;
        payment.createdBy = userId;
        payment.createdAt = System.currentTimeMillis();
        purchases.insertSupplierPayment(payment);
        return payment;
    }

    @Transaction
    public void recordAdjustment(String productId, double delta, String note, String userId, String unitLabel) {
        Product p = products.getById(productId);
        if (p == null) return;
        double after = Math.max(0, stock.currentQty(productId) + delta);
        adjustStock(null, productId, delta, MovementType.ADJUSTMENT, unitLabel, userId, note,
                System.currentTimeMillis());
    }

    @Transaction
    public void applyStockTake(StockTake stockTake, List<StockTakeItem> items) {
        if (stockTake.uid == null) {
            stockTake.uid = UUID.randomUUID().toString();
            stock.insertStockTake(stockTake);
        } else {
            stock.updateStockTake(stockTake);
        }
        for (StockTakeItem item : items) {
            if (item.uid == null) item.uid = UUID.randomUUID().toString();
            if (item.stockTakeId == null) item.stockTakeId = stockTake.uid;
            stock.insertStockTakeItem(item);
            if (item.diffQty != 0) {
                adjustStock(stockTake.uid, item.productId, item.diffQty, MovementType.STOCK_TAKE,
                        products.getById(item.productId) != null ? products.getById(item.productId).retailUnit : "",
                        stockTake.createdBy, "Stock take " + stockTake.name, stockTake.stockTakeDate);
            }
        }
        stockTake.status = "COMPLETE";
        stock.updateStockTake(stockTake);
    }

    @Transaction
    public void recordPayment(DebtPayment payment) {
        if (payment.uid == null) payment.uid = UUID.randomUUID().toString();
        if (payment.createdAt == 0) payment.createdAt = System.currentTimeMillis();
        suppliers.insertDebtPayment(payment);
    }

    @Transaction
    public void addExpense(String description, String category, double amount, long date) {
        if (amount <= 0) return;
        Expense e = new Expense();
        e.uid = UUID.randomUUID().toString();
        e.description = description == null ? "" : description.trim();
        e.category = category == null ? null : category.trim();
        e.amount = NumberUtil.round2(amount);
        e.expenseDate = date;
        e.createdAt = System.currentTimeMillis();
        expenses.insert(e);
    }

    @Transaction
    public void updateExpense(Expense expense) {
        if (expense == null || expense.uid == null) return;
        expense.description = expense.description == null ? "" : expense.description.trim();
        expense.category = expense.category == null ? null : expense.category.trim();
        expense.amount = NumberUtil.round2(expense.amount);
        expenses.update(expense);
    }

    @Transaction
    public void deleteExpense(String uid) {
        if (uid == null) return;
        expenses.delete(uid);
    }

    @Transaction
    public void recordOpeningStock(String productId, double qty, String unitLabel) {
        if (qty <= 0) return;
        Product p = products.getById(productId);
        if (p == null) return;
        StockMovement m = new StockMovement();
        m.uid = UUID.randomUUID().toString();
        m.productId = productId;
        m.movementType = MovementType.OPENING_STOCK.name();
        m.qty = qty;
        m.stockBefore = 0;
        m.stockAfter = qty;
        m.refId = null;
        m.unitLabel = unitLabel;
        m.note = "Opening stock";
        m.createdBy = null;
        m.createdAt = System.currentTimeMillis();
        stock.insertMovement(m);
    }

    private void adjustStock(String refId, String productId, double qty, MovementType type,
                             String unitLabel, String userId, String note, long time) {
        StockMovement m = new StockMovement();
        m.uid = UUID.randomUUID().toString();
        m.productId = productId;
        m.movementType = type.name();
        m.qty = qty;
        m.stockBefore = stock.currentQty(productId);
        m.stockAfter = m.stockBefore + qty;
        m.refId = refId;
        m.unitLabel = unitLabel;
        m.note = note;
        m.createdBy = userId;
        m.createdAt = time;
        stock.insertMovement(m);
    }
}
