package com.tayra.languages.core.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.tayra.languages.core.domain.morphology.Chinese
import com.tayra.languages.core.ui.theme.AppAccents

/** The colours of the four tones and the neutral one, Zhongwen's standard scheme. */
val ToneColors: List<Color> = AppAccents.tones

/** A Chinese reading with each syllable in its tone's colour; pinyin with tone marks, or zhuyin, whose syllables are spaced. */
fun toneColored(reading: String): AnnotatedString = buildAnnotatedString {
    val zhuyin = reading.any { it in 'ㄅ'..'ㄩ' }
    val parts = if (zhuyin) reading.split(' ').flatMapIndexed { i, s -> if (i == 0) listOf(s) else listOf(" ", s) }.map { it to zhuyinTone(it) }
    else Chinese.syllables(reading).map { it.text to it.tone }
    for ((text, tone) in parts) {
        if (tone in 1..5) withStyle(SpanStyle(color = ToneColors[tone - 1])) { append(text) } else append(text)
    }
}

private fun zhuyinTone(syllable: String): Int = when {
    syllable.isBlank() -> 0
    syllable.startsWith("˙") -> 5
    syllable.endsWith("ˊ") -> 2
    syllable.endsWith("ˇ") -> 3
    syllable.endsWith("ˋ") -> 4
    syllable.any { it in 'ㄅ'..'ㄩ' } -> 1
    else -> 0
}
