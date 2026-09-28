package com.tayra.languages.core.domain.language

import com.tayra.languages.core.domain.model.DictionaryType
import com.tayra.languages.core.domain.model.DictionaryUse
import com.tayra.languages.core.domain.model.LanguageDictionary

/**
 * An online dictionary that can be enabled for a language. [build] returns the lookup URL for a
 * learned/native pair with `{term}` where the word goes, or null when the site does not cover
 * that language; pairs the site lacks fall back to English where it has an English edition.
 */
class OnlineDictionary(
    val id: String,
    val name: String,
    /** Fragment of the URL that identifies the site, with `{src}`/`{tgt}` placeholders allowed. */
    val host: String,
    private val build: (source: LanguageOption, target: LanguageOption) -> String?,
) {
    fun url(source: LanguageOption, target: LanguageOption): String? =
        build(source, target)?.replace("{term}", LanguageDictionary.LOOKUP_PLACEHOLDER)

    fun hostFor(source: LanguageOption, target: LanguageOption): String =
        host.replace("{src}", source.code).replace("{tgt}", target.code)

    fun displayName(source: LanguageOption, target: LanguageOption): String =
        name.replace("{srcName}", source.name).replace("{tgtName}", target.name)

    fun toLanguageDictionary(source: LanguageOption, target: LanguageOption, sortOrder: Int): LanguageDictionary? =
        url(source, target)?.let { LanguageDictionary(useFor = DictionaryUse.TERMS, type = DictionaryType.POPUP, url = it, isActive = true, sortOrder = sortOrder) }
}

/** The catalog offered on the "manage dictionaries" screen. */
object OnlineDictionaries {
    private val ENGLISH = LanguageOption("en", "English")

    /** Lowercase English name of a language as most sites spell it in URLs. */
    private fun slug(option: LanguageOption): String = when (option.code) {
        "sl" -> "slovenian"
        else -> option.name.lowercase()
    }

    /** Picks the native language when the site pairs it with [source], otherwise English. */
    private fun partner(source: LanguageOption, target: LanguageOption, pairs: Set<String>): LanguageOption? = when {
        source.code == target.code -> null
        target.code == "en" && "en" in pairs -> target
        target.code in pairs -> target
        "en" in pairs -> ENGLISH
        else -> null
    }

    private val WORDREFERENCE = setOf("es", "fr", "it", "de", "pt", "ru", "pl", "ro", "cs", "el", "tr", "nl", "sv", "is")
    private val LINGUEE = setOf("pt", "es", "fr", "de", "it", "nl", "pl", "sv", "da", "fi", "el", "cs", "ro", "hu", "sk", "bg", "sl", "lt", "lv", "et", "ru")
    // Checked against the site: Czech, Danish, Greek, Hungarian and Slovak redirect to the home page.
    private val REVERSO_CONTEXT = setOf("en", "fr", "es", "de", "it", "pt", "ru", "nl", "pl", "ro", "sv", "tr", "uk")
    private val REVERSO_MAJOR = setOf("en", "fr", "es", "de", "it", "pt")
    private val DEEPL = setOf("bg", "cs", "da", "de", "el", "en", "es", "et", "fi", "fr", "hu", "it", "lt", "lv", "no", "nl", "pl", "pt", "ro", "ru", "sk", "sl", "sv", "tr", "uk")
    private val BABLA = setOf("de", "es", "fr", "it", "pt", "nl", "pl", "ru", "sv", "da", "fi", "no", "cs", "hu", "ro", "tr", "el")
    private val DICTCC = setOf("bg", "hr", "cs", "da", "nl", "fi", "fr", "el", "hu", "is", "it", "la", "no", "pl", "pt", "ro", "ru", "sr", "sk", "es", "sv", "tr", "uk")
    // PONS pairs every language below with German, but only these with English.
    private val PONS_ENGLISH = setOf("de", "fr", "es", "it", "pl", "pt", "sl", "ru", "bg")
    private val PONS_GERMAN = setOf("en", "fr", "es", "it", "pl", "pt", "sl", "ru", "nl", "tr", "el", "la", "hr", "bg", "sv", "cs", "ro", "da", "no", "fi", "hu")
    private val COLLINS = setOf("fr", "de", "es", "it", "pt")
    private val CAMBRIDGE_FROM_ENGLISH = setOf("fr", "de", "es", "it", "pt", "nl", "pl", "ru", "tr", "cs", "da", "no", "sv", "ca", "uk")
    private val CAMBRIDGE_TO_ENGLISH = setOf("fr", "de", "es", "it", "pt", "nl", "pl", "sv")
    private val REVERSO_CONJUGATOR = setOf("en", "fr", "es", "de", "it", "pt", "ru")

    /** Codes some sites spell differently from ISO 639-1. */
    private fun deepl(code: String) = if (code == "no") "nb" else code
    private fun glosbe(code: String) = if (code == "no") "nb" else code
    private fun wr(code: String) = when (code) { "el" -> "gr"; "cs" -> "cz"; else -> code }
    private fun linguee(option: LanguageOption) = if (option.code == "sl") "slovene" else slug(option)

