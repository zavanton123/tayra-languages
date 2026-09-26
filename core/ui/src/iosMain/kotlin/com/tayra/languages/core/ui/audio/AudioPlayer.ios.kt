package com.tayra.languages.core.ui.audio

import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemFailedToPlayToEndTimeNotification
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.darwin.NSObjectProtocol

actual class AudioPlayer actual constructor() {
    private val player = AVPlayer()
    private var observers: List<NSObjectProtocol> = emptyList()
    private var onFinished: (() -> Unit)? = null

    actual fun play(url: String, onFinished: () -> Unit) {
        stop()
        val nsUrl = NSURL.URLWithString(url) ?: return onFinished()
        val item = AVPlayerItem(uRL = nsUrl)
        this.onFinished = onFinished
        val center = NSNotificationCenter.defaultCenter
        observers = listOf(AVPlayerItemDidPlayToEndTimeNotification, AVPlayerItemFailedToPlayToEndTimeNotification).map { name ->
            center.addObserverForName(name, item, NSOperationQueue.mainQueue) { finish() }
        }
        player.replaceCurrentItemWithPlayerItem(item)
        player.play()
    }

    private fun finish() {
        val center = NSNotificationCenter.defaultCenter
        observers.forEach { center.removeObserver(it) }
        observers = emptyList()
        onFinished?.also { onFinished = null }?.invoke()
    }

    actual fun stop() {
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
        finish()
    }

    actual fun release() = stop()
}
