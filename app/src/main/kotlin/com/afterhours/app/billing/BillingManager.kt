package com.afterhours.app.billing

import android.app.Activity
import android.content.Context
import com.afterhours.app.AppConfig
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * One-time "Remove ads" purchase through Google Play Billing.
 * Needs a managed in-app product with id [AppConfig.REMOVE_ADS_PRODUCT_ID] in Play Console,
 * and only works in a build installed from a Play testing track.
 */
class BillingManager(context: Context, private val onOwnedChanged: (Boolean) -> Unit) : PurchasesUpdatedListener {
    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private var details: ProductDetails? = null

    private val _price = MutableStateFlow<String?>(null)
    /** Localised price, or null while the product is unavailable. */
    val price: StateFlow<String?> = _price

    fun connect() {
        if (client.isReady) { refresh(); return }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) refresh()
            }
            override fun onBillingServiceDisconnected() { /* retried on next connect() */ }
        })
    }

    /** Re-checks ownership (also restores a purchase on a new device) and loads the price. */
    fun refresh() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(AppConfig.REMOVE_ADS_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryProductDetailsAsync(QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build()) { result, list ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                details = list.firstOrNull()
                _price.value = details?.oneTimePurchaseOfferDetails?.formattedPrice
            }
        }
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) handle(purchases, fromQuery = true)
        }
    }

    /** Opens Google Play's purchase sheet. Returns false when the product is not available yet. */
    fun buy(activity: Activity): Boolean {
        val d = details ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build()))
            .build()
        return client.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> handle(purchases.orEmpty(), fromQuery = false)
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            else -> Unit // cancelled or failed: nothing changes
        }
    }

    private fun handle(purchases: List<Purchase>, fromQuery: Boolean) {
        val owned = purchases.filter {
            AppConfig.REMOVE_ADS_PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        owned.filter { !it.isAcknowledged }.forEach { p ->
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { }
        }
        // A query is the full truth (handles refunds); an update only ever adds ownership.
        if (fromQuery) onOwnedChanged(owned.isNotEmpty()) else if (owned.isNotEmpty()) onOwnedChanged(true)
    }

    fun close() { client.endConnection() }
}
