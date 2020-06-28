package me.jfenn.metronome

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

    override fun getPrice(): String? {
        return billing?.getPrice()
    }

    override fun getSku(): String? {
        return billing?.getSku()
    }

    override fun isPremium(): Boolean {
        return billing?.isPremium() ?: true
    }

    override fun onPremium(activity: AppCompatActivity) {
        billing?.onPremium(activity)
    }

    override fun buyPremium(activity: AppCompatActivity) {
        billing?.buyPremium(activity)
    }
}
