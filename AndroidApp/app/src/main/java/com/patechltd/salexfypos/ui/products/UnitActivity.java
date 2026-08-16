package com.patechltd.salexfypos.ui.products;

import android.widget.Toast;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.entity.Unit;

import java.util.List;
import java.util.UUID;

public class UnitActivity extends DirectoryListActivity<Unit> {

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
        repo.directory.updateUnit(item);
    }

    @Override
    protected void delete(Unit item) {
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
        Toast.makeText(this, "Could not delete unit", Toast.LENGTH_SHORT).show();
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
        item.name = name;
    }
}
