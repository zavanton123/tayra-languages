package com.idiomiko.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.idiomiko.core.ui.i18n.tr
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource
import idiomiko.core.ui.generated.resources.Res
import idiomiko.core.ui.generated.resources.alegreya
import idiomiko.core.ui.generated.resources.andika_bold
import idiomiko.core.ui.generated.resources.andika_regular
import idiomiko.core.ui.generated.resources.atkinson_bold
import idiomiko.core.ui.generated.resources.atkinson_regular
import idiomiko.core.ui.generated.resources.cormorant_garamond
import idiomiko.core.ui.generated.resources.crimson_pro
import idiomiko.core.ui.generated.resources.eb_garamond
import idiomiko.core.ui.generated.resources.fira_sans_bold
import idiomiko.core.ui.generated.resources.fira_sans_regular
import idiomiko.core.ui.generated.resources.gentium_bold
import idiomiko.core.ui.generated.resources.gentium_regular
import idiomiko.core.ui.generated.resources.inter
import idiomiko.core.ui.generated.resources.jetbrains_mono
import idiomiko.core.ui.generated.resources.lexend
import idiomiko.core.ui.generated.resources.libre_baskerville
import idiomiko.core.ui.generated.resources.literata
import idiomiko.core.ui.generated.resources.lora
import idiomiko.core.ui.generated.resources.manrope
import idiomiko.core.ui.generated.resources.noto_sans
import idiomiko.core.ui.generated.resources.noto_serif
import idiomiko.core.ui.generated.resources.nunito
import idiomiko.core.ui.generated.resources.open_sans
import idiomiko.core.ui.generated.resources.plex_sans
import idiomiko.core.ui.generated.resources.plex_serif_bold
import idiomiko.core.ui.generated.resources.plex_serif_regular
import idiomiko.core.ui.generated.resources.pt_sans_bold
import idiomiko.core.ui.generated.resources.pt_sans_regular
import idiomiko.core.ui.generated.resources.pt_serif_bold
import idiomiko.core.ui.generated.resources.pt_serif_regular
import idiomiko.core.ui.generated.resources.source_sans
import idiomiko.core.ui.generated.resources.source_serif
import idiomiko.core.ui.generated.resources.spectral_bold
import idiomiko.core.ui.generated.resources.spectral_regular
import idiomiko.core.ui.generated.resources.vollkorn

/**
 * The typefaces the reader can use: the system's own families, and open-licence fonts bundled with
 * the app (licences in composeResources/files/licenses), serif, then sans-serif, then monospace. Each
 * covers Latin and most Cyrillic and Greek too; other scripts fall back to a system font.
 */
