package com.tayra.languages.core.domain.bridge

import kotlinx.coroutines.flow.StateFlow
import kotlin.time.Instant

/**
 * The desktop app's connection to the Tayra browser extension: a server on this computer only,
 * which browsers paired with a code may call. Only the desktop app has one.
 */
interface BrowserExtensionBridge {
    val state: StateFlow<BridgeState>

    /** Shows a new pairing code, replacing any code shown before. */
    fun startPairing()

    fun cancelPairing()

    fun unpair(id: String)
}

data class BridgeState(
    /** The port it listens on; null when it isn't running. */
    val port: Int? = null,
    /** Why it couldn't start. */
    val error: String? = null,
    /** The code to type into the extension, while pairing. */
    val code: String? = null,
    val codeUntil: Instant? = null,
    val browsers: List<PairedBrowser> = emptyList(),
    /** The browsers the extension connects in by itself, with no code. */
    val automaticIn: List<String> = emptyList(),
)
