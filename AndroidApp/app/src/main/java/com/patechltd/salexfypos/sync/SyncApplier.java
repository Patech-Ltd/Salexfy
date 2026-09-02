package com.patechltd.salexfypos.sync;

import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Brand;
import com.patechltd.salexfypos.db.entity.Category;
import com.patechltd.salexfypos.db.entity.Customer;
import com.patechltd.salexfypos.db.entity.DebtPayment;
import com.patechltd.salexfypos.db.entity.Expense;
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
import com.patechltd.salexfypos.db.entity.Supplier;
import com.patechltd.salexfypos.db.entity.SupplierPayment;
import com.patechltd.salexfypos.db.entity.Unit;
import com.patechltd.salexfypos.db.entity.User;
import com.patechltd.salexfypos.util.AppLogger;

import java.util.List;

public final class SyncApplier {

    private SyncApplier() {
    }

    public static int apply(Repository repo, List<RemoteChange> changes) {
        int applied = 0;
        SyncTracker.mute();
        try {
            for (RemoteChange change : changes) {
                try {
                    if (applyOne(repo, change)) applied++;
                } catch (Throwable t) {
                    AppLogger.e("Sync apply failed " + change.entityType + "/" + change.recordId, t);
                }
            }
        } finally {
            SyncTracker.unmute();
        }
        return applied;
    }

    private static boolean applyOne(Repository repo, RemoteChange change) {
        if ("DELETE".equals(change.operation)) {
            return applyDelete(repo, change);
        }
        if (change.payload == null || change.payload.isEmpty()) return false;
        switch (change.entityType) {
            case SyncTracker.PRODUCT:
                repo.products.insert(SyncSerializer.<Product>fromJson(change.payload, Product.class));
                return true;
            case SyncTracker.PRODUCT_BARCODE:
                repo.products.insertBarcode(SyncSerializer.<ProductBarcode>fromJson(change.payload, ProductBarcode.class));
                return true;
            case SyncTracker.PRODUCT_UNIT:
                repo.products.insertProductUnit(SyncSerializer.<ProductUnit>fromJson(change.payload, ProductUnit.class));
                return true;
            case SyncTracker.CATEGORY:
                repo.directory.insertCategory(SyncSerializer.<Category>fromJson(change.payload, Category.class));
                return true;
            case SyncTracker.BRAND:
                repo.directory.insertBrand(SyncSerializer.<Brand>fromJson(change.payload, Brand.class));
                return true;
            case SyncTracker.UNIT:
                repo.directory.insertUnit(SyncSerializer.<Unit>fromJson(change.payload, Unit.class));
                return true;
            case SyncTracker.SUPPLIER:
                repo.suppliers.insertSupplier(SyncSerializer.<Supplier>fromJson(change.payload, Supplier.class));
                return true;
            case SyncTracker.CUSTOMER:
                repo.suppliers.insertCustomer(SyncSerializer.<Customer>fromJson(change.payload, Customer.class));
                return true;
            case SyncTracker.DEBT_PAYMENT:
                repo.suppliers.insertDebtPayment(SyncSerializer.<DebtPayment>fromJson(change.payload, DebtPayment.class));
                return true;
            case SyncTracker.PURCHASE:
                repo.purchases.insertPurchase(SyncSerializer.<Purchase>fromJson(change.payload, Purchase.class));
                return true;
            case SyncTracker.PURCHASE_ITEM:
                repo.purchases.insertPurchaseItem(SyncSerializer.<PurchaseItem>fromJson(change.payload, PurchaseItem.class));
                return true;
            case SyncTracker.SUPPLIER_PAYMENT:
                repo.purchases.insertSupplierPayment(SyncSerializer.<SupplierPayment>fromJson(change.payload, SupplierPayment.class));
                return true;
            case SyncTracker.SALE:
                repo.sales.insertSale(SyncSerializer.<Sale>fromJson(change.payload, Sale.class));
                return true;
            case SyncTracker.SALE_ITEM:
                repo.sales.insertSaleItem(SyncSerializer.<SaleItem>fromJson(change.payload, SaleItem.class));
                return true;
            case SyncTracker.SALE_PAYMENT:
                repo.sales.insertPayment(SyncSerializer.<SalePayment>fromJson(change.payload, SalePayment.class));
                return true;
            case SyncTracker.STOCK_MOVEMENT:
                repo.stock.insertMovement(SyncSerializer.<StockMovement>fromJson(change.payload, StockMovement.class));
                return true;
            case SyncTracker.STOCK_TAKE:
                repo.stock.insertStockTake(SyncSerializer.<StockTake>fromJson(change.payload, StockTake.class));
                return true;
            case SyncTracker.USER:
                repo.admin.insertUser(SyncSerializer.<User>fromJson(change.payload, User.class));
                return true;
            case SyncTracker.ROLE:
                repo.admin.insertRole(SyncSerializer.<Role>fromJson(change.payload, Role.class));
                return true;
            case SyncTracker.EXPENSE:
                repo.expenses.insert(SyncSerializer.<Expense>fromJson(change.payload, Expense.class));
                return true;
            default:
                return false;
        }
    }

