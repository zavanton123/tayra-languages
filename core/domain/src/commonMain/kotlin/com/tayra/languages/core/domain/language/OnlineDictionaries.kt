package com.tayra.languages.core.domain.language

import com.tayra.languages.core.domain.model.DictionaryType
import com.tayra.languages.core.domain.model.DictionaryUse
import com.tayra.languages.core.domain.model.LanguageDictionary

/**
 * An online dictionary that can be enabled for any language. The template takes the learned
 * language as `{src}`/`{srcName}`, the native language as `{tgt}`/`{tgtName}` (ISO 639-1 codes
 * and lowercase English names) and the looked-up word as `{term}`.
 */
data class OnlineDictionary(val id: String, val name: String, val template: String, val host: String) {
    fun url(source: LanguageOption, target: LanguageOption): String = fill(template, source, target).replace("{term}", LanguageDictionary.LOOKUP_PLACEHOLDER)

    /** The host fragment that identifies this dictionary for the language pair. */
    fun hostFor(source: LanguageOption, target: LanguageOption): String = fill(host, source, target)

    fun displayName(source: LanguageOption, target: LanguageOption): String =
        name.replace("{srcName}", source.name).replace("{tgtName}", target.name)

    private fun fill(text: String, source: LanguageOption, target: LanguageOption): String = text
        .replace("{src}", source.code)
        .replace("{tgt}", target.code)
        .replace("{srcName}", source.name.lowercase())
        .replace("{tgtName}", target.name.lowercase())

    fun toLanguageDictionary(source: LanguageOption, target: LanguageOption, sortOrder: Int) =
        LanguageDictionary(useFor = DictionaryUse.TERMS, type = DictionaryType.POPUP, url = url(source, target), isActive = true, sortOrder = sortOrder)
}

/** The catalog offered on the "manage dictionaries" screen. */
object OnlineDictionaries {
    val all: List<OnlineDictionary> = listOf(
        OnlineDictionary("wordreference", "WordReference", "https://www.wordreference.com/{src}{tgt}/{term}", "wordreference.com"),
        OnlineDictionary("linguee", "Linguee", "https://www.linguee.com/{srcName}-{tgtName}/search?query={term}", "linguee.com"),
        OnlineDictionary("glosbe", "Glosbe", "https://glosbe.com/{src}/{tgt}/{term}", "glosbe.com"),
        OnlineDictionary("reverso-context", "Context Reverso", "https://context.reverso.net/translation/{srcName}-{tgtName}/{term}", "context.reverso.net"),
        OnlineDictionary("google-translate", "Google Translate", "https://translate.google.com/?sl={src}&tl={tgt}&text={term}&op=translate", "translate.google.com"),
        OnlineDictionary("deepl", "DeepL Translator", "https://www.deepl.com/translator#{src}/{tgt}/{term}", "deepl.com"),
        OnlineDictionary("wiktionary-en", "Wiktionary (English)", "https://en.wiktionary.org/wiki/{term}", "en.wiktionary.org"),
        OnlineDictionary("wiktionary-native", "Wiktionary ({tgtName})", "https://{tgt}.wiktionary.org/wiki/{term}", "{tgt}.wiktionary.org"),
        OnlineDictionary("wiktionary-source", "Wiktionary ({srcName})", "https://{src}.wiktionary.org/wiki/{term}", "{src}.wiktionary.org"),
        OnlineDictionary("babla", "bab.la", "https://en.bab.la/dictionary/{srcName}-{tgtName}/{term}", "bab.la"),
        OnlineDictionary("dictcc", "dict.cc", "https://{src}{tgt}.dict.cc/?s={term}", "dict.cc"),
        OnlineDictionary("pons", "PONS", "https://en.pons.com/translate/{srcName}-{tgtName}/{term}", "pons.com"),
        OnlineDictionary("collins", "Collins", "https://www.collinsdictionary.com/dictionary/{srcName}-{tgtName}/{term}", "collinsdictionary.com"),
        OnlineDictionary("cambridge", "Cambridge Dictionary", "https://dictionary.cambridge.org/dictionary/{srcName}-{tgtName}/{term}", "dictionary.cambridge.org"),
        OnlineDictionary("reverso-conjugator", "Reverso Verb Conjugation", "https://conjugator.reverso.net/conjugation-{srcName}-verb-{term}.html", "conjugator.reverso.net"),
        OnlineDictionary("forvo", "Forvo (pronunciation)", "https://forvo.com/word/{term}/#{src}", "forvo.com"),
        OnlineDictionary("yandex", "Yandex Translate", "https://translate.yandex.com/?source_lang={src}&target_lang={tgt}&text={term}", "translate.yandex.com"),
        OnlineDictionary("wikipedia", "Wikipedia ({srcName})", "https://{src}.wikipedia.org/wiki/{term}", "{src}.wikipedia.org"),
        OnlineDictionary("google-search", "Google Search", "https://www.google.com/search?q={term}", "google.com/search"),
        OnlineDictionary("google-images", "Google Images", "https://www.google.com/search?tbm=isch&q={term}", "tbm=isch"),
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
}
