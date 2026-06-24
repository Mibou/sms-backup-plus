package sms.backup.plus.activity.donation;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import sms.backup.plus.BuildConfig;
import sms.backup.plus.R;
import sms.backup.plus.activity.ThemeActivity;
import sms.backup.plus.activity.donation.DonationListFragment.SkuSelectionListener;
import sms.backup.plus.utils.BundleBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.widget.Toast.LENGTH_LONG;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.*;
import static com.android.billingclient.api.BillingClient.ProductType.INAPP;
import static com.android.billingclient.api.Purchase.PurchaseState.PURCHASED;
import static sms.backup.plus.App.TAG;
import static sms.backup.plus.Consts.Billing.ALL_SKUS;
import static sms.backup.plus.Consts.Billing.DONATION_PREFIX;
import static sms.backup.plus.activity.donation.DonationActivity.DonationStatusListener.State.DONATED;
import static sms.backup.plus.activity.donation.DonationActivity.DonationStatusListener.State.NOT_AVAILABLE;
import static sms.backup.plus.activity.donation.DonationActivity.DonationStatusListener.State.NOT_DONATED;
import static sms.backup.plus.activity.donation.DonationActivity.DonationStatusListener.State.UNKNOWN;
import static sms.backup.plus.activity.donation.DonationListFragment.SKUS;

