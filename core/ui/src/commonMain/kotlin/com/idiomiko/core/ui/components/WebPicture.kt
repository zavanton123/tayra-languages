package com.idiomiko.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import com.idiomiko.core.domain.web.WebPictures
import com.idiomiko.core.ui.theme.AppMotion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap

/** Where pictures on the web are fetched from; null where none are (tests, previews), which leaves every picture to its [WebPicture] fallback. */
val LocalWebPictures = staticCompositionLocalOf<WebPictures?> { null }

/**
 * A picture from the web, cropped to fill the box, over [fallback], which shows while it loads and stays where it
 * cannot be fetched or read; there is never a broken image.
 */
@Composable
fun WebPicture(url: String?, modifier: Modifier = Modifier, contentDescription: String? = null, fallback: @Composable () -> Unit) {
    val source = LocalWebPictures.current
    val image by produceState(url?.let(PictureCache::cached), url, source) {
        if (url != null && source != null && value == null) value = PictureCache.load(url, source)
    }
    val shown by animateFloatAsState(if (image != null) 1f else 0f, AppMotion.spec())
    Box(modifier) {
        if (shown < 1f) Box(Modifier.matchParentSize()) { fallback() }
        image?.let { Image(it, contentDescription, Modifier.matchParentSize().alpha(shown).testTag("web-picture"), contentScale = ContentScale.Crop) }
    }
}

/** The pictures shown lately, so a card scrolled back to shows its picture at once; and the addresses that gave none, so they are not asked again. */
private object PictureCache {
    private const val KEPT = 80
    private val lock = Mutex()
    private val pictures = LinkedHashMap<String, ImageBitmap>()
    private val failed = HashSet<String>()

    fun cached(url: String): ImageBitmap? = pictures[url]

    suspend fun load(url: String, source: WebPictures): ImageBitmap? {
        lock.withLock { pictures[url]?.let { return it }; if (url in failed) return null }
        val image = source.bytes(url)?.let { bytes -> withContext(Dispatchers.Default) { runCatching { bytes.decodeToImageBitmap() }.getOrNull() } }
        lock.withLock {
            if (image == null) failed += url
            else {
                pictures[url] = image
                while (pictures.size > KEPT) pictures.remove(pictures.keys.first())
            }
        }
        return image
    }
}
