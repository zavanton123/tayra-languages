package com.tayra.languages.core.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import co.touchlab.kermit.Logger
import kotlinx.coroutines.launch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.cef.CefApp
import org.cef.CefClient
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.browser.CefMessageRouter
import org.cef.browser.CefRendering
import org.cef.callback.CefQueryCallback
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefMessageRouterHandlerAdapter
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.handler.CefResourceRequestHandler
import org.cef.handler.CefResourceRequestHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Chromium as JetBrains Runtime carries it (its jcef module): the desktop app's player of YouTube
 * videos and video links. Only touched once [JcefRuntime.available] says it is there.
 */
internal object Jcef {
    private val log = Logger.withTag("Chromium")

    /** Started once, the first time a video is opened. */
    private val app: CefApp? by lazy {
        runCatching {
            // JetBrains Runtime knows where its Chromium lies and how it starts: its own configuration says so.
            val config = Class.forName("com.jetbrains.cef.JCefAppConfig")
            val instance = config.getMethod("getInstance").invoke(null)
            @Suppress("UNCHECKED_CAST")
            // The app starts and stops the video itself, as the phones' web views are let to.
            val args = (config.getMethod("getAppArgs").invoke(instance) as Array<String>) + "--autoplay-policy=no-user-gesture-required"
            val settings = config.getMethod("getCefSettings").invoke(instance) as CefSettings
            settings.windowless_rendering_enabled = false
            settings.cache_path = File(System.getProperty("java.io.tmpdir"), "tayra-chromium").absolutePath
            CefApp.startup(args)
            // JetBrains Runtime 25 added the path of an out-of-process Chromium server (null: its own) to the call 21 has.
            val start = CefApp::class.java.methods.filter { it.name == "getInstance" }
            val two = start.firstOrNull { it.parameterTypes.contentEquals(arrayOf(Array<String>::class.java, CefSettings::class.java)) }
            val three = start.firstOrNull { it.parameterTypes.contentEquals(arrayOf(Array<String>::class.java, CefSettings::class.java, File::class.java)) }
            when {
                two != null -> two.invoke(null, args, settings)
                three != null -> three.invoke(null, args, settings, null)
                else -> error("this JetBrains Runtime starts Chromium in a way the app does not know")
            } as CefApp
        }.onFailure { log.w(it) { "Chromium did not start" } }.getOrNull()
    }

    /** One client for every player, kept for the app's life, each player's browser told apart in its handlers. */
    private val client: CefClient? by lazy { app?.createClient()?.also(::listen) }
    private val players = ConcurrentHashMap<CefBrowser, JcefController>()

    /** The videos YouTube has refused to embed, so they open on its own page at once from then on. */
    val refused: MutableSet<String> = ConcurrentHashMap.newKeySet()

    fun open(url: String, player: JcefController): CefBrowser? =
        client?.createBrowser(url, CefRendering.DEFAULT, false)?.also { players[it] = player }

    fun close(browser: CefBrowser) {
        players.remove(browser)
        runCatching { browser.close(true) }
    }

