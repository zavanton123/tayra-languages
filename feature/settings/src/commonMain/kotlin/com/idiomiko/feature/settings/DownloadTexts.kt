package com.idiomiko.feature.settings

import com.idiomiko.core.ui.components.formatSize
import com.idiomiko.core.ui.i18n.tr

internal fun sizeText(bytes: Long, estimated: Boolean): String = if (estimated) tr("about {0}", formatSize(bytes)) else formatSize(bytes)

/** Why a download failed, in words: a server's status code is no help to a reader. */
internal fun downloadFailure(message: String): String {
    val code = Regex("""HTTP (\d{3})""").find(message)?.groupValues?.get(1)?.toIntOrNull()
    return when {
        code == null -> tr("Download failed: {0}", message)
        code >= 500 -> tr("The download server is busy. Try again in a moment.")
        code == 404 -> tr("The file is no longer on the download server.")
        else -> tr("Download failed: {0}", message)
    }
}
