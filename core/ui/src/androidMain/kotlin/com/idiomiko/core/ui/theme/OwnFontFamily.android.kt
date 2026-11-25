package com.idiomiko.core.ui.theme

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import java.io.File

@Composable
internal actual fun rememberFontFamily(key: String, bytes: ByteArray): FontFamily? {
    val context = LocalContext.current
    return remember(key, bytes) {
        // Android reads fonts from files: the bytes go to the cache, and a file it cannot read falls back.
        val file = File(context.cacheDir, "fonts/$key.ttf")
        runCatching {
            file.parentFile?.mkdirs()
            if (!file.exists() || file.length() != bytes.size.toLong()) file.writeBytes(bytes)
            Typeface.createFromFile(file)
            FontFamily(Font(file))
        }.getOrNull()
    }
}
