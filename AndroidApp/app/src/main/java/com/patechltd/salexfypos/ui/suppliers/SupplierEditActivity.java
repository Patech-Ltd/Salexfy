package com.patechltd.salexfypos.ui.suppliers;

import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Supplier;
import com.patechltd.salexfypos.util.DialogUtil;

import java.util.UUID;

public class SupplierEditActivity extends AppCompatActivity {

    private Repository repo;
    private TextInputEditText name, phone, email, address, notes;
    private MaterialButton delete;
    private Supplier supplier;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_partner_edit);
        repo = Repository.get(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        name = findViewById(R.id.input_name);
        phone = findViewById(R.id.input_phone);
        email = findViewById(R.id.input_email);
        address = findViewById(R.id.input_address);
        notes = findViewById(R.id.input_notes);
        delete = findViewById(R.id.btn_delete);
        delete.setOnClickListener(v -> delete());

        findViewById(R.id.btn_save).setOnClickListener(v -> save());

        String id = getIntent().getStringExtra("supplierId");
        if (id == null) {
            toolbar.setTitle("Add Supplier");
            delete.setVisibility(android.view.View.GONE);
        } else {
            toolbar.setTitle("Edit Supplier");
            load(id);
        }
    }

    private void load(String id) {
        repo.run(() -> {
            supplier = repo.suppliers.getSupplier(id);
            runOnUiThread(() -> {
                if (supplier == null) {
                    finish();
                    return;
                }
                name.setText(supplier.name);
                phone.setText(supplier.phone == null ? "" : supplier.phone);
                email.setText(supplier.email == null ? "" : supplier.email);
                address.setText(supplier.address == null ? "" : supplier.address);
                notes.setText(supplier.notes == null ? "" : supplier.notes);
                delete.setVisibility(android.view.View.VISIBLE);
            });
        });
    }

    private void save() {
        String n = name.getText() == null ? "" : name.getText().toString().trim();
        if (n.isEmpty()) {
            DialogUtil.toast(this, "Name is required");
            return;
        }
        repo.run(() -> {
            if (supplier == null) {
                supplier = new Supplier();
                supplier.id = UUID.randomUUID().toString();
                supplier.createdAt = System.currentTimeMillis();
            }
            supplier.name = n;
            supplier.phone = phone.getText() == null ? "" : phone.getText().toString().trim();
            supplier.email = email.getText() == null ? "" : email.getText().toString().trim();
            supplier.address = address.getText() == null ? "" : address.getText().toString().trim();
            supplier.notes = notes.getText() == null ? "" : notes.getText().toString().trim();
            repo.suppliers.insertSupplier(supplier);
            runOnUiThread(() -> {
                DialogUtil.toast(this, "Saved");
                finish();
            });
        });
    }

    private void delete() {
        DialogUtil.confirm(this, "Delete supplier?",
                "Past purchases will be kept.", () -> repo.run(() -> {
                    try {
                        repo.suppliers.deleteSupplier(supplier);
                        runOnUiThread(() -> {
                            DialogUtil.toast(this, "Deleted");
                            finish();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> DialogUtil.toast(this, "Could not delete"));
                    }
                }));
    }
}
