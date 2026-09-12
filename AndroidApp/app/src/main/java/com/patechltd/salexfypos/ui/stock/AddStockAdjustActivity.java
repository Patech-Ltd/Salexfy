package com.patechltd.salexfypos.ui.stock;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.security.Session;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.KeyboardUtil;
import com.patechltd.salexfypos.util.SoundUtil;

/**
 * Full-page Add/Remove stock for one product. No absolute stock values are ever
 * written — every save is a logged add or remove with the previous and new
 * balance recorded as a stock movement.
 */
public class AddStockAdjustActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "productId";

    private Repository repo;
    private String productId;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_adjust);
        KeyboardUtil.makeAdjustResize(findViewById(R.id.root));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        repo = Repository.get(this);
        productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        if (productId == null) {
            finish();
            return;
        }

        repo.run(() -> {
            Product p = repo.products.getById(productId);
            if (p == null) {
                runOnUiThread(this::finish);
                return;
            }
            final String name = p.name;
            final String barcode = p.barcode;
            final String unit = p.retailUnit == null ? "" : p.retailUnit;
            final double current = repo.stock.currentQty(productId);
            runOnUiThread(() -> render(name, barcode, unit, current));
        });
    }

    private void render(String name, String barcode, String unit, double current) {
        final String unitSuffix = unit.isEmpty() ? "" : " " + unit;

        TextView nameView = findViewById(R.id.dg_name);
        TextView currentView = findViewById(R.id.dg_current);
        TextView previewView = findViewById(R.id.dg_preview);
        TextView changeView = findViewById(R.id.dg_change);
        TextInputLayout qtyLayout = findViewById(R.id.dg_qty_layout);
        EditText qtyView = qtyLayout.getEditText();
        EditText noteView = findViewById(R.id.dg_note);
        MaterialButtonToggleGroup toggle = findViewById(R.id.dg_toggle);
        MaterialButton save = findViewById(R.id.btn_save);

        nameView.setText(name);
        currentView.setText("Currently " + NumberUtil.qty(current) + unitSuffix
                + (barcode != null && !barcode.isEmpty() ? "  •  " + barcode : ""));
        qtyLayout.setSuffixText(unit.isEmpty() ? null : unit);

        final boolean[] addMode = {true};
        Runnable update = () -> {
            double qty = NumberUtil.parse(
                    qtyView.getText() == null ? "" : qtyView.getText().toString(), 0);
            double after = Math.max(0, current + (addMode[0] ? qty : -qty));
            double change = after - current;
            String sign = change > 0 ? "+" : change < 0 ? "-" : "";
            previewView.setText(NumberUtil.qty(after) + unitSuffix);
            previewView.setTextColor(getColor(change < 0
                    ? R.color.error : change > 0 ? R.color.success : R.color.amount_text));
            changeView.setText("Current " + NumberUtil.qty(current) + unitSuffix
                    + "  →  " + sign + NumberUtil.qty(Math.abs(change)) + unitSuffix
                    + (addMode[0] ? "  (adding)" : "  (removing)"));
        };

        qtyView.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable s) {
                qtyLayout.setError(null);
                update.run();
            }
        });

        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            addMode[0] = checkedId == R.id.btn_add;
            boolean add = addMode[0];
            qtyLayout.setStartIconDrawable(add ? R.drawable.ic_add : R.drawable.ic_delete);
            qtyLayout.setStartIconTintList(android.content.res.ColorStateList.valueOf(
                    getColor(add ? R.color.success : R.color.error)));
            save.setText(add ? "Add to stock" : "Remove from stock");
            save.setIconResource(add ? R.drawable.ic_add : R.drawable.ic_delete);
            update.run();
        });
        toggle.check(R.id.btn_add);

        LinearLayout chips = findViewById(R.id.dg_chips);
        for (final int amount : new int[]{1, 5, 10, 50, 100}) {
            MaterialButton chip = quickChip("+" + amount);
            chip.setOnClickListener(v -> {
                double base = NumberUtil.parse(
                        qtyView.getText() == null ? "" : qtyView.getText().toString(), 0);
                qtyView.setText(NumberUtil.qty(base + amount));
                qtyView.requestFocus();
            });
            chips.addView(chip);
        }

        save.setOnClickListener(v -> {
            double qty = NumberUtil.parse(
                    qtyView.getText() == null ? "" : qtyView.getText().toString(), 0);
            if (qty <= 0) {
                qtyLayout.setError("Enter a quantity");
                qtyView.requestFocus();
                return;
            }
            double after = Math.max(0, current + (addMode[0] ? qty : -qty));
            final double delta = after - current;
            if (Math.abs(delta) < 0.001) {
                qtyLayout.setError("Nothing to change");
                return;
            }
            String note = noteView.getText() == null ? "" : noteView.getText().toString().trim();
            if (note.isEmpty()) note = addMode[0] ? "Manual add" : "Manual remove";
            final String fNote = note;
            final String userId = Session.userId(this);
            final double fDelta = delta;
            repo.run(() -> {
                repo.recordAdjustment(productId, fDelta, fNote, userId, unit);
                handler.post(() -> {
                    SoundUtil.beep();
                    Toast.makeText(this,
                            (delta > 0 ? "Added " : "Removed ") + NumberUtil.qty(Math.abs(delta))
                                    + unitSuffix + "  •  " + name,
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
            });
        });
    }

    private MaterialButton quickChip(String text) {
        MaterialButton chip = new MaterialButton(this,
                null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        chip.setText(text);
        chip.setTextSize(13);
        chip.setMinWidth(0);
        chip.setMinimumWidth(0);
        chip.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
        lp.setMarginEnd(dp(6));
        chip.setLayoutParams(lp);
        return chip;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}