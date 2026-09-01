package com.patechltd.salexfypos.ui.products;

import android.widget.Toast;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.entity.Unit;

import java.util.List;
import java.util.UUID;

public class UnitActivity extends DirectoryListActivity<Unit> {

    private String nameBeforeRename;

    @Override
    protected List<Unit> loadAll() {
        return repo.directory.getUnits();
    }

    @Override
    protected String nameOf(Unit item) {
        return item.name + (item.isWholesale ? " (wholesale)" : "");
    }

    @Override
    protected void insert(Unit item) {
        repo.directory.insertUnit(item);
    }

    @Override
    protected void update(Unit item) {
        String oldName = nameBeforeRename;
        repo.directory.updateUnit(item);
        if (oldName != null && !oldName.equals(item.name)) {
            repo.products.renameRetailUnitForProducts(item.uid, item.name);
            repo.products.renameWholesaleUnitForProducts(item.uid, item.name);
            repo.products.renameProductUnitsSnapshot(item.uid, item.name);
            repo.products.renameLegacyRetailUnit(oldName, item.name);
            repo.products.renameLegacyWholesaleUnit(oldName, item.name);
        }
    }

    @Override
    protected void delete(Unit item) {
        boolean inUse = repo.products.findAnyProductUnitByUnitId(item.uid) != null
                || repo.products.countProductsUsingUnit(item.uid) > 0;
        if (inUse) {
            throw new IllegalStateException("Unit is used by products");
        }
        repo.directory.deleteUnit(item);
    }

    @Override
    protected Unit create(String name) {
        Unit u = new Unit();
        u.uid = UUID.randomUUID().toString();
        u.name = name;
        u.isWholesale = false;
        u.createdAt = System.currentTimeMillis();
        return u;
    }

    @Override
    protected void deleteBlocked() {
        Toast.makeText(this, "This unit is used by products. Change those products first.",
                Toast.LENGTH_LONG).show();
    }

    @Override
    protected int itemIcon() {
        return R.drawable.ic_stock;
    }

    @Override
    protected String getTitleText() {
        return "Units";
    }

    @Override
    protected void setName(Unit item, String name) {
        nameBeforeRename = item.name;
        item.name = name;
    }
}
