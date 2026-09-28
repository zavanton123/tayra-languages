package com.tayra.languages.core.data.settings

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings

/** Secrets as generic-password items in the iOS Keychain, under the app's own service name. */
@OptIn(ExperimentalSettingsImplementation::class)
class KeychainSecureStore(service: String = "com.tayra.languages.secure") : SecureStore {
    private val keychain = KeychainSettings(service)

    override val description: String = "kept in the iOS Keychain"

    override fun get(key: String): String? = keychain.getStringOrNull(key)

    override fun put(key: String, value: String) = keychain.putString(key, value)

    override fun remove(key: String) = keychain.remove(key)
}
