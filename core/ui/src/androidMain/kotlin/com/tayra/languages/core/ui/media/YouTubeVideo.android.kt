package com.tayra.languages.core.ui.media

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
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
    val webView = remember(source) { WebView(context) }
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
private class AndroidYouTubeController(scope: CoroutineScope, private val web: WebView, source: VideoSource.YouTube) : WebYouTubeController(scope) {

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
            override fun onPageFinished(view: WebView, url: String) = onPageLoaded()
        }
        web.loadUrl(YouTubeScripts.embedUrl(source.id, source.language), mapOf("Referer" to YouTubeScripts.REFERRER))
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
