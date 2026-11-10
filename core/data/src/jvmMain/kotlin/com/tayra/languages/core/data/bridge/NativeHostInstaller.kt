package com.tayra.languages.core.data.bridge

import co.touchlab.kermit.Logger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions
import java.security.SecureRandom

/**
 * Connects the Tayra extension to the app with no pairing code, through the browsers' native
 * messaging: the app registers a small helper with every browser on this computer, and a browser
 * lets only the extensions named in the registration start it, so only Tayra's extension gets the
 * token the helper gives. The helper is a shell script (PowerShell on Windows) that prints the
 * token from a file only this user can read; it needs no running app.
 */
class NativeHostInstaller(
    private val folder: File,
    private val home: File = File(System.getProperty("user.home")),
    private val os: String = System.getProperty("os.name").lowercase(),
    private val registry: (List<String>) -> Unit = ::runRegistry,
) {
    private val log = Logger.withTag("NativeHost")

    private var cached: String? = null

    /** The token the helper gives the extension, made once and kept. */
    @Synchronized
    fun token(): String = cached ?: readOrMake().also { cached = it }

    private fun readOrMake(): String {
        val file = File(folder, TOKEN_FILE)
        file.takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.length == TOKEN_LENGTH }?.let { return it }
        folder.mkdirs()
        val bytes = ByteArray(TOKEN_LENGTH / 2).also(SecureRandom()::nextBytes)
        val token = bytes.joinToString("") { "%02x".format(it) }
        writePrivate(file, token)
        return token
    }

    /** Writes the helper and registers it with each browser found; returns the browsers it was registered with. */
    fun install(): List<String> {
        token()
        val windows = os.contains("win")
        val host = if (windows) writeWindowsHost() else writeShellHost()
        val chromium = manifest(host, "allowed_origins", JsonArray(CHROME_EXTENSION_IDS.map { JsonPrimitive("chrome-extension://$it/") }))
        val firefox = manifest(host, "allowed_extensions", JsonArray(listOf(JsonPrimitive(FIREFOX_EXTENSION_ID))))
        return if (windows) registerOnWindows(chromium, firefox) else registerInFolders(chromium, firefox)
    }

    private fun manifest(host: File, allowedKey: String, allowed: JsonArray): String = json.encodeToString(
        kotlinx.serialization.json.JsonObject.serializer(),
        buildJsonObject {
            put("name", HOST_NAME)
            put("description", "Lets the Tayra extension use the Tayra app on this computer")
            put("path", host.absolutePath)
            put("type", "stdio")
            put(allowedKey, allowed)
        },
    )

    private fun writeShellHost(): File {
        val script = File(folder, "tayra-bridge-host")
        // Only ASCII in the answer, so its length in characters is its length in bytes; the length goes first, as four little-endian bytes.
        script.writeText(
            """
            |#!/bin/sh
            |# Gives the Tayra browser extension the token to use the Tayra app on this computer.
            |token=${'$'}(cat '${File(folder, TOKEN_FILE).absolutePath.replace("'", "'\\''")}' 2>/dev/null)
            |message="{\"token\":\"${'$'}token\"}"
            |length=${'$'}{#message}
            |printf "${'$'}(printf '\\%03o\\%03o\\%03o\\%03o' ${'$'}((length & 255)) ${'$'}((length >> 8 & 255)) ${'$'}((length >> 16 & 255)) ${'$'}((length >> 24 & 255)))"
            |printf '%s' "${'$'}message"
            |""".trimMargin(),
        )
        script.setExecutable(true, true)
        return script
    }

    private fun writeWindowsHost(): File {
        val powershell = File(folder, "tayra-bridge-host.ps1")
        powershell.writeText(
            """
            |# Gives the Tayra browser extension the token to use the Tayra app on this computer.
            |${'$'}token = (Get-Content -Raw '${File(folder, TOKEN_FILE).absolutePath.replace("'", "''")}').Trim()
            |${'$'}bytes = [Text.Encoding]::UTF8.GetBytes('{"token":"' + ${'$'}token + '"}')
            |${'$'}out = [Console]::OpenStandardOutput()
            |${'$'}out.Write([BitConverter]::GetBytes([int]${'$'}bytes.Length), 0, 4)
            |${'$'}out.Write(${'$'}bytes, 0, ${'$'}bytes.Length)
            |${'$'}out.Flush()
            |""".trimMargin(),
        )
        val batch = File(folder, "tayra-bridge-host.bat")
        batch.writeText("@echo off\r\npowershell -NoProfile -NonInteractive -ExecutionPolicy Bypass -File \"%~dp0tayra-bridge-host.ps1\"\r\n")
        return batch
    }

    /** macOS and Linux: each browser reads hosts from a folder of its own; only browsers whose folder exists get one. */
    private fun registerInFolders(chromium: String, firefox: String): List<String> {
        val mac = os.contains("mac")
        val browsers = if (mac) MAC_BROWSERS else LINUX_BROWSERS
        val done = mutableListOf<String>()
        for ((name, path) in browsers) {
            val root = File(home, path)
            if (!root.isDirectory) continue
            val hosts = File(root, if (name == "Firefox") (if (mac) "NativeMessagingHosts" else "native-messaging-hosts") else "NativeMessagingHosts")
            try {
                hosts.mkdirs()
                File(hosts, "$HOST_NAME.json").writeText(if (name == "Firefox") firefox else chromium)
                done += name
            } catch (e: Exception) {
                log.w(e) { "could not register the helper with $name" }
            }
        }
        return done
    }

    /** Windows: the manifests sit beside the helper, and each browser finds them through the registry. */
    private fun registerOnWindows(chromium: String, firefox: String): List<String> {
        val chromiumFile = File(folder, "$HOST_NAME.chromium.json").apply { writeText(chromium) }
        val firefoxFile = File(folder, "$HOST_NAME.firefox.json").apply { writeText(firefox) }
        val done = mutableListOf<String>()
        for ((name, key) in WINDOWS_KEYS) {
            val file = if (name == "Firefox") firefoxFile else chromiumFile
            try {
                registry(listOf("reg", "add", "HKCU\\$key\\$HOST_NAME", "/ve", "/t", "REG_SZ", "/d", file.absolutePath, "/f"))
                done += name
            } catch (e: Exception) {
                log.w(e) { "could not register the helper with $name" }
            }
        }
        return done
    }

    private fun writePrivate(file: File, text: String) {
        file.writeText(text)
        runCatching { Files.setPosixFilePermissions(file.toPath(), PosixFilePermissions.fromString("rw-------")) }
    }

    companion object {
        const val HOST_NAME = "app.tayra.bridge"
        const val TOKEN_FILE = "bridge-token"
        private const val TOKEN_LENGTH = 64

        /** The extension's ID in Chrome and the browsers built on Chromium: the development builds' (fixed by the manifest's key). */
        val CHROME_EXTENSION_IDS = listOf("emhgfoemkgabcjfadhmekfkjgjjhhfcd")
        const val FIREFOX_EXTENSION_ID = "youtube@tayra.app"

        private val MAC_BROWSERS = listOf(
            "Chrome" to "Library/Application Support/Google/Chrome",
            "Chrome Beta" to "Library/Application Support/Google/Chrome Beta",
            "Chromium" to "Library/Application Support/Chromium",
            "Edge" to "Library/Application Support/Microsoft Edge",
            "Brave" to "Library/Application Support/BraveSoftware/Brave-Browser",
            "Vivaldi" to "Library/Application Support/Vivaldi",
            "Arc" to "Library/Application Support/Arc/User Data",
            "Firefox" to "Library/Application Support/Mozilla",
        )
        private val LINUX_BROWSERS = listOf(
            "Chrome" to ".config/google-chrome",
            "Chrome Beta" to ".config/google-chrome-beta",
            "Chromium" to ".config/chromium",
            "Edge" to ".config/microsoft-edge",
            "Brave" to ".config/BraveSoftware/Brave-Browser",
            "Vivaldi" to ".config/vivaldi",
            "Firefox" to ".mozilla",
        )
        private val WINDOWS_KEYS = listOf(
            "Chrome" to "Software\\Google\\Chrome\\NativeMessagingHosts",
            "Edge" to "Software\\Microsoft\\Edge\\NativeMessagingHosts",
            "Firefox" to "Software\\Mozilla\\NativeMessagingHosts",
        )

        private val json = Json { prettyPrint = true }

        private fun runRegistry(command: List<String>) {
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            process.inputStream.readAllBytes()
            check(process.waitFor() == 0) { "reg failed" }
        }
    }
}
