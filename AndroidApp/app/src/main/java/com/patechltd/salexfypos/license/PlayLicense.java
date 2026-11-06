package com.patechltd.salexfypos.license;

import android.app.Activity;
import android.content.Context;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.patechltd.salexfypos.util.Prefs;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Google Play licensing: the app is usable while the shop owns the configured product
 * (see {@link LicenseConfig}). Entitlement is re-read from Play on every check; if Play is
 * unreachable the last successful check is honoured for PLAY_OFFLINE_GRACE_DAYS.
 */
public final class PlayLicense {

    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final long CLOCK_SKEW_MS = 10 * 60 * 1000L;
    private static final int TIMEOUT_SEC = 10;

    private PlayLicense() {
    }

    private static String productType() {
        return LicenseConfig.PLAY_IS_SUBSCRIPTION
                ? BillingClient.ProductType.SUBS : BillingClient.ProductType.INAPP;
    }

    private static BillingClient newClient(Context context, PurchasesUpdatedListener l) {
        return BillingClient.newBuilder(context.getApplicationContext())
                .setListener(l)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build();
    }

    private static boolean owns(List<Purchase> purchases) {
        if (purchases == null) return false;
        for (Purchase p : purchases) {
            if (p.getPurchaseState() == Purchase.PurchaseState.PURCHASED
                    && p.getProducts().contains(LicenseConfig.PLAY_PRODUCT_ID)) {
                return true;
            }
        }
        return false;
    }

