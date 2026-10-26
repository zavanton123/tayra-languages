package com.tayra.languages.core.domain.account

/** The subscriptions the stores sell, by the ids they are set up with in Google Play and App Store Connect. */
object StoreProducts {
    const val MONTHLY = "tayra.sync.monthly"
    const val YEARLY = "tayra.sync.yearly"
    val all = listOf(MONTHLY, YEARLY)
}

/** A subscription as the store describes it to the buyer: its name and price in their currency. */
data class StoreProduct(val id: String, val title: String, val price: String)

/** A purchase the store made, as the account service checks and takes it. */
sealed interface StoreReceipt {
    data class GooglePlay(val productId: String, val purchaseToken: String) : StoreReceipt

    /** The signed transaction StoreKit gave for the purchase. */
    data class AppStore(val signedTransaction: String) : StoreReceipt
}

/** Thrown when the store could not make a purchase; [message] says why, in the store's words. */
class StoreException(message: String) : Exception(message)

/**
 * The platform's store, in the phone apps: Google Play Billing on Android, StoreKit on iOS. A
 * purchase carries the account's id, so the store's own notifications reach the account too.
 */
interface StorePurchases {
    /** The [ids] the store sells here, in the buyer's currency; fewer when some are not set up. */
    suspend fun products(ids: List<String>): List<StoreProduct>

    /** Buys [productId] for the account [accountId]; null when the buyer cancelled. Throws [StoreException]. */
    suspend fun buy(productId: String, accountId: String): StoreReceipt?

    /** The subscriptions this store account holds now, bought on this device or another. */
    suspend fun owned(): List<StoreReceipt>
}

/** Buying the subscription in the phone apps: the store sells it, and the account service is told. */
class Subscriptions(private val store: StorePurchases, private val accounts: AccountRepository) {

    suspend fun products(): List<StoreProduct> = store.products(StoreProducts.all)

    /** Buys [productId] for the signed-in account and hands the purchase to the service; whether it was bought. */
    suspend fun subscribe(productId: String): Boolean {
        val account = accounts.account()
        val receipt = store.buy(productId, account.id) ?: return false
        accounts.reportPurchase(receipt)
        return true
    }

    /** Hands the service the subscriptions the store account holds, as after reinstalling; how many. */
    suspend fun restore(): Int {
        val owned = store.owned()
        owned.forEach { accounts.reportPurchase(it) }
        return owned.size
    }
}
