package com.patechltd.salexfypos.ui.stock;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.security.Session;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Friendly one-shot stock page: shows each product's current stock with a
 * big editable "actual stock" field and quick-add chips (+10, +50, fill to
 * need). Used from checkout when selling more than you have, and from the
 * product page.
 */
public class QuickStockActivity extends AppCompatActivity {

    public static final String EXTRA_IDS = "ids";
    public static final String EXTRA_NAMES = "names";
    public static final String EXTRA_UNITS = "units";
    public static final String EXTRA_CURRENT = "current";
    public static final String EXTRA_NEEDED = "needed";
    public static final String EXTRA_HINT = "hint";

    private static class Row {
        final String productId;
        final String name;
        final String unit;
        final double current;
        final double needed;
        final EditText input;

        Row(String productId, String name, String unit, double current, double needed, EditText input) {
            this.productId = productId;
            this.name = name;
            this.unit = unit;
            this.current = current;
            this.needed = needed;
            this.input = input;
        }
    }

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Row> rows = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_stock);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        repo = Repository.get(this);

        String[] ids = getIntent().getStringArrayExtra(EXTRA_IDS);
        if (ids == null || ids.length == 0) {
            finish();
            return;
        }
        String[] names = getIntent().getStringArrayExtra(EXTRA_NAMES);
        String[] units = getIntent().getStringArrayExtra(EXTRA_UNITS);
        double[] current = getIntent().getDoubleArrayExtra(EXTRA_CURRENT);
        double[] needed = getIntent().getDoubleArrayExtra(EXTRA_NEEDED);

        String hint = getIntent().getStringExtra(EXTRA_HINT);
        if (hint != null && !hint.isEmpty()) {
            ((TextView) findViewById(R.id.hint)).setText(hint);
        }

        LinearLayout container = findViewById(R.id.rows_container);
        for (int i = 0; i < ids.length; i++) {
            double cur = current != null && i < current.length ? current[i] : 0;
            double need = needed != null && i < needed.length ? needed[i] : 0;
            String name = names != null && i < names.length && names[i] != null ? names[i] : "Product";
            String unit = units != null && i < units.length && units[i] != null ? units[i] : "";
            container.addView(buildRow(i, ids[i], name, unit, cur, need));
        }

        findViewById(R.id.btn_save).setOnClickListener(v -> save());
    }

    private View buildRow(int index, String productId, String name, String unit,
                          double current, double needed) {
        com.google.android.material.card.MaterialCardView card =
                new com.google.android.material.card.MaterialCardView(this);
        card.setRadius(dp(14));
        card.setCardBackgroundColor(getResources().getColor(R.color.surface));
        card.setCardElevation(dp(1));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        if (index > 0) cardLp.topMargin = dp(10);
        card.setLayoutParams(cardLp);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(14), dp(12), dp(14), dp(12));

        TextView title = new TextView(this);
        title.setText(name);
        title.setTextColor(getResources().getColor(R.color.text_primary));
        title.setTextSize(15);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        column.addView(title);

        TextView stockView = new TextView(this);
        boolean shortStock = current < needed - 0.001;
        String stockText = "Current stock: " + NumberUtil.qty(current)
                + (unit.isEmpty() ? "" : " " + unit);
        if (shortStock) {
            stockText += "  •  Needed: " + NumberUtil.qty(needed);
        }
        stockView.setText(stockText);
        stockView.setTextColor(getResources().getColor(
                shortStock ? R.color.error : R.color.text_secondary));
        stockView.setTextSize(13);
        stockView.setPadding(0, dp(4), 0, dp(4));
        column.addView(stockView);

        LinearLayout inputRow = new LinearLayout(this);
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(this);
        label.setText("Actual stock:");
        label.setTextColor(getResources().getColor(R.color.text_primary));
        label.setTextSize(13);
        inputRow.addView(label);

        EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        double initial = needed > current ? needed : current;
        input.setText(NumberUtil.qty(initial));
        input.setTextSize(14);
        input.setSelectAllOnFocus(true);
        input.setMinEms(4);
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        inputLp.setMarginStart(dp(10));
        inputRow.addView(input, inputLp);
        column.addView(inputRow);

        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, dp(8), 0, 0);
        chips.addView(quickChip("+10", () -> bump(input, 10)));
        chips.addView(quickChip("+50", () -> bump(input, 50)));
        if (shortStock) {
            chips.addView(quickChip("Fill to need", () -> input.setText(NumberUtil.qty(needed))));
        }
        column.addView(chips);

        card.addView(column);
        rows.add(new Row(productId, name, unit, current, needed, input));
        return card;
    }

    private MaterialButton quickChip(String text, Runnable action) {
        MaterialButton chip = new MaterialButton(this,
                null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        chip.setText(text);
        chip.setTextSize(12);
        chip.setMinWidth(0);
        chip.setMinimumWidth(0);
        chip.setPadding(dp(10), 0, dp(10), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(36));
        lp.setMarginEnd(dp(6));
        chip.setLayoutParams(lp);
        chip.setOnClickListener(v -> action.run());
        return chip;
    }

    private void bump(EditText input, double amount) {
        double base = NumberUtil.parse(input.getText() == null ? "" : input.getText().toString(), 0);
        input.setText(NumberUtil.qty(base + amount));
    }

    private void save() {
        final List<Row> snapshot = new ArrayList<>(rows);
        final String userId = Session.userId(this);
        repo.run(() -> {
            int changed = 0;
            for (Row row : snapshot) {
                double target = NumberUtil.parse(
                        row.input.getText() == null ? "" : row.input.getText().toString(),
                        row.current);
                double delta = target - row.current;
                if (Math.abs(delta) < 0.001) continue;
                Product p = repo.products.getById(row.productId);
                if (p == null) continue;
                repo.recordAdjustment(row.productId, delta,
                        "Quick stock from " + (getIntent().getBooleanExtra("fromCheckout", false)
                                ? "checkout" : "product page"),
                        userId, row.unit);
                changed++;
            }
            final int fChanged = changed;
            handler.post(() -> {
                Toast.makeText(this,
                        fChanged > 0 ? "Stock updated" : "No changes to save",
                        Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            });
        });
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
