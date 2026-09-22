package com.tayra.languages.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

class WebImportException(message: String, cause: Throwable? = null) : Exception(message, cause)

data class WebPageContent(val title: String, val sourceUrl: String, val text: String)

/**
 * Fetches a web page and extracts its headings and paragraphs as plain text.
 */
class WebPageImporter(private val client: HttpClient) {

    suspend fun import(url: String): WebPageContent {
        val html = try {
            val response = client.get(url)
            if (!response.status.isSuccess()) throw WebImportException("Could not load $url (${response.status})")
            response.bodyAsText()
        } catch (e: WebImportException) {
            throw e
        } catch (e: Exception) {
            throw WebImportException("Could not load $url (${e.message ?: e::class.simpleName})", e)
        }
        return WebPageContent(
            title = HtmlText.title(html)?.take(150) ?: url,
            sourceUrl = url,
            text = HtmlText.paragraphs(html).joinToString("\n\n"),
        )
    }
}

/** Minimal HTML text extraction without a full parser. */
object HtmlText {
    private val blockTags = Regex("<(h[1-4]|p)\\b[^>]*>(.*?)</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val titleTag = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val scriptsAndStyles = Regex("<(script|style)\\b[^>]*>.*?</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val tags = Regex("<[^>]+>")
    private val whitespace = Regex("\\s+")

    fun title(html: String): String? = titleTag.find(html)?.groupValues?.get(1)?.let { clean(it) }?.takeIf { it.isNotEmpty() }

    fun paragraphs(html: String): List<String> {
        val stripped = scriptsAndStyles.replace(html, "")
        return blockTags.findAll(stripped).map { clean(it.groupValues[2]) }.filter { it.isNotEmpty() }.toList()
    }

    private fun clean(fragment: String): String = decodeEntities(tags.replace(fragment, " ")).replace(whitespace, " ").trim()

    private fun decodeEntities(text: String): String = text
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&apos;", "'")
        .replace(Regex("&#(\\d+);")) { m -> m.groupValues[1].toIntOrNull()?.let { codePointToString(it) } ?: m.value }
        .replace(Regex("&#x([0-9a-fA-F]+);")) { m -> m.groupValues[1].toIntOrNull(16)?.let { codePointToString(it) } ?: m.value }

    private fun codePointToString(codePoint: Int): String {
        if (codePoint < 0x10000) return codePoint.toChar().toString()
        val offset = codePoint - 0x10000
        val high = (0xD800 + (offset shr 10)).toChar()
        val low = (0xDC00 + (offset and 0x3FF)).toChar()
        return "$high$low"
    }
}
