package com.idiomiko.core.data.bridge

import co.touchlab.kermit.Logger
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import com.idiomiko.core.domain.bridge.AddVideoRequest
import com.idiomiko.core.domain.bridge.AddVideoResponse
import com.idiomiko.core.domain.bridge.BridgeAccess
import com.idiomiko.core.domain.bridge.BookmarkRequest
import com.idiomiko.core.domain.bridge.BookmarkResponse
import com.idiomiko.core.domain.bridge.BridgeError
import com.idiomiko.core.domain.bridge.BridgeVideos
import com.idiomiko.core.domain.bridge.OpenVideoRequest
import com.idiomiko.core.domain.bridge.BridgeException
import com.idiomiko.core.domain.bridge.BridgeHello
import com.idiomiko.core.domain.bridge.BridgeStatus
import com.idiomiko.core.domain.bridge.BrowserBridgeApi
import com.idiomiko.core.domain.bridge.ChangesResponse
import com.idiomiko.core.domain.bridge.LookupResponse
import com.idiomiko.core.domain.bridge.SaveTermRequest
import com.idiomiko.core.domain.bridge.SaveTermResponse
import com.idiomiko.core.domain.bridge.StatusChanges
import com.idiomiko.core.domain.bridge.TranscriptRequest
import com.idiomiko.core.domain.bridge.TranscriptResponse
import com.idiomiko.core.domain.bridge.TranslateRequest
import com.idiomiko.core.domain.bridge.TranslateResponse
import java.io.IOException
import java.net.BindException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * The bridge as the desktop app runs it: the JDK's HTTP server on 127.0.0.1, so nothing outside this
 * computer reaches it. Web pages can call it too, so only the extension's own pages are let in (by
 * their Origin), the Host must be this computer's (no DNS rebinding), and every call but saying
 * hello needs the token the extension gets from the app's helper ([NativeHostInstaller]).
 */
