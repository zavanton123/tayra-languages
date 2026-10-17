package com.tayra.languages.core.data.news

import com.tayra.languages.core.data.files.HtmlText
import com.tayra.languages.core.data.files.Markup
import com.tayra.languages.core.data.files.MarkupToken
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * The text of a news article's page, one paragraph per line, without the menus, links, captions
 * and teasers around it. Two readings are tried: the paragraphs of the page, gathered under the
 * element that holds most of their text (the way Readability finds an article), and the whole text
 * many news sites give search engines in schema.org data (`articleBody`); the longer one wins, as a
 * page can show only part of its text.
 */
internal object ArticleText {

    fun extract(html: String): String {
        val shown = paragraphs(html)
        val given = structured(html)
        return if (given != null && letters(given) > letters(shown) * 13 / 10) given else shown
    }

    /** Elements whose text is never the article's. */
    private val SKIPPED = setOf(
        "script", "style", "noscript", "nav", "aside", "footer", "header", "form", "button", "figure", "figcaption",
        "svg", "select", "template", "iframe", "menu", "dialog", "label",
    )

    /** Elements the article's text comes in. */
    private val PARAGRAPHS = setOf("p", "h2", "h3", "blockquote")

    /** Elements that end a paragraph whose closing tag was left out, as HTML allows. */
    private val BLOCKS = setOf("p", "div", "section", "article", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "table", "blockquote", "main")
    private val VOID = setOf("br", "img", "hr", "input", "meta", "link", "source", "wbr", "col", "area", "base", "embed", "track", "param")

    private class Element(val id: Int, val name: String)
    private class Paragraph(val text: String, val ancestors: List<Int>)

    private fun paragraphs(html: String): String {
        val stack = ArrayDeque<Element>()
        var nextId = 0
        var skipping = 0
        val found = mutableListOf<Paragraph>()
        var open: StringBuilder? = null
        var openAncestors: List<Int> = emptyList()
        var openId = -1

        fun finish() {
            val text = open?.toString()?.replace(Regex("\\s+"), " ")?.trim()
            if (!text.isNullOrEmpty()) found += Paragraph(text, openAncestors)
            open = null
        }

        for (token in Markup.tokens(html)) {
            when (token) {
                is MarkupToken.Open -> {
                    if (token.name in BLOCKS && open != null) {
                        finish()
                        // A block closes the paragraph left open, as browsers do: it does not nest inside it.
                        if (stack.any { it.id == openId }) while (stack.removeLast().id != openId) Unit
                    }
                    if (token.selfClosing || token.name in VOID) {
                        if (token.name == "br" && open != null) open?.append(' ')
                        continue
                    }
                    val element = Element(nextId++, token.name)
                    // Class names are no guide: sites lay out their articles in a "sidebar" or "teaser" as readily as anything else.
                    if (skipping > 0 || token.name in SKIPPED) skipping++
                    stack.addLast(element)
                    if (skipping == 0 && token.name in PARAGRAPHS) {
                        open = StringBuilder()
                        openId = element.id
                        openAncestors = stack.dropLast(1).takeLast(ANCESTORS).map { it.id }.reversed()
                    }
                }
                is MarkupToken.Text -> if (skipping == 0) open?.append(token.text)
                is MarkupToken.Close -> {
                    if (stack.none { it.name == token.name }) continue
                    while (stack.isNotEmpty()) {
                        val element = stack.removeLast()
                        if (skipping > 0) skipping--
                        if (element.name in PARAGRAPHS && open != null) finish()
                        if (element.name == token.name) break
                    }
                }
            }
        }
        finish()

        // Each paragraph adds its length to its parent, half to its grandparent, a third to the next.
        val scores = HashMap<Int, Int>()
        for (paragraph in found) {
            if (paragraph.text.length < SCORED_LENGTH) continue
            paragraph.ancestors.forEachIndexed { level, id -> scores[id] = (scores[id] ?: 0) + paragraph.text.length / (level + 1) }
        }
        val best = scores.maxByOrNull { it.value }?.key ?: return ""
        return found.filter { best in it.ancestors && (it.text.length >= SCORED_LENGTH || it.text.last() in ENDINGS) }
            .joinToString("\n") { it.text }
    }

    /** The longest `articleBody` in the page's schema.org data, if any. */
    private fun structured(html: String): String? {
        val json = Json { isLenient = true; ignoreUnknownKeys = true }
        val bodies = mutableListOf<String>()
        fun walk(element: JsonElement) {
            when (element) {
                is JsonObject -> element.forEach { (key, value) ->
                    if (key == "articleBody" && value is JsonPrimitive && value.isString) bodies += value.content else walk(value)
                }
                is JsonArray -> element.forEach(::walk)
                else -> Unit
            }
        }
        var inData = false
        for (token in Markup.tokens(html)) {
            when (token) {
                is MarkupToken.Open -> inData = token.name == "script" && token.attributes["type"]?.contains("ld+json") == true
                is MarkupToken.Text -> if (inData) runCatching { walk(json.parseToJsonElement(token.text)) }
                is MarkupToken.Close -> inData = false
            }
        }
        val body = bodies.maxByOrNull { it.length } ?: return null
        val text = (if ('<' in body) HtmlText.text(body) else body).lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
        return text.takeIf { it.isNotEmpty() }
    }

    /** How long a text is, in letters: words are no measure in the scripts written without spaces. */
    internal fun letters(text: String): Int = text.count { it.isLetterOrDigit() }

    private const val ANCESTORS = 3
    private const val SCORED_LENGTH = 40
    private val ENDINGS = setOf('.', '!', '?', '…', '。', '！', '？', ':', '"', '»', '”')
}
