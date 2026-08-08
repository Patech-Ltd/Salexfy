package com.patechltd.salexfypos;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.patechltd.salexfypos.security.Session;
import com.patechltd.salexfypos.ui.crash.CrashRecoveryActivity;
import com.patechltd.salexfypos.ui.help.HelpFragment;
import com.patechltd.salexfypos.ui.login.LoginActivity;
import com.patechltd.salexfypos.ui.more.MoreFragment;
import com.patechltd.salexfypos.ui.products.ProductsFragment;
import com.patechltd.salexfypos.ui.reports.ReportsFragment;
import com.patechltd.salexfypos.ui.sell.SellFragment;
import com.patechltd.salexfypos.ui.stock.StockFragment;
import com.patechltd.salexfypos.util.Prefs;

public class MainActivity extends AppCompatActivity {

    private SellFragment sellFragment;
    private ProductsFragment productsFragment;
    private StockFragment stockFragment;
    private ReportsFragment reportsFragment;
    private MoreFragment moreFragment;
    private int currentTab = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String pendingCrash = Prefs.getString(this, Prefs.KEY_PENDING_CRASH, "");
        if (pendingCrash != null && !pendingCrash.isEmpty()) {
            startActivity(new Intent(this, CrashRecoveryActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            finish();
            return;
        }

        if (!Session.isLoggedIn(this)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_sell) {
                selectTab(0);
            } else if (id == R.id.nav_products) {
                selectTab(1);
            } else if (id == R.id.nav_stock) {
                selectTab(2);
            } else if (id == R.id.nav_reports) {
                selectTab(3);
            } else {
                selectTab(4);
            }
            return true;
        });
        selectTab(0);
    }

    private void selectTab(int tab) {
        if (tab == currentTab) return;
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction tx = fm.beginTransaction();
        Fragment toShow = null;
        if (sellFragment != null) tx.hide(sellFragment);
        if (productsFragment != null) tx.hide(productsFragment);
        if (stockFragment != null) tx.hide(stockFragment);
        if (reportsFragment != null) tx.hide(reportsFragment);
        if (moreFragment != null) tx.hide(moreFragment);

        switch (tab) {
            case 0:
                if (sellFragment == null) {
                    sellFragment = new SellFragment();
                    tx.add(R.id.fragment_container, sellFragment);
                }
                toShow = sellFragment;
                break;
            case 1:
                if (productsFragment == null) {
                    productsFragment = new ProductsFragment();
                    tx.add(R.id.fragment_container, productsFragment);
                }
                toShow = productsFragment;
                break;
            case 2:
                if (stockFragment == null) {
                    stockFragment = new StockFragment();
                    tx.add(R.id.fragment_container, stockFragment);
                }
                toShow = stockFragment;
                break;
            case 3:
                if (reportsFragment == null) {
                    reportsFragment = new ReportsFragment();
                    tx.add(R.id.fragment_container, reportsFragment);
                }
                toShow = reportsFragment;
                break;
            case 4:
                if (moreFragment == null) {
                    moreFragment = new MoreFragment();
                    tx.add(R.id.fragment_container, moreFragment);
                }
                toShow = moreFragment;
                break;
        }
        if (toShow != null) tx.show(toShow);
        tx.commitAllowingStateLoss();
        currentTab = tab;
    }

    public void openTab(int tab) {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        int id = tab == 0 ? R.id.nav_sell
                : tab == 1 ? R.id.nav_products
                : tab == 2 ? R.id.nav_stock
                : tab == 3 ? R.id.nav_reports : R.id.nav_more;
        nav.setSelectedItemId(id);
        selectTab(tab);
    }

    public void logout() {
        Session.end(this);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
