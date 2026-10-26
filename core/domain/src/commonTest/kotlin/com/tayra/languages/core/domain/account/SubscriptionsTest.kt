package com.tayra.languages.core.domain.account

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Buying in the store is for the signed-in account, and every purchase reaches the account service. */
class SubscriptionsTest {

    private class Store : StorePurchases {
        var boughtFor: String? = null
        var cancel = false
        override suspend fun products(ids: List<String>) = ids.map { StoreProduct(it, it.substringAfterLast('.'), "€3.99") }
        override suspend fun buy(productId: String, accountId: String): StoreReceipt? {
            if (cancel) return null
            boughtFor = accountId
            return StoreReceipt.GooglePlay(productId, "token-1")
        }
        override suspend fun owned() = listOf(StoreReceipt.AppStore("jws-1"), StoreReceipt.AppStore("jws-2"))
    }

    private class Accounts : AccountRepository {
        val reported = mutableListOf<StoreReceipt>()
        override val available = true
        override val signedIn = MutableStateFlow<String?>("reader@example.com")
        override suspend fun register(email: String, password: String) = error("")
        override suspend fun signIn(email: String, password: String) = error("")
        override suspend fun signOut() {}
        override suspend fun account() = AccountInfo("reader@example.com", true, true, emptyList(), Plan(false), id = "8c0e2c3a-0000-4000-8000-000000000001")
        override suspend fun sendConfirmationCode() {}
        override suspend fun confirmEmail(code: String) {}
        override suspend fun forgotPassword(email: String) {}
        override suspend fun resetPassword(email: String, code: String, password: String) {}
        override suspend fun devices() = emptyList<SignedInDevice>()
        override suspend fun signOutDevice(id: String) {}
        override suspend fun deleteAccount() {}
        override suspend fun reportPurchase(receipt: StoreReceipt) { reported += receipt }
    }

    @Test
    fun aPurchaseIsTheAccountsAndReachesTheService() = runTest {
        val store = Store()
        val accounts = Accounts()
        val subscriptions = Subscriptions(store, accounts)
        assertEquals(listOf(StoreProducts.MONTHLY, StoreProducts.YEARLY), subscriptions.products().map { it.id })

        assertTrue(subscriptions.subscribe(StoreProducts.YEARLY))
        assertEquals("8c0e2c3a-0000-4000-8000-000000000001", store.boughtFor)
        assertEquals(listOf<StoreReceipt>(StoreReceipt.GooglePlay(StoreProducts.YEARLY, "token-1")), accounts.reported)

        store.cancel = true
        assertFalse(subscriptions.subscribe(StoreProducts.MONTHLY), "the buyer cancelled")
        assertEquals(1, accounts.reported.size)

        assertEquals(2, subscriptions.restore())
        assertEquals(3, accounts.reported.size)
    }
}
