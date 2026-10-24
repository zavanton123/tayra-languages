package com.tayra.languages.core.domain.practice

import kotlin.random.Random

/** What a word lacks for a type of question. */
enum class PracticeNeed {
    /** The word has no saved translation. */
    WORD_MEANING,

    /** Too few other saved words have a meaning to choose from. */
    OTHER_MEANINGS,

    /** The word's sentence has no translation. */
    SENTENCE_TRANSLATION,
}

/** How many of the words a type of question can be asked of, and what the others lack. */
class TypeAvailability(val type: ExerciseType, val words: Int, val need: PracticeNeed?)

/** What a setup gives for the words: each type's availability and the number of questions. */
class PracticePreview(val words: Int, val types: Map<ExerciseType, TypeAvailability>, val questions: Int) {
    fun of(type: ExerciseType): TypeAvailability = types.getValue(type)
}

/** Says what a practice will ask before it starts, by the rules [PracticeBuilder] asks by. */
object PracticePlanner {
    /**
     * Each type's availability for [words], ticked or not, and the questions [setup] gives, not
     * counting sentences from elsewhere, which are only known once fetched. [meanings] and
     * [translations] are as for [PracticeBuilder.build].
     */
    fun preview(
        words: List<PracticeWord>,
        setup: PracticeSetup,
        meanings: List<String> = emptyList(),
        translations: Map<String, String> = emptyMap(),
    ): PracticePreview {
        val targets = words.distinctBy { it.text.lowercase() }
        val types = ExerciseType.entries.associateWith { type ->
            val askable = targets.count { PracticeBuilder.canAsk(type, it, targets, meanings, translations) }
            TypeAvailability(type, askable, if (askable == targets.size) null else need(type, targets, translations))
        }
        val questions = PracticeBuilder.build(targets, emptyList(), random = Random(0), setup = setup.copy(contexts = setup.contexts + PracticeContext.ORIGINAL), meanings = meanings, translations = translations).size
        return PracticePreview(targets.size, types, if (PracticeContext.ORIGINAL in setup.contexts) questions else 0)
    }

    /** What most of the words that cannot be asked [type] lack. */
    private fun need(type: ExerciseType, words: List<PracticeWord>, translations: Map<String, String>): PracticeNeed? = when (type) {
        ExerciseType.MEANING -> if (words.any { it.translation.isNullOrBlank() }) PracticeNeed.WORD_MEANING else PracticeNeed.OTHER_MEANINGS
        ExerciseType.HEAR_SENTENCE -> PracticeNeed.WORD_MEANING
        ExerciseType.TRANSLATION -> PracticeNeed.SENTENCE_TRANSLATION.takeIf { words.any { it.sentence !in translations } }
        ExerciseType.GAP, ExerciseType.HEAR_WORD, ExerciseType.DICTATION -> null
    }
}

/** Ready-made setups the setup dialog offers; changing anything after picking one makes a custom setup. */
enum class PracticePreset(val setup: PracticeSetup) {
    /** Recognising the words: choices only. */
    QUICK_REVIEW(PracticeSetup(types = setOf(ExerciseType.GAP, ExerciseType.MEANING, ExerciseType.HEAR_WORD), responses = setOf(PracticeResponse.CHOOSE))),

    /** Reading, listening and translation, chosen and typed. */
    BALANCED(PracticeSetup(types = ExerciseType.entries.toSet())),

    /** Every listening question. */
    LISTENING(PracticeSetup(types = ExerciseType.entries.filter { it.skill == PracticeSkill.LISTENING }.toSet())),

    /** Recall: every question that can be typed, typed. */
    WRITING(PracticeSetup(types = ExerciseType.entries.filter { PracticeResponse.TYPE in it.responses }.toSet(), responses = setOf(PracticeResponse.TYPE))),
    ;

    companion object {
        /** The setup the dialog opens on the first time. */
        val DEFAULT = BALANCED

        /** The preset [setup] is, if it is one. */
        fun of(setup: PracticeSetup): PracticePreset? = entries.firstOrNull { it.setup == setup }
    }
}

/** A setup the learner saved under a [name], offered beside the ready-made presets. */
data class SavedPreset(val name: String, val setup: PracticeSetup) {
    companion object {
        private const val FIELD = '\u001F'
        private const val ENTRY = '\u001E'

        /** The presets as text for the settings, names kept as typed. */
        fun encode(presets: List<SavedPreset>): String = presets.joinToString(ENTRY.toString()) { it.name.replace(FIELD, ' ').replace(ENTRY, ' ') + FIELD + it.setup.encode() }

        /** The presets [encode] wrote; entries it cannot read are left out. */
        fun decode(text: String): List<SavedPreset> = text.split(ENTRY).mapNotNull { entry ->
            val parts = entry.split(FIELD, limit = 2)
            if (parts.size == 2 && parts[0].isNotBlank()) SavedPreset(parts[0], PracticeSetup.decode(parts[1])) else null
        }
    }
}
