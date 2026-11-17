package com.idiomiko.core.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.idiomiko.core.domain.service.NetworkStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

/** The phone's connection as Android tells it: metered when mobile data or a hotspot is paid for by the byte. */
class AndroidNetworkStatus(context: Context) : NetworkStatus {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)

    override fun unmetered(): Boolean {
        val network = connectivity?.activeNetwork ?: return false
        return connectivity.getNetworkCapabilities(network)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true
    }

    override val changes: Flow<Unit> = callbackFlow {
        val manager = connectivity ?: return@callbackFlow awaitClose {}
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(Unit)
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.conflate()
}
