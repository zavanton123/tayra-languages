package com.tayra.languages.core.ui.media

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.time.TimeSource

/** A subtitle track a YouTube video has; [automatic] for speech recognised by YouTube. */
data class YouTubeTrack(val languageCode: String, val automatic: Boolean, val name: String, val url: String)

/** What the YouTube player says about the video it opened. */
data class YouTubeDetails(val title: String?, val channel: String?, val lengthMs: Long, val tracks: List<YouTubeTrack>)

/** A player that can hand over the subtitles of the YouTube video it plays, as YouTube's "json3" text. */
interface YouTubeCaptions {
    /** The video's title, channel and subtitle tracks, once the player has them; null when it never does. */
    suspend fun details(): YouTubeDetails?

    /** The [track] and, with [translateTo], YouTube's translation of it into that language; null for any that can't be had. */
    suspend fun captions(track: YouTubeTrack, translateTo: String?): Pair<String?, String?>
}

/** Why YouTube's player refused a video, by its error code. */
fun youTubeProblem(code: Int): VideoProblem = when (code) {
    100 -> VideoProblem.Failed("This video is no longer on YouTube.")
    101, 150, 152, 153 -> VideoProblem.Failed("The owner of this video doesn't let it play outside YouTube.")
    else -> VideoProblem.Failed("YouTube could not play this video.")
}

/**
 * YouTube's own embedded player in a web view, driven through the page: it plays the video, says
 * where it is, and fetches the subtitles the way the player itself does, with the token its own
 * requests carry. Platforms give the web view ([evaluate] runs a script and hands back its value as
 * text) and pass every message the page posts to [onMessage].
 */
abstract class WebYouTubeController(private val scope: CoroutineScope) : VideoController, YouTubeCaptions {
    override val position = MutableStateFlow(0L)
    override val playing = MutableStateFlow(false)
    override val durationMs = MutableStateFlow(0L)
    override val rate = MutableStateFlow(1f)
    override val problem = MutableStateFlow<VideoProblem?>(null)
    override val latencyMs: Long get() = 60

    /** Runs [script] in the page; its value as text (a JSON string for objects), or null. */
    protected abstract suspend fun evaluate(script: String): String?

    private val ready = CompletableDeferred<Unit>()
    private val waiting = mutableMapOf<Int, CompletableDeferred<String?>>()
    private var nextId = 0
    private var poller: Job? = null

    /** The place the player last told, and when, to count on from between its answers. */
    private var told = 0L
    private var toldAt = TimeSource.Monotonic.markNow()
    private var pendingSeek: Long? = null

    /** The page finished loading: the script that bridges it is put in. */
    fun onPageLoaded() {
        scope.launch { evaluate(YouTubeScripts.BRIDGE) }
    }

    /** A message the page posted (see [YouTubeScripts.BRIDGE]). */
    open fun onMessage(text: String) {
        val message = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return
        when (message["type"]?.jsonPrimitive?.content) {
            "ready" -> if (ready.complete(Unit)) startPolling()
            "state" -> onState(message["state"]?.jsonPrimitive?.intOrNull ?: return)
            "error" -> {
                problem.value = youTubeProblem(message["code"]?.jsonPrimitive?.intOrNull ?: 0)
                playing.value = false
            }
            "result" -> {
                val id = message["id"]?.jsonPrimitive?.intOrNull ?: return
                waiting.remove(id)?.complete(
                    when (val value = message["value"]) {
                        null, is JsonNull -> null
                        is JsonObject, is JsonArray -> value.toString()
                        else -> value.jsonPrimitive.content
                    },
                )
            }
        }
    }

    private fun onState(state: Int) {
        // 1 plays, 3 buffers on the way to playing, 2 pauses, 0 ends, 5 is cued.
        playing.value = state == 1 || state == 3
        if (state == 0) position.value = durationMs.value
    }

    private fun startPolling() {
        pendingSeek?.let { at -> pendingSeek = null; seekTo(at) }
        poller = scope.launch {
            while (isActive) {
                read()
                delay(if (playing.value) POLL_PLAYING_MS else POLL_PAUSED_MS)
            }
        }
    }

    private suspend fun read() {
        val text = evaluate("window.__tayra ? __tayra.status() : null") ?: return
        val values = runCatching { Json.parseToJsonElement(text) as? JsonArray }.getOrNull() ?: return
        val seconds = values.getOrNull(0)?.jsonPrimitive?.doubleOrNull ?: return
        val state = values.getOrNull(1)?.jsonPrimitive?.intOrNull ?: return
        val length = values.getOrNull(2)?.jsonPrimitive?.doubleOrNull ?: 0.0
        values.getOrNull(3)?.jsonPrimitive?.doubleOrNull?.let { rate.value = it.toFloat() }
        if (length > 0) durationMs.value = (length * 1000).toLong()
        onState(state)
        told = (seconds * 1000).toLong()
        toldAt = TimeSource.Monotonic.markNow()
        position.value = told
    }

