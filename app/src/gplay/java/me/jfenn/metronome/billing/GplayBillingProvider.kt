package me.jfenn.metronome.billing

import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.android.billingclient.api.*
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.jfenn.metronome.Metronome
import me.jfenn.metronome.R

class GplayBillingProvider(
        private val applicationContext: Metronome
) : BillingInterface, BillingClientStateListener, PurchasesUpdatedListener {

    private var billingClient : BillingClient? = null

    private var hasPremium = false
    private var isNetworkError = true
    private var price: String? = null

    override fun onCreateActivity() {
        billingClient = BillingClient.newBuilder(applicationContext)
                .setListener(this)
                .enablePendingPurchases()
                .build()

        billingClient?.startConnection(this)
    }

    override fun onDestroyActivity() {
        billingClient = null
    }

    override fun getPrice(): String = price ?: applicationContext.getString(R.string.title_no_connection)

    override fun getSku(): String {
        val skuRes = applicationContext.resources.getIdentifier("sku", "string", applicationContext.packageName)
        return if (skuRes != 0) applicationContext.getString(skuRes) else ""
    }

    fun getSkuParams() = SkuDetailsParams.newBuilder().setSkusList(arrayListOf(sku)).setType(BillingClient.SkuType.INAPP).build()

    override fun isPremium(): Boolean {
        if (isNetworkError) billingClient?.startConnection(this)
        return hasPremium || isNetworkError
    }

    override fun onPremium(activity: AppCompatActivity) {
        if (!hasPremium) {
            val view = LayoutInflater.from(activity).inflate(R.layout.dialog_premium, null)

            Glide.with(activity)
                    .load("https://jfenn.me/images/headers/metronomePremium.png")
                    .into(view.findViewById(R.id.image))

            MaterialDialog(activity).show {
                customView(view = view, noVerticalPadding = true)
                cancelable(false)
                positiveButton(text = getPrice()) {
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
        activity.lifecycleScope.launch(Dispatchers.IO) {
            billingClient?.let { client ->
                val details = client.querySkuDetails(getSkuParams()).skuDetailsList?.getOrNull(0)

                activity.runOnUiThread {
                    details?.let {
                        billingClient?.launchBillingFlow(
                                activity,
                                BillingFlowParams.newBuilder()
                                        .setSkuDetails(it)
                                        .build())
                    } ?: run {
                        Toast.makeText(activity, R.string.title_no_connection, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, p1: MutableList<Purchase>?) {
        hasPremium = result.responseCode == BillingClient.BillingResponseCode.OK || result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED
        isNetworkError = false
    }

    override fun onBillingSetupFinished(result: BillingResult) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            isNetworkError = false

            GlobalScope.launch(context = Dispatchers.IO) {
                val skuDetailsResult = billingClient?.querySkuDetails(getSkuParams())
                skuDetailsResult?.let {
                    price = it.skuDetailsList?.getOrNull(0)?.price
                }

                val purchase = billingClient?.queryPurchases(BillingClient.SkuType.INAPP)?.purchasesList?.lastOrNull()
                hasPremium = purchase?.purchaseState == Purchase.PurchaseState.PURCHASED && purchase.sku == sku
            }
        }
    }

    override fun onBillingServiceDisconnected() {
        isNetworkError = true
    }

}