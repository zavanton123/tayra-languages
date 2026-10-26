package com.tayra.languages.store

import com.tayra.languages.core.domain.account.StoreException
import com.tayra.languages.core.domain.account.StoreProduct
import com.tayra.languages.core.domain.account.StorePurchases
import com.tayra.languages.core.domain.account.StoreReceipt
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** A subscription as the App Store describes it, made by the Swift side. */
data class StoreKitProduct(val id: String, val title: String, val price: String)

/**
 * What the Swift side implements with StoreKit 2. Plain callbacks rather than suspend functions,
 * because Kotlin cannot call a suspend function that Swift implements.
 */
interface StoreKitBridge {
    fun products(ids: List<String>, callback: (products: List<StoreKitProduct>?, error: String?) -> Unit)

    /**
     * Buys [productId] with [accountToken], the account's id, as its appAccountToken. The callback gets
     * the signed transaction, or an error, or neither when the buyer cancelled.
     */
    fun buy(productId: String, accountToken: String, callback: (signedTransaction: String?, error: String?) -> Unit)

    /** The signed transactions of the subscriptions the App Store account holds, after syncing with the App Store. */
    fun owned(callback: (signedTransactions: List<String>?, error: String?) -> Unit)
}

/** StoreKit 2 on iOS, reached through [StoreKitBridge]. */
class BridgedStorePurchases(private val bridge: StoreKitBridge) : StorePurchases {
    override suspend fun products(ids: List<String>): List<StoreProduct> = suspendCancellableCoroutine { continuation ->
        bridge.products(ids) { products, error ->
            if (error != null) continuation.resumeWithException(StoreException(error))
            else continuation.resume(products.orEmpty().map { StoreProduct(it.id, it.title, it.price) }.sortedBy { ids.indexOf(it.id) })
        }
    }

    override suspend fun buy(productId: String, accountId: String): StoreReceipt? = suspendCancellableCoroutine { continuation ->
        bridge.buy(productId, accountId) { signed, error ->
            if (error != null) continuation.resumeWithException(StoreException(error))
            else continuation.resume(signed?.let(StoreReceipt::AppStore))
        }
    }

    override suspend fun owned(): List<StoreReceipt> = suspendCancellableCoroutine { continuation ->
        bridge.owned { signed, error ->
            if (error != null) continuation.resumeWithException(StoreException(error))
            else continuation.resume(signed.orEmpty().map(StoreReceipt::AppStore))
        }
    }
}
