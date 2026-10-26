package com.tayra.languages.android

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.tayra.languages.core.domain.account.StoreException
import com.tayra.languages.core.domain.account.StoreProduct
import com.tayra.languages.core.domain.account.StorePurchases
import com.tayra.languages.core.domain.account.StoreReceipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Google Play Billing as the Android app's store. A purchase carries the account's id as Play's
 * obfuscated account id, which the service reads back from Play; the service acknowledges it.
 */
class PlayStorePurchases(context: Context, private val activity: () -> Activity?) : StorePurchases {
    private val updates = MutableSharedFlow<Pair<BillingResult, List<Purchase>?>>(extraBufferCapacity = 4)
    private val client = BillingClient.newBuilder(context)
        .setListener { result, purchases -> updates.tryEmit(result to purchases) }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()
    private val connecting = Mutex()
    private val details = mutableMapOf<String, ProductDetails>()

    override suspend fun products(ids: List<String>): List<StoreProduct> {
        connect()
        val products = ids.map { QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(ProductType.SUBS).build() }
        val result = client.queryProductDetails(QueryProductDetailsParams.newBuilder().setProductList(products).build())
        check(result.billingResult)
        val found = result.productDetailsList.orEmpty().onEach { details[it.productId] = it }.associateBy { it.productId }
        return ids.mapNotNull { id ->
            val product = found[id] ?: return@mapNotNull null
            // The base plan's price: its last phase, after any introductory one.
            val price = basePlan(product)?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice ?: return@mapNotNull null
            StoreProduct(id, product.name, price)
        }
    }

    override suspend fun buy(productId: String, accountId: String): StoreReceipt? = coroutineScope {
        connect()
        val product = details[productId] ?: run { products(listOf(productId)); details[productId] } ?: throw StoreException("Google Play does not sell $productId here")
        val offer = basePlan(product) ?: throw StoreException("Google Play has no plan for $productId")
        val activity = activity() ?: throw StoreException("Open the app to subscribe")
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).setOfferToken(offer.offerToken).build()))
            .setObfuscatedAccountId(accountId)
            .build()
        val answer = async { updates.first() }
        val launched = withContext(Dispatchers.Main) { client.launchBillingFlow(activity, params) }
        if (launched.responseCode != BillingResponseCode.OK) {
            answer.cancel()
            check(launched)
        }
        val (result, purchases) = answer.await()
        when (result.responseCode) {
            BillingResponseCode.USER_CANCELED -> null
            BillingResponseCode.ITEM_ALREADY_OWNED -> owned().firstOrNull { (it as StoreReceipt.GooglePlay).productId == productId }
            BillingResponseCode.OK -> {
                val purchase = purchases.orEmpty().firstOrNull { productId in it.products } ?: throw StoreException("Google Play did not say what was bought")
                if (purchase.purchaseState == Purchase.PurchaseState.PENDING) throw StoreException("The payment is pending; the subscription starts once it is paid")
                StoreReceipt.GooglePlay(productId, purchase.purchaseToken)
            }
            else -> throw StoreException(result.debugMessage.ifBlank { "Google Play answered ${result.responseCode}" })
        }
    }

    override suspend fun owned(): List<StoreReceipt> {
        connect()
        val result = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build())
        check(result.billingResult)
        return result.purchasesList.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .mapNotNull { purchase -> purchase.products.firstOrNull()?.let { StoreReceipt.GooglePlay(it, purchase.purchaseToken) } }
    }

    /** The plan bought at its regular price: the offer that is no promotion. */
    private fun basePlan(product: ProductDetails) =
        product.subscriptionOfferDetails?.let { offers -> offers.firstOrNull { it.offerId == null } ?: offers.firstOrNull() }

    private suspend fun connect() = connecting.withLock {
        if (client.isReady) return@withLock
        val result = suspendCancellableCoroutine { continuation ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (continuation.isActive) continuation.resume(result)
                }

                override fun onBillingServiceDisconnected() {}
            })
        }
        check(result)
    }

    private fun check(result: BillingResult) {
        if (result.responseCode != BillingResponseCode.OK) throw StoreException(result.debugMessage.ifBlank { "Google Play answered ${result.responseCode}" })
    }
}
