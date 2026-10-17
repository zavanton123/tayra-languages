package com.tayra.languages.core.data.yomitan

import com.tayra.languages.core.data.db.newSqliteFile
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.ImportedDictionaryInfo
import com.tayra.languages.core.domain.dictionary.DictionaryImporter
import com.tayra.languages.core.domain.morphology.Unicode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Converts Yomitan dictionaries into the app's packs, as tools/build_yomitan_dictionary.py converts
 * wiktionary-to-yomitan's: each term bank row becomes an entry (a word in one part of speech) whose
 * definitions are its senses, and the rows that only point to another word ("non-lemma") its forms.
 * Structured content is read as wty writes it (glosses lists, example sentences, tags) and, for other
 * dictionaries, as their glossary lists, any list, or else its text. Pitch accents and IPA come from
 * the term meta banks, with the frequencies they give, and kanji from the kanji banks. A dictionary
 * of frequencies or of kanji alone is a pack of those alone.
 */
class YomitanConverter : DictionaryImporter {

    override suspend fun inspect(file: ByteArray): ImportedDictionaryInfo = info(ZipArchive(file))

    override suspend fun convert(file: ByteArray, sourceLanguage: String, targetLanguage: String, onProgress: (Float) -> Unit): ByteArray = withContext(Dispatchers.Default) {
        val archive = ZipArchive(file)
        val info = info(archive)
        val content = read(archive, sourceLanguage, onProgress)
        if (content.isEmpty) throw ZipException("${info.title} has no terms, frequencies or kanji")
        onProgress(0.95f)
        newSqliteFile(statements(content, info, sourceLanguage, targetLanguage))
    }

