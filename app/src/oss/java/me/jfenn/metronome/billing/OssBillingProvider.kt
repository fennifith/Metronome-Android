package me.jfenn.metronome.billing

import android.content.Intent
import android.net.Uri
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatCheckBox
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.bumptech.glide.Glide
import me.jfenn.metronome.Metronome
import me.jfenn.metronome.R
import me.jfenn.metronome.utils.PreferenceDelegate

class OssBillingProvider(
        private val metronome: Metronome
) : BillingInterface {

    private var hasPremium by PreferenceDelegate(metronome, KEY_DONATE, false)

    override fun onCreateActivity() {
        // nope
    }

    override fun onDestroyActivity() {
        // nope
    }

    override fun getPrice(): String {
        return metronome.getString(R.string.action_donate)
    }

    override fun getSku(): String {
        // nope
        return ""
    }

    override fun isPremium(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(metronome)
        return prefs.getBoolean(KEY_DONATE, false)
    }

    override fun onPremium(activity: AppCompatActivity) {
        if (!hasPremium) {
            val view = LayoutInflater.from(activity).inflate(R.layout.dialog_premium, null)

            Glide.with(activity)
                    .load("https://jfenn.me/images/headers/metronomePremium.png")
                    .into(view.findViewById(R.id.image))

            view.findViewById<AppCompatCheckBox>(R.id.hideCheckBox).apply {
                visibility = View.VISIBLE
                setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                    hasPremium = isChecked
                }
            }

            MaterialDialog(activity).show {
                customView(view = view, noVerticalPadding = true)
                cancelable(false)
                positiveButton(text = price) {
                    buyPremium(activity)
                    it.dismiss()
                }
                negativeButton(R.string.title_use_anyway) {
                    it.dismiss()
                }
            }
        }
    }

    override fun buyPremium(activity: AppCompatActivity) {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(URL_DONATE)))
    }

    companion object {
        private const val URL_DONATE = "https://jfenn.me/links/liberapay"
        private const val KEY_DONATE = "me.jfenn.metronome.billing.OssBillingProvider.KEY_DONATE"
    }
}