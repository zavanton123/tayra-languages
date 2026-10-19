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
        val shown = paragraphs(html, NEWS)
        val given = structured(html)
        return if (given != null && letters(given) > letters(shown) * 13 / 10) given else shown
    }

    /**
     * The article of any web page, read as Firefox's Reader View reads one: hidden elements, and
     * those whose class or id name a comment, a share bar, an ad or a related list, are left out;
     * text that is mostly links is dropped; text loose in a block and list items count as
     * paragraphs; and a container named for an article weighs more. A page whose names mislead
     * those rules is read again without the names.
     */
    fun page(html: String): String {
        val strict = paragraphs(html, PAGE)
        val shown = if (letters(strict) >= RETRY_LETTERS) strict else paragraphs(html, PAGE_UNNAMED).takeIf { letters(it) > letters(strict) } ?: strict
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

    /** Elements within a line of text: they do not end the text loose in a block. */
    private val INLINE = setOf(
        "a", "span", "b", "i", "em", "strong", "u", "small", "sup", "sub", "mark", "abbr", "cite", "q", "time", "code", "font",
        "s", "del", "ins", "bdi", "bdo", "kbd", "var", "dfn", "ruby", "rt", "rp", "img", "wbr",
    )

    /*
     * The names that tell a page's parts apart follow Mozilla Readability's (Apache License 2.0),
     * the library behind Firefox's Reader View.
     */
    private val UNLIKELY = Regex(
        "-ad-|ai2html|banner|breadcrumbs|combx|comment|community|cover-wrap|disqus|extra|footer|gdpr|header|legends|menu|related|remark|" +
            "replies|rss|shoutbox|sidebar|skyscraper|social|sponsor|supplemental|ad-break|agegate|pagination|pager|popup|yom-remote|" +
            "cookie|consent|newsletter|subscribe|share|promo|recommend|teaser|paywall",
        RegexOption.IGNORE_CASE,
    )
    private val MAYBE = Regex("and|article|body|column|content|main|mathjax|shadow", RegexOption.IGNORE_CASE)
    private val POSITIVE = Regex("article|body|content|entry|hentry|h-entry|main|page|post|text|blog|story", RegexOption.IGNORE_CASE)
    private val NEGATIVE = Regex(
        "-ad-|hidden|^hid$| hid$| hid |^hid |banner|combx|comment|com-|contact|footer|gdpr|masthead|media|meta|outbrain|promo|related|" +
            "scroll|share|shoutbox|sidebar|skyscraper|sponsor|shopping|tags|widget",
        RegexOption.IGNORE_CASE,
    )
    private val UNLIKELY_ROLES = setOf("menu", "menubar", "complementary", "navigation", "alert", "alertdialog", "dialog", "banner", "contentinfo")

    /** Elements never left out for their names: the page itself and the article's own containers. */
    private val KEPT = setOf("html", "body", "article", "main")

    /**
     * How paragraphs are found. The news reader's sites lay out their articles in a "sidebar" or
     * "teaser" as readily as anything else, so it reads no names; a page in general is read by them.
     */
    private class Rules(
        val paragraphs: Set<String>,
        /** The paragraphs a block closes when it opens inside them, as browsers do; others can hold blocks. */
        val closedByBlocks: Set<String>,
        val names: Boolean,
        val page: Boolean,
    )

    private val NEWS = Rules(PARAGRAPHS, PARAGRAPHS, names = false, page = false)
    private val PAGE = Rules(PARAGRAPHS + setOf("h4", "li", "pre", "dd"), setOf("p", "h2", "h3", "h4"), names = true, page = true)
    private val PAGE_UNNAMED = Rules(PAGE.paragraphs, PAGE.closedByBlocks, names = false, page = true)

    private class Element(val id: Int, val name: String)
    private class Paragraph(val text: String, val ancestors: List<Int>, val name: String)

    /** Whether a page hides [token], or names it for something besides the article. */
    private fun leftOut(token: MarkupToken.Open, rules: Rules): Boolean {
        if (!rules.page) return false
        val attributes = token.attributes
        if ("hidden" in attributes || attributes["aria-hidden"] == "true") return true
        val style = attributes["style"]?.replace(" ", "")?.lowercase()
        if (style != null && ("display:none" in style || "visibility:hidden" in style)) return true
        if (attributes["role"] in UNLIKELY_ROLES) return true
        if (!rules.names || token.name in KEPT) return false
        val names = "${attributes["class"].orEmpty()} ${attributes["id"].orEmpty()}"
        return UNLIKELY.containsMatchIn(names) && !MAYBE.containsMatchIn(names)
    }

    /** How much more, or less, a container's paragraphs count toward it, by its tag and its names. */
    private fun weight(token: MarkupToken.Open, rules: Rules): Double {
        if (!rules.page) return 1.0
        var weight = if (token.name == "article" || token.name == "main") 1.25 else 1.0
        if (rules.names) {
            val names = "${token.attributes["class"].orEmpty()} ${token.attributes["id"].orEmpty()}"
            if (POSITIVE.containsMatchIn(names)) weight += 0.25
            if (NEGATIVE.containsMatchIn(names)) weight -= 0.25
        }
        return weight
    }

    private fun paragraphs(html: String, rules: Rules): String {
        val stack = ArrayDeque<Element>()
        var nextId = 0
        var skipping = 0
        val found = mutableListOf<Paragraph>()
        val weights = HashMap<Int, Double>()
        val parents = HashMap<Int, Int>()
        var open: StringBuilder? = null
        var openAncestors: List<Int> = emptyList()
        var openId = -1
        var openName = ""
        // On a page: text outside any paragraph, and how much of the text being gathered is in links.
        val loose = StringBuilder()
        var linkDepth = 0
        var linked = 0
        var looseLinked = 0

        fun keep(text: String, links: Int, ancestors: List<Int>, name: String) {
            val clean = text.replace(Regex("\\s+"), " ").trim()
            if (clean.isEmpty()) return
            // A line that is mostly links is a menu, a list of tags or of other articles.
            if (rules.page && links * 2 > letters(clean)) return
            found += Paragraph(clean, ancestors, name)
        }

        fun finish() {
            open?.let { keep(it.toString(), linked, openAncestors, openName) }
            open = null
            linked = 0
        }

        fun finishLoose() {
            if (loose.isNotEmpty()) keep(loose.toString(), looseLinked, stack.takeLast(ANCESTORS).map { it.id }.reversed(), "")
            loose.clear()
            looseLinked = 0
        }

        for (token in Markup.tokens(html)) {
            when (token) {
                is MarkupToken.Open -> {
                    if (rules.page && (token.name !in INLINE || token.name == "br" && open == null)) finishLoose()
                    if (token.name in BLOCKS && open != null) {
                        val closed = openName in rules.closedByBlocks
                        finish()
                        // A block closes the paragraph left open, as browsers do: it does not nest inside it.
                        if (closed && stack.any { it.id == openId }) while (stack.removeLast().id != openId) Unit
                    }
                    if (token.selfClosing || token.name in VOID) {
                        if (token.name == "br" && open != null) open?.append(' ')
                        continue
                    }
                    val element = Element(nextId++, token.name)
                    if (skipping > 0 || token.name in SKIPPED || leftOut(token, rules)) skipping++
                    stack.lastOrNull()?.let { parents[element.id] = it.id }
                    stack.addLast(element)
                    if (skipping == 0) {
                        weight(token, rules).takeIf { it != 1.0 }?.let { weights[element.id] = it }
                        if (token.name == "a") linkDepth++
                    }
                    if (skipping == 0 && token.name in rules.paragraphs) {
                        if (open != null) finish()
                        open = StringBuilder()
                        openId = element.id
                        openName = token.name
                        openAncestors = stack.dropLast(1).takeLast(ANCESTORS).map { it.id }.reversed()
                    }
                }
                is MarkupToken.Text -> if (skipping == 0) {
                    val links = if (linkDepth > 0) letters(token.text) else 0
                    val paragraph = open
                    if (paragraph != null) {
                        paragraph.append(token.text)
                        linked += links
                    } else if (rules.page) {
                        loose.append(token.text)
                        looseLinked += links
                    }
                }
                is MarkupToken.Close -> {
                    if (stack.none { it.name == token.name }) continue
                    if (rules.page && token.name !in INLINE) finishLoose()
                    while (stack.isNotEmpty()) {
                        val element = stack.removeLast()
                        if (skipping > 0) skipping-- else if (element.name == "a" && linkDepth > 0) linkDepth--
                        if (element.name in rules.paragraphs && open != null && element.id == openId) finish()
                        else if (element.name in PARAGRAPHS && open != null && !rules.page) finish()
                        if (element.name == token.name) break
                    }
                }
            }
        }
        finish()
        finishLoose()

        // Each paragraph adds its length to its parent, half to its grandparent, a third to the next.
        val scores = HashMap<Int, Double>()
        for (paragraph in found) {
            if (paragraph.text.length < SCORED_LENGTH) continue
            paragraph.ancestors.forEachIndexed { level, id ->
                val share = if (rules.page) paragraph.text.length.toDouble() / (level + 1) * (weights[id] ?: 1.0) else (paragraph.text.length / (level + 1)).toDouble()
                scores[id] = (scores[id] ?: 0.0) + share
            }
        }
        val (best, bestScore) = scores.maxByOrNull { it.value } ?: return ""
        // On a page, an article split into sections is taken whole: the containers beside the best one
        // that hold a good part of its text, and the long paragraphs between them, as Readability does.
        val parent = parents[best]
        val chosen = if (rules.page && parent != null) scores.filter { (id, score) -> parents[id] == parent && score >= bestScore * SIBLING_SHARE }.keys + best else setOf(best)
        val kept = found.filter { paragraph ->
            val inside = paragraph.ancestors.any { it in chosen } || rules.page && paragraph.ancestors.firstOrNull() == parent && paragraph.text.length >= SIBLING_PARAGRAPH
            inside && (paragraph.text.length >= SCORED_LENGTH || paragraph.text.last() in ENDINGS || rules.page && paragraph.name in OUTLINE)
        }.toMutableList()
        // A heading left at the end heads what was left out: the links, the references, the comments.
        if (rules.page) while (kept.lastOrNull()?.name in HEADINGS) kept.removeLast()
        return kept.joinToString("\n") { it.text }
    }

    /** On a page, the headings and list items kept however short: the article's outline and its lists. */
    private val OUTLINE = setOf("h2", "h3", "h4", "li", "dd")
    private val HEADINGS = setOf("h2", "h3", "h4")

    /** The longest `articleBody` in the page's schema.org data, if any. */
    internal fun structured(html: String): String? {
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

    /** On a page, how much of the best container's score a container beside it has to have to be taken too. */
    private const val SIBLING_SHARE = 0.2

    /** On a page, how long a paragraph between the article's containers has to be to be taken too. */
    private const val SIBLING_PARAGRAPH = 80

    /** A page read by its names to fewer letters than this is read again without them. */
    private const val RETRY_LETTERS = 500
    private val ENDINGS = setOf('.', '!', '?', '…', '。', '！', '？', ':', '"', '»', '”')
}
