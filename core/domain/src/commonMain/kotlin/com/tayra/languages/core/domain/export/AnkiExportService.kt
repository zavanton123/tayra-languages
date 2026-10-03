package com.tayra.languages.core.domain.export

import com.tayra.languages.core.domain.dictionary.DictionaryLookup
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.dictionary.OfflineDictionary
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
            sentence?.let { cloze(it, word) } ?: "{{c1::$word}}",
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

    /** The sentence with its first whole occurrence of [word] blanked out; the word alone when it does not occur. */
    internal fun cloze(sentence: String, word: String): String {
        var from = 0
        while (true) {
            val at = sentence.indexOf(word, from, ignoreCase = true)
            if (at < 0) return "{{c1::$word}}<br>$sentence"
            val end = at + word.length
            val whole = (at == 0 || !sentence[at - 1].isLetterOrDigit()) && (end == sentence.length || !sentence[end].isLetterOrDigit())
            if (whole) return sentence.substring(0, at) + "{{c1::" + sentence.substring(at, end) + "}}" + sentence.substring(end)
            from = end
        }
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
        {{#Item Title}}<div class="tl-title">{{Item Title}}</div>{{/Item Title}}
        <div class="tl-sentence" lang="{{Language}}">{{cloze:Cloze}}</div>
        {{#Translation}}<div class="tl-translation" lang="{{Translation Language}}">{{Translation}}</div>{{/Translation}}
    </div>
</div>
""".trim()

        private val BACK = """
<div class="tl-page">
    <div class="tl-card">
        {{#Item Title}}<div class="tl-title">{{Item Title}}</div>{{/Item Title}}
        <div class="tl-sentence" lang="{{Language}}">{{cloze:Cloze}}</div>
        {{#Translation}}<div class="tl-translation" lang="{{Translation Language}}">{{Translation}}</div>{{/Translation}}
        <div class="tl-word-row">
            <span class="tl-word" lang="{{Language}}">{{Word}}</span>
            {{#Part of Speech}}<span class="tl-pos">{{Part of Speech}}</span>{{/Part of Speech}}
            {{#Word Definition}}<span class="tl-definition" lang="{{Translation Language}}">{{Word Definition}}</span>{{/Word Definition}}
        </div>
        {{#Word Transliteration}}<div class="tl-reading">{{Word Transliteration}}</div>{{/Word Transliteration}}
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
    font-size: 23px;
    text-align: center;
    color: #1f1f1f;
    background: #f3f4f6;
}
.nightMode.card {
    color: #f2f2f2;
    background: #1b1b1d;
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
    max-width: 620px;
    box-sizing: border-box;
    padding: 22px 26px;
    border-radius: 16px;
    background: #ffffff;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}
.nightMode .tl-card {
    background: #3a3a3d;
    box-shadow: none;
}
.tl-title {
    font-size: 13px;
    color: #8a8a8a;
    margin-bottom: 10px;
}
.tl-sentence {
    font-size: 23px;
    line-height: 1.4;
}
.cloze {
    font-weight: normal;
    color: #ffffff;
    background: #2e5fa8;
    border-radius: 6px;
    padding: 1px 7px;
}
.tl-translation {
    font-size: 15px;
    color: #6b6b6b;
    margin-top: 10px;
}
.nightMode .tl-translation {
    color: #b8b8b8;
}
.tl-word-row {
    margin-top: 18px;
    padding-top: 14px;
    border-top: 1px solid #e3e3e6;
    display: flex;
    justify-content: center;
    align-items: baseline;
    gap: 10px;
    flex-wrap: wrap;
}
.nightMode .tl-word-row {
    border-top-color: #4d4d50;
}
.tl-word {
    font-size: 20px;
    font-weight: 500;
}
.tl-pos {
    font-size: 12px;
    color: #2e5fa8;
    border: 1px solid #2e5fa8;
    border-radius: 10px;
    padding: 1px 8px;
}
.nightMode .tl-pos {
    color: #8ab4f8;
    border-color: #8ab4f8;
}
.tl-definition {
    font-size: 16px;
    color: #444444;
}
.nightMode .tl-definition {
    color: #d8d8d8;
}
.tl-reading {
    font-size: 14px;
    color: #8a8a8a;
    margin-top: 6px;
}
.replay-button {
    margin-top: 16px;
}
""".trim()
    }
}
