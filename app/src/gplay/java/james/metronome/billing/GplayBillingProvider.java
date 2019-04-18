package james.metronome.billing;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.vending.billing.IInAppBillingService;
import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import james.metronome.Metronome;
import james.metronome.R;

public class GplayBillingProvider implements BillingInterface {

    private static final int VERSION_BILLING_API = 3;

    private IInAppBillingService service;
    private ServiceConnection serviceConnection;
    private Metronome metronome;

    private boolean isPremium;
    private boolean isNetworkError = true;
    private String price;

    public GplayBillingProvider(Metronome metronome) {
        this.metronome = metronome;

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                service = IInAppBillingService.Stub.asInterface(iBinder);
                new GetPurchaseThread(GplayBillingProvider.this, service).start();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                service = null;
            }
        };
    }

    @Override
    public void onCreateActivity() {
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        getContext().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroyActivity() {
        if (service != null)
            getContext().unbindService(serviceConnection);
    }

    @Override
    public String getPrice(Context context) {
        return price != null ? price : context.getString(R.string.title_no_connection);
    }

    @Nullable
    @Override
    public String getSku(Context context) {
        int skuRes = metronome.getResources().getIdentifier("sku", "string", context.getPackageName());
        if (skuRes != 0)
            return context.getString(skuRes);

        return null;
    }

    @Override
    public boolean isPremium(Context context) {
        if (isNetworkError && service != null)
            new GetPurchaseThread(this, service).start();
        return isPremium || isNetworkError;
    }

    @Override
    public void onPremium(Activity activity) {
        if (!isPremium(activity)) {
            View view = LayoutInflater.from(activity).inflate(R.layout.dialog_premium, null);
            Glide.with(activity).load("https://jfenn.me/images/headers/metronomePremium.png").into((ImageView) view.findViewById(R.id.image));

            new MaterialDialog.Builder(activity)
                    .customView(view, false)
                    .backgroundColor(Color.WHITE)
                    .cancelable(false)
                    .positiveText(activity.getString(R.string.title_get_premium, getPrice(activity)))
                    .positiveColor(ContextCompat.getColor(activity, R.color.colorAccent))
                    .onPositive((dialog, which) -> {
                        buyPremium(activity);
                        dialog.dismiss();
                    })
                    .negativeText(R.string.title_use_anyway)
                    .negativeColor(ContextCompat.getColor(activity, R.color.textColorSecondaryInverse))
                    .onNegative((dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    @Override
    public void buyPremium(Activity activity) {
        if (service != null) {
            Bundle buyIntentBundle;
            try {
                buyIntentBundle = service.getBuyIntent(VERSION_BILLING_API, activity.getPackageName(), getSku(activity), "inapp", null);
            } catch (RemoteException | NullPointerException e) {
                e.printStackTrace();
                return;
            }

            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            if (pendingIntent != null) {
                try {
                    activity.startIntentSenderForResult(pendingIntent.getIntentSender(), Billing.REQUEST_PURCHASE, new Intent(), 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onPremiumBought(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data.hasExtra("INAPP_PURCHASE_DATA")) {
            try {
                JSONObject object = new JSONObject(data.getStringExtra("INAPP_PURCHASE_DATA"));
                String sku = getSku(metronome);
                if (sku != null && sku.equals(object.getString("productId")))
                    isPremium = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Context getContext() {
        return metronome;
    }

    private static class GetPurchaseThread extends Thread {

        private WeakReference<GplayBillingProvider> providerReference;
        private IInAppBillingService service;
        private String packageName;

        @Nullable
        private String sku;

        private String price;

        public GetPurchaseThread(GplayBillingProvider provider, IInAppBillingService service) {
            providerReference = new WeakReference<>(provider);
            this.service = service;
            packageName = provider.getContext().getPackageName();
            sku = provider.getSku(provider.getContext());
        }

        @Override
        public void run() {
            if (sku == null)
                return;

            Bundle querySkus = new Bundle();
            querySkus.putStringArrayList("ITEM_ID_LIST", new ArrayList<>(Collections.singletonList(sku)));

            Bundle skuDetails;
            try {
                skuDetails = service.getSkuDetails(VERSION_BILLING_API, packageName, "inapp", querySkus);
            } catch (RemoteException e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    GplayBillingProvider provider = providerReference.get();
                    if (provider != null)
                        Toast.makeText(provider.getContext(), R.string.msg_purchase_refresh_error, Toast.LENGTH_SHORT).show();
                });
                return;
            }

            if (skuDetails.getInt("RESPONSE_CODE") == 0) {
                ArrayList<String> responseList = skuDetails.getStringArrayList("DETAILS_LIST");
                if (responseList != null && responseList.size() > 0) {
                    try {
                        JSONObject object = new JSONObject(responseList.get(0));
                        price = object.getString("price");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    new Handler(Looper.getMainLooper()).post(() -> {
                        Bundle ownedItems;
                        try {
                            ownedItems = service.getPurchases(VERSION_BILLING_API, packageName, "inapp", null);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                            return;
                        }

                        if (ownedItems.getInt("RESPONSE_CODE") == 0) {
                            GplayBillingProvider provider = providerReference.get();
                            if (provider != null) {
                                provider.isNetworkError = false;
                                provider.price = price;

                                List<String> ownedSkus = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                                provider.isPremium = ownedSkus != null && ownedSkus.size() > 0 && ownedSkus.get(0).equals(sku);
                            }
                        }
                    });
                }
            }
        }
    }

}
