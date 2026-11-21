package com.idiomiko.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idiomiko.core.ui.theme.AppAccents
import com.idiomiko.core.ui.theme.AppText

/** A drawn cover's height as a multiple of its width. */
const val BOOK_COVER_RATIO = 1.38f

/** From this wide a drawn cover names the language too. */
val LARGE_BOOK_COVER = 64.dp

/**
 * The colours of the drawn cover of the text with [title], from the top left to the bottom right: picked from the
 * title, so a text keeps its look wherever it shows and texts side by side differ.
 */
fun coverGradient(title: String): Pair<Color, Color> {
    val gradients = AppAccents.coverGradients
    return gradients[stableHash(title).mod(gradients.size)]
}

/**
 * A drawn cover for the book with [title]; a large one names the language by its [code] at the top, or leaves the top
 * clear without it. [typeShare] is the title's size as a share of the width, smaller where badges share the cover.
 * Without [titled] it is only the gradient and the spine, for a thumbnail beside the title.
 */
@Composable
fun BookCover(title: String, width: Dp, code: String? = null, typeShare: Float? = null, titled: Boolean = true) {
    val (start, end) = coverGradient(title)
    val large = width >= LARGE_BOOK_COVER
    val shape = RoundedCornerShape(topStart = width * 0.08f, bottomStart = width * 0.08f, topEnd = width * 0.16f, bottomEnd = width * 0.16f)
    // A picture: the title beside it is what is read out.
    Box(
        Modifier.size(width = width, height = width * BOOK_COVER_RATIO).clearAndSetSemantics {}.clip(shape).drawBehind {
            // Mostly downward and a little to the right, as the light falls on a book standing on a shelf.
            drawRect(Brush.linearGradient(listOf(start, end), start = Offset.Zero, end = Offset(size.width * 0.36f, size.height)))
            // The spine.
            drawRect(AppAccents.ink.copy(alpha = SPINE_SHADE), size = Size(size.width * 0.1f, size.height))
        },
    ) {
        if (!titled) return@Box
        val shown = title.inLearnedScript()
        // The type shrinks until the title's longest word fits a line, so no word is cut in two.
        val room = width.value * (1 - TYPE_START - TYPE_END)
        val longest = shown.split(' ').maxOfOrNull { it.length }?.coerceAtLeast(1) ?: 1
        val typeSize = minOf(width.value * (typeShare ?: if (large) 0.15f else 0.18f), room / (longest * GLYPH_WIDTH))
        val type = AppText.sectionTitle.copy(
            color = AppAccents.onAccent,
            fontSize = typeSize.sp,
            lineHeight = (typeSize * 1.15f).sp,
            shadow = Shadow(AppAccents.ink.copy(alpha = TYPE_SHADOW), Offset(0f, 1f), 2f),
        )
        val inset = Modifier.fillMaxSize().padding(start = width * TYPE_START, end = width * TYPE_END, top = width * 0.12f, bottom = width * 0.12f)
        if (large) {
            Column(inset, verticalArrangement = if (code != null) Arrangement.SpaceBetween else Arrangement.Bottom) {
                if (code != null) Text(code, style = AppText.smallTitle.copy(fontSize = (width.value * 0.15f).sp, color = AppAccents.onAccent.copy(alpha = CODE_ALPHA)), maxLines = 1)
                Text(shown, style = type, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        } else {
            Text(shown, inset, style = type, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A string's hash computed the same way on every platform, so a cover looks the same everywhere. */
private fun stableHash(text: String): Int = text.fold(0) { hash, c -> 31 * hash + c.code }

/** The cover's margins beside its type, as shares of its width: the spine's side is wider. */
private const val TYPE_START = 0.15f
private const val TYPE_END = 0.06f

/** About how wide a bold letter is, in ems. */
private const val GLYPH_WIDTH = 0.58f
private const val SPINE_SHADE = 0.14f
private const val TYPE_SHADOW = 0.15f
private const val CODE_ALPHA = 0.85f
