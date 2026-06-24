package sms.backup.plus.activity.donation;

import android.os.Parcel;
import android.os.Parcelable;
import com.android.billingclient.api.ProductDetails;

import static com.android.billingclient.api.BillingClient.ProductType.INAPP;

/**
 * A parcelable, comparable view of a billing product.
 *
 * <p>Play Billing 5+ replaced {@link com.android.billingclient.api.SkuDetails} with
 * {@link ProductDetails}, which is neither {@link Parcelable} nor reconstructable from JSON.
 * This class therefore captures the fields we display and carries them across the
 * {@link android.os.Bundle} to {@link DonationListFragment}; the activity keeps the backing
 * {@link ProductDetails} and looks it up again by product id when a selection is made.
 */
public class Sku implements Parcelable, Comparable<Sku> {
    private final String type;
    private final String sku;
    private final String price;
    private final String title;
    private final String description;
    private final long priceAmountMicros;

    Sku(ProductDetails details) {
        this(details.getProductType(),
                details.getProductId(),
                details.getOneTimePurchaseOfferDetails() != null
                        ? details.getOneTimePurchaseOfferDetails().getFormattedPrice() : "",
                details.getTitle(),
                details.getDescription(),
                details.getOneTimePurchaseOfferDetails() != null
                        ? details.getOneTimePurchaseOfferDetails().getPriceAmountMicros() : 0L);
    }

    /**
     * @param type SKU type
     * @param sku  the product Id
     * @param price formatted price of the item, including its currency sign
     * @param title  the title of the product
     * @param description the description of the product
     * @param priceAmountMicros the price in micro-units, where 1,000,000 micro-units equal one unit of the currency
     */
    Sku(String type, String sku, String price, String title, String description, long priceAmountMicros) {
        this.type = type;
        this.sku = sku;
        this.price = price;
        this.title = title;
        this.description = description;
        this.priceAmountMicros = priceAmountMicros;
    }

    private Sku(Parcel in) {
        type = in.readString();
        sku = in.readString();
        price = in.readString();
        title = in.readString();
        description = in.readString();
        priceAmountMicros = in.readLong();
    }

    public static final Creator<Sku> CREATOR = new Creator<Sku>() {
        @Override
        public Sku createFromParcel(Parcel in) {
            return new Sku(in);
        }

        @Override
        public Sku[] newArray(int size) {
            return new Sku[size];
        }
    };

    public String getType() {
        return type;
    }
    public String getSku() {
        return sku;
    }
    public String getPrice() {
        return price;
    }
    public String getTitle() {
        return title;
    }
    public String getDescription() {
        return description;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(type);
        parcel.writeString(sku);
        parcel.writeString(price);
        parcel.writeString(title);
        parcel.writeString(description);
        parcel.writeLong(priceAmountMicros);
    }

    @Override
    public int compareTo(Sku other) {
        int diff = Long.compare(priceAmountMicros, other.priceAmountMicros);
        if (diff == 0) {
            diff = title.compareTo(other.title);
        }
        return diff;
    }

    /**
     * Reserved product ids that Google Play answers with static test responses. These are only
     * shown in debug builds; they have no backing {@link ProductDetails} so selecting them does
     * not launch a real purchase flow.
     *
     * @see <a href="https://developer.android.com/google/play/billing/test">Test in-app billing</a>
     */
    static class Test {
        static final String TEST_PREFIX = "android.test.";
        static final String TEST_PRICE = "$0.00";

        static final Sku PURCHASED =
                new Sku(INAPP, TEST_PREFIX + "purchased", TEST_PRICE, "Test (purchased)", "Purchased", 0);
        static final Sku CANCELED =
                new Sku(INAPP, TEST_PREFIX + "canceled", TEST_PRICE, "Test (canceled)", "Canceled", 0);
        static final Sku REFUNDED =
                new Sku(INAPP, TEST_PREFIX + "refunded", TEST_PRICE, "Test (refunded)", "Refunded", 0);
        static final Sku UNAVAILABLE =
                new Sku(INAPP, TEST_PREFIX + "item_unavailable", TEST_PRICE, "Test (unavailable)", "Unavailable", 0);

        static final Sku[] SKUS = {
                PURCHASED, CANCELED, REFUNDED, UNAVAILABLE
        };
    }
}
