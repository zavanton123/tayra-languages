package com.tayra.languages.core.data.settings

import co.touchlab.kermit.Logger
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Crypt32Util
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.util.Base64
import java.util.prefs.Preferences

/** Picks the protected storage the desktop OS offers. */
fun desktopSecureStore(): SecureStore {
    val os = System.getProperty("os.name").lowercase()
    return when {
        os.contains("mac") -> MacKeychainStore()
        os.contains("win") -> WindowsDpapiStore()
        else -> LinuxSecretStore()
    }
}

/** Generic-password items in the login keychain, through the Security framework. */
class MacKeychainStore(private val service: String = "com.tayra.languages") : SecureStore {
    override val description: String = "kept in the macOS Keychain"

    private interface Security : Library {
        fun SecKeychainAddGenericPassword(keychain: Pointer?, serviceLen: Int, service: ByteArray, accountLen: Int, account: ByteArray, passwordLen: Int, password: ByteArray, itemRef: PointerByReference?): Int
        fun SecKeychainFindGenericPassword(keychain: Pointer?, serviceLen: Int, service: ByteArray, accountLen: Int, account: ByteArray, passwordLen: IntByReference, password: PointerByReference, itemRef: PointerByReference?): Int
        fun SecKeychainItemDelete(itemRef: Pointer): Int
        fun SecKeychainItemFreeContent(attrList: Pointer?, data: Pointer?): Int
    }

    private val security: Security by lazy { Native.load("Security", Security::class.java) }
    private val serviceBytes = service.toByteArray()

    override fun get(key: String): String? {
        val account = key.toByteArray()
        val length = IntByReference()
        val data = PointerByReference()
        val status = security.SecKeychainFindGenericPassword(null, serviceBytes.size, serviceBytes, account.size, account, length, data, null)
        if (status != 0) return null
        return try {
            String(data.value.getByteArray(0, length.value), Charsets.UTF_8)
        } finally {
            security.SecKeychainItemFreeContent(null, data.value)
        }
    }

    override fun put(key: String, value: String) {
        remove(key)
        val account = key.toByteArray()
        val password = value.toByteArray()
        val status = security.SecKeychainAddGenericPassword(null, serviceBytes.size, serviceBytes, account.size, account, password.size, password, null)
        check(status == 0) { "Keychain refused the item (status $status)" }
    }

    override fun remove(key: String) {
        val account = key.toByteArray()
        val item = PointerByReference()
        val status = security.SecKeychainFindGenericPassword(null, serviceBytes.size, serviceBytes, account.size, account, IntByReference(), PointerByReference(), item)
        if (status == 0) security.SecKeychainItemDelete(item.value)
    }
}

/** Values sealed with the Windows Data Protection API for the current user, kept in the registry-backed preferences. */
class WindowsDpapiStore : SecureStore {
    override val description: String = "encrypted for this Windows user account"
    private val node = Preferences.userRoot().node("com/tayra/languages/secure")

    override fun get(key: String): String? = node.get(key, null)?.let { stored ->
        String(Crypt32Util.cryptUnprotectData(Base64.getDecoder().decode(stored)), Charsets.UTF_8)
    }

    override fun put(key: String, value: String) {
        node.put(key, Base64.getEncoder().encodeToString(Crypt32Util.cryptProtectData(value.toByteArray(Charsets.UTF_8))))
    }

    override fun remove(key: String) = node.remove(key)
}

/**
 * The desktop keyring through `secret-tool` (libsecret) when it is installed; otherwise a file
 * in the app's data folder that only the owner can read.
 */
class LinuxSecretStore : SecureStore {
    private val dir = File(DatabaseDriverFactory.dataDirectory(), "secure")
    private val hasSecretTool: Boolean by lazy { runCatching { run(listOf("secret-tool", "--version"), null).first == 0 }.getOrDefault(false) }

    override val description: String get() = if (hasSecretTool) "kept in the desktop keyring" else "kept in a file only your user can read"

    override fun get(key: String): String? {
        if (hasSecretTool) {
            val (code, out) = run(listOf("secret-tool", "lookup", "app", "tayra-languages", "key", key), null)
            return if (code == 0 && out.isNotEmpty()) out else null
        }
        return File(dir, key).takeIf { it.exists() }?.readText()
    }

    override fun put(key: String, value: String) {
        if (hasSecretTool) {
            val (code, _) = run(listOf("secret-tool", "store", "--label=Tayra Languages $key", "app", "tayra-languages", "key", key), value)
            check(code == 0) { "secret-tool could not store the value" }
            return
        }
        dir.mkdirs()
        val file = File(dir, key)
        file.writeText(value)
        runCatching { Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-------")) }
            .onFailure { Logger.w { "Could not restrict permissions on ${file.absolutePath}" } }
    }

    override fun remove(key: String) {
        if (hasSecretTool) run(listOf("secret-tool", "clear", "app", "tayra-languages", "key", key), null)
        File(dir, key).delete()
    }

    /** Runs a command, feeding [input] on stdin so a secret never appears in the process list. */
    private fun run(command: List<String>, input: String?): Pair<Int, String> {
        val process = ProcessBuilder(command).redirectErrorStream(false).start()
        process.outputStream.use { out -> input?.let { out.write(it.toByteArray()) } }
        val output = process.inputStream.bufferedReader().readText().trim()
        return process.waitFor() to output
    }
}
