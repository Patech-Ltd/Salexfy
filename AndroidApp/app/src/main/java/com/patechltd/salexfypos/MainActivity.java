package com.patechltd.salexfypos;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.badge.BadgeDrawable;
import com.patechltd.salexfypos.security.Session;
import com.patechltd.salexfypos.ui.crash.CrashRecoveryActivity;
import com.patechltd.salexfypos.ui.login.AppLockActivity;
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
    private boolean launchedFromCreate = false;
    private BottomNavigationView bottomNav;

    private long lastInteractionTime;
    private final Handler idleHandler = new Handler(Looper.getMainLooper());
    private static final long DEFAULT_IDLE_TIMEOUT_MS = 2 * 60 * 1000;
    private static final int CHECK_IDLE_INTERVAL_MS = 10_000;

    private final Runnable idleChecker = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || isDestroyed()) return;
            if (!Prefs.getBoolean(MainActivity.this, Prefs.KEY_APP_LOCK, false)) {
                idleHandler.removeCallbacks(this);
                return;
            }
            long timeout = Prefs.getLong(MainActivity.this, Prefs.KEY_APP_LOCK_TIMEOUT_MS, DEFAULT_IDLE_TIMEOUT_MS);
            if (timeout <= 0) {
                idleHandler.removeCallbacks(this);
                return;
            }
            long elapsed = System.currentTimeMillis() - lastInteractionTime;
            if (elapsed >= timeout) {
                idleHandler.removeCallbacks(this);
                lockApp();
            } else {
                idleHandler.postDelayed(this, CHECK_IDLE_INTERVAL_MS);
            }
        }
    };

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

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
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
        launchedFromCreate = true;
        lastInteractionTime = System.currentTimeMillis();
        runLicenseGateCheck();
    }

    private void runLicenseGateCheck() {
        new Thread(() -> {
            try {
                com.patechltd.salexfypos.license.LicenseManager.Result r =
                        com.patechltd.salexfypos.license.LicenseManager.validate(this);
                if (!r.valid) {
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        Intent i = new Intent(this,
                                com.patechltd.salexfypos.license.LicenseGateActivity.class);
                        i.putExtra("message", r.message);
                        i.putExtra("expired", r.expired);
                        i.putExtra("expiresAt", r.expiresAt);
                        startActivity(i);
                    });
                }
            } catch (Throwable ignored) {
                // fail-open on unexpected errors
            }
        }).start();
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        lastInteractionTime = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (launchedFromCreate) {
            launchedFromCreate = false;
            lastInteractionTime = System.currentTimeMillis();
            startIdleChecker();
            return;
        }

        if (Prefs.getBoolean(this, Prefs.KEY_APP_LOCK_NEEDS_REAUTH, false)) {
            Prefs.putBoolean(this, Prefs.KEY_APP_LOCK_NEEDS_REAUTH, false);
            lockApp();
        } else if (Prefs.getBoolean(this, Prefs.KEY_APP_LOCK, false)) {
            long lastUnlock = Prefs.getLong(this, Prefs.KEY_APP_LOCK_LAST_UNLOCK, 0);
            long timeout = Prefs.getLong(this, Prefs.KEY_APP_LOCK_TIMEOUT_MS, DEFAULT_IDLE_TIMEOUT_MS);
            long idle = System.currentTimeMillis() - lastUnlock;
            if (timeout > 0 && idle >= timeout) {
                lockApp();
            }
        }
        lastInteractionTime = System.currentTimeMillis();
        startIdleChecker();
    }

    @Override
    protected void onPause() {
        super.onPause();
        idleHandler.removeCallbacks(idleChecker);
        if (Prefs.getBoolean(this, Prefs.KEY_APP_LOCK, false)) {
            Prefs.putBoolean(this, Prefs.KEY_APP_LOCK_NEEDS_REAUTH, true);
        }
    }

    private void lockApp() {
        Intent intent = new Intent(this, AppLockActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    private void startIdleChecker() {
        idleHandler.removeCallbacks(idleChecker);
        if (Prefs.getBoolean(this, Prefs.KEY_APP_LOCK, false)) {
            idleHandler.postDelayed(idleChecker, CHECK_IDLE_INTERVAL_MS);
        }
    }

    public void updateHeldBadge(int count) {
        if (bottomNav == null) return;
        BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_sell);
        if (count > 0) {
            badge.setNumber(count);
            badge.setVisible(true);
        } else {
            badge.setVisible(false);
        }
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
        int id = tab == 0 ? R.id.nav_sell
                : tab == 1 ? R.id.nav_products
                : tab == 2 ? R.id.nav_stock
                : tab == 3 ? R.id.nav_reports : R.id.nav_more;
        bottomNav.setSelectedItemId(id);
        selectTab(tab);
    }

    public void logout() {
        Session.end(this);
        Prefs.putBoolean(this, Prefs.KEY_APP_LOCK_NEEDS_REAUTH, false);
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
