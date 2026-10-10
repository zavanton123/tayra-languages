package com.tayra.languages.core.domain.export

import com.tayra.languages.core.domain.dictionary.DictionaryLookup
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
import com.tayra.languages.core.domain.flashcards.Cloze
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.model.Term
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.domain.service.SentenceTranslator
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Exports terms as Anki cloze notes, one per term: the sentence the term was read in with the
 * term blanked out, the sentence's translation, the term's translation, lemma and part of
 * speech, and the sentence read aloud. The note type matches Language Reactor's cards.
 */
class AnkiExportService(
    private val terms: TermRepository,
    private val languages: LanguageRepository,
    private val dictionary: OfflineDictionary,
    private val translator: SentenceTranslator,
    private val audio: SentenceAudio,
    private val settings: SettingsRepository,
    private val packager: AnkiPackager,
    private val clock: Clock = Clock.System,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    /** The package, the ids of the terms in it and how many terms were left out as already exported. */
    class Export(val fileName: String, val bytes: ByteArray, val termIds: List<Long>, val skipped: Int)

    /**
     * Builds the package for [termIds], leaving out terms exported before; null when none is
     * left. [onProgress] reports how many terms are done out of the total.
     */
    suspend fun export(termIds: Collection<Long>, onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): Export? {
        val now = clock.now()
        val native = settings.current.nativeLanguage.trim().lowercase().ifEmpty { "en" }
        val order = termIds.withIndex().associate { (i, id) -> id to i }
        val all = terms.getByIds(termIds).sortedBy { order[it.id] }
        val selected = all.filter { it.ankiExportedAt == null }
        if (selected.isEmpty()) return null
        val notes = mutableListOf<AnkiNote>()
        val media = LinkedHashMap<String, AnkiMedia>()
        selected.forEachIndexed { index, term ->
            onProgress(index, selected.size)
            val language = languages.getById(term.languageId) ?: return@forEachIndexed
            notes += noteFor(term, language, native, now, media)
        }
        onProgress(selected.size, selected.size)
        val collection = AnkiCollection(noteType(), notes, media.values.toList())
        return Export("${stamp(now)}.apkg", packager.pack(collection, now), selected.map { it.id }, all.size - selected.size)
    }

    /** Records that the terms of [export] are in Anki now, once its file was saved. */
    suspend fun markExported(export: Export) = terms.markAnkiExported(export.termIds, clock.now())

    private suspend fun noteFor(term: Term, language: Language, native: String, now: Instant, media: MutableMap<String, AnkiMedia>): AnkiNote {
        val code = LanguageCodes.codeFor(language.name) ?: language.name.lowercase()
        val word = term.displayText
        val sentence = term.sentence?.trim()?.takeIf { it.isNotEmpty() }
        val lookup = attempt { lookup(code, native, word) }
        val lemma = term.parents.firstOrNull()?.displayText ?: lookup?.parentSuggestion ?: word
        val partOfSpeech = lookup?.entries?.firstOrNull()?.pos?.replaceFirstChar { it.uppercase() }.orEmpty()
        val translation = sentence?.let { attempt { translator.translate(it, language) } }.orEmpty()
        val spoken = sentence ?: word
        val clip = attempt { audio.audioFor(spoken, code) }?.let { wav ->
            val name = "tayra-${Sha1.hex("$code|$spoken".encodeToByteArray()).take(16)}.wav"
            media.getOrPut(name) { AnkiMedia(name, wav) }
            "[sound:$name]"
        }.orEmpty()
        val fields = listOf(
            sentence?.let { cloze(it, Cloze.familyRange(it, term, terms), word) } ?: "{{c1::$word}}",
            "WORD|$lemma|$code",
            sentence ?: word,
            translation,
            word,
            lemma,
            partOfSpeech,
            "",
            term.translation.orEmpty(),
            SOURCE,
            code,
            native,
            term.romanization.orEmpty(),
            "",
            "",
            "",
            "",
            stamp(term.createdAt ?: now, pretty = true),
            stamp(now, pretty = true),
            sentence.orEmpty(),
            translation,
            "",
            "",
            clip,
        )
        return AnkiNote(
            guid = Sha1.hex("tayra|$code|${term.textLc}".encodeToByteArray()).take(16),
            deck = "Tayra Languages::${language.name}",
            fields = fields,
            tags = listOf("Tayra_Languages::lang::$code", "Tayra_Languages::source::reading"),
        )
    }

    private suspend fun lookup(code: String, native: String, word: String): DictionaryLookup? {
        val pack = DictionaryPacks.find(code, native)?.id ?: return null
        if (!dictionary.isAvailable(pack)) return null
        return dictionary.lookup(pack, word)
    }

    /** The sentence with [found] (the word, or the form of its family read there) blanked out; [word] before the sentence when nothing was found. */
    internal fun cloze(sentence: String, found: IntRange?, word: String): String {
        if (found == null) return "{{c1::$word}}<br>$sentence"
        return sentence.substring(0, found.first) + "{{c1::" + sentence.substring(found) + "}}" + sentence.substring(found.last + 1)
    }

    private fun stamp(instant: Instant, pretty: Boolean = false): String {
        val t = instant.toLocalDateTime(timeZone())
        fun two(n: Int) = n.toString().padStart(2, '0')
        val date = "${t.year}-${two(t.month.ordinal + 1)}-${two(t.day)}"
        return if (pretty) "$date ${two(t.hour)}:${two(t.minute)}" else "${date}_${two(t.hour)}-${two(t.minute)}"
    }

    private inline fun <T> attempt(block: () -> T?): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }

    companion object {
        const val SOURCE = "Tayra Languages"

        val FIELDS = listOf(
            "Cloze", "Item Key", "Subtitle", "Translation", "Word", "Lemma", "Part of Speech", "Color", "Word Definition", "Source",
            "Language", "Translation Language", "Word Transliteration", "Phrase Transliteration", "Subtitle Index", "Item ID", "Item Title",
            "Date Created", "Date Modified", "Context", "Context Translation", "Prev Image", "Next Image", "Audio Clip",
        )

        fun noteType() = AnkiNoteType("Tayra Languages", FIELDS, FRONT, BACK, CSS)

        private val FRONT = """
<div class="tl-page">
    <div class="tl-card">
        <div class="tl-top">
            {{#Item Title}}<div class="tl-title">{{Item Title}}</div>{{/Item Title}}
            <div class="tl-sentence" lang="{{Language}}">{{cloze:Cloze}}</div>
            {{#Translation}}<div class="tl-translation" lang="{{Translation Language}}">{{Translation}}</div>{{/Translation}}
        </div>
    </div>
</div>
""".trim()

        private val BACK = """
<div class="tl-page">
    <div class="tl-card">
        <div class="tl-top">
            {{#Item Title}}<div class="tl-title">{{Item Title}}</div>{{/Item Title}}
            <div class="tl-sentence" lang="{{Language}}">{{cloze:Cloze}}</div>
            {{#Translation}}<div class="tl-translation" lang="{{Translation Language}}">{{Translation}}</div>{{/Translation}}
        </div>
        <div class="tl-bottom">
            <div class="tl-word-row">
                <span class="tl-word" lang="{{Language}}">{{Word}}</span>
                {{#Part of Speech}}<span class="tl-pos">{{Part of Speech}}</span>{{/Part of Speech}}
                {{#Word Definition}}<span class="tl-definition" lang="{{Translation Language}}">{{Word Definition}}</span>{{/Word Definition}}
            </div>
            {{#Word Transliteration}}<div class="tl-reading">{{Word Transliteration}}</div>{{/Word Transliteration}}
        </div>
    </div>
    {{Audio Clip}}
</div>
""".trim()

        private val CSS = """
html, body {
    height: 100%;
    margin: 0;
    padding: 0;
}
.card {
    margin: 0;
    padding: 0;
    min-height: 100vh;
    box-sizing: border-box;
    display: flex;
    align-items: center;
    justify-content: center;
    font-family: Arial, Helvetica, sans-serif;
    text-align: center;
    color: #222222;
    background: #e9ecf2;
}
.nightMode.card {
    background: #17191d;
}
.tl-page {
    width: 100%;
    box-sizing: border-box;
    padding: 24px;
    display: flex;
    flex-direction: column;
    align-items: center;
}
.tl-card {
    width: 100%;
    max-width: 860px;
    border-radius: 22px;
    overflow: hidden;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.18);
}
.nightMode .tl-card {
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.45);
}
.tl-top {
    background: #24324f;
    color: #ffffff;
    padding: 36px 40px;
}
.tl-title {
    font-size: 16px;
    color: #aab6d0;
    margin-bottom: 12px;
}
.tl-sentence {
    font-size: 38px;
    line-height: 1.35;
}
.cloze {
    color: #ffd166;
    font-weight: 600;
}
.tl-translation {
    font-size: 22px;
    line-height: 1.4;
    color: #aab6d0;
    margin-top: 16px;
}
.tl-bottom {
    background: #f7f5ef;
    color: #222222;
    padding: 24px 40px;
}
.tl-word-row {
    display: flex;
    justify-content: center;
    align-items: baseline;
    gap: 16px;
    flex-wrap: wrap;
}
.tl-word {
    font-size: 32px;
    font-weight: 500;
}
.tl-pos {
    font-size: 14px;
    letter-spacing: 0.1em;
    text-transform: uppercase;
    color: #7a6a3a;
    background: #ffe9a8;
    border-radius: 6px;
    padding: 3px 9px;
}
.tl-definition {
    font-size: 24px;
    color: #444444;
}
.tl-reading {
    font-size: 18px;
    color: #8a7f66;
    margin-top: 8px;
}
.replay-button {
    margin-top: 20px;
}
.replay-button svg {
    width: 52px;
    height: 52px;
}
@media (max-width: 600px) {
    .tl-top, .tl-bottom { padding-left: 22px; padding-right: 22px; }
    .tl-sentence { font-size: 28px; }
    .tl-translation { font-size: 18px; }
    .tl-word { font-size: 26px; }
    .tl-definition { font-size: 20px; }
}
""".trim()
    }
}