    val all: List<OnlineDictionary> = listOf(
        OnlineDictionary("wordreference", "WordReference", "wordreference.com") { s, t ->
            when {
                s.code == "en" && t.code in WORDREFERENCE -> "https://www.wordreference.com/en${wr(t.code)}/{term}"
                s.code == "en" -> "https://www.wordreference.com/definition/{term}"
                s.code in WORDREFERENCE -> "https://www.wordreference.com/${wr(s.code)}en/{term}"
                else -> null
            }
        },
        OnlineDictionary("linguee", "Linguee", "linguee.com") { s, t ->
            when {
                s.code == "en" && t.code in LINGUEE -> "https://www.linguee.com/english-${linguee(t)}/search?query={term}"
                s.code in LINGUEE -> "https://www.linguee.com/english-${linguee(s)}/search?query={term}"
                else -> null
            }
        },
        OnlineDictionary("glosbe", "Glosbe", "glosbe.com") { s, t ->
            if (s.code == t.code) null else "https://glosbe.com/${glosbe(s.code)}/${glosbe(t.code)}/{term}"
        },
        OnlineDictionary("reverso-context", "Context Reverso", "context.reverso.net") { s, t ->
            if (s.code !in REVERSO_CONTEXT) null else {
                // The major languages pair with everything supported; Russian only with the major ones.
                val other = when {
                    t.code == s.code -> if (s.code == "en") null else ENGLISH
                    t.code in REVERSO_MAJOR -> t
                    t.code == "ru" && s.code in REVERSO_MAJOR -> t
                    s.code != "en" -> ENGLISH
                    else -> null
                }
                other?.let { "https://context.reverso.net/translation/${slug(s)}-${slug(it)}/{term}" }
            }
        },
        OnlineDictionary("google-translate", "Google Translate", "translate.google.com") { s, t ->
            "https://translate.google.com/?sl=${s.code}&tl=${if (t.code == s.code) "en" else t.code}&text={term}&op=translate"
        },
        OnlineDictionary("deepl", "DeepL Translator", "deepl.com") { s, t ->
            if (s.code !in DEEPL) null else "https://www.deepl.com/translator#${deepl(s.code)}/${deepl(if (t.code in DEEPL && t.code != s.code) t.code else "en")}/{term}"
        },
        OnlineDictionary("wiktionary-en", "Wiktionary (English)", "en.wiktionary.org") { _, _ -> "https://en.wiktionary.org/wiki/{term}" },
        OnlineDictionary("wiktionary-native", "Wiktionary ({tgtName})", "{tgt}.wiktionary.org") { s, t ->
            if (t.code == "en" || t.code == s.code) null else "https://${t.code}.wiktionary.org/wiki/{term}"
        },
        OnlineDictionary("wiktionary-source", "Wiktionary ({srcName})", "{src}.wiktionary.org") { s, _ ->
            if (s.code == "en") null else "https://${s.code}.wiktionary.org/wiki/{term}"
        },
        OnlineDictionary("babla", "bab.la", "bab.la") { s, t ->
            when {
                s.code == "en" && t.code in BABLA -> "https://en.bab.la/dictionary/english-${slug(t)}/{term}"
                s.code in BABLA -> "https://en.bab.la/dictionary/${slug(s)}-english/{term}"
                else -> null
            }
        },
        OnlineDictionary("dictcc", "dict.cc", "dict.cc") { s, t ->
            when {
                s.code == "en" && t.code == "de" -> "https://www.dict.cc/?s={term}"
                s.code == "en" && t.code in DICTCC -> "https://en${t.code}.dict.cc/?s={term}"
                s.code == "de" -> "https://www.dict.cc/?s={term}"
                s.code in DICTCC && t.code == "de" -> "https://de${s.code}.dict.cc/?s={term}"
                s.code in DICTCC -> "https://en${s.code}.dict.cc/?s={term}"
                else -> null
            }
        },
        OnlineDictionary("pons", "PONS", "pons.com") { s, t ->
            when {
                s.code == "en" && t.code in PONS_ENGLISH -> "https://en.pons.com/translate/english-${slug(t)}/{term}"
                s.code in PONS_GERMAN && t.code == "de" -> "https://en.pons.com/translate/${slug(s)}-german/{term}"
                s.code in PONS_ENGLISH -> "https://en.pons.com/translate/${slug(s)}-english/{term}"
                s.code != "de" && s.code in PONS_GERMAN && t.code != "de" -> null
                else -> null
            }
        },
        OnlineDictionary("collins", "Collins", "collinsdictionary.com") { s, t ->
            when {
                s.code == "en" && t.code in COLLINS -> "https://www.collinsdictionary.com/dictionary/english-${slug(t)}/{term}"
                s.code == "en" -> "https://www.collinsdictionary.com/dictionary/english/{term}"
                s.code in COLLINS -> "https://www.collinsdictionary.com/dictionary/${slug(s)}-english/{term}"
                else -> null
            }
        },
        OnlineDictionary("cambridge", "Cambridge Dictionary", "dictionary.cambridge.org") { s, t ->
            when {
                s.code == "en" && t.code in CAMBRIDGE_FROM_ENGLISH -> "https://dictionary.cambridge.org/dictionary/english-${slug(t)}/{term}"
                s.code == "en" -> "https://dictionary.cambridge.org/dictionary/english/{term}"
                s.code in CAMBRIDGE_TO_ENGLISH -> "https://dictionary.cambridge.org/dictionary/${slug(s)}-english/{term}"
                else -> null
            }
        },
        OnlineDictionary("reverso-conjugator", "Reverso Verb Conjugation", "conjugator.reverso.net") { s, _ ->
            if (s.code in REVERSO_CONJUGATOR) "https://conjugator.reverso.net/conjugation-${slug(s)}-verb-{term}.html" else null
        },
        OnlineDictionary("forvo", "Forvo (pronunciation)", "forvo.com") { s, _ -> "https://forvo.com/word/{term}/#${s.code}" },
        OnlineDictionary("yandex", "Yandex Translate", "translate.yandex.com") { s, t ->
            "https://translate.yandex.com/?source_lang=${s.code}&target_lang=${if (t.code == s.code) "en" else t.code}&text={term}"
        },
        OnlineDictionary("wikipedia", "Wikipedia ({srcName})", "{src}.wikipedia.org") { s, _ -> "https://${s.code}.wikipedia.org/wiki/{term}" },
        OnlineDictionary("google-search", "Google Search", "google.com/search") { _, _ -> "https://www.google.com/search?q={term}" },
        OnlineDictionary("google-images", "Google Images", "tbm=isch") { _, _ -> "https://www.google.com/search?tbm=isch&q={term}" },
    )

