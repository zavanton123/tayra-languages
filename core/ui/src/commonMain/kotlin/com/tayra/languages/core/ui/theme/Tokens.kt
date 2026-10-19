package com.tayra.languages.core.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The accents besides the theme's colour roles: icons in tinted tiles, chart series, the tiles of
 * Home and Statistics. They are the same in every theme and are drawn on a light wash of
 * themselves (`tint.copy(alpha = …)`), which reads on light and dark surfaces alike.
 */
object AppAccents {
    val blue = Color(0xFF3B6FE0)
    val green = Color(0xFF2E9D57)
    val teal = Color(0xFF1F8A8A)
    val purple = Color(0xFF7C4DDB)
    val orange = Color(0xFFEA7A1B)
    val pink = Color(0xFFD9488B)
    val red = Color(0xFFD64545)
    val amber = Color(0xFFD68A00)
    val brown = Color(0xFF8C564B)
    val gray = Color(0xFF8A8F98)

    /** Accents that tell the items of a list apart, in this order: language codes, courses, chart series. */
    val sequence: List<Color> = listOf(blue, green, purple, red, amber, teal, pink, orange, brown)

    /** The soft colours of the books' drawn covers. */
    val covers: List<Color> = listOf(
        Color(0xFF5B8DEF), Color(0xFF34B5A0), Color(0xFF9B7BEA), Color(0xFFF0A13A), Color(0xFFE2738F),
        Color(0xFF6CA85A), Color(0xFF4A90E2), Color(0xFF4CC38A),
    )

    /** A mark on a solid accent or status colour, such as a tick on a filled box. */
    val onAccent = Color.White

    /** Text on the learning statuses' pale tints, which are the same in every theme. */
    val ink = Color(0xFF1B1F24)

    /** The four tones of Chinese and the neutral one, in Zhongwen's standard scheme, which learners know. */
    val tones: List<Color> = listOf(Color(0xFFEE363E), Color(0xFFF47C36), Color(0xFF73BB4F), Color(0xFF649CD3), Color(0xFFA0A0A0))

    /** The marker of a hidden word and the text on it, as in a cloze card. */
    val highlight = Color(0xFFFFD166)
    val highlightSoft = Color(0xFFFFE9A8)
    val onHighlight = Color(0xFF7A6A3A)
}

/** The colours of a flashcard's answers, as Anki has them. */
object RatingColors {
    val again = AppAccents.red
    val hard = AppAccents.amber
    val good = AppAccents.green
    val easy = AppAccents.blue
}

/** The design system's borders. */
object AppBorders {
    /** The 1dp line around cards, fields and panels, in the theme's outline colour unless given another. */
    @Composable
    fun hairline(color: Color = MaterialTheme.colorScheme.outlineVariant): BorderStroke = BorderStroke(1.dp, color)

    /** The 1.5dp ring of something chosen or active: a selected option, a pressed toolbar button, a drop target. */
    @Composable
    fun selected(color: Color = MaterialTheme.colorScheme.primary): BorderStroke = BorderStroke(1.5.dp, color)
}

/**
 * How strongly a colour shows through, for tints (`color.copy(alpha = AppAlpha.wash)`), from the
 * faintest wash behind a row to text that is dimmed but readable.
 */
object AppAlpha {
    /** Barely there: the hover or stripe of a row. */
    const val faint = 0.04f

    /** The wash behind a tinted card, banner or chosen row. */
    const val wash = 0.06f

    /** A light tint: a badge's or an icon tile's background. */
    const val tint = 0.1f

    /** A clearer tint: the chosen segment, a pill's background. */
    const val soft = 0.14f

    /** A tinted outline or a faint line. */
    const val line = 0.25f

    /** A disabled control, or a mark that should recede. */
    const val disabled = 0.4f

    /** Half: a muted outline or a dimmed track. */
    const val half = 0.5f

    /** Secondary text or an icon on a tint. */
    const val muted = 0.65f

    /** Nearly solid: a fill that lets a little of what is behind it through. */
    const val strong = 0.85f
}

/** Sizes shared by controls and icons. One-off dimensions of a screen are named constants in its file. */
object AppSizes {
    /** The height of a control that stands on its own: search fields, toggles, dropdowns, filter buttons. */
    val control = 44.dp

    /** The least a thing that is tapped should measure. */
    val touch = 48.dp

    /** The least height of a row in a list or a card. */
    val row = 56.dp

