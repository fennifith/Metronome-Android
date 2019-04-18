package james.metronome.billing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public interface BillingInterface {

    void onCreateActivity();
    void onDestroyActivity();
    String getPrice(Context context);
    String getSku(Context context);
    boolean isPremium(Context context);
    void onPremium(Activity activity);
    void buyPremium(Activity activity);
    void onPremiumBought(int resultCode, Intent data);

}
