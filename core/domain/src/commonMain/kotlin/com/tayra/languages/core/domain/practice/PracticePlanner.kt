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

    /** Too few other forms of the word are known. */
    WORD_FORMS,

    /** The word's sentence is too short or too long to build from its words. */
    SENTENCE_LENGTH,

    /** Fewer than four of the words have meanings that cannot be taken for each other. */
    MORE_WORDS,
}

/** How many of the words a type of question can be asked of, and what the others lack. */
class TypeAvailability(val type: ExerciseType, val words: Int, val need: PracticeNeed?)

/** What a setup gives for the words: each type's availability, the number of questions, and how many words have sentences from elsewhere. */
class PracticePreview(val words: Int, val types: Map<ExerciseType, TypeAvailability>, val questions: Int, val wordsWithExamples: Int = 0) {
    fun of(type: ExerciseType): TypeAvailability = types.getValue(type)
}

/** Says what a practice will ask before it starts, by the rules [PracticeBuilder] asks by. */
object PracticePlanner {
    /**
     * Each type's availability for [words], ticked or not, and the questions [setup] gives with the
     * sentences from elsewhere kept so far. [meanings], [translations] and [examples] are as for
     * [PracticeBuilder.build].
     */
    fun preview(
        words: List<PracticeWord>,
        setup: PracticeSetup,
        meanings: List<String> = emptyList(),
        translations: Map<String, String> = emptyMap(),
        /** The sentences from elsewhere kept for the words, by the word's key. */
        examples: Map<String, List<PracticeExample>> = emptyMap(),
        /** The words' families, by the word's key. */
        families: Map<String, WordFamily> = emptyMap(),
    ): PracticePreview {
        val targets = words.distinctBy { it.text.lowercase() }
        val matched = PracticeBuilder.matching(targets, Random(0))?.pairs?.size ?: 0
        val types = ExerciseType.entries.associateWith { type ->
            val askable = if (type == ExerciseType.MATCHING) matched else targets.count { PracticeBuilder.canAsk(type, it, targets, meanings, translations, families) }
            TypeAvailability(type, askable, if (askable == targets.size) null else need(type, targets, translations, families))
        }
        val questions = PracticeBuilder.build(targets, emptyList(), examples, Random(0), setup, meanings, translations, families = families).size
        return PracticePreview(targets.size, types, questions, targets.count { examples[it.key].orEmpty().isNotEmpty() })
    }

    /** What most of the words that cannot be asked [type] lack. */
    private fun need(type: ExerciseType, words: List<PracticeWord>, translations: Map<String, String>, families: Map<String, WordFamily>): PracticeNeed? = when (type) {
        ExerciseType.MEANING -> if (words.any { it.translation.isNullOrBlank() }) PracticeNeed.WORD_MEANING else PracticeNeed.OTHER_MEANINGS
        // Up to five are matched at a time; fewer than four with meanings make no round.
        ExerciseType.MATCHING -> PracticeNeed.MORE_WORDS.takeIf { PracticeBuilder.matching(words, Random(0)) == null }
        ExerciseType.HEAR_SENTENCE, ExerciseType.TRUE_FALSE -> PracticeNeed.WORD_MEANING
        ExerciseType.TRANSLATION -> PracticeNeed.SENTENCE_TRANSLATION.takeIf { words.any { it.sentence !in translations } }
        ExerciseType.BUILD -> if (words.any { it.sentence !in translations }) PracticeNeed.SENTENCE_TRANSLATION else PracticeNeed.SENTENCE_LENGTH
        ExerciseType.SOUND_ALIKE -> PracticeNeed.WORD_FORMS
        ExerciseType.FORM -> if (words.any { (families[it.key]?.forms?.size ?: 0) < 4 }) PracticeNeed.WORD_FORMS else PracticeNeed.SENTENCE_TRANSLATION
        ExerciseType.GAP, ExerciseType.HEAR_WORD, ExerciseType.DICTATION -> null
    }
}

/** Ready-made setups the setup dialog offers; changing anything after picking one makes a custom setup. */
enum class PracticePreset(val setup: PracticeSetup) {
    /** Recognising the words: choices only. */
    QUICK_REVIEW(PracticeSetup(types = setOf(ExerciseType.GAP, ExerciseType.MEANING, ExerciseType.HEAR_WORD), responses = setOf(PracticeResponse.CHOOSE), adaptive = true)),

    /** Reading, listening and translation, chosen and typed. */
    BALANCED(PracticeSetup(types = ExerciseType.entries.toSet(), responses = PracticeResponse.entries.toSet(), adaptive = true)),

    /** Every listening question. */
    LISTENING(PracticeSetup(types = ExerciseType.entries.filter { it.skill == PracticeSkill.LISTENING }.toSet(), adaptive = true)),

    /** Recall: every question that can be typed, typed. */
    WRITING(PracticeSetup(types = ExerciseType.entries.filter { PracticeResponse.TYPE in it.responses }.toSet(), responses = setOf(PracticeResponse.TYPE), adaptive = true)),
    ;

    companion object {
        /** The setup the dialog opens on the first time. */
        val DEFAULT = BALANCED

        /** The preset [setup] asks what, if it is one: its length and adapting are the learner's own. */
        fun of(setup: PracticeSetup): PracticePreset? = entries.firstOrNull { it.setup.copy(length = setup.length, adaptive = setup.adaptive) == setup }
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
