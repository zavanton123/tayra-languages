package com.idiomiko.core.domain.frequency

import com.idiomiko.core.domain.language.LanguageCodes
import com.idiomiko.core.domain.model.TermStatus
import com.idiomiko.core.domain.repository.LanguageRepository
import com.idiomiko.core.domain.repository.TermRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import com.idiomiko.core.domain.morphology.ChineseScript
import com.idiomiko.core.domain.morphology.ChineseScripts

/** A word of a frequency list, with the forms of it that were counted ("diz", "disse" for "dizer"). */
data class FrequencyWord(
    /** 1 for the most common word. */
    val rank: Int,
    /** As a dictionary writes it, so German nouns keep their capital ("Haus"). */
    val word: String,
    /** Lowercase. */
    val forms: List<String>,
) {
    /** The word as saved terms are matched: lowercase. */
    val key: String = word.lowercase()
}

/** A language's most common words, most common first, and where the counts came from. */
data class FrequencyList(val source: String, val words: List<FrequencyWord>) {
    /**
     * The list as a Chinese language read in [script] has it: its words shown in that script, and
     * their counted forms in it, as written, and in simplified characters, as saved words are matched;
     * unchanged without a script.
     */
    fun inScript(script: ChineseScript?): FrequencyList = if (script == null) this else copy(
        words = words.map { word ->
            val forms = word.forms.map { ChineseScripts.convert(it, script) } + word.forms + word.forms.map { ChineseScripts.convert(it, ChineseScript.SIMPLIFIED) }
            word.copy(word = ChineseScripts.convert(word.word, script), forms = forms.distinct())
        },
    )

    companion object {
        /**
         * Reads a list made by tools/build_frequency_list.py: `# source: ...` and other comment
         * lines, then one word per line with its lowercase forms after a tab, separated by spaces.
         */
        fun parse(text: String): FrequencyList {
            var source = ""
            val words = ArrayList<FrequencyWord>()
            text.lineSequence().forEach { line ->
                when {
                    line.startsWith("# source:") -> source = line.removePrefix("# source:").trim()
                    line.startsWith("#") || line.isBlank() -> Unit
                    else -> {
                        val word = line.substringBefore('\t').trim()
                        val forms = line.substringAfter('\t', "").split(' ').filter { it.isNotBlank() }
                        words += FrequencyWord(words.size + 1, word, forms)
                    }
                }
            }
            return FrequencyList(source, words)
        }
    }
}

/** The frequency lists the app has, by language. */
interface FrequencyLists {
    /** The list for the language with the ISO 639-1 [languageCode], or null when the app has none. */
    suspend fun list(languageCode: String): FrequencyList?

    /** The words the app has, besides its list's, for cutting the language's text into words (Lao's), or null. */
    suspend fun lexicon(languageCode: String): List<String>? = null
}

/**
 * The frequency lists fetched from the web, where the app does not bundle them (Android, to keep its download small):
 * one gzip file a language, `<code>.tsv.gz`, on the release [URL] names; tools/publish_frequency_lists.sh uploads them.
 */
fun interface FrequencyListDownloads {
    /** The list's file for the language with the ISO 639-1 [languageCode], kept after the first time; null when there is none to fetch. */
    suspend fun fetch(languageCode: String): ByteArray?

    companion object {
        /** Raised, with a new release, when the lists change in a way the installed copies must not keep. */
        const val VERSION = 1
        const val URL = "https://github.com/zavanton123/tayra-languages/releases/download/frequency-v$VERSION"
    }
}

/** How far the reader has got with a word, as the frequency list groups it. */
enum class WordKnowledge { KNOWN, LEARNING, IGNORED, NEW }

data class RankedWord(val word: FrequencyWord, val status: TermStatus) {
    val knowledge: WordKnowledge
        get() = when {
            status == TermStatus.WELL_KNOWN -> WordKnowledge.KNOWN
            status.isLearning -> WordKnowledge.LEARNING
            status == TermStatus.IGNORED -> WordKnowledge.IGNORED
            else -> WordKnowledge.NEW
        }
}

/** A hundred words of the list, by rank. */
data class FrequencyBand(val index: Int, val words: List<RankedWord>) {
    val firstRank: Int get() = index * WordFrequencyOverview.BAND_SIZE + 1
    val lastRank: Int get() = firstRank + words.size - 1
    fun count(knowledge: WordKnowledge): Int = words.count { it.knowledge == knowledge }

    /** Known, or set aside as not worth learning. */
    val settled: Int get() = words.count { it.knowledge == WordKnowledge.KNOWN || it.knowledge == WordKnowledge.IGNORED }
}

