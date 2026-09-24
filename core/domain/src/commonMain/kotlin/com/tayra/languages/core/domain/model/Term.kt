package com.tayra.languages.core.domain.model

/**
 * Learning status of a term. The numeric values match the classic LWT/Lute statuses.
 */
enum class TermStatus(val value: Int, val label: String, val abbreviation: String) {
    UNKNOWN(0, "Unknown", "?"),
    NEW_1(1, "New (1)", "1"),
    NEW_2(2, "New (2)", "2"),
    LEARNING_3(3, "Learning (3)", "3"),
    LEARNING_4(4, "Learning (4)", "4"),
    LEARNED(5, "Learned", "5"),
    IGNORED(98, "Ignored", "I"),
    WELL_KNOWN(99, "Well Known", "W");

    val isLearning: Boolean get() = value in 1..5

    companion object {
        /** Statuses in the order used for "bump status up/down". */
        val progression: List<TermStatus> = listOf(UNKNOWN, NEW_1, NEW_2, LEARNING_3, LEARNING_4, LEARNED, WELL_KNOWN)

        /** Statuses a user may pick in a term form. */
        val selectable: List<TermStatus> = listOf(NEW_1, NEW_2, LEARNING_3, LEARNING_4, LEARNED, WELL_KNOWN, IGNORED)

        fun fromValue(value: Int): TermStatus =
            entries.firstOrNull { it.value == value } ?: throw IllegalArgumentException("Unknown status $value")

        fun fromValueOrNull(value: Int): TermStatus? = entries.firstOrNull { it.value == value }

        fun shifted(status: TermStatus, delta: Int): TermStatus {
            val index = progression.indexOf(status)
            if (index < 0) return status
            return progression[(index + delta).coerceIn(0, progression.lastIndex)]
        }
    }
}

/** Zero-width space, used to join tokens inside multi-word terms and sentences. */
const val ZWS: Char = '\u200B'
const val ZWS_STRING: String = "\u200B"

/**
 * A saved term (a word or multi-word expression) in a language.
 *
 * [text] is the tokenised term text: tokens are joined by [ZWS].
 */
data class Term(
    val id: Long = 0,
    val languageId: Long,
    val text: String,
    val textLc: String,
    val status: TermStatus = TermStatus.NEW_1,
    val translation: String? = null,
    val romanization: String? = null,
    val tokenCount: Int = 1,
    val syncStatus: Boolean = false,
    val flashMessage: String? = null,
    val parents: List<TermRef> = emptyList(),
) {
    val displayText: String get() = text.replace(ZWS_STRING, "")
    val isMultiword: Boolean get() = tokenCount > 1
    val isSaved: Boolean get() = id != 0L
}

/** A lightweight reference to another term. */
data class TermRef(
    val id: Long,
    val text: String,
    val status: TermStatus,
    val translation: String? = null,
) {
    val displayText: String get() = text.replace(ZWS_STRING, "")
}

/**
 * Editable term data as shown in the term form. All values are plain strings.
 */
data class TermDraft(
    val id: Long? = null,
    val languageId: Long,
    val text: String,
    val originalText: String = text,
    val translation: String = "",
    val romanization: String = "",
    val status: TermStatus = TermStatus.NEW_1,
    val statusExplicitlySet: Boolean = false,
    val syncStatus: Boolean = false,
    val parents: List<String> = emptyList(),
) {
    val isNew: Boolean get() = id == null

    /** Syncing status with a parent only makes sense with exactly one parent. */
    val effectiveSyncStatus: Boolean get() = syncStatus && parents.size == 1
}

/** A term found by an autocomplete search. */
data class TermMatch(
    val id: Long,
    val text: String,
    val translation: String?,
    val status: TermStatus,
    val hasChildren: Boolean,
)

/** Where a term (or a relative) appears in read texts. */
data class TermReference(
    val bookId: Long,
    val pageId: Long,
    val pageNumber: Int,
    val bookTitle: String,
    val pageCount: Int,
    val sentence: String,
) {
    val title: String get() = "$bookTitle ($pageNumber/$pageCount)"
}

data class TermReferences(
    val term: List<TermReference>,
    val children: List<TermReference>,
    val parents: List<Pair<String, List<TermReference>>>,
) {
    val isEmpty: Boolean get() = term.isEmpty() && children.isEmpty() && parents.all { it.second.isEmpty() }
}