    private fun listen(client: CefClient) {
        val router = CefMessageRouter.create()
        router.addHandler(object : CefMessageRouterHandlerAdapter() {
            override fun onQuery(browser: CefBrowser, frame: CefFrame, queryId: Long, request: String, persistent: Boolean, callback: CefQueryCallback): Boolean {
                callback.success("")
                players[browser]?.let { player -> java.awt.EventQueue.invokeLater { player.receive(request) } }
                return true
            }
        }, true)
        client.addMessageRouter(router)
        // Whatever would open a window of its own (an advert's page, say) opens in the browser.
        client.addLifeSpanHandler(object : CefLifeSpanHandlerAdapter() {
            override fun onBeforePopup(browser: CefBrowser, frame: CefFrame, targetUrl: String?, targetFrameName: String?): Boolean {
                targetUrl?.let { url -> runCatching { java.awt.Desktop.getDesktop().browse(java.net.URI(url)) } }
                return true
            }
        })
        client.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadStart(browser: CefBrowser, frame: CefFrame, transitionType: CefRequest.TransitionType) {
                if (frame.isMain) players[browser]?.onPageStarted()
            }

            override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                if (frame.isMain) players[browser]?.let { player -> java.awt.EventQueue.invokeLater { player.onPageLoaded() } }
            }
        })
        // YouTube refuses an embedded player with no referrer ("error 153"); its page is asked for with the app's.
        client.addRequestHandler(object : CefRequestHandlerAdapter() {
            override fun getResourceRequestHandler(
                browser: CefBrowser, frame: CefFrame, request: CefRequest, isNavigation: Boolean, isDownload: Boolean, requestInitiator: String?, disableDefaultHandling: BoolRef?,
            ): CefResourceRequestHandler? = if (isNavigation) referrer else null
        })
    }

    private val referrer = object : CefResourceRequestHandlerAdapter() {
        override fun onBeforeResourceLoad(browser: CefBrowser, frame: CefFrame, request: CefRequest): Boolean {
            if (request.url.startsWith("https://www.youtube-nocookie.com/embed/")) request.setReferrer(YouTubeScripts.REFERRER, CefRequest.ReferrerPolicy.REFERRER_POLICY_DEFAULT)
            return false
        }
    }
}

/** A YouTube video or a video link in JetBrains Runtime's Chromium. */
@Composable
internal fun JcefVideo(source: VideoSource, onController: (VideoController) -> Unit, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    val controller = remember(source) { JcefController(scope, source) }
    DisposableEffect(controller) {
        onController(controller)
        onDispose { controller.release() }
    }
    LaunchedEffect(controller) {
        controller.playing.collectLatest { playing -> if (playing) while (isActive) withFrameMillis { controller.tick() } }
    }
    Box(modifier.background(MaterialTheme.colorScheme.inverseSurface)) {
        // SwingPanel makes its view once: a new video's browser would never be shown, and so never start, without the key.
        controller.browser?.let { browser -> key(browser) { SwingPanel(factory = { browser.uiComponent }, modifier = Modifier.fillMaxSize()) } }
    }
}

/**
 * The page driven as on the phones. Chromium's pages reach the app through `cefQuery`, which also
 * carries back the values of the scripts the app runs, as JCEF's own calls give none.
 */
internal class JcefController(scope: CoroutineScope, private val source: VideoSource) : WebYouTubeController(scope) {
    private val answers = ConcurrentHashMap<Int, CompletableDeferred<String?>>()
    private val next = AtomicInteger(0)
    private var onWatchPage = source is VideoSource.YouTube && source.id in Jcef.refused
    val browser: CefBrowser?

    init {
        val url = when (source) {
            is VideoSource.YouTube -> if (onWatchPage) YouTubeScripts.watchUrl(source.id, source.language) else YouTubeScripts.embedUrl(source.id, source.language)
            is VideoSource.Link -> linkPage(source.url).toURI().toString()
        }
        browser = Jcef.open(url, this)
        if (browser == null) problem.value = VideoProblem.Failed("The video could not be played.")
    }

    override fun openWatchPage(): Boolean {
        val video = source as? VideoSource.YouTube ?: return false
        val page = browser ?: return false
        if (onWatchPage) return false
        onWatchPage = true
        Jcef.refused += video.id
        page.loadURL(YouTubeScripts.watchUrl(video.id, video.language))
        return true
    }

    override fun reloadWatchPage(): Boolean {
        val page = browser?.takeIf { onWatchPage } ?: return false
        page.reload()
        return true
    }

    /** As early as can be, so YouTube's page is never seen around the player nor its video heard starting by itself. */
    fun onPageStarted() {
        if (onWatchPage && source is VideoSource.YouTube) browser?.executeJavaScript(YouTubeScripts.watchPage(source.id), browser.url, 0)
    }

    override fun onPageLoaded() {
        onPageStarted()
        super.onPageLoaded()
    }

