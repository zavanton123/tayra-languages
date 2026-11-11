package com.tayra.languages.core.domain.bridge

import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.frequency.FrequencyLists
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.media.SubtitleFormat
import com.tayra.languages.core.domain.media.SubtitleFormats
import com.tayra.languages.core.domain.media.SubtitleKind
import com.tayra.languages.core.domain.media.SubtitleTrack
import com.tayra.languages.core.domain.media.VideoService
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.render.MultiwordTermIndex
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.domain.term.TermTextNormalizer
import com.tayra.languages.core.domain.settings.SettingsRepository

class BridgeException(message: String) : Exception(message)

/**
 * What the browser extension asks the app, answered with the reader's own services: the lines of
 * a video cut into words with their statuses, a word's dictionary entries, saving a word, and
 * translating lines. Always in the language being learned.
 */
class BrowserBridgeApi(
    private val settings: SettingsRepository,
    private val languages: LanguageRepository,
    private val reading: ReadingService,
    private val terms: TermService,
    private val dictionary: OfflineDictionary,
    private val translator: SentenceTranslator,
    private val version: String? = null,
    private val frequency: FrequencyLists? = null,
) {
    private var rankedFor: String? = null
    private var rankOf: Map<String, Int> = emptyMap()

    suspend fun status(): BridgeStatus = BridgeStatus(learning()?.let(::describe), native(), version)

    suspend fun transcript(request: TranscriptRequest): TranscriptResponse {
        val language = learning()
        val original = parse(request.original)
        if (original.isEmpty()) throw BridgeException("The subtitles have no lines")
        val kind = if (request.automatic) SubtitleKind.YOUTUBE_AUTO else SubtitleKind.YOUTUBE
        val cues = VideoService.prepare(SubtitleTrack(original, kind, request.trackLanguage), request.translation?.let(::parse))
        val code = language?.let { LanguageCodes.codeFor(it.name) }
        val matches = language != null && code != null && code.equals(request.trackLanguage.substringBefore('-'), ignoreCase = true)
        val index: MultiwordTermIndex? = if (matches) reading.multiwordIndex(language!!) else null
        val lines = cues.map { cue ->
            BridgeLine(
                beginMs = cue.beginMs,
                endMs = cue.endMs,
                text = cue.text,
                translation = cue.translation,
                segments = if (index != null) segments(cue.text, language!!, index) else emptyList(),
            )
        }
        val ranks = if (matches) ranks(code!!, lines) else emptyMap()
        return TranscriptResponse(matches, language?.let(::describe), lines, ranks)
    }

    /** The rank of each word of [lines] that the frequency list of [code] has, as a word or as one of its forms. */
    private suspend fun ranks(code: String, lines: List<BridgeLine>): Map<String, Int> {
        if (rankedFor != code) {
            val list = frequency?.let { runCatching { it.list(code) }.getOrNull() }
            val ranks = HashMap<String, Int>()
            list?.words?.forEach { word -> (word.forms + word.key).forEach { ranks.getOrPut(it) { word.rank } } }
            rankOf = ranks
            rankedFor = code
        }
        return lines.asSequence().flatMap { it.segments }.mapNotNull { it.lc }.distinct().mapNotNull { lc -> rankOf[lc]?.let { lc to it } }.toMap()
    }

    private suspend fun segments(text: String, language: Language, index: MultiwordTermIndex): List<BridgeSegment> =
        reading.renderItems(text, language, index)
            .filter { !it.isParagraphMark && it.renderText.isNotEmpty() }
            .map { item ->
                if (item.isWord) BridgeSegment(item.renderText, term = item.text, lc = item.textLc, status = item.status.value)
                else BridgeSegment(item.renderText)
            }

    suspend fun lookup(term: String): LookupResponse {
        val language = learning() ?: throw BridgeException("No language is being learned")
        if (term.isBlank()) throw BridgeException("Nothing to look up")
        val found = terms.findOrNew(language.id, term)
        val draft = if (found.id == null) found.copy(status = TermStatus.UNKNOWN) else found
        val code = LanguageCodes.codeFor(language.name)
        val entries = if (code == null) emptyList() else {
            val native = native()
            val pack = DictionaryPacks.find(code, native)?.id ?: DictionaryId(code, native)
            runCatching { dictionary.lookup(pack, draft.text) }.getOrNull()?.entries.orEmpty().take(MAX_ENTRIES).map { entry ->
                BridgeEntry(
                    word = entry.word,
                    pos = entry.pos,
                    ipa = entry.ipa,
                    reading = entry.reading,
                    senses = entry.senses.take(MAX_SENSES).map { it.glosses },
                    dictionary = entry.dictionary,
                )
            }
        }
        val lc = TermTextNormalizer.termFromText(language, term).textLc
        return LookupResponse(draft.text, lc, draft.status.value, draft.translation, draft.parents, entries)
    }

    suspend fun save(request: SaveTermRequest): SaveTermResponse {
        val language = learning() ?: throw BridgeException("No language is being learned")
        val status = TermStatus.entries.firstOrNull { it.value == request.status } ?: throw BridgeException("No such status")
        if (request.term.isBlank()) throw BridgeException("Nothing to save")
        val found = terms.findOrNew(language.id, request.term)
        val draft = found.copy(
            status = status,
            statusExplicitlySet = true,
            translation = request.translation ?: found.translation,
        )
        terms.save(draft, request.sentence?.takeIf { it.isNotBlank() })
        val saved = terms.find(language.id, request.term) ?: throw BridgeException("The word was not saved")
        return SaveTermResponse(saved.textLc, saved.status.value)
    }

    suspend fun translate(request: TranslateRequest): TranslateResponse {
        val language = learning() ?: throw BridgeException("No language is being learned")
        if (request.lines.size > MAX_TRANSLATED_LINES) throw BridgeException("Too many lines at once")
        return TranslateResponse(request.lines.map { line -> if (line.isBlank()) null else translator.translate(line, language) })
    }

    private suspend fun learning(): Language? = settings.current.currentLanguageId.takeIf { it != 0L }?.let { languages.getById(it) }

    private fun native(): String = settings.current.nativeLanguage.ifBlank { "en" }

    private fun describe(language: Language) = BridgeLanguage(language.id, language.name, LanguageCodes.codeFor(language.name))

    private fun parse(json3: String) = runCatching { SubtitleFormats.parse(json3, SubtitleFormat.YOUTUBE_JSON) }.getOrDefault(emptyList())

    companion object {
        const val MAX_ENTRIES = 8
        const val MAX_SENSES = 6
        const val MAX_TRANSLATED_LINES = 50
    }
}
