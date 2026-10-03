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
<div class="dc-bg">
    <div class="dc-card">
        {{#Item Title}}
            <div class="dc-title">
                {{Item Title}}
            </div>
        {{/Item Title}}

        <div class="dc-images">
            {{Prev Image}}
            {{Next Image}}
        </div>

        <div class="dc-cloze">{{cloze:Cloze}}</div>
        <div class="dc-translation">{{Translation}}</div>
        <br>
    </div>
</div>
""".trim()

        private val BACK = """
<div class="dc-bg">
    <div class="dc-card">
        {{#Item Title}}
            <div class="dc-title">
                {{Item Title}}
            </div>
        {{/Item Title}}

        <div class="dc-images">
            {{Prev Image}}
            {{Next Image}}
        </div>

        <div class="dc-cloze" lang="{{Language}}">{{cloze:Cloze}}</div>
        <div class="dc-translation" lang="{{Translation Language}}">{{Translation}}</div>
        <br>
        <div>
            <ruby lang="{{Language}}">{{Word}}<rt>{{Word Transliteration}}</rt></ruby>{{#Part of Speech}}: {{Part of Speech}}{{/Part of Speech}}<br>
            <span lang="{{Translation Language}}">{{Word Definition}}</span>
        </div>
        <br>
    </div>
</div>
<br>
{{Audio Clip}}
""".trim()

        private val CSS = """
body {
    padding: 0;
    margin: 0;
}
.card {
    font-family: arial;
    font-size: 30px;
    text-align: center;
    color: black;
    background: rgb(255,243,248);
    background: linear-gradient(76deg, rgba(255,243,248,1) 0%, rgba(238,246,255,1) 100%);
}
.nightMode.card {
    background: black;
}
.dc-card {
    background-color: white;
    padding-top: 0.8rem;
    padding-bottom: 1rem;
    border-bottom: 0.5px solid grey;
}
.nightMode .dc-card {
    background: #333;
    border-bottom: none;
}
.dc-cloze ruby {
    font-size: 1.3rem;
    margin-inline: 0.15em;
}
.dc-cloze {
    padding-top: 0.8rem;
}
.cloze {
    font-weight: bold;
    color: blue;
}
.nightMode .cloze {
    color: #1569C7;
}
.dc-translation {
    padding-top: 0.4rem;
    color: fuchsia;
}
.dc-title {
    color: rgb(127, 127, 127);
    font-size: 0.8rem;
    text-align: center;
    margin-bottom: 0.65rem;
}
.dc-images {
    text-align: center;
    max-width: 450px;
    margin: 0 auto 0.5rem;
}
""".trim()
    }
}
