package com.tayra.languages.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.tayra.languages.core.ui.i18n.tr
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource
import tayra_languages.core.ui.generated.resources.Res
import tayra_languages.core.ui.generated.resources.literata
import tayra_languages.core.ui.generated.resources.lora
import tayra_languages.core.ui.generated.resources.open_sans

/**
 * The typefaces the reader can use: the system's own families, and open-licence fonts bundled with
 * the app (licences in composeResources/files/licenses) that cover Latin, Cyrillic and Greek; other
 * scripts fall back to a system font.
 */
enum class ReadingFont(val id: String, private val englishLabel: String, val bundled: Boolean) {
    SERIF("serif", "System serif", bundled = false),
    SANS("sans", "System sans-serif", bundled = false),
    LITERATA("literata", "Literata", bundled = true),
    LORA("lora", "Lora", bundled = true),
    OPEN_SANS("open_sans", "Open Sans", bundled = true),
    MONOSPACE("mono", "Monospace", bundled = false);

    /** The name in the interface language; the bundled fonts keep their own names. */
    val label: String get() = tr(englishLabel)

    companion object {
        /** The fonts to offer: where there are no system fonts (the web), only the bundled ones. */
        val choices: List<ReadingFont> = if (systemFontsAvailable) entries else entries.filter { it.bundled }

        /** The font for a stored id; a system font stands in for its nearest bundled one where there are none. */
        fun byId(id: String): ReadingFont {
            val font = entries.firstOrNull { it.id == id } ?: SERIF
            return if (systemFontsAvailable || font.bundled) font else if (font == SANS) OPEN_SANS else LITERATA
        }
    }
}

/** Whether the platform has fonts of its own to draw text with; a browser canvas has none. */
internal expect val systemFontsAvailable: Boolean

@Composable
fun ReadingFont.fontFamily(): FontFamily = when (this) {
    ReadingFont.SERIF -> FontFamily.Serif
    ReadingFont.SANS -> FontFamily.SansSerif
    ReadingFont.MONOSPACE -> FontFamily.Monospace
    ReadingFont.LITERATA -> bundled(Res.font.literata)
    ReadingFont.LORA -> bundled(Res.font.lora)
    ReadingFont.OPEN_SANS -> bundled(Res.font.open_sans)
}

/** A variable font file used at its regular and bold weights. */
@Composable
private fun bundled(font: FontResource): FontFamily = FontFamily(Font(font, FontWeight.Normal), Font(font, FontWeight.Bold))