    private static boolean applyDelete(Repository repo, RemoteChange change) {
        switch (change.entityType) {
            case SyncTracker.PRODUCT: {
                Product p = repo.products.getById(change.recordId);
                if (p != null) repo.products.delete(p);
                return true;
            }
            case SyncTracker.PRODUCT_BARCODE:
                repo.products.rawDeleteBarcode(change.recordId);
                return true;
            case SyncTracker.PRODUCT_UNIT:
                repo.products.rawDeleteProductUnit(change.recordId);
                return true;
            case SyncTracker.CATEGORY:
                repo.directory.rawDeleteCategory(change.recordId);
                return true;
            case SyncTracker.BRAND:
                repo.directory.rawDeleteBrand(change.recordId);
                return true;
            case SyncTracker.UNIT:
                repo.directory.rawDeleteUnit(change.recordId);
                return true;
            case SyncTracker.SUPPLIER: {
                Supplier s = repo.suppliers.getSupplier(change.recordId);
                if (s != null) repo.suppliers.deleteSupplier(s);
                return true;
            }
            case SyncTracker.CUSTOMER: {
                Customer c = repo.suppliers.getCustomer(change.recordId);
                if (c != null) repo.suppliers.deleteCustomer(c);
                return true;
            }
            case SyncTracker.DEBT_PAYMENT:
                repo.suppliers.rawDeleteDebtPayment(change.recordId);
                return true;
            case SyncTracker.PURCHASE: {
                Purchase p = repo.purchases.getPurchase(change.recordId);
                if (p != null) repo.purchases.deletePurchase(p);
                return true;
            }
            case SyncTracker.PURCHASE_ITEM:
                repo.purchases.rawDeleteItem(change.recordId);
                return true;
            case SyncTracker.SUPPLIER_PAYMENT:
                repo.purchases.rawDeleteSupplierPayment(change.recordId);
                return true;
            case SyncTracker.SALE:
                repo.sales.deleteSale(change.recordId);
                return true;
            case SyncTracker.SALE_ITEM:
                repo.sales.rawDeleteItem(change.recordId);
                return true;
            case SyncTracker.SALE_PAYMENT:
                repo.sales.rawDeletePayment(change.recordId);
                return true;
            case SyncTracker.STOCK_MOVEMENT:
                repo.stock.rawDeleteMovement(change.recordId);
                return true;
            case SyncTracker.STOCK_TAKE: {
                StockTake s = repo.stock.getStockTake(change.recordId);
                if (s != null) repo.stock.deleteStockTake(s);
                return true;
            }
            case SyncTracker.USER: {
                User u = repo.admin.getUser(change.recordId);
                if (u != null) repo.admin.deleteUser(u);
                return true;
            }
            case SyncTracker.ROLE: {
                Role r = repo.admin.getRole(change.recordId);
                if (r != null) repo.admin.deleteRole(r);
                return true;
            }
            case SyncTracker.EXPENSE:
                repo.expenses.delete(change.recordId);
                return true;
            default:
                return false;
        }
    }
}
