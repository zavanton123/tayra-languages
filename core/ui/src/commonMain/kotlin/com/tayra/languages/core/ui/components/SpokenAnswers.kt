package com.tayra.languages.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import com.tayra.languages.core.domain.practice.PracticeChecker
import com.tayra.languages.core.domain.practice.SpeechRetry
import com.tayra.languages.core.domain.practice.WordResult
import com.tayra.languages.core.ui.i18n.tr
import com.tayra.languages.core.ui.theme.AppAccents
import com.tayra.languages.core.ui.theme.AppSpacing
import com.tayra.languages.core.ui.theme.AppText

/**
 * An answer said aloud: a button to start and stop hearing it, what was [heard] or why it is to be
 * said again, and the way to type it instead. Its parts are tagged "[tag]-speak", "[tag]-heard"
 * and "[tag]-type-instead".
 */
@Composable
fun SpeakAnswer(
    listening: Boolean,
    recognising: Boolean,
    heard: String,
    retry: SpeechRetry?,
    answered: Boolean,
    onListen: () -> Unit,
    onStop: () -> Unit,
    onTypeInstead: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    rightToLeft: Boolean = false,
    buttonModifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.m)) {
        if (!answered && !recognising) {
            PrimaryButton(
                if (listening) tr("Stop") else tr("Speak"),
                onClick = if (listening) onStop else onListen,
                icon = AppIcons.Mic,
                size = ButtonSize.Large,
                modifier = buttonModifier.testTag("$tag-speak"),
            )
        }
        val note = when {
            recognising -> tr("Recognising…")
            listening -> tr("Listening…")
            heard.isNotBlank() -> tr("Heard: {0}", heard)
            retry == SpeechRetry.NOTHING -> tr("Nothing was heard. Try again.")
            retry == SpeechRetry.MUTED -> tr("The microphone gave no sound. Check it and try again.")
            retry == SpeechRetry.FAILED -> tr("That could not be made out. Try again.")
            else -> null
        }
        note?.let {
            Text(
                it,
                Modifier.testTag("$tag-heard"),
                style = MaterialTheme.typography.titleMedium.copy(textDirection = if (rightToLeft) TextDirection.Rtl else TextDirection.Ltr),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (!answered) QuietButton(tr("Type instead"), onClick = onTypeInstead, modifier = buttonModifier.testTag("$tag-type-instead"))
    }
}

/** The sentence [expected], word by word as it was typed or heard: right, right but for accents, wrong (with what was given), or missing. */
@Composable
fun SentenceCheck(expected: String, given: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val checks = remember(expected, given) { PracticeChecker.compareWords(expected, given) }
    Text(
        buildAnnotatedString {
            checks.forEachIndexed { index, check ->
                if (index > 0) append(" ")
                when (check.result) {
                    WordResult.RIGHT -> withStyle(SpanStyle(color = AppAccents.green)) { append(check.word) }
                    WordResult.ACCENT -> withStyle(SpanStyle(color = AppAccents.amber, fontWeight = AppText.labelStrong.fontWeight)) { append(check.word) }
                    WordResult.WRONG -> {
                        withStyle(SpanStyle(color = AppAccents.red, fontWeight = AppText.labelStrong.fontWeight)) { append(check.word) }
                        withStyle(SpanStyle(color = colors.onSurfaceVariant, textDecoration = TextDecoration.LineThrough)) { append(" ${check.typed}") }
                    }
                    WordResult.MISSING -> withStyle(SpanStyle(color = AppAccents.red, textDecoration = TextDecoration.Underline)) { append(check.word) }
                }
            }
        },
        modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
}
