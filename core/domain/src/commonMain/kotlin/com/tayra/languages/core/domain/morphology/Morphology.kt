package com.tayra.languages.core.domain.morphology

/**
 * A language's spelling variants and inflection rules, as Yomitan applies them to a word before
 * looking it up: the word's spellings ([preprocess]), every word each could be an inflection of
 * ([LanguageTransformer]), and those words' own spellings ([postprocess]).
 */
class LanguageMorphology internal constructor(
    val code: String,
    private val preprocessors: List<(String) -> List<String>>,
    private val postprocessors: List<(String) -> List<String>>,
    val transformer: LanguageTransformer?,
) {
    /** A word the clicked text may be a spelling or an inflection of. */
    data class Candidate(val text: String, val conditions: Int, val transforms: List<String>)

    /** Every spelling of [text] the processors give, the text itself first. */
    fun preprocess(text: String): List<String> = variants(text, preprocessors)

    fun postprocess(text: String): List<String> = variants(text, postprocessors)

    /** The words [text] may be a spelling or an inflection of, nearest first; the text itself is the first. */
    fun candidates(text: String): List<Candidate> {
        val out = LinkedHashMap<Pair<String, Int>, Candidate>()
        for (variant in preprocess(text)) {
            val deinflections = transformer?.transform(variant) ?: listOf(LanguageTransformer.Deinflection(variant, 0, emptyList()))
            for (d in deinflections) {
                for (spelling in postprocess(d.text)) {
                    val key = spelling to d.conditions
                    if (key !in out && out.size < MAX_CANDIDATES) out[key] = Candidate(spelling, d.conditions, d.trace)
                }
            }
        }
        return out.values.sortedBy { it.transforms.size }
    }

    /**
     * Whether a dictionary word of part of speech [pos] (as the packs name it: "noun", "verb")
     * can be what a candidate in [conditions] deinflected to. A candidate in no condition matches
     * any word; one that is, say, a verb only matches a verb, of any of the rules' verb classes.
     */
    fun accepts(conditions: Int, pos: String): Boolean {
        if (conditions == 0) return true
        val transformer = transformer ?: return true
        val names = POS_CONDITIONS[pos] ?: listOf(pos)
        // A part of speech the packs give more broadly than the rules ("verb" for Japanese's verb
        // classes v1, v5...) stands for all of its kinds; one the rules do not know at all, for any.
        val flags = transformer.conditions(names)
        return flags == 0 || LanguageTransformer.conditionsMatch(conditions, flags)
    }

    private fun variants(text: String, processors: List<(String) -> List<String>>): List<String> {
        var current = linkedSetOf(text)
        for (process in processors) {
            val next = LinkedHashSet<String>()
            for (variant in current) {
                for (processed in process(variant)) {
                    if (next.size < MAX_VARIANTS) next += processed
                }
            }
            current = next
        }
        return current.toList()
    }

    private companion object {
        const val MAX_VARIANTS = 4096
        const val MAX_CANDIDATES = 4096

        /** The packs' parts of speech, by the condition names Yomitan's rules give them. */
        val POS_CONDITIONS: Map<String, List<String>> = mapOf(
            "noun" to listOf("n"), "name" to listOf("n"), "verb" to listOf("v"), "adj" to listOf("adj", "adj-i"),
            "adv" to listOf("adv"), "pron" to listOf("pron"), "num" to listOf("num"), "aux" to listOf("aux"),
        )
    }
}

/** The morphology of the languages Yomitan describes, by the app's language code. */
object Morphology {
    /** Yomitan's code for a language where the app files it under another. */
    private val yomitanCodes = mapOf("sr" to "sh", "hr" to "sh", "bs" to "sh")

    /** Built on first use, once per language: parsing a language's rules takes a moment. */
    private val languages: Map<String, Lazy<LanguageMorphology>> by lazy {
        yomitanLanguages.associate { it.code to lazy { build(it) } }
    }

    fun forLanguage(code: String): LanguageMorphology? {
        val key = code.trim().lowercase()
        return languages[yomitanCodes[key] ?: key]?.value
    }

    private fun build(language: YomitanLanguage): LanguageMorphology {
        val code = language.code
        val rules = transformRules[code]
        return LanguageMorphology(
            code = code,
            preprocessors = language.preprocessors.mapNotNull { TextProcessors.byId[it] },
            postprocessors = language.postprocessors.mapNotNull { TextProcessors.byId[it] },
            transformer = rules?.let { LanguageTransformer(it()) },
        )
    }

    /** The text processor ids a language uses that are not ported, for tests to keep empty. */
    internal fun missingProcessors(code: String): List<String> {
        val language = yomitanLanguages.firstOrNull { it.code == (yomitanCodes[code] ?: code) } ?: return emptyList()
        return (language.preprocessors + language.postprocessors).filter { it !in TextProcessors.byId }
    }
}
