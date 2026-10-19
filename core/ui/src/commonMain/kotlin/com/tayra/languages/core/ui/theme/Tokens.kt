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
}

/** Sizes shared by controls. */
object AppSizes {
    /** The height of a control that stands on its own: search fields, toggles, dropdowns, filter buttons. */
    val control = 44.dp
    val iconSmall = 16.dp
    val icon = 20.dp
    val iconLarge = 24.dp
}

/** The design system's spacing scale, used by its components. */
object AppSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val s = 8.dp
    val m = 12.dp
    val l = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

/** Text styles besides the theme's typography. */
object AppText {
    /** Small capitals above content: a card's state ("NEW"), an exercise's instruction. */
    val overline = TextStyle(fontSize = 12.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold)

    /** The word on a flashcard. */
    fun cardWord(compact: Boolean) = TextStyle(fontSize = if (compact) 24.sp else 30.sp, fontWeight = FontWeight.Medium)

    /** The sentence on a flashcard or in an exercise, at [size] sp. */
    fun cardSentence(size: Float) = TextStyle(fontSize = size.sp, lineHeight = (size * 1.45f).sp, fontWeight = FontWeight.Medium)

    /** The translation under a card's sentence. */
    fun cardTranslation(compact: Boolean) = TextStyle(fontSize = if (compact) 15.sp else 18.sp, lineHeight = 26.sp)
}
