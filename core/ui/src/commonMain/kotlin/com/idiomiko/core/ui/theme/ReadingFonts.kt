package com.idiomiko.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.idiomiko.core.ui.i18n.tr
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource
import idiomiko.core.ui.generated.resources.Res
import idiomiko.core.ui.generated.resources.alegreya
import idiomiko.core.ui.generated.resources.atkinson_bold
import idiomiko.core.ui.generated.resources.atkinson_regular
import idiomiko.core.ui.generated.resources.eb_garamond
import idiomiko.core.ui.generated.resources.inter
import idiomiko.core.ui.generated.resources.jetbrains_mono
import idiomiko.core.ui.generated.resources.literata
import idiomiko.core.ui.generated.resources.lora
import idiomiko.core.ui.generated.resources.open_sans
import idiomiko.core.ui.generated.resources.pt_serif_bold
import idiomiko.core.ui.generated.resources.pt_serif_regular
import idiomiko.core.ui.generated.resources.source_sans
import idiomiko.core.ui.generated.resources.source_serif

/**
 * The typefaces the reader can use: the system's own families, and open-licence fonts bundled with
 * the app (licences in composeResources/files/licenses): serif, then sans-serif, then monospace. All cover
 * Latin; all but Atkinson Hyperlegible Cyrillic, and all but it and PT Serif Greek. Other scripts fall back
 * to a system font.
 */
enum class ReadingFont(val id: String, private val englishLabel: String, val bundled: Boolean) {
    SERIF("serif", "System serif", bundled = false),
    SANS("sans", "System sans-serif", bundled = false),
    ALEGREYA("alegreya", "Alegreya", bundled = true),
    EB_GARAMOND("eb_garamond", "EB Garamond", bundled = true),
    LITERATA("literata", "Literata", bundled = true),
    LORA("lora", "Lora", bundled = true),
    PT_SERIF("pt_serif", "PT Serif", bundled = true),
    SOURCE_SERIF("source_serif", "Source Serif 4", bundled = true),
    INTER("inter", "Inter", bundled = true),
    OPEN_SANS("open_sans", "Open Sans", bundled = true),
    SOURCE_SANS("source_sans", "Source Sans 3", bundled = true),
    ATKINSON("atkinson", "Atkinson Hyperlegible", bundled = true),
    MONOSPACE("mono", "Monospace", bundled = false),
    JETBRAINS_MONO("jetbrains_mono", "JetBrains Mono", bundled = true);

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
    ReadingFont.ALEGREYA -> bundled(Res.font.alegreya)
    ReadingFont.EB_GARAMOND -> bundled(Res.font.eb_garamond)
    ReadingFont.LITERATA -> bundled(Res.font.literata)
    ReadingFont.LORA -> bundled(Res.font.lora)
    ReadingFont.PT_SERIF -> FontFamily(Font(Res.font.pt_serif_regular, FontWeight.Normal), Font(Res.font.pt_serif_bold, FontWeight.Bold))
    ReadingFont.SOURCE_SERIF -> bundled(Res.font.source_serif)
    ReadingFont.INTER -> bundled(Res.font.inter)
    ReadingFont.OPEN_SANS -> bundled(Res.font.open_sans)
    ReadingFont.SOURCE_SANS -> bundled(Res.font.source_sans)
    ReadingFont.ATKINSON -> FontFamily(Font(Res.font.atkinson_regular, FontWeight.Normal), Font(Res.font.atkinson_bold, FontWeight.Bold))
    ReadingFont.JETBRAINS_MONO -> bundled(Res.font.jetbrains_mono)
}

/** A variable font file used at its regular and bold weights. */
@Composable
private fun bundled(font: FontResource): FontFamily = FontFamily(Font(font, FontWeight.Normal), Font(font, FontWeight.Bold))
