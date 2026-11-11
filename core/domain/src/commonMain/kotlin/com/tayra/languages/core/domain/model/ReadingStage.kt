package com.tayra.languages.core.domain.model

/**
 * The stages a page is worked through, in order; each is done when the reader checks it. A page
 * stores the stages done as [bit]s, which stay the same whatever the order: a stage added later
 * takes a new bit, wherever it is worked.
 */
enum class ReadingStage(val bit: Int) {
    /** Read the text aloud, marking the words as known or being learned. */
    READ_ALOUD(1),

    /** Listen to the text, the original blurred. */
    LISTEN(2),

    /** Answer the lesson's questions on the story, the story hidden and each answer blurred. Only lessons with questions have it. */
    QUESTIONS(128),

    /** Look the text's words over again and correct their statuses. */
    WORDS(4),

    /** Answer the practice questions on the text's words. */
    QUIZ(8),

    /** Review the flashcards of the text's words. */
    FLASHCARDS(16),

    /** Turn the translation back into the original, which is blurred until looked at. */
    REVERSE(32),

    /** Tell the text again in your own words, with the text hidden. */
    RETELL(64),
    ;

    companion object {
        /** The stages of a page, in order: every one, but the questions only on a page that has them. */
        fun of(hasQuestions: Boolean): List<ReadingStage> = entries.filter { hasQuestions || it != QUESTIONS }

        /** The bits of every stage of a page: one with all of them done. */
        fun allBits(hasQuestions: Boolean): Int = bitsOf(of(hasQuestions).toSet())

        fun fromBits(bits: Int): Set<ReadingStage> = entries.filterTo(LinkedHashSet()) { bits and it.bit != 0 }

        fun bitsOf(stages: Set<ReadingStage>): Int = stages.fold(0) { bits, stage -> bits or stage.bit }
    }
}