    private suspend fun info(archive: ZipArchive): ImportedDictionaryInfo {
        val text = archive.readText("index.json") ?: throw ZipException("Not a Yomitan dictionary: it has no index.json")
        val index = json.parseToJsonElement(text).jsonObject
        fun field(name: String) = (index[name] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
        return ImportedDictionaryInfo(
            title = field("title") ?: throw ZipException("Not a Yomitan dictionary: its index has no title"),
            revision = field("revision").orEmpty(),
            sourceLanguage = field("sourceLanguage"),
            targetLanguage = field("targetLanguage"),
            attribution = field("attribution"),
            frequencyMode = field("frequencyMode"),
        )
    }

    internal class Entry(val word: String, val pos: String, val senses: MutableList<JsonObject>, var reading: String?)

    internal class Frequency(val word: String, val reading: String?, val value: Int?, val display: String?)

    internal class Kanji(val character: String, val meanings: List<String>, val onyomi: String, val kunyomi: String, val stats: Map<String, String>)

    internal class Content(
        val entries: Map<Pair<String, String>, Entry>,
        val forms: Set<Pair<String, String>>,
        val pitch: List<Triple<String, String, String>>,
        val ipa: Map<String, String>,
        val frequencies: List<Frequency> = emptyList(),
        val kanji: List<Kanji> = emptyList(),
        val kanjiFrequency: Map<String, Int> = emptyMap(),
    ) {
        val isEmpty: Boolean get() = entries.isEmpty() && frequencies.isEmpty() && kanji.isEmpty()
    }

    internal suspend fun read(archive: ZipArchive, source: String, onProgress: (Float) -> Unit = {}): Content {
        val tags = HashMap<String, Pair<String, String>>()
        for (entry in banks(archive, "tag_bank_")) {
            for (row in json.parseToJsonElement(archive.read(entry).decodeToString()).jsonArray) {
                val cells = row.jsonArray
                val name = cells.string(0) ?: continue
                tags[name] = (cells.string(1).orEmpty()) to (cells.string(3)?.ifEmpty { null } ?: name)
            }
        }
        val clean = cleaner(source)
        val entries = LinkedHashMap<Pair<String, String>, Entry>()
        val forms = LinkedHashSet<Pair<String, String>>()
        val termBanks = banks(archive, "term_bank_")
        termBanks.forEachIndexed { index, bank ->
            for (row in json.parseToJsonElement(archive.read(bank).decodeToString()).jsonArray) {
                addTerm(row.jsonArray, tags, clean, source, entries, forms)
            }
            onProgress(0.9f * (index + 1) / termBanks.size)
        }
        val pitch = ArrayList<Triple<String, String, String>>()
        val ipa = HashMap<String, String>()
        val frequencies = ArrayList<Frequency>()
        for (bank in banks(archive, "term_meta_bank_")) {
            for (row in json.parseToJsonElement(archive.read(bank).decodeToString()).jsonArray) {
                val cells = row.jsonArray
                val word = cells.string(0)?.let(clean) ?: continue
                if (cells.string(1) == "freq") {
                    frequencyOf(cells.getOrNull(2))?.let { (reading, value, display) -> frequencies += Frequency(word, reading, value, display) }
                    continue
                }
                val data = cells.getOrNull(2) as? JsonObject ?: continue
                when (cells.string(1)) {
                    "pitch" -> {
                        val positions = (data["pitches"] as? JsonArray)?.mapNotNull { (it as? JsonObject)?.get("position")?.jsonPrimitive?.intOrNull }.orEmpty()
                        if (positions.isNotEmpty()) pitch += Triple(word, data.string("reading") ?: word, positions.distinct().joinToString(","))
                    }
                    "ipa" -> (data["transcriptions"] as? JsonArray)?.firstNotNullOfOrNull { (it as? JsonObject)?.string("ipa") }?.let { ipa.getOrPut(word) { it } }
                }
            }
        }
        val kanji = ArrayList<Kanji>()
        for (bank in banks(archive, "kanji_bank_")) {
            for (row in json.parseToJsonElement(archive.read(bank).decodeToString()).jsonArray) {
                val cells = row.jsonArray
                val character = cells.string(0)?.takeIf { it.isNotEmpty() } ?: continue
                val meanings = (cells.getOrNull(4) as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim()?.ifEmpty { null } }.orEmpty()
                val stats = (cells.getOrNull(5) as? JsonObject)?.mapNotNull { (key, value) -> (value as? JsonPrimitive)?.contentOrNull?.let { key to it } }?.toMap().orEmpty()
                kanji += Kanji(character, meanings, cells.string(1).orEmpty().trim(), cells.string(2).orEmpty().trim(), stats)
            }
        }
        val kanjiFrequency = HashMap<String, Int>()
        for (bank in banks(archive, "kanji_meta_bank_")) {
            for (row in json.parseToJsonElement(archive.read(bank).decodeToString()).jsonArray) {
                val cells = row.jsonArray
                val character = cells.string(0) ?: continue
                if (cells.string(1) == "freq") frequencyOf(cells.getOrNull(2))?.second?.let { kanjiFrequency.getOrPut(character) { it } }
            }
        }
        val lemmas = entries.values.mapTo(HashSet()) { it.word }
        return Content(entries, forms.filterTo(LinkedHashSet()) { it.second in lemmas }, pitch, ipa, frequencies, kanji, kanjiFrequency)
    }

    /**
     * A frequency as Yomitan's meta banks write it: a number, a text ("five (5)"), {value, displayValue},
     * or either of those under {reading, frequency}. Answers the reading, the number (the first in a
     * text) and the text to show, when it differs from the number.
     */
    private fun frequencyOf(data: JsonElement?): Triple<String?, Int?, String?>? {
        fun plain(element: JsonElement?): Pair<Int?, String?>? = when (element) {
            is JsonPrimitive -> if (element.isString) {
                Regex("\\d+").find(element.content)?.value?.toIntOrNull() to element.content
            } else {
                element.content.toDoubleOrNull()?.toInt() to null
            }
            is JsonObject -> element["value"]?.jsonPrimitive?.content?.toDoubleOrNull()?.toInt() to element.string("displayValue")
            else -> null
        }
        val (reading, value) = if (data is JsonObject && "frequency" in data) data.string("reading") to data["frequency"] else null to data
        val (number, display) = plain(value) ?: return null
        if (number == null && display.isNullOrBlank()) return null
        return Triple(reading, number, display?.takeIf { it != number?.toString() })
    }

    private fun addTerm(
        cells: JsonArray,
        tags: Map<String, Pair<String, String>>,
        clean: (String) -> String,
        source: String,
        entries: MutableMap<Pair<String, String>, Entry>,
        forms: MutableSet<Pair<String, String>>,
    ) {
        val word = cells.string(0)?.trim()?.let(clean)?.takeIf { it.isNotEmpty() } ?: return
        val reading = cells.string(1)?.trim()?.takeIf { it.isNotEmpty() && it != word }
        val defTags = cells.string(2)?.split(' ')?.filter { it.isNotEmpty() }.orEmpty()
        val posTag = defTags.firstOrNull { tags[it]?.first == "partOfSpeech" || it in POS || isVerbTag(it) }
        val pos = posTag?.let(::posOf) ?: cells.string(3)?.split(' ')?.firstNotNullOfOrNull { rule -> rule.takeIf { it.isNotEmpty() }?.let(::posOf) } ?: "unknown"
        val entryTags = defTags.filter { it != posTag && it != "non-lemma" && tags[it]?.first.let { c -> c != null && c != "partOfSpeech" && c != "" } }
            .map { tags[it]!!.second }
        val senses = ArrayList<JsonObject>()
        for (definition in cells.getOrNull(5) as? JsonArray ?: return) {
            when {
                definition is JsonArray -> definition.string(0)?.trim()?.let(clean)?.let { lemma ->
                    if (lemma != word && lemma.isNotEmpty()) forms += word to lemma
                }
                definition is JsonPrimitive && definition.isString -> definition.content.trim().takeIf { it.isNotEmpty() }?.let { senses += sense(listOf(it), entryTags) }
                definition is JsonObject -> when (definition.string("type")) {
                    "text" -> definition.string("text")?.trim()?.takeIf { it.isNotEmpty() }?.let { senses += sense(listOf(it), entryTags) }
                    "structured-content" -> senses += structuredSenses(definition["content"], entryTags)
                }
            }
        }
        if (reading != null) {
            forms += reading to word
            if (source == "zh") forms += reading.substringBefore(" (").lowercase().filter { !it.isWhitespace() } to word
        }
        if (senses.isEmpty()) return
        // Japanese and Chinese words have an entry per reading (打つ read うつ and ぶつ), as Yomitan shows them.
        val key = word to if (source in READING_LANGUAGES) "$pos\u0000${reading.orEmpty()}" else pos
        val existing = entries[key]
        if (existing == null) {
            entries[key] = Entry(word, pos, senses.distinct().toMutableList(), reading)
        } else {
            senses.filter { it !in existing.senses }.forEach { existing.senses += it }
            if (existing.reading == null) existing.reading = reading
        }
    }

    private fun sense(glosses: List<String>, tags: List<String>, examples: List<JsonObject> = emptyList()): JsonObject = buildJsonObject {
        put("glosses", buildJsonArray { glosses.forEach { add(JsonPrimitive(it)) } })
        val kept = tags.filter { it !in NOISE_TAGS }.distinct()
        if (kept.isNotEmpty()) put("tags", buildJsonArray { kept.forEach { add(JsonPrimitive(it)) } })
        if (examples.isNotEmpty()) put("examples", JsonArray(examples))
    }

    /** The senses of a structured-content definition, read in the first way that finds some. */
    private fun structuredSenses(content: JsonElement?, entryTags: List<String>): List<JsonObject> {
        // wiktionary-to-yomitan: each item of its glosses lists is a sense, nested lists senses of their own.
        val wty = blocks(content, "glosses").flatMap { items(it["content"]) }
        if (wty.isNotEmpty()) return wty.map { (gloss, tags, examples) -> sense(listOf(gloss), entryTags + tags, examples) }
        // Jitendex and wty's glossaries: a glossary list of translations per sense, its label before it.
        val glossaries = labelledGlossaries(content)
        if (glossaries.isNotEmpty()) return glossaries.map { (label, words) -> sense(words, entryTags + listOfNotNull(label)) }
        val listItems = firstList(content)?.let { list -> (list["content"].asList()).filterIsInstance<JsonObject>().filter { it.string("tag") == "li" } }.orEmpty()
            .map { normalised(textOf(it["content"])) }.filter { it.isNotEmpty() }
        if (listItems.isNotEmpty()) return listItems.map { sense(listOf(it), entryTags) }
        return listOfNotNull(normalised(textOf(content)).takeIf { it.isNotEmpty() }?.let { sense(listOf(it), entryTags) })
    }

    private fun items(node: JsonElement?): List<Triple<String, List<String>, List<JsonObject>>> {
        val out = ArrayList<Triple<String, List<String>, List<JsonObject>>>()
        fun walk(n: JsonElement?) {
            when (n) {
                is JsonArray -> n.forEach(::walk)
                is JsonObject -> {
                    if (n.string("tag") == "li" && n.data() !in setOf("synonym-item", "antonym-item")) {
                        val gloss = normalised(textOf(n["content"]))
                        if (gloss.isNotEmpty()) out += Triple(gloss, tagsOf(n["content"]), examplesOf(n["content"]))
                        nestedLists(n["content"]).forEach { walk(it) }
                        return
                    }
                    if (n.data() in SKIPPED_BLOCKS) return
                    walk(n["content"])
                }
                else -> {}
            }
        }
        walk(node)
        return out
    }

    private fun labelledGlossaries(node: JsonElement?): List<Pair<String?, List<String>>> {
        val out = ArrayList<Pair<String?, List<String>>>()
        fun walk(n: JsonElement?) {
            when (n) {
                is JsonArray -> {
                    var label: String? = null
                    for (child in n) {
                        val data = (child as? JsonObject)?.data()
                        when {
                            data == "sense-label" -> label = normalised(textOf((child as JsonObject)["content"])).ifEmpty { null }
                            data == "glossary" -> {
                                val words = (child as JsonObject)["content"].asList().filterIsInstance<JsonObject>()
                                    .filter { it.string("tag") == "li" }.map { normalised(textOf(it["content"])) }.filter { it.isNotEmpty() }.distinct()
                                if (words.isNotEmpty()) out += label to words
                                label = null
                            }
                            else -> walk(child)
                        }
                    }
                }
                is JsonObject -> if (n.data() == "glossary") walk(JsonArray(listOf(n))) else walk(n["content"])
                else -> {}
            }
        }
        walk(node)
        return out
    }

    private fun textOf(node: JsonElement?): String = when (node) {
        is JsonPrimitive -> if (node.isString) node.content else ""
        is JsonArray -> node.joinToString("") { textOf(it) }
        is JsonObject -> when {
            node.string("tag") in HIDDEN_TAGS -> ""
            node.string("tag") == "br" -> " "
            node.data() in SKIPPED_BLOCKS -> ""
            else -> textOf(node["content"])
        }
        else -> ""
    }

    private fun tagsOf(node: JsonElement?): List<String> {
        val out = ArrayList<String>()
        fun walk(n: JsonElement?) {
            when (n) {
                is JsonArray -> n.forEach(::walk)
                is JsonObject -> {
                    if (n.string("tag") in LIST_TAGS) return
                    if (n.data() == "tag") {
                        (n.string("title") ?: textOf(n["content"])).trim().takeIf { it.isNotEmpty() }?.let { out += it }
                        return
                    }
                    walk(n["content"])
                }
                else -> {}
            }
        }
        walk(node)
        return out
    }

    private fun examplesOf(node: JsonElement?): List<JsonObject> {
        val out = ArrayList<JsonObject>()
        fun walk(n: JsonElement?) {
            when (n) {
                is JsonArray -> n.forEach(::walk)
                is JsonObject -> {
                    if (n.string("tag") == "ol" || n.string("tag") == "ul") return
                    if (n.data() == "example-sentence") {
                        val parts = n["content"].asList().filterIsInstance<JsonObject>().associate { it.data() to normalised(textOf(it["content"])) }
                        parts["example-sentence-a"]?.takeIf { it.isNotEmpty() }?.let { text ->
                            out += buildJsonObject {
                                put("text", text)
                                parts["example-sentence-b"]?.takeIf { it.isNotEmpty() }?.let { put("translation", it) }
                            }
                        }
                        return
                    }
                    walk(n["content"])
                }
                else -> {}
            }
        }
        walk(node)
        return out.take(MAX_EXAMPLES)
    }

    private fun nestedLists(node: JsonElement?): List<JsonObject> {
        val out = ArrayList<JsonObject>()
        fun walk(n: JsonElement?) {
            when (n) {
                is JsonArray -> n.forEach(::walk)
                is JsonObject -> if (n.string("tag") in LIST_TAGS && n.data() !in setOf("synonyms-list", "antonyms-list")) out += n else if (n.data() !in SKIPPED_BLOCKS) walk(n["content"])
                else -> {}
            }
        }
        walk(node)
        return out
    }

    private fun blocks(node: JsonElement?, kind: String): List<JsonObject> {
        val out = ArrayList<JsonObject>()
        fun walk(n: JsonElement?) {
            when (n) {
                is JsonArray -> n.forEach(::walk)
                is JsonObject -> if (n.data() == kind) out += n else walk(n["content"])
                else -> {}
            }
        }
        walk(node)
        return out
    }

    private fun firstList(node: JsonElement?): JsonObject? = when (node) {
        is JsonArray -> node.firstNotNullOfOrNull { firstList(it) }
        is JsonObject -> if (node.string("tag") == "ol" || node.string("tag") == "ul") node else if (node.data() in SKIPPED_BLOCKS) null else firstList(node["content"])
        else -> null
    }

    internal fun statements(content: Content, info: ImportedDictionaryInfo, source: String, target: String): List<String> {
        val out = ArrayList<String>()
        out += "CREATE TABLE meta (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL)"
        out += "CREATE TABLE entries (id INTEGER NOT NULL PRIMARY KEY, word TEXT NOT NULL, word_lc TEXT NOT NULL, pos TEXT NOT NULL, ipa TEXT, senses TEXT NOT NULL)"
        out += "CREATE TABLE forms (form_lc TEXT NOT NULL, lemma TEXT NOT NULL, generated INTEGER NOT NULL DEFAULT 0)"
        // A word's readings keep the dictionary's order, the main one first.
        val order = content.entries.values.withIndex().associate { (i, e) -> e to i }
        val rows = content.entries.values.sortedWith(compareBy({ it.word.lowercase() }, { it.word }, { it.pos }, { order.getValue(it) }))
        rows.chunked(BATCH).forEachIndexed { batch, chunk ->
            out += "INSERT INTO entries(id, word, word_lc, pos, ipa, senses) VALUES " + chunk.mapIndexed { i, e ->
                val ipa = content.ipa[e.word] ?: e.reading?.takeIf { source !in READING_LANGUAGES }
                "(${batch * BATCH + i + 1}, ${q(e.word)}, ${q(lowercase(e.word, source))}, ${q(e.pos)}, ${ipa?.let(::q) ?: "NULL"}, ${q(JsonArray(e.senses).toString())})"
            }.joinToString(", ")
        }
        val formRows = content.forms.map { (form, lemma) -> lowercase(form, source) to lemma }.distinct()
        formRows.chunked(BATCH).forEach { chunk -> out += "INSERT INTO forms(form_lc, lemma, generated) VALUES " + chunk.joinToString(", ") { (form, lemma) -> "(${q(form)}, ${q(lemma)}, 0)" } }
        if (source in READING_LANGUAGES) {
            out += "CREATE TABLE readings (word TEXT NOT NULL, reading TEXT NOT NULL)"
            rows.mapNotNull { e -> e.reading?.let { e.word to (if (source == "zh") it.substringBefore(" (") else it) } }.distinct().chunked(BATCH).forEach { chunk ->
                out += "INSERT INTO readings VALUES " + chunk.joinToString(", ") { (word, reading) -> "(${q(word)}, ${q(reading)})" }
            }
            out += "CREATE INDEX readings_word ON readings(word)"
            out += "CREATE TABLE entry_readings (entry_id INTEGER NOT NULL PRIMARY KEY, reading TEXT NOT NULL)"
            rows.withIndex().filter { it.value.reading != null }.chunked(BATCH).forEach { chunk ->
                out += "INSERT INTO entry_readings VALUES " + chunk.joinToString(", ") { (i, e) -> "(${i + 1}, ${q(if (source == "zh") e.reading!!.substringBefore(" (") else e.reading!!)})" }
            }
        }
        if (content.pitch.isNotEmpty()) {
            out += "CREATE TABLE pitch (word TEXT NOT NULL, reading TEXT NOT NULL, positions TEXT NOT NULL)"
            content.pitch.chunked(BATCH).forEach { chunk -> out += "INSERT INTO pitch VALUES " + chunk.joinToString(", ") { (w, r, p) -> "(${q(w)}, ${q(r)}, ${q(p)})" } }
            out += "CREATE INDEX pitch_word ON pitch(word)"
        }
        if (content.frequencies.isNotEmpty()) {
            out += "CREATE TABLE frequencies (word TEXT NOT NULL, reading TEXT, value INTEGER, display TEXT)"
            content.frequencies.chunked(BATCH).forEach { chunk ->
                out += "INSERT INTO frequencies VALUES " + chunk.joinToString(", ") { f -> "(${q(f.word)}, ${f.reading?.let(::q) ?: "NULL"}, ${f.value ?: "NULL"}, ${f.display?.let(::q) ?: "NULL"})" }
            }
            out += "CREATE INDEX frequencies_word ON frequencies(word)"
        }
        if (content.kanji.isNotEmpty()) {
            out += "CREATE TABLE kanji (character TEXT NOT NULL PRIMARY KEY, meanings TEXT NOT NULL, onyomi TEXT NOT NULL, kunyomi TEXT NOT NULL, strokes INTEGER, grade INTEGER, jlpt INTEGER, frequency INTEGER)"
            content.kanji.distinctBy { it.character }.chunked(BATCH).forEach { chunk ->
                out += "INSERT INTO kanji VALUES " + chunk.joinToString(", ") { k ->
                    fun stat(vararg keys: String) = keys.firstNotNullOfOrNull { k.stats[it] }?.let { Regex("\\d+").find(it)?.value } ?: "NULL"
                    val frequency = content.kanjiFrequency[k.character]?.toString() ?: stat("freq", "frequency")
                    "(${q(k.character)}, ${q(JsonArray(k.meanings.map(::JsonPrimitive)).toString())}, ${q(k.onyomi)}, ${q(k.kunyomi)}, ${stat("strokes")}, ${stat("grade")}, ${stat("jlpt")}, $frequency)"
                }
            }
        }
        val sources = listOf(info.title, info.revision).filter { it.isNotEmpty() }.joinToString(" ") + " (imported Yomitan dictionary)"
        val meta = listOf(
            "format" to DictionaryId.FORMAT.toString(), "source_language" to source, "target_language" to target,
            "entry_count" to rows.size.toString(), "form_count" to formRows.size.toString(), "sources" to sources,
        ) + listOfNotNull(info.attribution?.let { "license" to it }, info.frequencyMode?.let { "frequency_mode" to it })
        out += "INSERT INTO meta(key, value) VALUES " + meta.joinToString(", ") { (k, v) -> "(${q(k)}, ${q(v)})" }
        out += "CREATE INDEX entries_word_lc ON entries(word_lc)"
        out += "CREATE INDEX forms_form_lc ON forms(form_lc)"
        out += "PRAGMA user_version = ${DictionaryId.FORMAT}"
        return out
    }

    private fun banks(archive: ZipArchive, prefix: String): List<ZipArchive.Entry> =
        archive.entries.filter { it.name.substringAfterLast('/').startsWith(prefix) && it.name.endsWith(".json") }
            .sortedBy { it.name.substringAfterLast('_').substringBefore('.').toIntOrNull() ?: 0 }

    internal companion object {
        val json = Json { ignoreUnknownKeys = true }

        /** Rows per INSERT: fewer statements, each well under SQLite's limits. */
        const val BATCH = 200
        const val MAX_EXAMPLES = 3

        val READING_LANGUAGES = setOf("ja", "zh")

        /** The parts of speech Yomitan dictionaries tag, as the app's packs name them (JMdict's codes besides wty's). */
        val POS = mapOf(
            "n" to "noun", "v" to "verb", "vt" to "verb", "vi" to "verb", "vr" to "verb", "vdt" to "verb", "adj" to "adj", "adv" to "adv",
            "name" to "name", "surn" to "name", "prop-n" to "name", "pron" to "pron", "pn" to "pron", "num" to "num", "prep" to "prep",
            "postp" to "postp", "conj" to "conj", "intj" to "intj", "int" to "intj", "ptcl" to "particle", "prt" to "particle",
            "artic" to "article", "det" to "det", "suf" to "suffix", "pref" to "prefix", "phrase" to "phrase", "exp" to "phrase",
            "prov" to "proverb", "char" to "character", "contr" to "contraction", "counter" to "counter", "ctr" to "counter",
            "adn" to "adnominal", "adj-pn" to "adnominal", "ptcpl" to "participle", "adj_noun" to "adj", "adj-i" to "adj",
            "adj-ix" to "adj", "adj-na" to "adj", "adj-no" to "adj", "aux-v" to "verb", "aux-adj" to "adj", "col" to "phrase",
            "abbrev" to "abbrev", "punct" to "punct", "sym" to "symbol", "classifier" to "classifier",
        )

        fun isVerbTag(tag: String) = tag.length >= 2 && tag[0] == 'v' && (tag[1].isDigit() || tag[1] in "skzn")

        fun posOf(tag: String): String? = POS[tag] ?: if (isVerbTag(tag)) "verb" else null

        /** Structured-content blocks that are not the gloss itself. */
        val SKIPPED_BLOCKS = setOf(
            "tags", "details-entry-examples", "extra-info", "example-sentence", "backlink", "synonyms", "antonyms",
            "related-words", "preamble", "attribution", "forms", "references",
        )
        val NOISE_TAGS = setOf("form-of", "alt-of", "no-gloss", "morpheme", "romanization")
        val LIST_TAGS = setOf("ol", "ul", "details")
        val HIDDEN_TAGS = setOf("ol", "ul", "details", "summary", "img", "rt", "rp")

        /** Headwords as the app looks them up: without the stress marks Slavic dictionaries write, or Latin's vowel lengths. */
        fun cleaner(source: String): (String) -> String = when (source) {
            "ru", "uk", "be", "bg", "mk", "hr", "sr", "bs" -> { word -> strip(word, "̀́") }
            "la" -> { word -> strip(word, "̄̆") }
            else -> { word -> word }
        }

        fun strip(word: String, marks: String): String =
            if (word.none { it.code >= 0xC0 }) word else Unicode.nfc(Unicode.nfd(word).filter { it !in marks })

        fun lowercase(word: String, source: String): String =
            (if (source == "tr") word.replace('I', 'ı').replace('İ', 'i') else word).lowercase()

        fun normalised(text: String): String = text.split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")

        fun q(value: String): String = "'" + value.replace("'", "''") + "'"

        fun JsonArray.string(index: Int): String? = (getOrNull(index) as? JsonPrimitive)?.takeIf { it.isString }?.content

        fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

        fun JsonObject.data(): String? = ((this["data"] as? JsonObject)?.get("content") as? JsonPrimitive)?.contentOrNull

        fun JsonElement?.asList(): List<JsonElement> = when (this) {
            is JsonArray -> this
            null -> emptyList()
            else -> listOf(this)
        }
    }
}
