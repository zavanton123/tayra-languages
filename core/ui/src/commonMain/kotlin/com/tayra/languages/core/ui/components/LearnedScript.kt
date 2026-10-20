package com.tayra.languages.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.tayra.languages.core.domain.morphology.ChineseScript
import com.tayra.languages.core.domain.morphology.ChineseScripts

/** The script the language being learned is read in: set for Chinese read in one, null otherwise. */
val LocalLearnedScript = staticCompositionLocalOf<ChineseScript?> { null }

/**
 * This text of the language being learned, such as a book's or a lesson's title, in the script the
 * language is read in, as its texts are shown; unchanged for every other language.
 */
@Composable
fun String.inLearnedScript(): String {
    val script = LocalLearnedScript.current ?: return this
    return remember(this, script) { ChineseScripts.convert(this, script) }
}
