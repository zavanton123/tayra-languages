package com.idiomiko.core.data.account

internal actual fun deviceLabel(): DeviceLabel {
    // The host name, as the computer calls itself; asking the network for it can stall, so it is not asked.
    val name = System.getenv("COMPUTERNAME") ?: System.getenv("HOSTNAME")
        ?: runCatching { ProcessBuilder("hostname").start().inputStream.bufferedReader().readText().trim() }.getOrNull()?.takeIf { it.isNotEmpty() }
        ?: System.getProperty("os.name")
    return DeviceLabel(name.removeSuffix(".local"), "desktop")
}

/** Set by IDIOMIKO_API_URL or -Didiomiko.apiUrl, for a service run locally. */
internal actual fun accountServerOverride(): String? =
    (System.getProperty("idiomiko.apiUrl") ?: System.getenv("IDIOMIKO_API_URL"))?.takeIf { it.isNotBlank() }