enum class ReadingFont(val id: String, private val englishLabel: String, val bundled: Boolean) {
    SERIF("serif", "System serif", bundled = false),
    ALEGREYA("alegreya", "Alegreya", bundled = true),
    CORMORANT_GARAMOND("cormorant_garamond", "Cormorant Garamond", bundled = true),
    CRIMSON_PRO("crimson_pro", "Crimson Pro", bundled = true),
    EB_GARAMOND("eb_garamond", "EB Garamond", bundled = true),
    GENTIUM("gentium", "Gentium Book Plus", bundled = true),
    PLEX_SERIF("plex_serif", "IBM Plex Serif", bundled = true),
    LIBRE_BASKERVILLE("libre_baskerville", "Libre Baskerville", bundled = true),
    LITERATA("literata", "Literata", bundled = true),
    LORA("lora", "Lora", bundled = true),
    NOTO_SERIF("noto_serif", "Noto Serif", bundled = true),
    PT_SERIF("pt_serif", "PT Serif", bundled = true),
    SOURCE_SERIF("source_serif", "Source Serif 4", bundled = true),
    SPECTRAL("spectral", "Spectral", bundled = true),
    VOLLKORN("vollkorn", "Vollkorn", bundled = true),
    SANS("sans", "System sans-serif", bundled = false),
    ANDIKA("andika", "Andika", bundled = true),
    ATKINSON("atkinson", "Atkinson Hyperlegible", bundled = true),
    FIRA_SANS("fira_sans", "Fira Sans", bundled = true),
    PLEX_SANS("plex_sans", "IBM Plex Sans", bundled = true),
    INTER("inter", "Inter", bundled = true),
    LEXEND("lexend", "Lexend", bundled = true),
    MANROPE("manrope", "Manrope", bundled = true),
    NOTO_SANS("noto_sans", "Noto Sans", bundled = true),
    NUNITO("nunito", "Nunito", bundled = true),
    OPEN_SANS("open_sans", "Open Sans", bundled = true),
    PT_SANS("pt_sans", "PT Sans", bundled = true),
    SOURCE_SANS("source_sans", "Source Sans 3", bundled = true),
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
    ReadingFont.CORMORANT_GARAMOND -> bundled(Res.font.cormorant_garamond)
    ReadingFont.CRIMSON_PRO -> bundled(Res.font.crimson_pro)
    ReadingFont.EB_GARAMOND -> bundled(Res.font.eb_garamond)
    ReadingFont.GENTIUM -> FontFamily(Font(Res.font.gentium_regular, FontWeight.Normal), Font(Res.font.gentium_bold, FontWeight.Bold))
    ReadingFont.PLEX_SERIF -> FontFamily(Font(Res.font.plex_serif_regular, FontWeight.Normal), Font(Res.font.plex_serif_bold, FontWeight.Bold))
    ReadingFont.LIBRE_BASKERVILLE -> bundled(Res.font.libre_baskerville)
    ReadingFont.LITERATA -> bundled(Res.font.literata)
    ReadingFont.LORA -> bundled(Res.font.lora)
    ReadingFont.NOTO_SERIF -> bundled(Res.font.noto_serif)
    ReadingFont.PT_SERIF -> FontFamily(Font(Res.font.pt_serif_regular, FontWeight.Normal), Font(Res.font.pt_serif_bold, FontWeight.Bold))
    ReadingFont.SOURCE_SERIF -> bundled(Res.font.source_serif)
    ReadingFont.SPECTRAL -> FontFamily(Font(Res.font.spectral_regular, FontWeight.Normal), Font(Res.font.spectral_bold, FontWeight.Bold))
    ReadingFont.VOLLKORN -> bundled(Res.font.vollkorn)
    ReadingFont.ANDIKA -> FontFamily(Font(Res.font.andika_regular, FontWeight.Normal), Font(Res.font.andika_bold, FontWeight.Bold))
    ReadingFont.ATKINSON -> FontFamily(Font(Res.font.atkinson_regular, FontWeight.Normal), Font(Res.font.atkinson_bold, FontWeight.Bold))
    ReadingFont.FIRA_SANS -> FontFamily(Font(Res.font.fira_sans_regular, FontWeight.Normal), Font(Res.font.fira_sans_bold, FontWeight.Bold))
    ReadingFont.PLEX_SANS -> bundled(Res.font.plex_sans)
    ReadingFont.INTER -> bundled(Res.font.inter)
    ReadingFont.LEXEND -> bundled(Res.font.lexend)
    ReadingFont.MANROPE -> bundled(Res.font.manrope)
    ReadingFont.NOTO_SANS -> bundled(Res.font.noto_sans)
    ReadingFont.NUNITO -> bundled(Res.font.nunito)
    ReadingFont.OPEN_SANS -> bundled(Res.font.open_sans)
    ReadingFont.PT_SANS -> FontFamily(Font(Res.font.pt_sans_regular, FontWeight.Normal), Font(Res.font.pt_sans_bold, FontWeight.Bold))
    ReadingFont.SOURCE_SANS -> bundled(Res.font.source_sans)
    ReadingFont.JETBRAINS_MONO -> bundled(Res.font.jetbrains_mono)
}

/** A variable font file used at its regular and bold weights. */
@Composable
private fun bundled(font: FontResource): FontFamily = FontFamily(Font(font, FontWeight.Normal), Font(font, FontWeight.Bold))
