package com.tayra.languages.feature.reading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tayra.languages.core.domain.language.LanguageDefinitions
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.parse.Lexicon
import com.tayra.languages.core.domain.parse.Lexicons
import com.tayra.languages.core.domain.parse.lowercase
import com.tayra.languages.core.domain.parse.parseTokens
import com.tayra.languages.core.domain.render.TextItemCalculator
import com.tayra.languages.core.ui.theme.AppThemes
import org.junit.After
import org.junit.Rule
import org.junit.Test
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The pieces an Arabic word is cut into (و + ال + كتاب) are drawn as the one word they are written as: each piece is
 * a span of its own, coloured by its status, and the letters must stay joined across the spans' edges.
 */
class ArabicShapingTest {

    @get:Rule
    val rule = createComposeRule()

    private val language = LanguageDefinitions.all.first { it.name == "Arabic" }.language

    @After
    fun forget() = Lexicons.set("ar", null)

    @Test
    fun aWordsPiecesStayJoined() {
        val ranks = mapOf("ال" to 1, "و" to 2, "ب" to 5, "لل" to 15, "كتاب" to 164, "بيت" to 198, "عالم" to 63)
        Lexicons.set("ar", Lexicon(ranks.keys, ranks))
        val text = "والكتاب للبيت بالعالم"
        val tokens = language.parseTokens(text)
        assertEquals(listOf("و", "ال", "كتاب", "لل", "بيت", "ب", "ال", "عالم"), tokens.filter { it.isWord }.map { it.token })
        fun term(word: String, status: TermStatus) = Term(id = word.hashCode().toLong(), languageId = 0, text = word, textLc = language.lowercase(word), status = status)
        val terms = listOf(term("كتاب", TermStatus.NEW_2), term("بيت", TermStatus.LEARNING_4), term("و", TermStatus.WELL_KNOWN), term("ال", TermStatus.WELL_KNOWN))
        val page = TextItemCalculator.toPage(TextItemCalculator.calculate(tokens, terms, language).items)
        // As the reader draws them: the pieces coloured each by its status, one of them selected and underlined.
        val selected = page.paragraphs.flatMap { it.sentences }.flatMap { it.items }.indexOfFirst { it.text == "بيت" }
        val style = TextStyle(fontSize = (18 * FONT_SCALE).sp, fontFamily = FontFamily.Serif, textDirection = TextDirection.Rtl, color = Color.Black)
        rule.setContent {
            Column(Modifier.background(Color.White).padding(8.dp).width(560.dp)) {
                ReadingText(
                    page = page,
                    theme = AppThemes.default,
                    showHighlights = true,
                    marked = setOf(selected),
                    hovered = null,
                    selection = null,
                    popupItem = null,
                    fontScale = FONT_SCALE,
                    lineHeight = 1.5f,
                    rightToLeft = true,
                    callbacks = ReadingTextCallbacks(
                        onClick = { _, _ -> },
                        onTap = {},
                        onLongPress = {},
                        onHover = {},
                        onDragStart = {},
                        onDrag = {},
                        onDragEnd = { _, _ -> },
                        popupContent = { _, _: androidx.compose.ui.unit.IntRect -> },
                    ),
                    modifier = Modifier.testTag("reader"),
                )
                Text(text, Modifier.testTag("joined").padding(vertical = 8.dp), style = style)
                // The same words with a zero-width non-joiner between their pieces, as they would look if the spans broke the shaping.
                Text("و‌ال‌كتاب لل‌بيت ب‌ال‌عالم", Modifier.testTag("apart").padding(vertical = 8.dp), style = style)
            }
        }
        System.getenv("ARABIC_SHAPING_SCREENSHOT")?.let { javax.imageio.ImageIO.write(rule.onRoot().captureToImage().toAwtImage(), "png", java.io.File(it)) }
        val reader = inkWidth(rule.onNodeWithTag("reader").captureToImage().toAwtImage())
        val joined = inkWidth(rule.onNodeWithTag("joined").captureToImage().toAwtImage())
        val apart = inkWidth(rule.onNodeWithTag("apart").captureToImage().toAwtImage())
        assertTrue(abs(reader - joined) <= 2, "the reader's words are as wide as the words written whole: $reader against $joined")
        // Where the fonts join the letters at all, pieces drawn apart are wider.
        if (apart != joined) assertTrue(abs(reader - joined) < abs(apart - joined), "the reader's words are not drawn apart: $reader, $joined, $apart")
    }

    /** How wide the dark letters of [image] reach, the status marks behind them, which are light, left out. */
    private fun inkWidth(image: BufferedImage): Int {
        var left = image.width
        var right = -1
        for (x in 0 until image.width) for (y in 0 until image.height) {
            val rgb = image.getRGB(x, y)
            val darkest = maxOf(rgb shr 16 and 0xFF, rgb shr 8 and 0xFF, rgb and 0xFF)
            if (darkest < INK) {
                left = minOf(left, x)
                right = maxOf(right, x)
            }
        }
        return if (right < 0) 0 else right - left + 1
    }

    private companion object {
        const val FONT_SCALE = 2f

        /** The brightest channel a letter's pixel has: the letters are black or the selection's orange, the marks pale. */
        const val INK = 200
    }
}
