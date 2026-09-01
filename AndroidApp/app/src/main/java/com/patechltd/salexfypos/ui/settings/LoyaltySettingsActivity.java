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

public class LoyaltySettingsActivity extends AppCompatActivity {

    private TextInputEditText loyaltyPoints, loyaltyValue;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loyalty_settings);

        ((com.google.android.material.appbar.MaterialToolbar) findViewById(R.id.toolbar))
                .setNavigationOnClickListener(v -> finish());

        loyaltyPoints = findViewById(R.id.input_loyalty_points);
        loyaltyValue = findViewById(R.id.input_loyalty_value);

        load();

        ((MaterialButton) findViewById(R.id.btn_save)).setOnClickListener(v -> save());
    }

    private void load() {
        loyaltyPoints.setText(String.valueOf(Prefs.getDouble(this, Prefs.KEY_LOYALTY_POINTS_PER_MONEY, 1.0)));
        loyaltyValue.setText(String.valueOf(Prefs.getDouble(this, Prefs.KEY_LOYALTY_POINT_VALUE, 0.5)));
    }

    private void save() {
        double pp = NumberUtil.parse(
                loyaltyPoints.getText() == null ? "" : loyaltyPoints.getText().toString(), 1.0);
        Prefs.putDouble(this, Prefs.KEY_LOYALTY_POINTS_PER_MONEY, Math.max(0, pp));
        double pv = NumberUtil.parse(
                loyaltyValue.getText() == null ? "" : loyaltyValue.getText().toString(), 0.5);
        Prefs.putDouble(this, Prefs.KEY_LOYALTY_POINT_VALUE, Math.max(0, pv));
        DialogUtil.toast(this, "Settings saved");
        finish();
    }
}
