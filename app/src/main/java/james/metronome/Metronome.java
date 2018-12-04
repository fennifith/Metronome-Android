package james.metronome;

import android.app.Activity;
import android.app.Application;
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

public class Metronome extends Application {

    private static final int VERSION_BILLING_API = 3;
    public static final int REQUEST_PURCHASE = 614;

    private IInAppBillingService service;
    private ServiceConnection serviceConnection;

    private boolean isPremium;
    private boolean isNetworkError = true;
    private String price;

    @Override
    public void onCreate() {
        super.onCreate();

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                service = IInAppBillingService.Stub.asInterface(iBinder);
                new GetPurchaseThread(Metronome.this, service).start();
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                service = null;
            }
        };
    }

    public void onCreateActivity() {
        Intent serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public void onDestroyActivity() {
        if (service != null)
            unbindService(serviceConnection);
    }

    public String getPrice() {
        return price != null ? price : getString(R.string.title_no_connection);
    }

    public boolean isPremium() {
        if (isNetworkError && service != null)
            new GetPurchaseThread(this, service).start();
        return isPremium || isNetworkError;
    }

    public void onPremium(final Activity activity) {
        if (!isPremium()) {
            View view = LayoutInflater.from(activity).inflate(R.layout.dialog_premium, null);
            Glide.with(this).load("https://jfenn.me/images/headers/metronomePremium.png").into((ImageView) view.findViewById(R.id.image));

            new MaterialDialog.Builder(activity)
                    .customView(view, false)
                    .backgroundColor(Color.WHITE)
                    .cancelable(false)
                    .positiveText(getString(R.string.title_get_premium, getPrice()))
                    .positiveColor(ContextCompat.getColor(this, R.color.colorAccent))
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            buyPremium(activity);
                            dialog.dismiss();
                        }
                    })
                    .negativeText(R.string.title_use_anyway)
                    .negativeColor(ContextCompat.getColor(this, R.color.textColorSecondaryInverse))
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        }
    }

    public void buyPremium(Activity activity) {
        if (service != null) {
            Bundle buyIntentBundle;
            try {
                buyIntentBundle = service.getBuyIntent(VERSION_BILLING_API, getPackageName(), getSku(), "inapp", null);
            } catch (RemoteException | NullPointerException e) {
                e.printStackTrace();
                return;
            }

            PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
            if (pendingIntent != null) {
                try {
                    activity.startIntentSenderForResult(pendingIntent.getIntentSender(), REQUEST_PURCHASE, new Intent(), 0, 0, 0);
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void onPremiumBought(int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data.hasExtra("INAPP_PURCHASE_DATA")) {
            try {
                JSONObject object = new JSONObject(data.getStringExtra("INAPP_PURCHASE_DATA"));
                String sku = getSku();
                if (sku != null && sku.equals(object.getString("productId")))
                    isPremium = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Nullable
    private String getSku() {
        int skuRes = getResources().getIdentifier("sku", "string", getPackageName());
        if (skuRes != 0)
            return getString(skuRes);

        return null;
    }

    private static class GetPurchaseThread extends Thread {

        private WeakReference<Metronome> metronomeReference;
        private IInAppBillingService service;
        private String packageName;

        @Nullable
        private String sku;

        private String price;

        public GetPurchaseThread(Metronome metronome, IInAppBillingService service) {
            metronomeReference = new WeakReference<>(metronome);
            this.service = service;
            packageName = metronome.getPackageName();
            sku = metronome.getSku();
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
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Metronome metronome = metronomeReference.get();
                        if (metronome != null)
                            Toast.makeText(metronome, R.string.msg_purchase_refresh_error, Toast.LENGTH_SHORT).show();
                    }
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

                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Bundle ownedItems;
                            try {
                                ownedItems = service.getPurchases(VERSION_BILLING_API, packageName, "inapp", null);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                                return;
                            }

                            if (ownedItems.getInt("RESPONSE_CODE") == 0) {
                                Metronome metronome = metronomeReference.get();
                                if (metronome != null) {
                                    metronome.isNetworkError = false;
                                    metronome.price = price;

                                    List<String> ownedSkus = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                                    metronome.isPremium = ownedSkus != null && ownedSkus.size() > 0 && ownedSkus.get(0).equals(sku);
                                }
                            }
                        }
                    });
                }
            }
        }
    }
}
