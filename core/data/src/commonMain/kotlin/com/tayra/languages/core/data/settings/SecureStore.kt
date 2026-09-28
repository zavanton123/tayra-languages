package com.tayra.languages.core.data.settings

/**
 * Where secrets such as API keys live: the platform's protected storage rather than the plain
 * settings store. Each platform binds its own implementation in its data module.
 */
interface SecureStore {
    /** One line for Settings saying where the secret is kept on this platform. */
    val description: String

    fun get(key: String): String?

    fun put(key: String, value: String)

    fun remove(key: String)
}

/** Keeps secrets for the process lifetime only; used in tests and as a last resort. */
class InMemorySecureStore(override val description: String = "kept in memory for this session only") : SecureStore {
    private val values = mutableMapOf<String, String>()
    override fun get(key: String): String? = values[key]
    override fun put(key: String, value: String) { values[key] = value }
    override fun remove(key: String) { values.remove(key) }
}
