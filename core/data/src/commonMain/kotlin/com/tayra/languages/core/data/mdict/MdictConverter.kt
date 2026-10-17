package com.tayra.languages.core.data.mdict

import com.tayra.languages.core.data.db.newSqliteFile
import com.tayra.languages.core.data.files.HtmlText
import com.tayra.languages.core.data.files.MarkupToken
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
 * Converts the user's own MDict dictionaries (.mdx) into the app's packs. A record is HTML, laid out
 * as its dictionary pleases, so an entry becomes one sense whose glosses are the lines the HTML
 * shows, the headword repeated at its top left out; a record that only says "@@@LINK=word" sends its
 * key to that word, as a form. The resources of a .mdd file (sounds, pictures, styles) are not read.
 * Entries are written to the pack as they are read, so a dictionary of millions of them fits.
 */
class MdictConverter : DictionaryImporter {

    override suspend fun inspect(file: ByteArray): ImportedDictionaryInfo = withContext(Dispatchers.Default) {
        val mdict = MdictFile(file)
        ImportedDictionaryInfo(
            title = mdict.title ?: "MDict dictionary",
            revision = "",
            attribution = mdict.description?.let(HtmlText::text)?.lines()?.joinToString(" ")?.take(MAX_DESCRIPTION),
        )
    }

    override suspend fun convert(file: ByteArray, sourceLanguage: String, targetLanguage: String, onProgress: (Float) -> Unit): ByteArray = withContext(Dispatchers.Default) {
        val mdict = MdictFile(file)
        val title = mdict.title ?: "MDict dictionary"
        newSqliteFile { execute ->
            execute("CREATE TABLE meta (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)")
            execute("CREATE TABLE entries (id INTEGER NOT NULL PRIMARY KEY, word TEXT NOT NULL, word_lc TEXT NOT NULL, pos TEXT NOT NULL, ipa TEXT, senses TEXT NOT NULL)")
            execute("CREATE TABLE forms (form_lc TEXT NOT NULL, lemma TEXT NOT NULL, generated INTEGER NOT NULL DEFAULT 0)")
            var count = 0
            val rows = ArrayList<String>(BATCH)
            val forms = LinkedHashSet<Pair<String, String>>()
            suspend fun flush() {
                if (rows.isEmpty()) return
                execute("INSERT INTO entries(id, word, word_lc, pos, ipa, senses) VALUES " + rows.joinToString(", "))
                rows.clear()
            }
            mdict.read({ onProgress(0.95f * it) }) { key, record ->
                if (key.isEmpty()) return@read
                val link = record.trim().takeIf { it.startsWith(LINK) }?.removePrefix(LINK)?.trim()
                if (link != null) {
                    if (link.isNotEmpty() && !link.equals(key, ignoreCase = true)) forms += lowercase(key, sourceLanguage) to link
                    return@read
                }
                val glosses = glosses(key, record).takeIf { it.isNotEmpty() } ?: return@read
                val senses = JsonArray(listOf(buildJsonObject { put("glosses", buildJsonArray { glosses.forEach { add(JsonPrimitive(it)) } }) }))
                rows += "(${++count}, ${q(key)}, ${q(lowercase(key, sourceLanguage))}, '', NULL, ${q(senses.toString())})"
                if (rows.size == BATCH) flush()
            }
            flush()
            if (count == 0) throw MdictException("$title has no entries this app can read")
            forms.chunked(BATCH).forEach { chunk -> execute("INSERT INTO forms(form_lc, lemma, generated) VALUES " + chunk.joinToString(", ") { (form, lemma) -> "(${q(form)}, ${q(lemma)}, 0)" }) }
            val meta = listOf(
                "format" to DictionaryId.FORMAT.toString(), "source_language" to sourceLanguage, "target_language" to targetLanguage,
                "entry_count" to count.toString(), "form_count" to forms.size.toString(), "sources" to "$title (imported MDict dictionary)",
            )
            execute("INSERT INTO meta(key, value) VALUES " + meta.joinToString(", ") { (k, v) -> "(${q(k)}, ${q(v)})" })
            execute("CREATE INDEX entries_word_lc ON entries(word_lc)")
            execute("CREATE INDEX forms_form_lc ON forms(form_lc)")
            execute("PRAGMA user_version = ${DictionaryId.FORMAT}")
        }.also { onProgress(1f) }
    }

    internal companion object {
        const val LINK = "@@@LINK="
        const val MAX_GLOSSES = 60
        const val MAX_GLOSS = 1_000
        const val MAX_DESCRIPTION = 300
        private val BR_CLOSE = Regex("</br\\s*>", RegexOption.IGNORE_CASE)

        /** The lines an entry shows, as glosses: HTML laid out as text, or plain text by its lines. */
        fun glosses(key: String, record: String): List<String> {
            // "</br>", which some dictionaries write for "<br>", ends a line too; links to other entries are no gloss.
            val text = if ('<' in record) HtmlText.text(record.replace(BR_CLOSE, "<br>"), ::isCrossReference) else record
            val lines = text.lines().map { it.replace(Regex("\\s+"), " ").trim() }.filter { it.isNotEmpty() }
            // Most dictionaries open an entry with its headword, which the app shows already.
            val body = if (lines.firstOrNull()?.let { sameWord(it, key) } == true) lines.drop(1) else lines
            return body.distinct().take(MAX_GLOSSES).map { it.take(MAX_GLOSS) }
        }

        /** A link to another entry of the dictionary (MDict's entry://, StarDict's bword:// or #word): a related word, not a meaning. */
        private fun isCrossReference(token: MarkupToken.Open): Boolean {
            if (token.name != "a") return false
            val href = token.attributes["href"].orEmpty()
            return href.startsWith("#") || href.startsWith("entry://") || href.startsWith("bword://")
        }

        private fun sameWord(line: String, key: String): Boolean {
            fun letters(s: String) = s.lowercase().filter { it.isLetterOrDigit() }
            return letters(line) == letters(key)
        }
    }
}
