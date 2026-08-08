package com.patechltd.salexfypos.db;

import android.content.Context;

import androidx.room.Transaction;

import com.patechltd.salexfypos.db.dao.AdminDao;
import com.patechltd.salexfypos.db.dao.CrashDao;
import com.patechltd.salexfypos.db.dao.DirectoryDao;
import com.patechltd.salexfypos.db.dao.ProductDao;
import com.patechltd.salexfypos.db.dao.PurchaseDao;
import com.patechltd.salexfypos.db.dao.SaleDao;
import com.patechltd.salexfypos.db.dao.StockDao;
import com.patechltd.salexfypos.db.dao.SupplierDao;
import com.patechltd.salexfypos.db.entity.DebtPayment;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.db.entity.Purchase;
import com.patechltd.salexfypos.db.entity.PurchaseItem;
import com.patechltd.salexfypos.db.entity.Sale;
import com.patechltd.salexfypos.db.entity.SaleItem;
import com.patechltd.salexfypos.db.entity.StockMovement;
import com.patechltd.salexfypos.db.entity.StockTake;
import com.patechltd.salexfypos.db.entity.StockTakeItem;
import com.patechltd.salexfypos.model.MovementType;

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
        for (SaleItem item : items) {
            if (item.id == null) item.id = UUID.randomUUID().toString();
            sales.insertSaleItem(item);
            adjustStock(sale.id, item.productId, -item.stockQty, MovementType.SALE, item.unitLabel,
                    sale.cashierId, null, sale.saleDate);
        }
        sale.status = "COMPLETE";
        sales.updateSale(sale);
    }

    @Transaction
    public void saveDraft(Sale sale, List<SaleItem> items) {
        if (sale.id == null) {
            sale.id = UUID.randomUUID().toString();
            sales.insertSale(sale);
        } else {
            sales.updateSale(sale);
        }
        sales.deleteItemsForSale(sale.id);
        for (SaleItem item : items) {
            if (item.id == null) item.id = UUID.randomUUID().toString();
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
        List<SaleItem> items = sales.getItems(sale.id);
        for (SaleItem item : items) {
            adjustStock(sale.id, item.productId, item.qty, MovementType.SALE_VOID, item.unitLabel,
                    sale.cashierId, "Voided sale " + sale.saleNo, System.currentTimeMillis());
        }
        sale.status = "VOID";
        sales.updateSale(sale);
    }

    @Transaction
    public void deleteDraft(Sale sale) {
        sale.status = "VOID";
        sales.updateSale(sale);
        sales.deleteItemsForSale(sale.id);
    }

    @Transaction
    public void savePurchase(Purchase purchase, List<PurchaseItem> items, boolean updateCost) {
        if (purchase.id == null) {
            purchase.id = UUID.randomUUID().toString();
            purchases.insertPurchase(purchase);
        } else {
            purchases.updatePurchase(purchase);
        }
        purchases.deleteItemsForPurchase(purchase.id);
        for (PurchaseItem item : items) {
            if (item.id == null) item.id = UUID.randomUUID().toString();
            purchases.insertPurchaseItem(item);
            adjustStock(purchase.id, item.productId, item.stockQty, MovementType.PURCHASE, item.unitLabel,
                    purchase.createdBy, "Purchase " + purchase.invoiceNo, purchase.purchaseDate);
            if (updateCost) {
                Product p = products.getById(item.productId);
                if (p != null) {
                    double current = stock.currentQty(item.productId);
                    double newQty = current + item.stockQty;
                    double newCost;
                    if (newQty > 0 && item.stockQty > 0) {
                        double costPerRetail = (item.qty > 0)
                                ? (item.unitPrice * item.qty / item.stockQty)
                                : item.unitPrice;
                        newCost = ((current * p.costPrice) + (item.stockQty * costPerRetail)) / newQty;
                    } else {
                        newCost = item.unitPrice;
                    }
                    p.costPrice = Math.round(newCost * 100.0) / 100.0;
                    p.updatedAt = System.currentTimeMillis();
                    products.update(p);
                }
            }
        }
    }

    @Transaction
    public void deletePurchase(Purchase purchase) {
        stock.deleteMovementsForRef(purchase.id, MovementType.PURCHASE.name());
        purchases.deletePurchase(purchase);
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
        if (stockTake.id == null) {
            stockTake.id = UUID.randomUUID().toString();
            stock.insertStockTake(stockTake);
        } else {
            stock.updateStockTake(stockTake);
        }
        for (StockTakeItem item : items) {
            if (item.id == null) item.id = UUID.randomUUID().toString();
            stock.insertStockTakeItem(item);
            if (item.diffQty != 0) {
                adjustStock(stockTake.id, item.productId, item.diffQty, MovementType.STOCK_TAKE,
                        products.getById(item.productId) != null ? products.getById(item.productId).retailUnit : "",
                        stockTake.createdBy, "Stock take " + stockTake.name, stockTake.stockTakeDate);
            }
        }
        stockTake.status = "COMPLETE";
        stock.updateStockTake(stockTake);
    }

    @Transaction
    public void recordPayment(DebtPayment payment) {
        if (payment.id == null) payment.id = UUID.randomUUID().toString();
        if (payment.createdAt == 0) payment.createdAt = System.currentTimeMillis();
        suppliers.insertDebtPayment(payment);
    }

    @Transaction
    public void recordOpeningStock(String productId, double qty, String unitLabel) {
        if (qty <= 0) return;
        Product p = products.getById(productId);
        if (p == null) return;
        StockMovement m = new StockMovement();
        m.id = UUID.randomUUID().toString();
        m.productId = productId;
        m.movementType = MovementType.OPENING_STOCK.name();
        m.qty = qty;
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
        m.id = UUID.randomUUID().toString();
        m.productId = productId;
        m.movementType = type.name();
        m.qty = qty;
        m.stockAfter = stock.currentQty(productId) + qty;
        m.refId = refId;
        m.unitLabel = unitLabel;
        m.note = note;
        m.createdBy = userId;
        m.createdAt = time;
        stock.insertMovement(m);
    }
}
