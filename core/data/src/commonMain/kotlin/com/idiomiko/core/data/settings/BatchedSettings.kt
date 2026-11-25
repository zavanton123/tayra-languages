package com.idiomiko.core.data.settings

import com.russhwolf.settings.Settings

/** A store that can take many changes as one, saving them once when a batch ends rather than after each. */
interface BatchedSettings : Settings {
    fun batch(block: () -> Unit)
}

/** Runs [block] as one change where the store allows it. */
internal fun Settings.inBatch(block: () -> Unit) = if (this is BatchedSettings) batch(block) else block()
