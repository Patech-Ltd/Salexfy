package com.patechltd.salexfypos.ui.products;

import android.widget.Toast;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.entity.Category;

import java.util.List;
import java.util.UUID;

public class CategoryActivity extends DirectoryListActivity<Category> {

    @Override
    protected List<Category> loadAll() {
        return repo.directory.getCategories();
    }

    @Override
    protected String nameOf(Category item) {
        return item.name;
    }

    @Override
    protected void insert(Category item) {
        repo.directory.insertCategory(item);
    }

    @Override
    protected void update(Category item) {
        repo.directory.updateCategory(item);
    }

    @Override
    protected void delete(Category item) {
        if (repo.products.getAll().stream().anyMatch(p -> item.uid.equals(p.categoryId))) {
            throw new IllegalStateException("category in use");
        }
        repo.directory.deleteCategory(item);
    }

    @Override
    protected Category create(String name) {
        Category c = new Category();
        c.uid = UUID.randomUUID().toString();
        c.name = name;
        c.sortOrder = entities.size() + 1;
        c.createdAt = System.currentTimeMillis();
        return c;
    }

    @Override
    protected void deleteBlocked() {
        Toast.makeText(this, "Cannot delete: some products use this category", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected int itemIcon() {
        return R.drawable.ic_filter;
    }

    @Override
    protected String getTitleText() {
        return "Categories";
    }

    @Override
    protected void setName(Category item, String name) {
        item.name = name;
    }
}