class DesktopBrowserBridge(
    private val api: BrowserBridgeApi,
    private val changes: StatusChanges,
    private val access: BridgeAccess,
    private val ports: IntRange = DEFAULT_PORTS,
    /** Registers the helper that gives the extension its token; null where the browsers can't start one. */
    private val nativeHost: NativeHostInstaller? = null,
    /** The Videos library; null where the app has none. */
    private val videos: BridgeVideos? = null,
) {
    private val log = Logger.withTag("BrowserBridge")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** The port it listens on; null when it isn't running. */
    @Volatile
    var port: Int? = null
        private set
    private var server: HttpServer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        if (server != null) return
        changes.start(scope)
        nativeHost?.let { installer ->
            scope.launch(Dispatchers.IO) {
                runCatching { installer.install() }
                    .onSuccess { log.i { "the helper is registered with ${it.ifEmpty { listOf("no browser") }.joinToString()}" } }
                    .onFailure { log.w(it) { "the helper could not be registered" } }
            }
        }
        for (port in ports) {
            try {
                val created = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0)
                created.createContext("/") { exchange -> exchange.use { handle(it, port) } }
                created.executor = Executors.newFixedThreadPool(THREADS) { r -> Thread(r, "idiomiko-bridge").apply { isDaemon = true } }
                created.start()
                server = created
                this.port = port
                return
            } catch (e: BindException) {
                continue
            } catch (e: IOException) {
                log.w(e) { "the bridge could not start on $port" }
            }
        }
        log.w { "the bridge could not start: ports ${ports.first}–${ports.last} are all in use" }
    }

    fun stop() {
        server?.stop(0)
        server = null
        port = null
    }

    private fun handle(exchange: HttpExchange, port: Int) {
        val headers = exchange.requestHeaders
        val origin = headers.getFirst("Origin")
        if (origin != null && !origin.startsWith("chrome-extension://") && !origin.startsWith("moz-extension://")) {
            return reply(exchange, 403, BridgeError("Only the Idiomiko extension may call the app"), BridgeError.serializer())
        }
        val host = headers.getFirst("Host")?.substringBeforeLast(':')
        if (host != null && host != "127.0.0.1" && host != "localhost" && host != "[::1]") {
            return reply(exchange, 403, BridgeError("Wrong host"), BridgeError.serializer())
        }
        val path = exchange.requestURI.path
        val method = exchange.requestMethod
        try {
            when {
                method == "GET" && path == "/v1/hello" -> reply(exchange, 200, BridgeHello(), BridgeHello.serializer())
                !access.accepts(headers.getFirst("Authorization")?.removePrefix("Bearer ")?.trim()) ->
                    reply(exchange, 401, BridgeError("Only the Idiomiko extension may call the app"), BridgeError.serializer())
                method == "GET" && path == "/v1/status" -> reply(exchange, 200, call { api.status() }, BridgeStatus.serializer())
                method == "POST" && path == "/v1/transcript" -> {
                    val request = read(exchange, TranscriptRequest.serializer())
                    reply(exchange, 200, call { api.transcript(request) }, TranscriptResponse.serializer())
                }
                method == "GET" && path == "/v1/lookup" -> {
                    val term = query(exchange)["term"] ?: throw BridgeException("Nothing to look up")
                    reply(exchange, 200, call { api.lookup(term) }, LookupResponse.serializer())
                }
                method == "POST" && path == "/v1/terms" -> {
                    val request = read(exchange, SaveTermRequest.serializer())
                    reply(exchange, 200, call { api.save(request) }, SaveTermResponse.serializer())
                }
                method == "POST" && path == "/v1/translate" -> {
                    val request = read(exchange, TranslateRequest.serializer())
                    reply(exchange, 200, call { api.translate(request) }, TranslateResponse.serializer())
                }
                method == "GET" && path == "/v1/speech" -> {
                    val text = query(exchange)["text"] ?: throw BridgeException("Nothing to say")
                    val audio = call { api.speech(text) }
                    if (audio == null) noContent(exchange) else replyBytes(exchange, audio, "audio/wav")
                }
                method == "POST" && path == "/v1/videos" -> {
                    val request = read(exchange, AddVideoRequest.serializer())
                    reply(exchange, 200, call { library().add(request) }, AddVideoResponse.serializer())
                }
                method == "POST" && path == "/v1/bookmarks" -> {
                    val request = read(exchange, BookmarkRequest.serializer())
                    reply(exchange, 200, call { library().bookmark(request) }, BookmarkResponse.serializer())
                }
                method == "POST" && path == "/v1/open" -> {
                    val request = read(exchange, OpenVideoRequest.serializer())
                    call { library().open(request) }
                    noContent(exchange)
                }
                method == "GET" && path == "/v1/changes" -> {
                    val since = query(exchange)["since"]?.toLongOrNull() ?: 0L
                    reply(exchange, 200, call { changes.since(since) }, ChangesResponse.serializer())
                }
                else -> reply(exchange, 404, BridgeError("No such call"), BridgeError.serializer())
            }
        } catch (e: BridgeException) {
            reply(exchange, 400, BridgeError(e.message ?: "Bad request"), BridgeError.serializer())
        } catch (e: Exception) {
            log.w(e) { "the bridge failed on $method $path" }
            reply(exchange, 500, BridgeError("Idiomiko could not do that"), BridgeError.serializer())
        }
    }

    private fun <T> call(block: suspend () -> T): T = runBlocking(Dispatchers.IO) { block() }

    private fun <T> read(exchange: HttpExchange, serializer: KSerializer<T>): T {
        val bytes = exchange.requestBody.use { it.readNBytes(MAX_BODY + 1) }
        if (bytes.size > MAX_BODY) throw BridgeException("Too large")
        return runCatching { json.decodeFromString(serializer, bytes.decodeToString()) }.getOrElse { throw BridgeException("Not understood") }
    }

    private fun query(exchange: HttpExchange): Map<String, String> =
        exchange.requestURI.rawQuery.orEmpty().split('&').filter { '=' in it }.associate { part ->
            val (key, value) = part.split('=', limit = 2)
            URLDecoder.decode(key, Charsets.UTF_8) to URLDecoder.decode(value, Charsets.UTF_8)
        }

    private fun <T> reply(exchange: HttpExchange, status: Int, body: T, serializer: KSerializer<T>) {
        val bytes = json.encodeToString(serializer, body).encodeToByteArray()
        exchange.responseHeaders.set("Content-Type", "application/json; charset=utf-8")
        exchange.responseHeaders.set("Cache-Control", "no-store")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun library(): BridgeVideos = videos ?: throw BridgeException("This app has no Videos library")

    private fun noContent(exchange: HttpExchange) {
        exchange.responseHeaders.set("Cache-Control", "no-store")
        exchange.sendResponseHeaders(204, -1)
    }

    private fun replyBytes(exchange: HttpExchange, bytes: ByteArray, type: String) {
        exchange.responseHeaders.set("Content-Type", type)
        exchange.responseHeaders.set("Cache-Control", "no-store")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    companion object {
        /** The extension looks for the app on these ports, in order. */
        val DEFAULT_PORTS = 47_831..47_835
        private const val THREADS = 4
        private const val MAX_BODY = 8 * 1024 * 1024
    }
}
