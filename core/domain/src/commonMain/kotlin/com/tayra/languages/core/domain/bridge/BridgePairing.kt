package com.tayra.languages.core.domain.bridge

import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** A browser the extension was paired from. */
data class PairedBrowser(val id: String, val browser: String, val pairedAt: Instant)

/** Where the paired browsers' tokens are kept between runs. */
interface PairedBrowserStore {
    fun load(): List<StoredPairing>
    fun save(pairings: List<StoredPairing>)
}

data class StoredPairing(val browser: PairedBrowser, val token: String)

/**
 * Pairs the extension with the app. The app shows a code for a few minutes; the extension sends it
 * back once and gets a token it sends with every call after. Any web page can call the app on this
 * computer, so nothing but pairing works without a token, and a few wrong codes end the pairing.
 */
class BridgePairing(
    private val store: PairedBrowserStore,
    private val clock: Clock = Clock.System,
    private val random: Random = Random.Default,
) {
    private var pairings: List<StoredPairing> = store.load()
    private var code: String? = null
    private var codeUntil: Instant = Instant.DISTANT_PAST
    private var wrongTries = 0

    val browsers: List<PairedBrowser> get() = pairings.map { it.browser }

    /** A new code to type into the extension, valid for [CODE_LIFETIME]. */
    fun start(): String {
        val fresh = (0 until CODE_LENGTH).joinToString("") { random.nextInt(10).toString() }
        code = fresh
        codeUntil = clock.now() + CODE_LIFETIME
        wrongTries = 0
        return fresh
    }

    fun cancel() {
        code = null
    }

    /** The token for a browser that sent the right code, or null. */
    fun pair(sent: String, browser: String): String? {
        val wanted = code ?: return null
        if (clock.now() > codeUntil) {
            code = null
            return null
        }
        if (!same(sent.trim(), wanted)) {
            if (++wrongTries >= MAX_WRONG_TRIES) code = null
            return null
        }
        code = null
        val token = (0 until TOKEN_BYTES).joinToString("") { random.nextInt(256).toString(16).padStart(2, '0') }
        val name = browser.trim().take(MAX_NAME).ifEmpty { "Browser" }
        val paired = PairedBrowser(id = token.take(ID_LENGTH), browser = name, pairedAt = clock.now())
        pairings = pairings + StoredPairing(paired, token)
        store.save(pairings)
        return token
    }

    fun accepts(token: String?): Boolean = token != null && pairings.any { same(it.token, token) }

    fun unpair(id: String) {
        pairings = pairings.filter { it.browser.id != id }
        store.save(pairings)
    }

    /** Compares without stopping at the first difference, so timing tells nothing about a token. */
    private fun same(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].code xor b[i].code)
        return diff == 0
    }

    companion object {
        const val CODE_LENGTH = 6
        val CODE_LIFETIME = 5.minutes
        const val MAX_WRONG_TRIES = 5
        private const val TOKEN_BYTES = 32
        private const val ID_LENGTH = 8
        private const val MAX_NAME = 40
    }
}