public class DonationActivity extends ThemeActivity implements
        ProductDetailsResponseListener,
        PurchasesUpdatedListener,
        SkuSelectionListener {

    public interface DonationStatusListener {
        enum State {
            DONATED,
            NOT_DONATED,
            UNKNOWN,
            NOT_AVAILABLE
        }
        void userDonationState(State state);
    }

    private static boolean DEBUG_IAB = BuildConfig.DEBUG;
    private @Nullable BillingClient billingClient;
    private final Map<String, ProductDetails> productDetails = new HashMap<>();
    private boolean stateSaved;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        billingClient = BillingClient.newBuilder(this)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build();
        billingClient.startConnection(new BillingClientStateListener() {

            @Override public void onBillingSetupFinished(BillingResult resultCode) {
                log("onBillingSetupFinished(" + resultCode + ")" + Thread.currentThread().getName());

                switch (resultCode.getResponseCode()) {
                    case OK:
                        queryAvailableSkus();
                        break;

                    default:
                        Toast.makeText(DonationActivity.this, R.string.donation_error_iab_unavailable, LENGTH_LONG).show();
                        Log.w(TAG, "Problem setting up in-app billing: " + resultCode);
                        finish();
                        break;
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                Log.d(TAG, "onBillingServiceDisconnected");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingClient != null) {
            billingClient.endConnection();
            billingClient = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        stateSaved = true;
    }

    @Override
    public void onProductDetailsResponse(BillingResult billingResult, List<ProductDetails> details) {
        log("onProductDetailsResponse(" + billingResult + ", " + details + ")");
        if (billingResult.getResponseCode() != OK) {
            Log.w(TAG, "failed to query inventory: " + billingResult);
            return;
        }

        if (isFinishing() || stateSaved) {
            Log.w(TAG, "activity no longer active");
            return;
        }

        productDetails.clear();
        List<Sku> skuList = new ArrayList<Sku>();
        for (ProductDetails d : details) {
            if (d.getProductId().startsWith(DONATION_PREFIX)) {
                productDetails.put(d.getProductId(), d);
                skuList.add(new Sku(d));
            }
        }
        showSelectDialog(skuList);
    }

    /**
     * @param result response code of the update
     * @param purchases list of updated purchases if present
     */
    @Override
    public void onPurchasesUpdated(BillingResult result, @Nullable List<Purchase> purchases) {
        log("onPurchasesUpdated(" + result + ", " + purchases + ")");
        String message;
        switch (result.getResponseCode()) {
            case OK:
                if (purchases != null)  {
                    for (Purchase p : purchases) {
                        acknowledgePurchase(p);
                    }
                }
                message = getString(R.string.ui_donation_success_message);
                break;
            case ITEM_UNAVAILABLE:
                message = getString(R.string.ui_donation_failure_message,
                        getString(R.string.donation_error_unavailable));
                break;
            case ITEM_ALREADY_OWNED:
                message = getString(R.string.ui_donation_failure_message,
                        getString(R.string.donation_error_already_owned));
                break;
            case USER_CANCELED:
                message = getString(R.string.ui_donation_failure_message,
                        getString(R.string.donation_error_canceled));
                break;
            default:
                message = getString(R.string.ui_donation_failure_message,
                        getString(R.string.donation_unspecified_error, result.getResponseCode()));
                break;
        }

        Toast.makeText(this, message, LENGTH_LONG).show();
        finish();
    }

    @Override
    public void selectedSku(Sku sku) {
        if (billingClient == null) return;
        if (DEBUG_IAB) {
            Log.v(TAG, "selectedSku("+sku.getSku()+")");
        }
        ProductDetails details = productDetails.get(sku.getSku());
        if (details == null) {
            Log.w(TAG, "no product details for " + sku.getSku());
            return;
        }
        BillingFlowParams.ProductDetailsParams params = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build();
        billingClient.launchBillingFlow(this, BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(params))
                .build()
        );
    }

    private void queryAvailableSkus() {
        if (billingClient == null) return;
        List<QueryProductDetailsParams.Product> products = new ArrayList<QueryProductDetailsParams.Product>();
        for (String id : ALL_SKUS) {
            products.add(QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(id)
                    .setProductType(INAPP)
                    .build());
        }
        billingClient.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder()
                .setProductList(products)
                .build(), this);
    }

    private void showSelectDialog(List<Sku> skuDetails) {
        if (billingClient == null) return;
        ArrayList<Sku> skus = new ArrayList<Sku>(skuDetails);
        if (DEBUG_IAB) {
            Collections.addAll(skus, Sku.Test.SKUS);
        }
        Collections.sort(skus);
        final DonationListFragment donationList = new DonationListFragment();
        donationList.setArguments(new BundleBuilder().putParcelableArrayList(SKUS, skus).build());
        donationList.show(getSupportFragmentManager(), null);
    }

    private static void log(String s) {
        if (DEBUG_IAB) {
            Log.d(TAG, s);
        }
    }

    // https://developer.android.com/google/play/billing/billing_library_overview#acknowledge
    private void acknowledgePurchase(final Purchase purchase) {
        if (purchase.getPurchaseState() == PURCHASED && !purchase.isAcknowledged() && billingClient != null) {
            AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.getPurchaseToken())
                    .build();

            billingClient.acknowledgePurchase(params, new AcknowledgePurchaseResponseListener() {
                @Override
                public void onAcknowledgePurchaseResponse(BillingResult billingResult) {
                    log("onAcknowledgePurchaseResponse(" + billingResult + ")");
                    if (billingResult.getResponseCode() != OK) {
                        Log.w(TAG, "not acknowledged purchase " + purchase + ":" + billingResult);
                    }
                }
            });
        }
    }


    public static void checkUserDonationStatus(Context context,
                                               final DonationStatusListener listener) {
        final BillingClient helper = BillingClient.newBuilder(context)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .setListener(new PurchasesUpdatedListener() {
            @Override
            public void onPurchasesUpdated(BillingResult result, @Nullable List<Purchase> purchases) {
                log("onPurchasesUpdated("+result+", "+purchases+")");
            }
        }).build();
        helper.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult result) {
                log("checkUserHasDonated: onBillingSetupFinished("+result+")");
                if (result.getResponseCode() == OK) {
                    helper.queryPurchasesAsync(
                            QueryPurchasesParams.newBuilder().setProductType(INAPP).build(),
                            new PurchasesResponseListener() {
                                @Override
                                public void onQueryPurchasesResponse(BillingResult queryResult, List<Purchase> purchases) {
                                    try {
                                        if (queryResult.getResponseCode() == OK) {
                                            listener.userDonationState(userHasDonated(purchases) ? DONATED : NOT_DONATED);
                                        } else {
                                            listener.userDonationState(UNKNOWN);
                                        }
                                    } finally {
                                        endConnectionQuietly(helper);
                                    }
                                }
                            });
                } else {
                    listener.userDonationState(result.getResponseCode() == BILLING_UNAVAILABLE ? NOT_AVAILABLE : UNKNOWN);
                    endConnectionQuietly(helper);
                }
            }
            public void onBillingServiceDisconnected() {
            }
        });
    }

    private static void endConnectionQuietly(BillingClient client) {
        try {
            client.endConnection();
        } catch (Exception ignored) {
        }
    }

    private static boolean userHasDonated(List<Purchase> result) {
        for (String sku : ALL_SKUS) {
            for (Purchase purchase : result) {
                if (purchase.getProducts().contains(sku)) {
                    return true;
                }
            }
        }
        return false;
    }
}
