package com.tayra.languages.core.data.stardict

import com.tayra.languages.core.data.db.newSqliteFile
import com.tayra.languages.core.data.files.HtmlText
import com.tayra.languages.core.data.mdict.MdictConverter
import com.tayra.languages.core.data.yomitan.YomitanConverter.Companion.BATCH
import com.tayra.languages.core.data.yomitan.YomitanConverter.Companion.lowercase
import com.tayra.languages.core.data.yomitan.YomitanConverter.Companion.q
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryImporter
import com.tayra.languages.core.domain.dictionary.ImportedDictionaryInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Converts StarDict dictionaries into the app's packs: the files of one (.ifo, .idx or .idx.gz,
 * .dict or .dict.dz, and .syn if it has synonyms) in a zip or tar archive, plain or gzipped, as
 * GoldenDict and KOReader read them. Each headword's entry becomes one sense whose glosses are the
 * lines its text shows (plain text, or HTML, Pango or XDXF markup laid out as text), its phonetic
 * field its IPA; each synonym becomes a form of its headword. Pictures and sounds are left out.
 */
class StardictConverter : DictionaryImporter {

    override suspend fun inspect(file: ByteArray): ImportedDictionaryInfo = withContext(Dispatchers.Default) {
        val ifo = Stardict.ifo(Archives.files(file))
        ImportedDictionaryInfo(
            title = ifo["bookname"]?.takeIf { it.isNotBlank() } ?: "StarDict dictionary",
            revision = ifo["date"].orEmpty(),
            attribution = ifo["description"]?.let { HtmlText.text(it.replace(BR, "<br>")).lines().joinToString(" ").take(MAX_DESCRIPTION) },
        )
    }

    override suspend fun convert(file: ByteArray, sourceLanguage: String, targetLanguage: String, onProgress: (Float) -> Unit): ByteArray = withContext(Dispatchers.Default) {
        val dictionary = Stardict.read(Archives.files(file))
        val title = dictionary.ifo["bookname"]?.takeIf { it.isNotBlank() } ?: "StarDict dictionary"
        newSqliteFile { execute ->
            execute("CREATE TABLE meta (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)")
            execute("CREATE TABLE entries (id INTEGER NOT NULL PRIMARY KEY, word TEXT NOT NULL, word_lc TEXT NOT NULL, pos TEXT NOT NULL, ipa TEXT, senses TEXT NOT NULL)")
            execute("CREATE TABLE forms (form_lc TEXT NOT NULL, lemma TEXT NOT NULL, generated INTEGER NOT NULL DEFAULT 0)")
            var count = 0
            val rows = ArrayList<String>(BATCH)
            suspend fun flush() {
                if (rows.isEmpty()) return
                execute("INSERT INTO entries(id, word, word_lc, pos, ipa, senses) VALUES " + rows.joinToString(", "))
                rows.clear()
            }
            dictionary.words.forEachIndexed { i, word ->
                val (glosses, ipa) = dictionary.entry(i)
                if (glosses.isNotEmpty()) {
                    val senses = JsonArray(listOf(buildJsonObject { put("glosses", buildJsonArray { glosses.forEach { add(JsonPrimitive(it)) } }) }))
                    rows += "(${++count}, ${q(word.text)}, ${q(lowercase(word.text, sourceLanguage))}, '', ${ipa?.let(::q) ?: "NULL"}, ${q(senses.toString())})"
                    if (rows.size == BATCH) flush()
                }
                if (i % PROGRESS_EVERY == 0) onProgress(0.95f * i / dictionary.words.size)
            }
            flush()
            if (count == 0) throw StardictException("$title has no entries this app can read")
            val forms = dictionary.synonyms.mapNotNull { (synonym, index) ->
                val word = dictionary.words.getOrNull(index)?.text ?: return@mapNotNull null
                (lowercase(synonym, sourceLanguage) to word).takeIf { !synonym.equals(word, ignoreCase = true) }
            }.distinct()
            forms.chunked(BATCH).forEach { chunk -> execute("INSERT INTO forms(form_lc, lemma, generated) VALUES " + chunk.joinToString(", ") { (form, lemma) -> "(${q(form)}, ${q(lemma)}, 0)" }) }
            val meta = listOf(
                "format" to DictionaryId.FORMAT.toString(), "source_language" to sourceLanguage, "target_language" to targetLanguage,
                "entry_count" to count.toString(), "form_count" to forms.size.toString(), "sources" to "$title (imported StarDict dictionary)",
            )
            execute("INSERT INTO meta(key, value) VALUES " + meta.joinToString(", ") { (k, v) -> "(${q(k)}, ${q(v)})" })
            execute("CREATE INDEX entries_word_lc ON entries(word_lc)")
            execute("CREATE INDEX forms_form_lc ON forms(form_lc)")
            execute("PRAGMA user_version = ${DictionaryId.FORMAT}")
        }.also { onProgress(1f) }
    }

    private companion object {
        const val MAX_DESCRIPTION = 300
        const val PROGRESS_EVERY = 5_000
        val BR = Regex("</?br\\s*/?>", RegexOption.IGNORE_CASE)
    }
}