    /** Counts the place on from the player's last answer, as the screen draws a frame. */
    fun tick() {
        if (!playing.value) return
        val counted = told + (toldAt.elapsedNow().inWholeMilliseconds * rate.value).toLong()
        val end = durationMs.value
        position.value = if (end > 0) counted.coerceAtMost(end) else counted
    }

    private fun command(script: String) {
        scope.launch { evaluate("window.__tayra && __tayra.player() && (function(p){ $script })(__tayra.player())") }
    }

    override fun play() {
        if (problem.value != null) return
        playing.value = true
        toldAt = TimeSource.Monotonic.markNow()
        command("p.playVideo()")
    }

    override fun pause() {
        playing.value = false
        command("p.pauseVideo()")
        scope.launch { read() }
    }

    override fun seekTo(ms: Long) {
        val at = ms.coerceAtLeast(0)
        if (!ready.isCompleted) {
            pendingSeek = at
            return
        }
        told = at
        toldAt = TimeSource.Monotonic.markNow()
        position.value = at
        command("p.seekTo(${at / 1000.0}, true)")
    }

    override fun setRate(rate: Float) {
        this.rate.value = rate
        command("p.setPlaybackRate($rate)")
    }

    override fun release() {
        poller?.cancel()
        waiting.values.forEach { it.complete(null) }
        waiting.clear()
    }

    /** Runs [call] (an expression giving a promise) in the page and waits for what it settles to. */
    private suspend fun call(call: String, timeoutMs: Long = CALL_TIMEOUT_MS): String? {
        val id = nextId++
        val answer = CompletableDeferred<String?>()
        waiting[id] = answer
        evaluate("__tayra.answer($id, $call)")
        return withTimeoutOrNull(timeoutMs) { answer.await() }.also { waiting.remove(id) }
    }

    override suspend fun details(): YouTubeDetails? {
        withTimeoutOrNull(READY_TIMEOUT_MS) { ready.await() } ?: return null.also { log.w { "the player never got ready" } }
        val text = call("__tayra.details()", DETAILS_TIMEOUT_MS) ?: return null.also { log.w { "the player gave no details" } }
        val root = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return null
        if (root["refused"]?.jsonPrimitive?.booleanOrNull == true) {
            problem.value = youTubeProblem(EMBEDDING_REFUSED)
            return null
        }
        if (root["playable"]?.jsonPrimitive?.booleanOrNull == false) return null
        val tracks = (root["tracks"] as? JsonArray).orEmpty().mapNotNull { element ->
            val o = element as? JsonObject ?: return@mapNotNull null
            YouTubeTrack(
                languageCode = o["lang"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                automatic = o["kind"]?.jsonPrimitive?.content == "asr",
                name = o["name"]?.jsonPrimitive?.content.orEmpty(),
                url = o["url"]?.jsonPrimitive?.content ?: return@mapNotNull null,
            )
        }
        return YouTubeDetails(
            title = root["title"]?.jsonPrimitive?.content,
            channel = root["author"]?.jsonPrimitive?.content,
            lengthMs = (root["length"]?.jsonPrimitive?.longOrNull ?: 0) * 1000,
            tracks = tracks,
        )
    }

    override suspend fun captions(track: YouTubeTrack, translateTo: String?): Pair<String?, String?> {
        withTimeoutOrNull(READY_TIMEOUT_MS) { ready.await() } ?: return null to null
        // The player's own subtitle request carries the token YouTube asks for; it is made by turning its subtitles on.
        call("__tayra.token(${Json.encodeToString(String.serializer(), track.languageCode)})", TOKEN_TIMEOUT_MS)
        val url = Json.encodeToString(String.serializer(), track.url)
        val original = call("__tayra.track($url, null)")?.takeIf { it.isNotBlank() }
        val translated = translateTo?.let { lang -> call("__tayra.track($url, ${Json.encodeToString(String.serializer(), lang)})")?.takeIf { it.isNotBlank() } }
        return original to translated
    }

    companion object {
        private val log = Logger.withTag("YouTube")
        private const val POLL_PLAYING_MS = 150L
        private const val POLL_PAUSED_MS = 1_000L
        private const val CALL_TIMEOUT_MS = 20_000L
        private const val READY_TIMEOUT_MS = 30_000L
        private const val DETAILS_TIMEOUT_MS = 20_000L
        private const val EMBEDDING_REFUSED = 150
        private const val TOKEN_TIMEOUT_MS = 12_000L
    }
}

/** What runs in YouTube's embedded player page. */
object YouTubeScripts {
    /** What the page is opened with as its referrer; YouTube refuses embeds without one ("error 153"). */
    const val REFERRER = "https://tayra.app/"

    /** The embedded player of the video [id], without YouTube's controls (the app has its own), in [language]. */
    fun embedUrl(id: String, language: String?): String =
        "https://www.youtube-nocookie.com/embed/$id?controls=0&disablekb=1&playsinline=1&rel=0&iv_load_policy=3&fs=0" +
            "&enablejsapi=1&origin=${REFERRER.trimEnd('/')}" + (language?.let { "&hl=$it&cc_lang_pref=$it" } ?: "")

