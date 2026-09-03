package com.patechltd.salexfypos.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.patechltd.salexfypos.db.dao.AdminDao;
import com.patechltd.salexfypos.db.dao.CrashDao;
import com.patechltd.salexfypos.db.dao.DirectoryDao;
import com.patechltd.salexfypos.db.dao.ExpenseDao;
import com.patechltd.salexfypos.db.dao.ProductDao;
import com.patechltd.salexfypos.db.dao.PurchaseDao;
import com.patechltd.salexfypos.db.dao.PaymentMethodDao;
import com.patechltd.salexfypos.db.dao.SaleDao;
import com.patechltd.salexfypos.db.dao.StockDao;
import com.patechltd.salexfypos.db.dao.SupplierDao;
import com.patechltd.salexfypos.db.dao.SyncDao;
import com.patechltd.salexfypos.db.entity.AppSetting;
import com.patechltd.salexfypos.db.entity.BackupLog;
import com.patechltd.salexfypos.db.entity.Brand;
import com.patechltd.salexfypos.db.entity.Category;
import com.patechltd.salexfypos.db.entity.CrashLog;
import com.patechltd.salexfypos.db.entity.Customer;
import com.patechltd.salexfypos.db.entity.DebtPayment;
import com.patechltd.salexfypos.db.entity.Expense;
import com.patechltd.salexfypos.db.entity.PendingProduct;
import com.patechltd.salexfypos.db.entity.PaymentMethod;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.db.entity.ProductBarcode;
import com.patechltd.salexfypos.db.entity.ProductUnit;
import com.patechltd.salexfypos.db.entity.Purchase;
import com.patechltd.salexfypos.db.entity.PurchaseItem;
import com.patechltd.salexfypos.db.entity.Role;
import com.patechltd.salexfypos.db.entity.Sale;
import com.patechltd.salexfypos.db.entity.SaleItem;
import com.patechltd.salexfypos.db.entity.SalePayment;
import com.patechltd.salexfypos.db.entity.StockMovement;
import com.patechltd.salexfypos.db.entity.StockTake;
import com.patechltd.salexfypos.db.entity.StockTakeItem;
import com.patechltd.salexfypos.db.entity.Supplier;
import com.patechltd.salexfypos.db.entity.SupplierPayment;
import com.patechltd.salexfypos.db.entity.SyncChange;
import com.patechltd.salexfypos.db.entity.SyncLog;
import com.patechltd.salexfypos.db.entity.Unit;
import com.patechltd.salexfypos.db.entity.User;