/** A StarDict dictionary's header, headwords, synonyms and entries. */
internal class Stardict(
    val ifo: Map<String, String>,
    val words: List<Word>,
    val synonyms: List<Pair<String, Int>>,
    private val dict: ByteArray,
) {
    /** A headword and where its entry is in the .dict file. */
    class Word(val text: String, val offset: Long, val size: Int)

    private val types: String? = ifo["sametypesequence"]?.takeIf { it.isNotEmpty() }

    /** The [index]th headword's entry: the lines it shows, and its phonetic field if it has one. */
    fun entry(index: Int): Pair<List<String>, String?> {
        val word = words[index]
        val start = word.offset.toInt()
        val end = minOf(start + word.size, dict.size)
        if (start !in 0..end) return emptyList<String>() to null
        val fields = fields(start, end)
        val ipa = fields.firstOrNull { it.first == 't' }?.second?.trim()?.takeIf { it.isNotEmpty() }
        val glosses = fields.filter { it.first in TEXT_TYPES }.flatMap { (type, value) ->
            when (type) {
                // XDXF repeats the headword in <k>; it and Pango mean their line breaks.
                'x' -> MdictConverter.glosses(word.text, value.replace(XDXF_KEY, "").replace("\n", "<br>"))
                'g' -> MdictConverter.glosses(word.text, value.replace("\n", "<br>"))
                else -> MdictConverter.glosses(word.text, value)
            }
        }
        return glosses.distinct().take(MdictConverter.MAX_GLOSSES) to ipa
    }

    /**
     * The entry's fields, each its type and its text: with a sametypesequence, the fields in its
     * order, every one but the last ending in a zero (or, for a binary one, after its size); without
     * one, each led by its type letter.
     */
    private fun fields(start: Int, end: Int): List<Pair<Char, String>> {
        val out = ArrayList<Pair<Char, String>>()
        var at = start
        val sequence = types
        var position = 0
        while (at < end) {
            val type = if (sequence != null) sequence.getOrNull(position++) ?: break else dict[at++].toInt().toChar()
            val last = sequence != null && position == sequence.length
            if (type.isUpperCase()) {
                // Binary data (a sound, a picture), after its size, unless it runs to the end.
                val size = if (last) end - at else int(at).also { at += 4 }
                at += size
            } else {
                var stop = at
                if (!last) while (stop < end && dict[stop].toInt() != 0) stop++ else stop = end
                out += type to dict.copyOfRange(at, stop).decodeToString()
                at = stop + 1
            }
        }
        return out
    }

    private fun int(at: Int): Int = (0 until 4).fold(0) { n, i -> (n shl 8) or (dict[at + i].toInt() and 0xFF) }

    companion object {
        /** Plain text, its locale's text, phonetics left out (they are IPA), Pango, XDXF, HTML, MediaWiki, KingSoft, WordNet. */
        private val TEXT_TYPES = setOf('m', 'l', 'g', 'x', 'h', 'w', 'k', 'y', 'n')
        private val XDXF_KEY = Regex("<k>.*?</k>", RegexOption.DOT_MATCHES_ALL)

        /** The .ifo file's key=value lines, from among [files]. */
        fun ifo(files: List<ArchiveFile>): Map<String, String> {
            val file = files.firstOrNull { it.name.lowercase().endsWith(".ifo") } ?: throw StardictException("Not a StarDict dictionary: it has no .ifo file")
            val text = file.bytes.decodeToString().removePrefix("﻿")
            if (!text.startsWith("StarDict's dict ifo file")) throw StardictException("Not a StarDict dictionary: its .ifo file has no StarDict header")
            return text.lines().drop(1).mapNotNull { line -> line.indexOf('=').takeIf { it > 0 }?.let { line.substring(0, it).trim() to line.substring(it + 1).trim() } }.toMap()
        }

        suspend fun read(files: List<ArchiveFile>): Stardict {
            val ifo = ifo(files)
            fun find(vararg endings: String) = endings.firstNotNullOfOrNull { ending -> files.firstOrNull { it.name.lowercase().endsWith(ending) } }
            val idxFile = find(".idx", ".idx.gz") ?: throw StardictException("The StarDict dictionary has no .idx file")
            val dictFile = find(".dict", ".dict.dz") ?: throw StardictException("The StarDict dictionary has no .dict file")
            val idx = if (Archives.isGzip(idxFile.bytes)) Archives.gunzip(idxFile.bytes) else idxFile.bytes
            val dict = if (Archives.isGzip(dictFile.bytes)) Archives.gunzip(dictFile.bytes) else dictFile.bytes
            val offsetWidth = if (ifo["idxoffsetbits"] == "64") 8 else 4
            val words = ArrayList<Word>()
            var at = 0
            while (at < idx.size) {
                var end = at
                while (end < idx.size && idx[end].toInt() != 0) end++
                if (end + 1 + offsetWidth + 4 > idx.size) break
                val text = idx.copyOfRange(at, end).decodeToString()
                var offset = 0L
                for (i in 0 until offsetWidth) offset = (offset shl 8) or (idx[end + 1 + i].toLong() and 0xFF)
                val size = (0 until 4).fold(0) { n, i -> (n shl 8) or (idx[end + 1 + offsetWidth + i].toInt() and 0xFF) }
                words += Word(text, offset, size)
                at = end + 1 + offsetWidth + 4
            }
            val synonyms = ArrayList<Pair<String, Int>>()
            find(".syn")?.bytes?.let { syn ->
                var i = 0
                while (i < syn.size) {
                    var end = i
                    while (end < syn.size && syn[end].toInt() != 0) end++
                    if (end + 5 > syn.size) break
                    val index = (0 until 4).fold(0) { n, k -> (n shl 8) or (syn[end + 1 + k].toInt() and 0xFF) }
                    synonyms += syn.copyOfRange(i, end).decodeToString() to index
                    i = end + 5
                }
            }
            return Stardict(ifo, words, synonyms, dict)
        }
    }
}
