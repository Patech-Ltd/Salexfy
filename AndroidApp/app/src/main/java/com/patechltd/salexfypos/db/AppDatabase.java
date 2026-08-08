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
import com.patechltd.salexfypos.db.dao.ProductDao;
import com.patechltd.salexfypos.db.dao.PurchaseDao;
import com.patechltd.salexfypos.db.dao.SaleDao;
import com.patechltd.salexfypos.db.dao.StockDao;
import com.patechltd.salexfypos.db.dao.SupplierDao;
import com.patechltd.salexfypos.db.entity.AppSetting;
import com.patechltd.salexfypos.db.entity.BackupLog;
import com.patechltd.salexfypos.db.entity.Brand;
import com.patechltd.salexfypos.db.entity.Category;
import com.patechltd.salexfypos.db.entity.CrashLog;
import com.patechltd.salexfypos.db.entity.Customer;
import com.patechltd.salexfypos.db.entity.DebtPayment;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.db.entity.ProductBarcode;
import com.patechltd.salexfypos.db.entity.Purchase;
import com.patechltd.salexfypos.db.entity.PurchaseItem;
import com.patechltd.salexfypos.db.entity.Role;
import com.patechltd.salexfypos.db.entity.Sale;
import com.patechltd.salexfypos.db.entity.SaleItem;
import com.patechltd.salexfypos.db.entity.StockMovement;
import com.patechltd.salexfypos.db.entity.StockTake;
import com.patechltd.salexfypos.db.entity.StockTakeItem;
import com.patechltd.salexfypos.db.entity.Supplier;
import com.patechltd.salexfypos.db.entity.Unit;
import com.patechltd.salexfypos.db.entity.User;

@Database(
        entities = {
                Product.class, ProductBarcode.class, Category.class, Brand.class, Unit.class,
                Supplier.class, Customer.class, DebtPayment.class,
                Purchase.class, PurchaseItem.class,
                Sale.class, SaleItem.class,
                StockMovement.class, StockTake.class, StockTakeItem.class,
                User.class, Role.class, AppSetting.class,
                CrashLog.class, BackupLog.class
        },
        version = 3,
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

    public abstract SaleDao saleDao();

    public abstract StockDao stockDao();

    public abstract AdminDao adminDao();

    public abstract CrashDao crashDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, DATABASE_NAME)
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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

    public static void destroyInstance() {
        instance = null;
    }
}
