package com.tayra.languages.core.ui.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVPlayerTimeControlStatusPaused
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.playImmediatelyAtRate
import platform.AVFoundation.rate
import platform.AVFoundation.seekToTime
import platform.AVFoundation.setRate
import platform.AVFoundation.timeControlStatus
import platform.AVKit.AVPlayerViewController
import platform.CoreGraphics.CGRectZero
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.CoreMedia.kCMTimeZero
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.Foundation.setValue
import platform.WebKit.WKAudiovisualMediaTypeNone
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKUserScript
import platform.WebKit.WKUserScriptInjectionTime
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject
import platform.darwin.NSObjectProtocol
import kotlin.coroutines.resume

actual val videoSupported: Boolean = true

@Composable
actual fun VideoPlayer(source: VideoSource, onController: (VideoController) -> Unit, modifier: Modifier) {
    when (source) {
        is VideoSource.Link -> LinkVideo(source.url, onController, modifier)
        is VideoSource.YouTube -> YouTubeVideo(source, onController, modifier)
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
private fun LinkVideo(address: String, onController: (VideoController) -> Unit, modifier: Modifier) {
    val controller = remember(address) { AvVideoController(address) }
    DisposableEffect(controller) {
        onController(controller)
        onDispose { controller.release() }
    }
    // Read every frame while it plays, a few times a second while it does not (to notice the length, an end, a failure).
    LaunchedEffect(controller) {
        while (isActive) {
            if (controller.playing.value) withFrameMillis { controller.tick() } else {
                controller.tick()
                delay(PAUSED_POLL_MS)
            }
        }
    }
    UIKitViewController(
        factory = { AVPlayerViewController().apply { player = controller.player; showsPlaybackControls = false; videoGravity = AVLayerVideoGravityResizeAspect } },
        modifier = modifier,
    )
}

@OptIn(ExperimentalForeignApi::class)
private class AvVideoController(address: String) : VideoController {
    private val url = NSURL.URLWithString(address)
    val player = AVPlayer(playerItem = url?.let { AVPlayerItem(uRL = it) })

    override val position = MutableStateFlow(0L)
    override val playing = MutableStateFlow(false)
    override val durationMs = MutableStateFlow(0L)
    override val rate = MutableStateFlow(1f)
    override val problem = MutableStateFlow<VideoProblem?>(if (url == null) VideoProblem.Failed("The video could not be played.") else null)

    private val ended: NSObjectProtocol = NSNotificationCenter.defaultCenter.addObserverForName(
        AVPlayerItemDidPlayToEndTimeNotification, player.currentItem, NSOperationQueue.mainQueue,
    ) { playing.value = false }

    fun tick() {
        val item = player.currentItem ?: return
        val seconds = CMTimeGetSeconds(player.currentTime())
        if (!seconds.isNaN()) position.value = (seconds * 1000).toLong()
        val length = CMTimeGetSeconds(item.duration)
        if (!length.isNaN() && length > 0) durationMs.value = (length * 1000).toLong()
        if (item.status == AVPlayerItemStatusFailed) {
            problem.value = VideoProblem.Failed("The video could not be played.")
        }
        if (player.timeControlStatus == AVPlayerTimeControlStatusPaused) playing.value = false
    }

    override fun play() {
        if (problem.value != null) return
        player.playImmediatelyAtRate(rate.value)
        playing.value = true
    }

    override fun pause() {
        player.pause()
        playing.value = false
        tick()
    }

    override fun seekTo(ms: Long) {
        val at = ms.coerceAtLeast(0)
        player.seekToTime(CMTimeMakeWithSeconds(at / 1000.0, 600), toleranceBefore = kCMTimeZero.readValue(), toleranceAfter = kCMTimeZero.readValue())
        position.value = at
    }

    override fun setRate(rate: Float) {
        this.rate.value = rate
        if (playing.value) player.setRate(rate)
    }

    override fun release() {
        NSNotificationCenter.defaultCenter.removeObserver(ended)
        player.pause()
    }
}

/** A YouTube video in YouTube's embedded player, in a web view. */
@OptIn(ExperimentalForeignApi::class)
@Composable
private fun YouTubeVideo(source: VideoSource.YouTube, onController: (VideoController) -> Unit, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    val controller = remember(source) { WebKitYouTubeController(scope, source) }
    DisposableEffect(controller) {
        onController(controller)
        onDispose { controller.release() }
    }
    LaunchedEffect(controller) {
        while (isActive) {
            if (controller.playing.value) withFrameMillis { controller.tick() } else delay(PAUSED_POLL_MS)
        }
    }
    UIKitView(factory = { controller.web }, modifier = modifier)
}

/** What the page's `webkit.messageHandlers.tayra.postMessage` reaches. */
private class PageMessages(private val onText: (String) -> Unit) : NSObject(), WKScriptMessageHandlerProtocol {
    override fun userContentController(userContentController: WKUserContentController, didReceiveScriptMessage: WKScriptMessage) {
        (didReceiveScriptMessage.body as? String)?.let(onText)
    }
}

@OptIn(ExperimentalForeignApi::class)
private class WebKitYouTubeController(scope: CoroutineScope, source: VideoSource.YouTube) : WebYouTubeController(scope) {
    private val messages = PageMessages { onMessage(it) }

    val web: WKWebView = run {
        val config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        config.mediaTypesRequiringUserActionForPlayback = WKAudiovisualMediaTypeNone
        config.userContentController.addScriptMessageHandler(messages, "tayra")
        // Put in when the page's document is there; WebKit runs it for every page load on its own.
        config.userContentController.addUserScript(
            WKUserScript(source = YouTubeScripts.BRIDGE, injectionTime = WKUserScriptInjectionTime.WKUserScriptInjectionTimeAtDocumentEnd, forMainFrameOnly = true),
        )
        WKWebView(frame = CGRectZero.readValue(), configuration = config)
    }

    init {
        val request = NSMutableURLRequest.requestWithURL(NSURL(string = YouTubeScripts.embedUrl(source.id, source.language)))
        request.setValue(YouTubeScripts.REFERRER, forHTTPHeaderField = "Referer")
        web.loadRequest(request)
    }

    override suspend fun evaluate(script: String): String? = suspendCancellableCoroutine { done ->
        web.evaluateJavaScript(script) { result, _ ->
            if (done.isActive) done.resume((result as? String) ?: result?.toString())
        }
    }

    override fun release() {
        super.release()
        web.configuration.userContentController.removeScriptMessageHandlerForName("tayra")
        web.stopLoading()
    }
}

private const val PAUSED_POLL_MS = 250L