    fun receive(text: String) {
        val message = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull()
        val id = message?.get("evalId")?.jsonPrimitive?.intOrNull
        if (id == null) return onMessage(text)
        val value = message["value"]
        answers.remove(id)?.complete(
            when {
                value == null || value is JsonNull -> null
                value is JsonPrimitive && value.isString -> value.content
                else -> value.toString()
            },
        )
    }

    override fun onMessage(text: String) {
        // A link Chromium can't decode (H.264 is not in the open build) is better watched in the browser.
        if (source is VideoSource.Link && "\"error\"" in text) {
            problem.value = VideoProblem.Elsewhere(source.url)
            playing.value = false
            return
        }
        super.onMessage(text)
    }

    override suspend fun evaluate(script: String): String? {
        val page = browser ?: return null
        val id = next.getAndIncrement()
        val answer = CompletableDeferred<String?>()
        answers[id] = answer
        val expression = script.trim().trimEnd(';')
        page.executeJavaScript(
            "(function(){var v=null;try{v=($expression);}catch(e){}" +
                "if(window.cefQuery)window.cefQuery({request:JSON.stringify({evalId:$id,value:v===undefined?null:v}),onSuccess:function(){},onFailure:function(){}});})()",
            page.url, 0,
        )
        return withTimeoutOrNull(EVAL_TIMEOUT_MS) { answer.await() }.also { answers.remove(id) }
    }

    override fun release() {
        super.release()
        answers.values.forEach { it.complete(null) }
        browser?.let(Jcef::close)
    }

    private companion object {
        const val EVAL_TIMEOUT_MS = 5_000L
    }
}

/** A page of its own around a video link, offering the same calls as YouTube's page; HLS streams play through hls.js. */
private fun linkPage(url: String): File {
    val page = File(System.getProperty("java.io.tmpdir"), "tayra-video-${url.hashCode().toUInt()}.html")
    val quoted = JsonPrimitive(url).toString()
    page.writeText(
        """
        <!doctype html><html><head><meta charset="utf-8"><style>html,body{margin:0;height:100%;background:#000}video{width:100%;height:100%;object-fit:contain}</style>
        <script src="https://cdn.jsdelivr.net/npm/hls.js@1.5.17/dist/hls.min.js"></script></head>
        <body><video id="v" preload="auto" playsinline></video><script>
        (function () {
          var v = document.getElementById('v'), url = $quoted, t = window.__tayra = {};
          function post(m) { if (window.cefQuery) window.cefQuery({ request: JSON.stringify(m), onSuccess: function () {}, onFailure: function () {} }); }
          if (/\.m3u8(\?|$)/i.test(url) && window.Hls && Hls.isSupported()) { var hls = new Hls(); hls.loadSource(url); hls.attachMedia(v);
            hls.on(Hls.Events.ERROR, function (e, d) { if (d.fatal) post({ type: 'error', code: -1 }); }); } else { v.src = url; }
          var player = { playVideo: function () { v.play(); }, pauseVideo: function () { v.pause(); },
            seekTo: function (s) { v.currentTime = s; }, setPlaybackRate: function (r) { v.playbackRate = r; } };
          t.player = function () { return player; };
          function state() { return v.ended ? 0 : (v.paused ? 2 : 1); }
          t.status = function () { return JSON.stringify([v.currentTime, state(), isFinite(v.duration) ? v.duration : 0, v.playbackRate]); };
          t.answer = function (id, p) { Promise.resolve(p).then(function (x) { post({ type: 'result', id: id, value: x }); }, function () { post({ type: 'result', id: id, value: null }); }); };
          ['play', 'pause', 'ended'].forEach(function (name) { v.addEventListener(name, function () { post({ type: 'state', state: state() }); }); });
          v.addEventListener('error', function () { post({ type: 'error', code: -1 }); });
          v.addEventListener('loadedmetadata', function () { post({ type: 'ready' }); });
        })();
        </script></body></html>
        """.trimIndent(),
    )
    return page
}
