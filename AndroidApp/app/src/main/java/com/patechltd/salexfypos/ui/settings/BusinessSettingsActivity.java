package com.patechltd.salexfypos.ui.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;

public class BusinessSettingsActivity extends AppCompatActivity {

    private TextInputEditText shopName, address, phone, currency, tax, footer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_settings);

        ((com.google.android.material.appbar.MaterialToolbar) findViewById(R.id.toolbar))
                .setNavigationOnClickListener(v -> finish());

        shopName = findViewById(R.id.input_shop_name);
        address = findViewById(R.id.input_shop_address);
        phone = findViewById(R.id.input_phone);
        currency = findViewById(R.id.input_currency);
        tax = findViewById(R.id.input_tax);
        footer = findViewById(R.id.input_footer);

        load();

        ((MaterialButton) findViewById(R.id.btn_save)).setOnClickListener(v -> save());
    }

    private void load() {
        shopName.setText(Prefs.getString(this, Prefs.KEY_SHOP_NAME, "My Shop"));
        address.setText(Prefs.getString(this, Prefs.KEY_SHOP_ADDRESS, ""));
        phone.setText(Prefs.getString(this, Prefs.KEY_SHOP_PHONE, ""));
        currency.setText(Prefs.currency(this));
        tax.setText(String.valueOf(Prefs.taxPercent(this)));
        footer.setText(Prefs.getString(this, Prefs.KEY_RECEIPT_FOOTER, "Thank you!"));
    }

    private void save() {
        Prefs.putString(this, Prefs.KEY_SHOP_NAME,
                shopName.getText() == null ? "" : shopName.getText().toString().trim());
        Prefs.putString(this, Prefs.KEY_SHOP_ADDRESS,
                address.getText() == null ? "" : address.getText().toString().trim());
        Prefs.putString(this, Prefs.KEY_SHOP_PHONE,
                phone.getText() == null ? "" : phone.getText().toString().trim());
        Prefs.putString(this, Prefs.KEY_CURRENCY,
                currency.getText() == null ? "KSh" : currency.getText().toString().trim());
        double t = NumberUtil.parse(tax.getText() == null ? "" : tax.getText().toString(), 0);
        Prefs.putDouble(this, Prefs.KEY_TAX_PERCENT, Math.max(0, t));
        Prefs.putString(this, Prefs.KEY_RECEIPT_FOOTER,
                footer.getText() == null ? "" : footer.getText().toString().trim());
        DialogUtil.toast(this, "Settings saved");
        finish();
    }
}
