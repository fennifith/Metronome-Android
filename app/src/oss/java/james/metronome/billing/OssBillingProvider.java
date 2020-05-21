package james.metronome.billing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;

import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.core.content.ContextCompat;
import james.metronome.Metronome;
import james.metronome.R;

public class OssBillingProvider implements BillingInterface {

    private static final String URL_DONATE = "https://jfenn.me/links/liberapay";
    private static final String KEY_DONATE = "james.metronome.billing.OssBillingProvider.KEY_DONATE";

    public OssBillingProvider(Metronome metronome) {
        // nope
    }

    @Override
    public void onCreateActivity() {
        // nope
    }

    @Override
    public void onDestroyActivity() {
        // nope
    }

    @Override
    public String getPrice(Context context) {
        return context.getString(R.string.action_donate);
    }

    @Override
    public String getSku(Context context) {
        // nope
        return null;
    }

    @Override
    public boolean isPremium(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(KEY_DONATE, false);
    }

    @Override
    public void onPremium(Activity activity) {
        if (!isPremium(activity)) {
            View view = LayoutInflater.from(activity).inflate(R.layout.dialog_premium, null);
            Glide.with(activity).load("https://jfenn.me/images/headers/metronomePremium.png").into((ImageView) view.findViewById(R.id.image));

            AppCompatCheckBox checkBox = view.findViewById(R.id.hideCheckBox);
            checkBox.setVisibility(View.VISIBLE);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
                prefs.edit().putBoolean(KEY_DONATE, isChecked).apply();
            });

            new MaterialDialog.Builder(activity)
                    .customView(view, false)
                    .backgroundColor(Color.WHITE)
                    .cancelable(false)
                    .positiveText(getPrice(activity))
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
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL_DONATE)));
    }

    @Override
    public void onPremiumBought(int resultCode, Intent data) {
        // nope
    }
}
