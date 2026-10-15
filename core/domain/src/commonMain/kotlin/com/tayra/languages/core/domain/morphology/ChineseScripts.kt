package com.tayra.languages.core.domain.morphology

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The two ways Chinese is written: simplified (mainland China, Singapore) and traditional (Taiwan, Hong Kong). */
enum class ChineseScript(val id: String) {
    SIMPLIFIED("simplified"),
    TRADITIONAL("traditional");

    companion object {
        fun of(id: String?): ChineseScript? = entries.firstOrNull { it.id == id }
    }
}

/** Reads one of OpenCC's dictionary files (STPhrases.txt...) bundled with the app; null when it is missing. */
fun interface ChineseScriptTables {
    suspend fun read(name: String): String?
}

/**
 * Converts Chinese text between the scripts with Open Chinese Convert's dictionaries (OpenCC,
 * Apache-2.0, Copyright Carbo Kuo and contributors), the converter GoldenDict uses: a chain of
 * stages, each replacing the longest word it knows at every place, its phrases before its single
 * characters, so 头发 becomes 頭髮 while 发现 becomes 發現. To traditional, the chain is OpenCC's
 * s2tw: standard traditional, then Taiwan's variants of characters (為, 著); to simplified, tw2s.
 * Words are not replaced by regional ones (出租车 stays 出租車, not 計程車).
 */
class ChineseConverter internal constructor(private val stages: List<List<Map<String, String>>>) {
    private val longest = stages.map { stage -> stage.map { dict -> dict.keys.maxOfOrNull { it.length } ?: 0 } }

    fun convert(text: String): String = stages.indices.fold(text) { current, stage -> convertStage(current, stage) }

    private fun convertStage(text: String, stage: Int): String {
        val dicts = stages[stage]
        val out = StringBuilder(text.length)
        var i = 0
        while (i < text.length) {
            var replaced = false
            for ((d, dict) in dicts.withIndex()) {
                val max = minOf(longest[stage][d], text.length - i)
                for (length in max downTo 1) {
                    val value = dict[text.substring(i, i + length)] ?: continue
                    out.append(value)
                    i += length
                    replaced = true
                    break
                }
                if (replaced) break
            }
            if (!replaced) {
                // A character outside the Basic Multilingual Plane is a surrogate pair; it is copied whole.
                val length = if (text[i].isHighSurrogate() && i + 1 < text.length) 2 else 1
                out.append(text, i, i + length)
                i += length
            }
        }
        return out.toString()
    }

    companion object {
        /** An OpenCC dictionary file: "key<TAB>value [value...]", the first value taken. */
        fun parse(file: String): Map<String, String> = HashMap<String, String>().also { map ->
            for (line in file.lineSequence()) {
                if (line.isEmpty() || line.startsWith("#")) continue
                val tab = line.indexOf('\t')
                if (tab <= 0) continue
                val value = line.substring(tab + 1).substringBefore(' ').trim()
                if (value.isNotEmpty()) map[line.substring(0, tab)] = value
            }
        }

        /** A dictionary read backwards, each value to its key, as OpenCC makes TWVariantsRev from TWVariants. */
        fun reversed(dict: Map<String, String>): Map<String, String> = dict.entries.associate { (key, value) -> value to key }
    }
}

/**
 * The converters, loaded once from the bundled tables; parsing is synchronous, so [prepare] is called
 * before a Chinese text is read, as the lexicons are.
 */
object ChineseScripts {
    private val lock = Mutex()
    private var toSimplified: ChineseConverter? = null
    private var toTraditional: ChineseConverter? = null

    val isReady: Boolean get() = toSimplified != null && toTraditional != null

    suspend fun prepare(tables: ChineseScriptTables) = lock.withLock {
        if (isReady) return@withLock
        suspend fun dict(name: String) = tables.read(name)?.let(ChineseConverter::parse) ?: emptyMap()
        val twVariants = dict("TWVariants.txt")
        toTraditional = ChineseConverter(listOf(listOf(dict("STPhrases.txt"), dict("STCharacters.txt")), listOf(dict("TWVariantsPhrases.txt"), twVariants)))
        toSimplified = ChineseConverter(listOf(listOf(dict("TWVariantsRevPhrases.txt"), ChineseConverter.reversed(twVariants)), listOf(dict("TSPhrases.txt"), dict("TSCharacters.txt"))))
    }

    /** [text] in [script]; unchanged when the converters are not loaded. */
    fun convert(text: String, script: ChineseScript): String = when (script) {
        ChineseScript.SIMPLIFIED -> toSimplified?.convert(text) ?: text
        ChineseScript.TRADITIONAL -> toTraditional?.convert(text) ?: text
    }

    /** For tests: converters made from tables given directly. */
    internal fun set(simplified: ChineseConverter?, traditional: ChineseConverter?) {
        toSimplified = simplified
        toTraditional = traditional
    }
}
