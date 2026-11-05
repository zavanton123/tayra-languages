package com.tayra.languages.core.ui.media

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.coroutines.resume

/** A YouTube video in YouTube's embedded player, in a web view. */
@Composable
internal fun YouTubeVideo(source: VideoSource.YouTube, onController: (VideoController) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Left to wrap its content, a web view lays its page out with no height, and YouTube's player takes that height.
    val webView = remember(source) { WebView(context).apply { layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT) } }
    val controller = remember(source) { AndroidYouTubeController(scope, webView, source) }
    DisposableEffect(controller) {
        onController(controller)
        onDispose {
            controller.release()
            webView.destroy()
        }
    }
    LaunchedEffect(controller) {
        controller.playing.collectLatest { playing ->
            if (playing) while (isActive) withFrameMillis { controller.tick() }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { controller.pause() }
    AndroidView(factory = { webView }, modifier = modifier)
}

@SuppressLint("SetJavaScriptEnabled")
private class AndroidYouTubeController(scope: CoroutineScope, private val web: WebView, private val source: VideoSource.YouTube) : WebYouTubeController(scope) {
    private var onWatchPage = source.id in embedRefused

    /** What the page's `TayraBridge.postMessage` reaches; called on a background thread, so passed on to the main one. */
    private inner class Bridge {
        @JavascriptInterface
        fun postMessage(text: String) {
            web.post { onMessage(text) }
        }
    }

    init {
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.mediaPlaybackRequiresUserGesture = false
        web.addJavascriptInterface(Bridge(), "TayraBridge")
        web.webViewClient = object : WebViewClient() {
            // An advert's links go to the browser, or they would take the player's place.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame || YouTubeScripts.staysInPlayer(request.url.host, request.url.path)) return false
                runCatching { view.context.startActivity(Intent(Intent.ACTION_VIEW, request.url)) }
                return true
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) = keepToPlayer()
            override fun onPageFinished(view: WebView, url: String) = onPageLoaded()
        }
        if (onWatchPage) {
            loadWatchPage()
        } else {
            web.loadUrl(YouTubeScripts.embedUrl(source.id, source.language), mapOf("Referer" to YouTubeScripts.REFERRER))
        }
    }

    override fun openWatchPage(): Boolean {
        if (onWatchPage) return false
        onWatchPage = true
        embedRefused += source.id
        loadWatchPage()
        return true
    }

    /** As a computer: YouTube's mobile site covers its player with a dialog about its app. */
    private fun loadWatchPage() {
        val chrome = Regex("Chrome/[0-9.]+").find(web.settings.userAgentString)?.value ?: "Chrome/145.0.0.0"
        web.settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) $chrome Safari/537.36"
        web.loadUrl(YouTubeScripts.watchUrl(source.id, source.language))
    }

    override fun reloadWatchPage(): Boolean {
        if (!onWatchPage) return false
        web.reload()
        return true
    }

    private fun keepToPlayer() {
        if (onWatchPage) web.evaluateJavascript(YouTubeScripts.watchPage(source.id), null)
    }

    override fun onPageLoaded() {
        keepToPlayer()
        super.onPageLoaded()
    }

    override suspend fun evaluate(script: String): String? = suspendCancellableCoroutine { done ->
        web.post {
            runCatching {
                web.evaluateJavascript(script) { result -> if (done.isActive) done.resume(decode(result)) }
            }.onFailure { if (done.isActive) done.resume(null) }
        }
    }

    /** evaluateJavascript gives the value as JSON: a string comes quoted. */
    private fun decode(result: String?): String? {
        if (result == null) return null
        val element = runCatching { Json.parseToJsonElement(result) }.getOrNull() ?: return null
        return when {
            element is JsonNull -> null
            element is JsonPrimitive && element.isString -> element.content
            else -> element.toString()
        }
    }
}
