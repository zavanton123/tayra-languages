package com.tayra.languages.core.ui.audio

import platform.AVFoundation.AVPlayer
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.Foundation.NSURL

actual class AudioPlayer actual constructor() {
    private val player = AVPlayer()

    actual fun play(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        player.replaceCurrentItemWithPlayerItem(platform.AVFoundation.AVPlayerItem(uRL = nsUrl))
        player.play()
    }

    actual fun stop() {
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
    }

    actual fun release() = stop()
}
