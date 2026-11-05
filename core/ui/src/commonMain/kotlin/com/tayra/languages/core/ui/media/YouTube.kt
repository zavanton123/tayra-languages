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

    private var ready = CompletableDeferred<Unit>()
    /** Counts the pages this player has moved on to, so what was asked of an earlier one is asked again. */
    private var page = 0
    private var pendingPlay = false
    private val waiting = mutableMapOf<Int, CompletableDeferred<String?>>()
    private var nextId = 0
    private var poller: Job? = null

    /** The place the player last told, and when, to count on from between its answers. */
    private var told = 0L
    private var toldAt = TimeSource.Monotonic.markNow()
    private var pendingSeek: Long? = null

    /** The page finished loading: the script that bridges it is put in. */
    open fun onPageLoaded() {
        scope.launch { evaluate(YouTubeScripts.BRIDGE) }
    }

    /** YouTube won't embed this video: a platform that has another page to play it on goes there and says so. */
    protected open fun openWatchPage(): Boolean = false

    /** Loads YouTube's own page again, for a player whose subtitles need it; false where this is not that page. */
    protected open fun reloadWatchPage(): Boolean = false

    private fun startOver() {
        page++
        poller?.cancel()
        ready.also { ready = CompletableDeferred() }.complete(Unit)
        waiting.values.forEach { it.complete(null) }
        waiting.clear()
    }

    private fun onRefused(code: Int) {
        // The refused page says so more than once, and may still answer after the player has left it.
        if (page > 0) return
        if (openWatchPage()) {
            startOver()
        } else {
            problem.value = youTubeProblem(code)
            playing.value = false
        }
    }

    /** A message the page posted (see [YouTubeScripts.BRIDGE]). */
    open fun onMessage(text: String) {
        val message = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return
        when (message["type"]?.jsonPrimitive?.content) {
            "ready" -> if (ready.complete(Unit)) startPolling()
            "state" -> onState(message["state"]?.jsonPrimitive?.intOrNull ?: return)
            "refused" -> onRefused(EMBEDDING_REFUSED)
            "error" -> {
                val code = message["code"]?.jsonPrimitive?.intOrNull ?: 0
                if (code in REFUSED_CODES) {
                    onRefused(code)
                } else {
                    problem.value = youTubeProblem(code)
                    playing.value = false
                }
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
        if (pendingPlay) {
            pendingPlay = false
            play()
        }
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
        if (!ready.isCompleted) pendingPlay = true
        command("window.__tayraHold = false; p.playVideo(); var v = p.querySelector('video'); if (v && v.paused && p.classList.contains('ad-showing')) v.play()")
    }

    override fun pause() {
        playing.value = false
        pendingPlay = false
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
        // A second time when the player has moved on to another page meanwhile.
        repeat(2) {
            val asked = page
            withTimeoutOrNull(READY_TIMEOUT_MS) { ready.await() } ?: return null.also { log.w { "the player never got ready" } }
            if (page != asked) return@repeat
            val text = call("__tayra.details()", DETAILS_TIMEOUT_MS)
            if (page != asked) return@repeat
            if (text == null) return null.also { log.w { "the player gave no details" } }
            val root = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: return null
            if (root["refused"]?.jsonPrimitive?.booleanOrNull == true) {
                onRefused(EMBEDDING_REFUSED)
                if (page != asked) return@repeat
                return null
            }
            return parsedDetails(root)
        }
        return null
    }

    private fun parsedDetails(root: JsonObject): YouTubeDetails? {
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
        val first = fetchCaptions(track, translateTo) ?: return null to null
        if (first.first != null || !reloadWatchPage()) return first
        // Having had its subtitles turned on, YouTube's own page asks for them by itself the next time it loads, token and all.
        startOver()
        return fetchCaptions(track, translateTo) ?: (null to null)
    }

    private suspend fun fetchCaptions(track: YouTubeTrack, translateTo: String?): Pair<String?, String?>? {
        val asked = page
        withTimeoutOrNull(READY_TIMEOUT_MS) { ready.await() } ?: return null
        if (page != asked) return fetchCaptions(track, translateTo)
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
        private val REFUSED_CODES = setOf(101, 150, 152, 153)
        private const val TOKEN_TIMEOUT_MS = 90_000L
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

    /** YouTube's own page of the video [id], for what it won't let the embedded player show. */
    fun watchUrl(id: String, language: String?): String =
        "https://www.youtube.com/watch?v=$id" + (language?.let { "&hl=$it&cc_lang_pref=$it" } ?: "")

    /**
     * Turns YouTube's own page of the video [id] into a player like the embedded one: only the player
     * shows, filling the view, and without YouTube's controls, as the app has its own. It waits for the
     * app's word to start, adverts too, and stays on this video when it ends. Adverts play as YouTube sends them.
     */
    fun watchPage(id: String): String = """
        (function () {
          if (window.__tayraWatch) return;
          window.__tayraWatch = true;
          window.__tayraHold = true;
          var style = document.createElement('style');
          style.textContent =
            'html, body { overflow: hidden !important; background: #000 !important; }' +
            '#movie_player { position: fixed !important; top: 0 !important; left: 0 !important; width: 100vw !important; height: 100vh !important; z-index: 2147483647 !important; background: #000; }' +
            '#movie_player video { top: 0 !important; left: 0 !important; width: 100vw !important; height: 100vh !important; object-fit: contain !important; }' +
            '#masthead-container, tp-yt-app-drawer, ytd-mini-guide-renderer, .ytp-chrome-top, .ytp-chrome-bottom, .ytp-gradient-top, .ytp-gradient-bottom, .ytp-pause-overlay, .ytp-ce-element, .ytp-endscreen-content, .ytp-autonav-endscreen-countdown-overlay, .ytp-cards-teaser, .iv-branding { display: none !important; }';
          document.documentElement.appendChild(style);
          setInterval(function () {
            var p = document.getElementById('movie_player');
            if (!p || !p.getPlayerState) return;
            if (window.__tayraHold) {
              if (p.getPlayerState() === 1) p.pauseVideo();
              var video = p.querySelector('video');
              if (video && !video.paused && p.classList.contains('ad-showing')) video.pause();
            }
            var cancel = document.querySelector('.ytp-autonav-endscreen-upnext-cancel-button');
            if (cancel) cancel.click();
            var data = p.getVideoData && p.getVideoData();
            if (data && data.video_id && data.video_id !== '$id') p.cueVideoById('$id');
          }, 100);
        })()
    """.trimIndent()

    /**
     * The bridge to the page: posts what happens (`ready`, `refused`, `state`, `error`, `result`) through the
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
          // The token of this video's subtitles only: an advert before it asks for its own.
          function keep(url) {
            try {
              var asked = new URL(url, location.href).searchParams;
              var here = new URL(location.href);
              var video = here.searchParams.get('v') || here.pathname.split('/').pop();
              if (asked.get('pot') && asked.get('v') === video) t.pot = asked.get('pot');
            } catch (e) {}
          }
          // For a request that passes the hooks below, sent with a copy of fetch the page took before they were in. Read as it is made: YouTube's page empties this record as it goes.
          try {
            new PerformanceObserver(function (list) {
              list.getEntries().forEach(function (e) { if (e.name.indexOf('/api/timedtext') >= 0) keep(e.name); });
            }).observe({ type: 'resource', buffered: true });
          } catch (e) {}
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
            // On YouTube's own page the player asks for subtitles only once it plays, which is after any advert: muted, and paused again after.
            var own = window.__tayraWatch;
            var held = window.__tayraHold;
            if (own) {
              window.__tayraHold = false;
              p.mute();
              p.playVideo();
              var video = p.querySelector('video');
              if (video && video.paused) video.play().catch(function () {});
              for (var w = 0; w < 300 && p.classList.contains('ad-showing'); w++) await sleep(250);
            }
            // The player's own entry for the track: a language code alone doesn't find automatic subtitles. Its list comes a moment after the module.
            function turnOn() {
              try {
                p.loadModule('captions');
                var list = p.getOption('captions', 'tracklist') || [];
                p.setOption('captions', 'track', list.filter(function (x) { return x.languageCode === lang; })[0] || { languageCode: lang });
              } catch (e) {}
            }
            // Off first: remembering subtitles as on, the player asks as the page loads, before it has a token, and gets nothing; it asks again only on a change.
            function turnOff() { try { p.setOption('captions', 'track', {}); } catch (e) {} }
            for (var i = 0; i < 40 && !t.pot; i++) {
              if (own && i % 4 === 0) turnOff();
              if (i % 4 === 1) turnOn();
              await sleep(250);
            }
            try { p.setOption('captions', 'track', {}); } catch (e) {}
            if (own) { p.pauseVideo(); p.seekTo(0, true); p.unMute(); window.__tayraHold = held; }
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
              (function refusedYet(looks) {
                if (document.querySelector('.ytp-embed-error')) post({ type: 'refused' });
                else if (looks < 120) setTimeout(function () { refusedYet(looks + 1); }, 500);
              })(0);
            } else {
              setTimeout(wait, 200);
            }
          })();
        })();
    """.trimIndent()
}
