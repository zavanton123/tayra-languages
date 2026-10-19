package com.tayra.languages.core.data.web

import com.tayra.languages.core.data.files.Markup
import com.tayra.languages.core.data.files.MarkupToken
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** What a web page says about itself: its title, language, author and site, from its head and its schema.org data. */
internal data class PageMeta(val title: String?, val languageCode: String?, val author: String?, val siteName: String?) {

    companion object {
        fun read(html: String, host: String): PageMeta {
            val meta = HashMap<String, String>()
            var lang: String? = null
            var titleText: String? = null
            var heading: String? = null
            val data = mutableListOf<JsonObject>()
            var inside: String? = null
            val text = StringBuilder()
            for (token in Markup.tokens(html)) {
                when (token) {
                    is MarkupToken.Open -> when (token.name) {
                        "html" -> lang = token.attributes["lang"] ?: token.attributes["xml:lang"]
                        "meta" -> {
                            val key = (token.attributes["property"] ?: token.attributes["name"] ?: token.attributes["http-equiv"])?.lowercase()
                            val content = token.attributes["content"]?.trim()
                            if (key != null && !content.isNullOrEmpty() && key !in meta) meta[key] = content
                        }
                        "title", "h1" -> if (inside == null) { inside = token.name; text.clear() }
                        "script" -> if (token.attributes["type"]?.contains("ld+json") == true) { inside = "script"; text.clear() }
                    }
                    is MarkupToken.Text -> if (inside != null) text.append(token.text)
                    is MarkupToken.Close -> if (token.name == inside) {
                        val value = text.toString().replace(Regex("\\s+"), " ").trim()
                        when (inside) {
                            "title" -> if (titleText == null) titleText = value.ifEmpty { null }
                            "h1" -> if (heading == null) heading = value.ifEmpty { null }
                            "script" -> runCatching { collect(json.parseToJsonElement(text.toString()), data) }
                        }
                        inside = null
                    }
                }
            }
            val article = data.firstOrNull { (it["@type"]?.let(::strings).orEmpty()).any { type -> ARTICLE.containsMatchIn(type) } }
            val siteName = meta["og:site_name"] ?: article?.let { name(it["publisher"]) } ?: meta["application-name"]
            val title = (meta["og:title"] ?: meta["twitter:title"] ?: article?.let { string(it["headline"]) } ?: titleText ?: heading)
                ?.let { cleanTitle(it, siteName, host) }
            val language = (lang ?: meta["content-language"] ?: meta["og:locale"] ?: article?.let { string(it["inLanguage"]) })
                ?.replace('_', '-')?.substringBefore(',')?.trim()?.ifEmpty { null }
            val author = (meta["author"] ?: article?.let { name(it["author"]) } ?: meta["article:author"])?.takeUnless { it.startsWith("http") }
            return PageMeta(title, language, author, siteName)
        }

        private val json = Json { isLenient = true; ignoreUnknownKeys = true }

        private val ARTICLE = Regex("Article|BlogPosting|Report|Story", RegexOption.IGNORE_CASE)

        /** Every object in a block of schema.org data, its @graph's included. */
        private fun collect(element: JsonElement, into: MutableList<JsonObject>) {
            when (element) {
                is JsonObject -> {
                    into += element
                    element["@graph"]?.let { collect(it, into) }
                }
                is JsonArray -> element.forEach { collect(it, into) }
                else -> Unit
            }
        }

        private fun string(element: JsonElement?): String? = (element as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()?.ifEmpty { null }

        private fun strings(element: JsonElement): List<String> = when (element) {
            is JsonArray -> element.mapNotNull(::string)
            else -> listOfNotNull(string(element))
        }

        /** The name of a person or an organisation, given as a name, an object with one, or a list of them. */
        private fun name(element: JsonElement?): String? = when (element) {
            is JsonPrimitive -> string(element)
            is JsonObject -> string(element["name"])
            is JsonArray -> element.mapNotNull(::name).takeIf { it.isNotEmpty() }?.joinToString(", ")
            else -> null
        }

        private val SEPARATOR = Regex("\\s+[|\\-–—·»]+\\s+")

        /**
         * A page's title without the site's name it often ends in ("How to bake bread | Recipes"):
         * the part that names the site, by its name or its address, is dropped; otherwise, as
         * Readability does, the part before the last separator is kept when it is a title of its own.
         */
        internal fun cleanTitle(title: String, siteName: String?, host: String): String {
            val parts = title.split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size < 2) return title.trim()
            // The address's names but its ending: "wikipedia" in pt.wikipedia.org, which "Wikipédia, a enciclopédia livre" starts with.
            val labels = host.lowercase().split('.').dropLast(1).filter { it.length >= 3 && it != "www" }.toSet()
            val site = siteName?.let(::folded)
            val kept = parts.filterNot { part -> folded(part).let { name -> name == site || labels.any { name.startsWith(it) } } }
            if (kept.size in 1 until parts.size) return title.substring(title.indexOf(kept.first()), title.lastIndexOf(kept.last()) + kept.last().length).trim()
            // A short part after the last separator is the site's or a section's name, as long as what comes before is a title of its own.
            var cleaned = title.trim()
            while (true) {
                val last = SEPARATOR.findAll(cleaned).lastOrNull() ?: break
                val before = cleaned.substring(0, last.range.first).trim()
                if (words(before) < 3 || words(cleaned.substring(last.range.last + 1)) > 4) break
                cleaned = before
            }
            return cleaned
        }

        /** [text] lowercased without its spaces and the accents on its letters, to compare names by. */
        private fun folded(text: String): String = buildString {
            for (c in text.lowercase()) {
                if (c.isWhitespace()) continue
                append(ACCENTS[c] ?: c)
            }
        }

        private val ACCENTS: Map<Char, Char> = buildMap {
            for ((plain, marked) in listOf('a' to "áàâãäå", 'e' to "éèêë", 'i' to "íìîï", 'o' to "óòôõö", 'u' to "úùûü", 'c' to "ç", 'n' to "ñ", 'y' to "ýÿ")) {
                marked.forEach { put(it, plain) }
            }
        }

        private fun words(text: String) = text.split(Regex("\\s+")).count { it.isNotBlank() }
    }
}
