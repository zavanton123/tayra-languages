package com.tayra.languages.core.data.account

internal actual fun deviceLabel(): DeviceLabel {
    // The host name, as the computer calls itself; asking the network for it can stall, so it is not asked.
    val name = System.getenv("COMPUTERNAME") ?: System.getenv("HOSTNAME")
        ?: runCatching { ProcessBuilder("hostname").start().inputStream.bufferedReader().readText().trim() }.getOrNull()?.takeIf { it.isNotEmpty() }
        ?: System.getProperty("os.name")
    return DeviceLabel(name.removeSuffix(".local"), "desktop")
}

/** Set by TAYRA_API_URL or -Dtayra.apiUrl, for a service run locally. */
internal actual fun accountServerOverride(): String? =
    (System.getProperty("tayra.apiUrl") ?: System.getenv("TAYRA_API_URL"))?.takeIf { it.isNotBlank() }