    /** Icons, smallest first: inside small chips, in rows and buttons, beside titles, and as a tile's or notice's picture. */
    val iconTiny = 14.dp
    val iconSmall = 16.dp
    val iconCompact = 18.dp
    val icon = 20.dp
    val iconMedium = 22.dp
    val iconLarge = 24.dp
    val iconXLarge = 32.dp
    val iconHuge = 40.dp

    /** The coloured dot of a status or a legend, and the tick of a list. */
    val dotSmall = 6.dp
    val dot = 8.dp
    val dotLarge = 10.dp
    val dotXLarge = 12.dp

    /** The height of a bar that shows an amount: a progress track, a distribution, a chart's bar. */
    val trackThin = 4.dp
    val track = 6.dp
    val trackThick = 8.dp
    val trackBold = 12.dp

    /** Square tiles and avatars: a language code, an icon tile, a cover's thumbnail. */
    val tileSmall = 32.dp
    val tile = 40.dp
    val tileMedium = 44.dp
    val tileLarge = 48.dp
    val tileXLarge = 56.dp

    /** The widths of dialogs and panels. */
    val dialogNarrow = 440.dp
    val dialog = 560.dp
    val dialogWide = 760.dp
    val sidePanel = 420.dp

    /** How wide a page's text column and a page's content may grow on a wide window. */
    val readableWidth = 760.dp
    val contentWidth = 1100.dp
    val pageWidth = 1480.dp
}

/**
 * The design system's spacing scale, on a 4dp grid: the padding inside things and the gaps between
 * them. Screens take their spacing from here.
 */
object AppSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
    val huge = 40.dp
    val giant = 48.dp
}

/** How far something is raised above the page, by its shadow. */
object AppElevation {
    /** A thumb or a small control that sits on a track. */
    val low = 2.dp

    /** A tooltip. */
    val medium = 4.dp

    /** A menu, a toast or a floating button. */
    val raised = 6.dp

    /** A popup card over the page. */
    val high = 8.dp
}

/**
 * The design system's text roles: the theme's typography with the weight each role has. Screens
 * use these rather than setting a weight of their own; a weight that changes with state (a chosen
 * tab) may still be set beside them.
 */
object AppText {
    /** A number that is the point of a card, such as an estimate of words known. */
    val figure: TextStyle @Composable get() = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold)

    /** A page's title. */
    val pageTitle: TextStyle @Composable get() = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold)

    /** A screen's title where a smaller one fits, and the title of a large dialog. */
    val screenTitle: TextStyle @Composable get() = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)

    /** A heading within a page, a dialog's title, a large number in a tile. */
    val headline: TextStyle @Composable get() = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)

    /** A card's or a section's title. */
    val sectionTitle: TextStyle @Composable get() = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)

    /** An item's title: a row's, a book's, a notice's. */
    val itemTitle: TextStyle @Composable get() = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)

    /** A small title, as of a group within a card. */
    val smallTitle: TextStyle @Composable get() = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)

    /** Body text that stands out: a chosen value, a link, a button's label in a row. */
    val emphasis: TextStyle @Composable get() = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)

    /** A smaller label that stands out: a field's name, a chip's label, a count. */
    val label: TextStyle @Composable get() = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)

    /** A smaller label that stands out more, such as a total. */
    val labelStrong: TextStyle @Composable get() = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)

    /** The text of a badge, a tag or a small caption that stands out. */
    val badge: TextStyle @Composable get() = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)

    /** Small capitals above content: a card's state ("NEW"), an exercise's instruction. */
    val overline = TextStyle(fontSize = 12.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)

    /** The word on a flashcard. */
    fun cardWord(compact: Boolean) = TextStyle(fontSize = if (compact) 24.sp else 30.sp, fontWeight = FontWeight.Medium)

    /** The sentence on a flashcard or in an exercise, at [size] sp. */
    fun cardSentence(size: Float) = TextStyle(fontSize = size.sp, lineHeight = (size * 1.45f).sp, fontWeight = FontWeight.Medium)

    /** The translation under a card's sentence. */
    fun cardTranslation(compact: Boolean) = TextStyle(fontSize = if (compact) 15.sp else 18.sp, lineHeight = 26.sp)

    /** The translation on a flashcard's back. */
    fun cardBack(compact: Boolean) = TextStyle(fontSize = if (compact) 18.sp else 22.sp)

    /** A word in a list of cards, at [size] sp (16 in a table, 19 on a phone, 24 in a detail). */
    fun listWord(size: Int, weight: FontWeight = FontWeight.SemiBold) = TextStyle(fontSize = size.sp, fontWeight = weight)
}