@Database(
        entities = {
                Product.class, ProductBarcode.class, ProductUnit.class, PendingProduct.class,
                Category.class, Brand.class, Unit.class,
                PaymentMethod.class,
                Supplier.class, Customer.class, DebtPayment.class,
                Purchase.class, PurchaseItem.class, SupplierPayment.class,
                Sale.class, SaleItem.class, SalePayment.class,
                StockMovement.class, StockTake.class, StockTakeItem.class,
                User.class, Role.class, AppSetting.class,
                CrashLog.class, BackupLog.class,
                Expense.class,
                SyncChange.class, SyncLog.class
        },
        version = 13,
        exportSchema = false
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "salexfy.db";

    private static volatile AppDatabase instance;

    public abstract ProductDao productDao();

    public abstract DirectoryDao directoryDao();

    public abstract SupplierDao supplierDao();

    public abstract PurchaseDao purchaseDao();

    public abstract PaymentMethodDao paymentMethodDao();

    public abstract SaleDao saleDao();

    public abstract StockDao stockDao();

    public abstract AdminDao adminDao();

    public abstract CrashDao crashDao();

    public abstract ExpenseDao expenseDao();

    public abstract SyncDao syncDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, DATABASE_NAME)
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                                    MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                                    MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `product_barcodes` "
                    + "(`id` TEXT NOT NULL, `productId` TEXT, `barcode` TEXT, "
                    + "PRIMARY KEY(`id`))");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_product_barcodes_productId` "
                    + "ON `product_barcodes` (`productId`)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_product_barcodes_barcode` "
                    + "ON `product_barcodes` (`barcode`)");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `customers` ADD COLUMN `loyaltyPoints` REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `customers` ADD COLUMN `totalSpent` REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `sales` ADD COLUMN `pointsEarned` REAL NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE `sales` ADD COLUMN `pointsRedeemed` REAL NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `expenses` ("
                    + "`id` TEXT NOT NULL, "
                    + "`description` TEXT, "
                    + "`category` TEXT, "
                    + "`amount` REAL NOT NULL DEFAULT 0, "
                    + "`expenseDate` INTEGER NOT NULL DEFAULT 0, "
                    + "`createdBy` TEXT, "
                    + "`createdAt` INTEGER NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY(`id`))");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_expenses_expenseDate` "
                    + "ON `expenses` (`expenseDate`)");
        }
    };

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `products` ADD COLUMN `imagePath` TEXT");
        }
    };

    static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `sale_payments` ("
                    + "`id` TEXT NOT NULL, "
                    + "`saleId` TEXT, "
                    + "`method` TEXT, "
                    + "`amount` REAL NOT NULL, "
                    + "`customerId` TEXT, "
                    + "`customerName` TEXT, "
                    + "PRIMARY KEY(`id`), "
                    + "FOREIGN KEY(`saleId`) REFERENCES `sales`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_payments_saleId` "
                    + "ON `sale_payments` (`saleId`)");
        }
    };

    static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE IF EXISTS `sale_payments`");
            database.execSQL("CREATE TABLE IF NOT EXISTS `sale_payments` ("
                    + "`id` TEXT NOT NULL, "
                    + "`saleId` TEXT, "
                    + "`method` TEXT, "
                    + "`amount` REAL NOT NULL, "
                    + "`customerId` TEXT, "
                    + "`customerName` TEXT, "
                    + "PRIMARY KEY(`id`), "
                    + "FOREIGN KEY(`saleId`) REFERENCES `sales`(`id`) "
                    + "ON UPDATE NO ACTION ON DELETE CASCADE)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_sale_payments_saleId` "
                    + "ON `sale_payments` (`saleId`)");
        }
    };

    static final Migration MIGRATION_7_8 = new Migration(7, 8) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `sync_changes` ("
                    + "`seq` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`entityType` TEXT NOT NULL, "
                    + "`recordId` TEXT NOT NULL, "
                    + "`operation` TEXT NOT NULL, "
                    + "`payload` TEXT, "
                    + "`synced` INTEGER NOT NULL DEFAULT 0, "
                    + "`attempts` INTEGER NOT NULL DEFAULT 0, "
                    + "`lastError` TEXT, "
                    + "`createdAt` INTEGER NOT NULL DEFAULT 0, "
                    + "`updatedAt` INTEGER NOT NULL DEFAULT 0)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_changes_synced` "
                    + "ON `sync_changes` (`synced`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_changes_entityType` "
                    + "ON `sync_changes` (`entityType`)");
            database.execSQL("CREATE TABLE IF NOT EXISTS `sync_logs` ("
                    + "`uid` TEXT NOT NULL, "
                    + "`timestamp` INTEGER NOT NULL DEFAULT 0, "
                    + "`type` TEXT, "
                    + "`status` TEXT, "
                    + "`message` TEXT, "
                    + "`details` TEXT, "
                    + "PRIMARY KEY(`uid`))");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_logs_timestamp` "
                    + "ON `sync_logs` (`timestamp`)");
        }
    };

    static final Migration MIGRATION_8_9 = new Migration(8, 9) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `products` ADD COLUMN `retailUnitId` TEXT");
            database.execSQL("ALTER TABLE `products` ADD COLUMN `wholesaleUnitId` TEXT");
            database.execSQL("CREATE TABLE IF NOT EXISTS `product_units` ("
                    + "`id` TEXT NOT NULL, "
                    + "`productId` TEXT, "
                    + "`unitId` TEXT, "
                    + "`unitName` TEXT, "
                    + "`factor` REAL NOT NULL DEFAULT 1, "
                    + "`price` REAL NOT NULL DEFAULT 0, "
                    + "`barcode` TEXT, "
                    + "`isBase` INTEGER NOT NULL DEFAULT 0, "
                    + "`sortOrder` INTEGER NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY(`id`))");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_product_units_productId` "
                    + "ON `product_units` (`productId`)");
            database.execSQL("INSERT INTO product_units "
                    + "(id, productId, unitId, unitName, factor, price, barcode, isBase, sortOrder) "
                    + "SELECT lower(hex(randomblob(16))), id, NULL, "
                    + "CASE WHEN retailUnit IS NULL OR retailUnit = '' THEN 'Pcs' ELSE retailUnit END, "
                    + "1, retailPrice, barcode, 1, 0 FROM products");
            database.execSQL("INSERT INTO product_units "
                    + "(id, productId, unitId, unitName, factor, price, barcode, isBase, sortOrder) "
                    + "SELECT lower(hex(randomblob(16))), id, NULL, wholesaleUnit, "
                    + "CASE WHEN wholesaleFactor < 1 THEN 1 ELSE wholesaleFactor END, "
                    + "wholesalePrice, NULL, 0, 1 FROM products "
                    + "WHERE wholesaleUnit IS NOT NULL AND wholesaleUnit != '' "
                    + "AND (wholesalePrice > 0 OR wholesaleFactor > 1)");
            database.execSQL("ALTER TABLE `sale_items` ADD COLUMN `factor` REAL NOT NULL DEFAULT 1");
        }
    };

    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `pending_products` ("
                    + "`id` TEXT NOT NULL, "
                    + "`name` TEXT, "
                    + "`barcode` TEXT, "
                    + "`cost` REAL NOT NULL DEFAULT 0, "
                    + "`price` REAL NOT NULL DEFAULT 0, "
                    + "`qty` REAL NOT NULL DEFAULT 1, "
                    + "`categoryId` TEXT, "
                    + "`unitId` TEXT, "
                    + "`unitName` TEXT, "
                    + "`createdAt` INTEGER NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY(`id`))");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_pending_products_barcode` "
                    + "ON `pending_products` (`barcode`)");
        }
    };

    /** Ensures the two tables added in v11 exist, recovering from stale same-version databases. */
    static final Migration MIGRATION_11_12 = new Migration(11, 12) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `supplier_payments` ("
                    + "`id` TEXT NOT NULL, "
                    + "`purchaseId` TEXT, "
                    + "`amount` REAL NOT NULL DEFAULT 0, "
                    + "`paymentDate` INTEGER NOT NULL DEFAULT 0, "
                    + "`method` TEXT, "
                    + "`notes` TEXT, "
                    + "`createdBy` TEXT, "
                    + "`createdAt` INTEGER NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY(`id`))");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_payments_purchaseId` "
                    + "ON `supplier_payments` (`purchaseId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_payments_paymentDate` "
                    + "ON `supplier_payments` (`paymentDate`)");
            database.execSQL("CREATE TABLE IF NOT EXISTS `payment_methods` ("
                    + "`id` TEXT NOT NULL, "
                    + "`name` TEXT, "
                    + "`isCredit` INTEGER NOT NULL, "
                    + "`isSystem` INTEGER NOT NULL, "
                    + "`active` INTEGER NOT NULL, "
                    + "`sortOrder` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`id`))");
        }
    };

    static final Migration MIGRATION_10_11 = new Migration(10, 11) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `supplier_payments` ("
                    + "`id` TEXT NOT NULL, "
                    + "`purchaseId` TEXT, "
                    + "`amount` REAL NOT NULL DEFAULT 0, "
                    + "`paymentDate` INTEGER NOT NULL DEFAULT 0, "
                    + "`method` TEXT, "
                    + "`notes` TEXT, "
                    + "`createdBy` TEXT, "
                    + "`createdAt` INTEGER NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY(`id`))");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_payments_purchaseId` "
                    + "ON `supplier_payments` (`purchaseId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_supplier_payments_paymentDate` "
                    + "ON `supplier_payments` (`paymentDate`)");
            database.execSQL("CREATE TABLE IF NOT EXISTS `payment_methods` ("
                    + "`id` TEXT NOT NULL, "
                    + "`name` TEXT, "
                    + "`isCredit` INTEGER NOT NULL, "
                    + "`isSystem` INTEGER NOT NULL, "
                    + "`active` INTEGER NOT NULL, "
                    + "`sortOrder` INTEGER NOT NULL, "
                    + "PRIMARY KEY(`id`))");
        }
    };

    static final Migration MIGRATION_12_13 = new Migration(12, 13) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE `purchase_items` ADD COLUMN `factor` REAL NOT NULL DEFAULT 1");
        }
    };

    /** Builds the database instance on a background thread so first real
     *  access is fast. Safe to call from Application.onCreate. */
    public static void warmUp(final Context context) {
        new Thread(() -> {
            try {
                getInstance(context);
            } catch (Throwable ignored) {
            }
        }, "salexfy-db-warmup").start();
    }

    public static void destroyInstance() {
        instance = null;
    }
}
