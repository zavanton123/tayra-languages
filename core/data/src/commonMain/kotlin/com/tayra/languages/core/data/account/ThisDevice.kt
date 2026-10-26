package com.tayra.languages.core.data.account

import app.cash.sqldelight.async.coroutines.awaitAsOne
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.db.databaseDispatcher
import com.tayra.languages.sync.DeviceInfo
import kotlinx.coroutines.withContext

/** How this device is shown in the account's list of devices: its name and what kind of device it is. */
internal data class DeviceLabel(val name: String, val platform: String)

internal expect fun deviceLabel(): DeviceLabel

/** The account service this build talks to unless Settings name another; null for none. */
internal expect fun defaultAccountServer(): String?

/** This device as the account service knows it: the id its sync state was given, and its label. */
internal suspend fun thisDevice(provider: DatabaseProvider): DeviceInfo {
    val id = withContext(databaseDispatcher) { provider.database().syncQueries.state().awaitAsOne().device_id }
    val label = deviceLabel()
    return DeviceInfo(id, label.name, label.platform)
}