    /** Display name for a stored dictionary URL: the catalog name when it matches, else the host. */
    fun displayName(url: String, source: LanguageOption, target: LanguageOption): String {
        match(url, source, target)?.let { return it.displayName(source, target) }
        val host = host(url)
        // Other Wiktionary and Wikipedia editions are named after their language.
        for (site in listOf("wiktionary.org" to "Wiktionary", "wikipedia.org" to "Wikipedia")) {
            if (host.endsWith("." + site.first)) {
                val code = host.removeSuffix("." + site.first)
                val language = LanguageCodes.option(code)?.name ?: code.uppercase()
                return "${site.second} ($language)"
            }
        }
        return host.ifEmpty { "Dictionary" }
    }

    /** The catalog entry a stored URL comes from, matched by its host for the language pair. */
    fun match(url: String, source: LanguageOption, target: LanguageOption): OnlineDictionary? {
        val lower = url.lowercase()
        // More specific fragments first so "tbm=isch" wins over "google.com/search".
        return all.map { it to it.hostFor(source, target) }.sortedByDescending { it.second.length }.firstOrNull { lower.contains(it.second) }?.first
    }

    fun host(url: String): String = url.substringAfter("://").substringBefore("/").removePrefix("www.")

    /**
     * Display names for a language's dictionaries, made distinct when several share a host by
     * appending the most telling part of their path ("infopedia.pt · verbos portugueses"), or a
     * number when the path says nothing useful.
     */
    fun labels(dictionaries: List<LanguageDictionary>, source: LanguageOption, target: LanguageOption): Map<LanguageDictionary, String> {
        val base = dictionaries.associateWith { displayName(it.url, source, target) }
        val result = HashMap<LanguageDictionary, String>()
        base.entries.groupBy { it.value }.forEach { (name, group) ->
            if (group.size == 1) {
                result[group.single().key] = name
                return@forEach
            }
            val qualifiers = group.map { pathQualifier(it.key.url) }
            val usable = qualifiers.toSet().size == qualifiers.size && qualifiers.all { it.isNotBlank() }
            group.forEachIndexed { index, entry ->
                result[entry.key] = if (usable) "$name \u00b7 ${qualifiers[index]}" else "$name \u00b7 ${index + 1}"
            }
        }
        return result
    }

    private val GENERIC_SEGMENTS = setOf(
        "dicionarios", "dicionario", "dictionary", "dictionaries", "diccionario", "dictionnaire", "busca", "search", "translate",
        "translation", "traduction", "wiki", "word", "dict", "go.php", "webverbix", "index.php", "lookup", "define", "definition",
    )

    /** The last readable path segment before the placeholder, with dashes turned into spaces. */
    private fun pathQualifier(url: String): String {
        val path = url.substringAfter("://").substringAfter("/", "").substringBefore(LanguageDictionary.LOOKUP_PLACEHOLDER)
            .substringBefore(LanguageDictionary.LEGACY_PLACEHOLDER).substringBefore('?').substringBefore('#')
        val segment = path.split('/').map { it.trim() }.lastOrNull { it.isNotBlank() && it.lowercase() !in GENERIC_SEGMENTS && it.any { c -> c.isLetter() } }
            ?: return ""
        return segment.replace(Regex("[-_+]"), " ").trim()
    }
}
