package me.jfenn.metronome

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.multidex.MultiDexApplication
import me.jfenn.metronome.billing.Billing
import me.jfenn.metronome.billing.BillingInterface

/**
 * Metronome's application class, handles global states and fun things
 * that should be accessible to the rest of the app / UI.
 */
class Metronome : MultiDexApplication(), BillingInterface {

    private var billing: BillingInterface? = null

    override fun onCreate() {
        super.onCreate()
        billing = Billing.get(this)
        if (billing == null)
            Toast.makeText(this, "no billing interface!", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateActivity() {
        billing?.onCreateActivity()
    }

    override fun onDestroyActivity() {
        billing?.onDestroyActivity()
    }

    override fun getPrice(context: Context): String? {
        return billing?.getPrice(context)
    }

    override fun getSku(context: Context): String? {
        return billing?.getSku(context)
    }

    override fun isPremium(context: Context): Boolean {
        return billing?.isPremium(context) ?: true
    }

    override fun onPremium(activity: Activity) {
        billing?.onPremium(activity)
    }

    override fun buyPremium(activity: Activity) {
        billing?.buyPremium(activity)
    }

    override fun onPremiumBought(resultCode: Int, data: Intent) {
        billing?.onPremiumBought(resultCode, data)
    }
}
