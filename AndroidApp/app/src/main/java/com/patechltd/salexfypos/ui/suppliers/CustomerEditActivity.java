package com.patechltd.salexfypos.ui.suppliers;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Customer;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.UUID;

public class CustomerEditActivity extends AppCompatActivity {

    private Repository repo;
    private TextInputEditText name, phone, email, address, creditLimit, notes;
    private MaterialButton delete;
    private Customer customer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_edit);
        repo = Repository.get(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        name = findViewById(R.id.input_name);
        phone = findViewById(R.id.input_phone);
        email = findViewById(R.id.input_email);
        address = findViewById(R.id.input_address);
        creditLimit = findViewById(R.id.input_credit_limit);
        notes = findViewById(R.id.input_notes);
        delete = findViewById(R.id.btn_delete);
        delete.setOnClickListener(v -> delete());

        findViewById(R.id.btn_save).setOnClickListener(v -> save());

        String id = getIntent().getStringExtra("customerId");
        if (id == null) {
            toolbar.setTitle("Add Customer");
            delete.setVisibility(android.view.View.GONE);
        } else {
            toolbar.setTitle("Edit Customer");
            load(id);
        }
    }

    private void load(String id) {
        repo.run(() -> {
            customer = repo.suppliers.getCustomer(id);
            runOnUiThread(() -> {
                if (customer == null) {
                    finish();
                    return;
                }
                name.setText(customer.name);
                phone.setText(customer.phone == null ? "" : customer.phone);
                email.setText(customer.email == null ? "" : customer.email);
                address.setText(customer.address == null ? "" : customer.address);
                creditLimit.setText(NumberUtil.qty(customer.creditLimit));
                notes.setText(customer.notes == null ? "" : customer.notes);
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
            if (customer == null) {
                customer = new Customer();
                customer.id = UUID.randomUUID().toString();
                customer.createdAt = System.currentTimeMillis();
            }
            customer.name = n;
            customer.phone = phone.getText() == null ? "" : phone.getText().toString().trim();
            customer.email = email.getText() == null ? "" : email.getText().toString().trim();
            customer.address = address.getText() == null ? "" : address.getText().toString().trim();
            customer.creditLimit = NumberUtil.parse(creditLimit.getText() == null ? "" : creditLimit.getText().toString(), 0);
            customer.notes = notes.getText() == null ? "" : notes.getText().toString().trim();
            repo.suppliers.insertCustomer(customer);
            runOnUiThread(() -> {
                DialogUtil.toast(this, "Saved");
                finish();
            });
        });
    }

    private void delete() {
        DialogUtil.confirm(this, "Delete customer?",
                "Sale history will be kept.", () -> repo.run(() -> {
                    try {
                        repo.suppliers.deleteCustomer(customer);
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
