package com.tayra.languages.core.ui.components

import com.tayra.languages.core.domain.practice.PracticeMode
import com.tayra.languages.core.ui.i18n.tr

/** The mode's name, as offered to choose from. */
fun PracticeMode.label(): String = when (this) {
    PracticeMode.MIXED -> tr("Mixed")
    PracticeMode.CHOICE -> tr("Multiple choice")
    PracticeMode.TYPING -> tr("Typing")
    PracticeMode.LISTENING -> tr("Listening")
}

/** What the mode asks, in a few words. */
fun PracticeMode.description(): String = when (this) {
    PracticeMode.MIXED -> tr("Every kind of question in turn")
    PracticeMode.CHOICE -> tr("Pick the right answer from four")
    PracticeMode.TYPING -> tr("Type the word or the sentence")
    PracticeMode.LISTENING -> tr("Hear the word or the sentence first")
}