    private static void acknowledgeAll(BillingClient client, List<Purchase> purchases) {
        if (purchases == null) return;
        for (Purchase p : purchases) {
            if (p.getPurchaseState() == Purchase.PurchaseState.PURCHASED && !p.isAcknowledged()) {
                client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(p.getPurchaseToken()).build(), r -> { });
            }
        }
    }

    /** Blocking (call from a background thread). */
    static LicenseManager.Result validate(Context context) {
        final AtomicReference<List<Purchase>> found = new AtomicReference<>();
        final AtomicReference<Boolean> ok = new AtomicReference<>(false);
        final CountDownLatch done = new CountDownLatch(1);
        final BillingClient client = newClient(context, (r, p) -> { });
        try {
            client.startConnection(new BillingClientStateListener() {
                @Override
                public void onBillingSetupFinished(BillingResult setup) {
                    if (setup.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                        done.countDown();
                        return;
                    }
                    client.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
                            .setProductType(productType()).build(), (result, purchases) -> {
                        if (result.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                            ok.set(true);
                            found.set(purchases);
                            acknowledgeAll(client, purchases);
                        }
                        done.countDown();
                    });
                }

                @Override
                public void onBillingServiceDisconnected() {
                    done.countDown();
                }
            });
            done.await(TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // fall through to offline path
        }

        long now = System.currentTimeMillis();
        if (ok.get()) {
            try {
                client.endConnection();
            } catch (Exception ignored) {
                // nothing to do
            }
            if (owns(found.get())) {
                markVerified(context, now);
                return new LicenseManager.Result(true, false, "Licensed via Google Play", 0);
            }
            Prefs.putLong(context, Prefs.KEY_PLAY_VERIFIED_AT, 0);
            return new LicenseManager.Result(false, false,
                    "No active Google Play purchase found for this account.", 0);
        }
        try {
            client.endConnection();
        } catch (Exception ignored) {
            // nothing to do
        }
        return offline(context, now);
    }

    private static LicenseManager.Result offline(Context context, long now) {
        long verified = Prefs.getLong(context, Prefs.KEY_PLAY_VERIFIED_AT, 0);
        long watermark = Prefs.getLong(context, Prefs.KEY_LICENSE_WATERMARK, 0);
        if (verified <= 0) {
            return new LicenseManager.Result(false, false,
                    "Could not reach Google Play. Connect to the internet to verify your purchase.", 0);
        }
        if (watermark > 0 && now + CLOCK_SKEW_MS < watermark) {
            return new LicenseManager.Result(false, false,
                    "Device clock was set back. Correct the date/time and reconnect.", 0);
        }
        long left = verified + LicenseConfig.PLAY_OFFLINE_GRACE_DAYS * DAY - now;
        if (left <= 0) {
            return new LicenseManager.Result(false, false,
                    "License check overdue. Connect to the internet to verify with Google Play.", 0);
        }
        if (now > watermark) Prefs.putLong(context, Prefs.KEY_LICENSE_WATERMARK, now);
        return new LicenseManager.Result(true, false,
                "Offline - verify with Google Play within " + (left / DAY + 1) + " day(s)", 0);
    }

    private static void markVerified(Context context, long now) {
        Prefs.putLong(context, Prefs.KEY_PLAY_VERIFIED_AT, now);
        if (now > Prefs.getLong(context, Prefs.KEY_LICENSE_WATERMARK, 0)) {
            Prefs.putLong(context, Prefs.KEY_LICENSE_WATERMARK, now);
        }
    }

    // ------------------------------------------------------------------ purchase flow

    public interface PurchaseCallback {
        void onPurchased();

        void onMessage(String message);
    }

    /** Keeps the client alive for the purchase sheet; call {@link #close} in onDestroy. */
    public static final class Session {
        private BillingClient client;

        public void close() {
            if (client != null) {
                try {
                    client.endConnection();
                } catch (Exception ignored) {
                    // nothing to do
                }
                client = null;
            }
        }
    }

    public static void startPurchase(final Activity activity, final Session session,
                                     final PurchaseCallback cb) {
        session.close();
        final BillingClient[] holder = new BillingClient[1];
        holder[0] = newClient(activity, (result, purchases) -> {
            int code = result.getResponseCode();
            if (code == BillingClient.BillingResponseCode.OK && owns(purchases)) {
                acknowledgeAll(holder[0], purchases);
                markVerified(activity, System.currentTimeMillis());
                activity.runOnUiThread(cb::onPurchased);
            } else if (code == BillingClient.BillingResponseCode.USER_CANCELED) {
                activity.runOnUiThread(() -> cb.onMessage("Purchase cancelled"));
            } else if (code == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                activity.runOnUiThread(() -> cb.onMessage("Already purchased - tap \"Restore purchase\""));
            } else {
                activity.runOnUiThread(() -> cb.onMessage("Purchase failed: " + result.getDebugMessage()));
            }
        });
        session.client = holder[0];
        holder[0].startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult setup) {
                if (setup.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                    activity.runOnUiThread(() -> cb.onMessage("Google Play Billing unavailable"));
                    return;
                }
                QueryProductDetailsParams.Product product = QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(LicenseConfig.PLAY_PRODUCT_ID).setProductType(productType()).build();
                holder[0].queryProductDetailsAsync(QueryProductDetailsParams.newBuilder()
                        .setProductList(java.util.Collections.singletonList(product)).build(), (r, list) -> {
                    if (r.getResponseCode() != BillingClient.BillingResponseCode.OK
                            || list.getProductDetailsList().isEmpty()) {
                        activity.runOnUiThread(() -> cb.onMessage("Product not available in Google Play"));
                        return;
                    }
                    ProductDetails d = list.getProductDetailsList().get(0);
                    BillingFlowParams.ProductDetailsParams.Builder pd = BillingFlowParams.ProductDetailsParams
                            .newBuilder().setProductDetails(d);
                    if (LicenseConfig.PLAY_IS_SUBSCRIPTION) {
                        if (d.getSubscriptionOfferDetails() == null || d.getSubscriptionOfferDetails().isEmpty()) {
                            activity.runOnUiThread(() -> cb.onMessage("No subscription offer available"));
                            return;
                        }
                        pd.setOfferToken(d.getSubscriptionOfferDetails().get(0).getOfferToken());
                    }
                    BillingFlowParams flow = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(java.util.Collections.singletonList(pd.build())).build();
                    activity.runOnUiThread(() -> holder[0].launchBillingFlow(activity, flow));
                });
            }

            @Override
            public void onBillingServiceDisconnected() {
                // user can tap Buy again
            }
        });
    }
}
