package com.patechltd.salexfypos.ui.products;

import android.widget.Toast;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.entity.Brand;

import java.util.List;
import java.util.UUID;

public class BrandActivity extends DirectoryListActivity<Brand> {

    @Override
    protected List<Brand> loadAll() {
        return repo.directory.getBrands();
    }

    @Override
    protected String nameOf(Brand item) {
        return item.name;
    }

    @Override
    protected void insert(Brand item) {
        repo.directory.insertBrand(item);
    }

    @Override
    protected void update(Brand item) {
        repo.directory.updateBrand(item);
    }

    @Override
    protected void delete(Brand item) {
        if (repo.products.getAll().stream().anyMatch(p -> item.id.equals(p.brandId))) {
            throw new IllegalStateException("brand in use");
        }
        repo.directory.deleteBrand(item);
    }

    @Override
    protected Brand create(String name) {
        Brand b = new Brand();
        b.id = UUID.randomUUID().toString();
        b.name = name;
        b.createdAt = System.currentTimeMillis();
        return b;
    }

    @Override
    protected void deleteBlocked() {
        Toast.makeText(this, "Cannot delete: some products use this brand", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected int itemIcon() {
        return R.drawable.ic_star;
    }

    @Override
    protected String getTitleText() {
        return "Brands";
    }

    @Override
    protected void setName(Brand item, String name) {
        item.name = name;
    }
}