/**
 * The words known, as [WordFrequencyService.observeKnownWords] counts them, against the frequency list:
 * the next [milestone] to reach, a round number of the list's most common words, and how many of those
 * are known. Without a list, [milestone] is null.
 */
data class VocabularyProgress(val known: Int, val milestone: Int?, val knownInMilestone: Int) {
    /** From 0 to 1: the share of the milestone's words known. */
    val share: Float get() = milestone?.takeIf { it > 0 }?.let { (knownInMilestone.toFloat() / it).coerceIn(0f, 1f) } ?: 0f

    companion object {
        /** The numbers of words to reach, as far as the list goes. */
        val MILESTONES = listOf(100, 250, 500, 1000, 2000, 3000, 5000, 7500, 10_000, 15_000, 20_000)

        /**
         * [known] words, and whether each word of the list is known, the most common first: the next
         * milestone is the first past [known] that the list reaches, or the whole list once it is passed.
         */
        fun of(known: Int, knownByRank: List<Boolean>): VocabularyProgress {
            if (knownByRank.isEmpty()) return VocabularyProgress(known, null, 0)
            val milestone = MILESTONES.firstOrNull { it > known && it <= knownByRank.size } ?: knownByRank.size
            return VocabularyProgress(known, milestone, knownByRank.take(milestone).count { it })
        }
    }
}

data class WordFrequencyOverview(val languageName: String, val source: String, val words: List<RankedWord>) {
    val bands: List<FrequencyBand> = words.chunked(BAND_SIZE).mapIndexed { index, chunk -> FrequencyBand(index, chunk) }

    fun count(knowledge: WordKnowledge): Int = words.count { it.knowledge == knowledge }

    /** The first band whose words are not yet mostly known: where the reader's vocabulary stands. */
    val levelBand: Int? = bands.indexOfFirst { it.settled < it.words.size * LEVEL_SHARE }.takeIf { it >= 0 }

    companion object {
        const val BAND_SIZE = 100
        const val LEVEL_SHARE = 0.9
    }
}

/** A language's frequency list with the reader's status of each word. */
class WordFrequencyService(
    private val lists: FrequencyLists,
    private val terms: TermRepository,
    private val languages: LanguageRepository,
) {
    /** The list of the language with [languageId], kept up to date as words are saved; null when the app has no list for it. */
    fun observe(languageId: Long): Flow<WordFrequencyOverview?> = flow {
        val language = languages.getById(languageId)
        val list = language?.let { LanguageCodes.codeFor(it.name) }?.let { lists.list(it) }?.inScript(language.chineseScript)
        if (language == null || list == null) {
            emit(null)
            return@flow
        }
        emitAll(
            terms.observeWordStatuses(languageId).map { statuses ->
                WordFrequencyOverview(language.name, list.source, list.words.map { RankedWord(it, statusOf(it, statuses)) })
            },
        )
    }

    /**
     * How many words the reader knows in the language with [languageId], counting each word once
     * whatever forms of it were saved: a known list word, through any of its forms ("casas" known
     * counts for "casa"), or a known saved word that the list has neither as a word nor as a form.
     * Phrases are not words. Without a list, every known saved word counts.
     */
    fun observeKnownWords(languageId: Long): Flow<Int> = observeVocabulary(languageId).map { it.known }

    /** The words known, as [observeKnownWords] counts them, and how far they go through the list's most common words. */
    fun observeVocabulary(languageId: Long): Flow<VocabularyProgress> = flow {
        val language = languages.getById(languageId)
        val list = language?.let { LanguageCodes.codeFor(it.name) }?.let { lists.list(it) }?.inScript(language.chineseScript)
        val listed = list?.words?.flatMapTo(HashSet()) { it.forms + it.key }.orEmpty()
        emitAll(
            terms.observeWordStatuses(languageId).map { statuses ->
                val known = statuses.filterValues { it == TermStatus.WELL_KNOWN }.keys
                val byRank = list?.words?.map { statusOf(it, statuses) == TermStatus.WELL_KNOWN }.orEmpty()
                VocabularyProgress.of(byRank.count { it } + known.count { it !in listed }, byRank)
            },
        )
    }

    companion object {
        /**
         * The word's own status, or else the furthest status among its forms the reader has saved,
         * since a word is usually saved as it was met in a text ("disse" known makes "dizer" known).
         */
        fun statusOf(word: FrequencyWord, statuses: Map<String, TermStatus>): TermStatus {
            statuses[word.key]?.takeIf { it != TermStatus.UNKNOWN }?.let { return it }
            return word.forms.mapNotNull { statuses[it] }.filter { it != TermStatus.UNKNOWN }.maxByOrNull { if (it == TermStatus.IGNORED) 0 else it.value }
                ?: TermStatus.UNKNOWN
        }
    }
}
