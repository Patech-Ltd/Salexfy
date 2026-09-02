package com.patechltd.salexfypos.ui.help;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.patechltd.salexfypos.R;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Revamped help page: tappable section cards that expand/collapse to reveal
 * short guides for every part of the app.
 */
public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        LinearLayout container = findViewById(R.id.help_container);

        addSection(container, R.drawable.ic_cart, "Selling & Checkout",
                "Open the Sell tab and search a product or scan a barcode / QR code to add it to "
                        + "the cart.\n\n"
                        + "Tap a line to change the quantity or price. Switch between retail and "
                        + "wholesale prices with the toggle.\n\n"
                        + "Tap Checkout, pick a payment method (Cash, Card, M-Pesa or On Credit) and "
                        + "the receipt is ready to print or share.\n\n"
                        + "Hold a sale to set it aside and recall it later. A draft sale is saved "
                        + "automatically even if you leave the screen.");

        addSection(container, R.drawable.ic_inventory, "Products & Stock",
                "Add products one by one, use Quick Add for speed, or add many at once with the "
                        + "Batch button on the Products tab.\n\n"
                        + "Every product can have a retail and a wholesale price, a Buying Price, a "
                        + "barcode / SKU and a reorder level.\n\n"
                        + "The Products tab shows the live stock balance for each item and flags "
                        + "items that are running low.\n\n"
                        + "On the Stock tab you can record Purchases, make quick stock adjustments, "
                        + "and run a full count with Stock Take.");

        addSection(container, R.drawable.ic_truck, "Purchases",
                "Record purchases from your suppliers on the Stock tab. Set the quantity and "
                        + "purchase price for each item and stock is added automatically.\n\n"
                        + "Don't see your supplier? You can add a new one right from the purchase "
                        + "screen and it is selected for you.\n\n"
                        + "You can preview and edit a purchase before confirming it.");

        addSection(container, R.drawable.ic_wallet, "Customers & Debtors",
                "Keep a list of customers for credit sales and track what each one owes you.\n\n"
                        + "Sell On Credit to record a debt, then record payments against it from the "
                        + "customer's page.\n\n"
                        + "Every customer shows a history of their purchases and balances in one "
                        + "place.");

        addSection(container, R.drawable.ic_reports, "Reports & TOT",
                "The Reports tab shows sales, profit, TOT tax, top products and cashier totals for "
                        + "a day or any date range.\n\n"
                        + "More → TOT Report is your monthly turnover tax statement: 1.5% of total "
                        + "sales (not profit), with a day-by-day breakdown per month.\n\n"
                        + "Export any report to Excel or PDF straight from the device.");

        addSection(container, R.drawable.ic_excel, "Excel Exports",
                "You can export your data to Excel files and save them to the device or "
                        + "Google Drive:\n\n"
                        + "• Products list - on the Products tab\n"
                        + "• Sales list - on the Sales screen (respects the dates you selected)\n"
                        + "• Reports and the monthly TOT statement\n\n"
                        + "Just tap the export button and choose where to save the .xlsx file.");

        addSection(container, R.drawable.ic_users, "Users & Roles",
                "More → Users & Roles lets you manage who can use the app.\n\n"
                        + "Each role has its own permissions - for example only managers can view "
                        + "reports or edit prices.\n\n"
                        + "Ask your administrator if you don't have access to a screen.");

        addSection(container, R.drawable.ic_backup, "Backup & Restore",
                "The app keeps automatic daily backups on internal storage (the last few are "
                        + "kept).\n\n"
                        + "For a manual copy go to Settings → Backup now.\n\n"
                        + "Restoring replaces all current data - always make a fresh backup first.");

        addSection(container, R.drawable.ic_settings, "Settings",
                "Settings covers your shop details (name, currency, tax), printer and scanner "
                        + "set-up, loyalty points, and data sync.\n\n"
                        + "Tax is used for the turnover tax (TOT) on sales and is set to 1.5% "
                        + "by default.");

        addSection(container, R.drawable.ic_help, "Troubleshooting",
                "If something goes wrong:\n\n"
                        + "• Check More → Debug & Crash Logs for recent errors.\n"
                        + "• Make sure you are signed in with the right role and permissions.\n"
                        + "• Restore the most recent backup if data looks wrong.\n\n"
                        + "If the problem continues, contact your shop administrator for support.");
    }

    private void addSection(LinearLayout container, int iconRes, String title, String body) {
        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(dp(14));
        card.setCardElevation(0);
        card.setStrokeWidth(dp(1));
        card.setCardBackgroundColor(ContextCompat.getColor(this, R.color.surface));
        card.setStrokeColor(ContextCompat.getColor(this, R.color.outline));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(cardLp);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        card.addView(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(12), dp(16), dp(12));
        root.addView(header);

        ImageView icon = new ImageView(this);
        icon.setImageResource(iconRes);
        icon.setBackgroundResource(R.drawable.circle_brand_light);
        icon.setPadding(dp(10), dp(10), dp(10), dp(10));
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp(40), dp(40));
        header.addView(icon, iconLp);

        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        titleView.setTextSize(15);
        titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);
        titleView.setPadding(dp(14), 0, 0, 0);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        header.addView(titleView, titleLp);

        ImageView chevron = new ImageView(this);
        chevron.setImageResource(R.drawable.ic_chevron_down);
        LinearLayout.LayoutParams chevronLp = new LinearLayout.LayoutParams(dp(22), dp(22));
        header.addView(chevron, chevronLp);

        TextView bodyView = new TextView(this);
        bodyView.setText(body);
        bodyView.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        bodyView.setTextSize(14);
        bodyView.setLineSpacing(0, 1.25f);
        bodyView.setPadding(dp(16), 0, dp(16), dp(16));
        bodyView.setVisibility(View.GONE);
        root.addView(bodyView);

        header.setOnClickListener(v -> {
            boolean open = bodyView.getVisibility() == View.VISIBLE;
            bodyView.setVisibility(open ? View.GONE : View.VISIBLE);
            chevron.animate().rotation(open ? 0 : 180).setDuration(150).start();
        });

        container.addView(card);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}