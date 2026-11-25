package com.idiomiko.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import org.jetbrains.skia.Data
import org.jetbrains.skia.FontMgr

@Composable
internal actual fun rememberFontFamily(key: String, bytes: ByteArray): FontFamily? = remember(key, bytes) {
    // Skia is asked first, so a file it cannot read falls back instead of failing as the text is drawn.
    val readable = runCatching { FontMgr.default.makeFromData(Data.makeFromBytes(bytes)) != null }.getOrDefault(false)
    if (readable) FontFamily(Font(key, bytes)) else null
}