    /**
     * The bridge to the page: posts what happens (`ready`, `state`, `error`, `result`) through the
     * platform's `TayraBridge.postMessage`, keeps the token of the player's own subtitle requests,
     * and offers `__tayra.status()`, `details()`, `token(lang)` and `track(url, tlang)`.
     */
    val BRIDGE: String = """
        (function () {
          if (window.__tayra) return;
          var t = window.__tayra = { pot: null };
          function post(message) {
            var text = JSON.stringify(message);
            try {
              if (window.TayraBridge && window.TayraBridge.postMessage) window.TayraBridge.postMessage(text);
              else if (window.webkit && window.webkit.messageHandlers && window.webkit.messageHandlers.tayra) window.webkit.messageHandlers.tayra.postMessage(text);
              else if (window.cefQuery) window.cefQuery({ request: text, onSuccess: function () {}, onFailure: function () {} });
            } catch (e) {}
          }
          function keep(url) {
            try { var pot = new URL(url, location.href).searchParams.get('pot'); if (pot) t.pot = pot; } catch (e) {}
          }
          var open = XMLHttpRequest.prototype.open;
          XMLHttpRequest.prototype.open = function (method, url) {
            if (String(url).indexOf('/api/timedtext') >= 0) keep(String(url));
            return open.apply(this, arguments);
          };
          var fetch0 = window.fetch;
          window.fetch = function (input) {
            var url = typeof input === 'string' ? input : (input && input.url);
            if (url && String(url).indexOf('/api/timedtext') >= 0) keep(String(url));
            return fetch0.apply(this, arguments);
          };
          var style = document.createElement('style');
          style.textContent = '.ytp-caption-window-container, .caption-window { display: none !important; }';
          (document.head || document.documentElement).appendChild(style);
          t.player = function () { return document.getElementById('movie_player'); };
          t.status = function () {
            var p = t.player();
            if (!p || !p.getCurrentTime) return null;
            return JSON.stringify([p.getCurrentTime(), p.getPlayerState(), p.getDuration(), p.getPlaybackRate ? p.getPlaybackRate() : 1]);
          };
          t.answer = function (id, promise) {
            Promise.resolve(promise).then(function (value) { post({ type: 'result', id: id, value: value }); },
              function () { post({ type: 'result', id: id, value: null }); });
          };
          function response() { var p = t.player(); return p && p.getPlayerResponse ? p.getPlayerResponse() : null; }
          function sleep(ms) { return new Promise(function (r) { setTimeout(r, ms); }); }
          t.details = async function () {
            var r = response();
            for (var i = 0; i < 40 && (!r || !r.videoDetails); i++) {
              // A video that may not be embedded gets this screen, with no error event and no details.
              if (document.querySelector('.ytp-embed-error')) return JSON.stringify({ refused: true });
              // The embedded player learns about its video only once it is asked to play; muted, and paused again at once.
              var p = t.player();
              if (i === 0 && p && p.mute) { p.mute(); p.playVideo(); }
              await sleep(250);
              r = response();
            }
            var p2 = t.player();
            if (p2 && p2.pauseVideo) { p2.pauseVideo(); p2.seekTo(0, true); if (p2.unMute) p2.unMute(); }
            if (!r) return null;
            var list = r.captions && r.captions.playerCaptionsTracklistRenderer;
            var tracks = (list && list.captionTracks || []).map(function (c) {
              var name = c.name ? (c.name.simpleText || (c.name.runs || []).map(function (x) { return x.text; }).join('')) : '';
              return { lang: c.languageCode, kind: c.kind || '', name: name, url: c.baseUrl };
            });
            var d = r.videoDetails || {};
            var status = r.playabilityStatus && r.playabilityStatus.status;
            return JSON.stringify({ title: d.title, author: d.author, length: Number(d.lengthSeconds || 0), tracks: tracks, playable: status ? status === 'OK' : true });
          };
          t.token = async function (lang) {
            if (t.pot) return t.pot;
            var p = t.player();
            try { p.loadModule('captions'); p.setOption('captions', 'track', { languageCode: lang }); } catch (e) {}
            for (var i = 0; i < 40 && !t.pot; i++) await sleep(250);
            try { p.setOption('captions', 'track', {}); } catch (e) {}
            return t.pot;
          };
          t.track = async function (url, tlang) {
            var u = String(url).replace(/&fmt=[^&]*/g, '').replace(/&tlang=[^&]*/g, '') + '&fmt=json3';
            if (tlang) u += '&tlang=' + encodeURIComponent(tlang);
            if (t.pot) u += '&c=WEB&pot=' + encodeURIComponent(t.pot);
            var response = await fetch0(u, { credentials: 'include' });
            return response.ok ? await response.text() : null;
          };
          (function wait() {
            var p = t.player();
            if (p && p.addEventListener && p.getPlayerState) {
              // The page's own player wants functions here; the iframe API's names of global functions break it.
              p.addEventListener('onStateChange', function (state) { post({ type: 'state', state: state }); });
              p.addEventListener('onError', function (code) { post({ type: 'error', code: code }); });
              post({ type: 'ready' });
            } else {
              setTimeout(wait, 200);
            }
          })();
        })();
    """.trimIndent()
}
