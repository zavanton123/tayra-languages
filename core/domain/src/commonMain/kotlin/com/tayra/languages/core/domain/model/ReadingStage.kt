package com.tayra.languages.core.domain.model

/** The stages a page is worked through, in order; each is done when the reader checks it. */
enum class ReadingStage {
    /** Read the text aloud, marking the words as known or being learned. */
    READ_ALOUD,

    /** Listen to the text, the original blurred. */
    LISTEN,

    /** Look the text's words over again and correct their statuses. */
    WORDS,

    /** Answer the practice questions on the text's words. */
    QUIZ,

    /** Review the flashcards of the text's words. */
    FLASHCARDS,

    /** Turn the translation back into the original, which is blurred until looked at. */
    REVERSE,

    /** Tell the text again in your own words, with the text hidden. */
    RETELL,
    ;

    val bit: Int get() = 1 shl ordinal

    companion object {
        /** Every stage's bit: a page with all of them done. */
        val ALL_BITS: Int = (1 shl entries.size) - 1

        fun fromBits(bits: Int): Set<ReadingStage> = entries.filterTo(LinkedHashSet()) { bits and it.bit != 0 }

        fun bitsOf(stages: Set<ReadingStage>): Int = stages.fold(0) { bits, stage -> bits or stage.bit }
    }
}
