package com.idiomiko.core.data.network

import com.idiomiko.core.domain.service.NetworkStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import platform.Network.nw_path_get_status
import platform.Network.nw_path_is_constrained
import platform.Network.nw_path_is_expensive
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_global_queue
import platform.darwin.DISPATCH_QUEUE_PRIORITY_DEFAULT
import kotlin.concurrent.Volatile

/** The phone's connection as iOS tells it: expensive on mobile data or a personal hotspot, constrained in Low Data Mode. */
class IosNetworkStatus : NetworkStatus {
    @Volatile
    private var free = false

    private val monitor = nw_path_monitor_create().also { monitor ->
        nw_path_monitor_set_update_handler(monitor) { path -> free = isFree(path) }
        nw_path_monitor_set_queue(monitor, dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u))
        nw_path_monitor_start(monitor)
    }

    override fun unmetered(): Boolean = free

    override val changes: Flow<Unit> = callbackFlow {
        val watcher = nw_path_monitor_create()
        nw_path_monitor_set_update_handler(watcher) { path ->
            free = isFree(path)
            trySend(Unit)
        }
        nw_path_monitor_set_queue(watcher, dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT.toLong(), 0u))
        nw_path_monitor_start(watcher)
        awaitClose { nw_path_monitor_cancel(watcher) }
    }.conflate()

    private fun isFree(path: platform.Network.nw_path_t): Boolean =
        nw_path_get_status(path) == nw_path_status_satisfied && !nw_path_is_expensive(path) && !nw_path_is_constrained(path)
}
