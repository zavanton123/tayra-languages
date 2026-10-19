package com.tayra.languages.core.data.web

import com.tayra.languages.core.data.files.FileImportException
import com.tayra.languages.core.data.files.FileTextExtractor
import com.tayra.languages.core.data.files.TextDecoding
import com.tayra.languages.core.data.news.ArticleText
import com.tayra.languages.core.domain.web.WebArticle
import com.tayra.languages.core.domain.web.WebPageException
import com.tayra.languages.core.domain.web.WebPageReader
import io.ktor.client.HttpClient
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/** Whether this platform can fetch any web page: a browser may only fetch the sites that allow it. */
internal expect val webPagesSupported: Boolean

/**
 * Reads a web page's article for a book ([ArticleText.page]), with the title, language and author
 * the page gives ([PageMeta]); a link to a document (an e-book, a PDF, a text file) is read as the
 * file it is.
 */
class HttpWebPageReader(private val client: HttpClient) : WebPageReader {

    override suspend fun read(address: String): WebArticle {
        val url = address(address) ?: throw WebPageException.InvalidAddress(address)
        val response = try {
            client.get(url) {
                header(HttpHeaders.UserAgent, USER_AGENT)
                header(HttpHeaders.Accept, "text/html,application/xhtml+xml,*/*;q=0.8")
                timeout { requestTimeoutMillis = TIMEOUT; socketTimeoutMillis = TIMEOUT }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw WebPageException.Unreachable(null, e)
        }
        if (!response.status.isSuccess()) throw WebPageException.Unreachable(response.status.value)
        val bytes = response.bodyAsBytes()
        val type = response.contentType()
        // Where the page ended up, after any redirects.
        val final = response.request.url
        if (isPage(type, bytes)) {
            val html = decode(bytes, type?.parameter("charset"))
            val meta = PageMeta.read(html, final.host)
            val lines = ArticleText.page(html).lines()
            // A page often repeats its title as the article's first heading.
            val text = (if (lines.firstOrNull()?.equals(meta.title, ignoreCase = true) == true) lines.drop(1) else lines).joinToString("\n")
            if (ArticleText.letters(text) < MIN_LETTERS) throw WebPageException.NoArticle()
            return WebArticle(final.toString(), text, meta.title, meta.languageCode, meta.author, meta.siteName)
        }
        val name = fileName(final, type) ?: throw WebPageException.NoArticle()
        val extracted = try {
            FileTextExtractor.extract(name, bytes)
        } catch (e: FileImportException) {
            throw WebPageException.NoArticle()
        }
        return WebArticle(final.toString(), extracted.text, extracted.title ?: name.substringBeforeLast('.'))
    }

    companion object {
        /** Sites are asked as a browser asks, with a way to reach the app's makers. */
        const val USER_AGENT = "Mozilla/5.0 (compatible; TayraLanguages/1.0; +https://github.com/zavanton123/tayra-languages)"
        private const val TIMEOUT = 30_000L

        /** An article shorter than this, in letters, is not one: an error page, a page of links. */
        private const val MIN_LETTERS = 300

        /** [text] as an http or https address: one typed without its scheme ("example.com/article") is taken to be https. */
        internal fun address(text: String): String? {
            val trimmed = text.trim()
            if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return null
            val withScheme = if ("://" in trimmed) trimmed else "https://$trimmed"
            val url = runCatching { Url(withScheme) }.getOrNull() ?: return null
            if (url.protocol.name != "http" && url.protocol.name != "https") return null
            if ('.' !in url.host && url.host != "localhost") return null
            return withScheme
        }

        private val DOCUMENTS = mapOf(
            "application/pdf" to "pdf", "application/epub+zip" to "epub", "text/plain" to "txt", "text/markdown" to "md",
            "application/x-fictionbook+xml" to "fb2", "application/rtf" to "rtf", "text/rtf" to "rtf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to "docx",
            "application/vnd.oasis.opendocument.text" to "odt", "application/x-mobipocket-ebook" to "mobi", "application/zip" to "zip",
        )

        private fun isPage(type: ContentType?, bytes: ByteArray): Boolean {
            if (type != null && (type.match(ContentType.Text.Html) || type.contentSubtype == "xhtml+xml")) return true
            // A site that names no type, or the wrong one, still starts its page as one.
            val start = bytes.copyOfRange(0, minOf(bytes.size, 512)).decodeToString().trimStart().lowercase()
            return (type == null || type.match(ContentType.Text.Plain) || type.match(ContentType.Application.OctetStream)) &&
                (start.startsWith("<!doctype html") || start.startsWith("<html"))
        }

        /** The name to read a linked document by: its own when it has a known extension, else one its type gives. */
        private fun fileName(url: Url, type: ContentType?): String? {
            val last = url.segments.lastOrNull()?.takeIf { it.isNotBlank() } ?: url.host
            val extension = last.substringAfterLast('.', "").lowercase()
            if (extension in FileTextExtractor.supportedExtensions) return last
            val byType = type?.let { DOCUMENTS["${it.contentType}/${it.contentSubtype}".lowercase()] } ?: return null
            return "${last.substringBeforeLast('.')}.$byType"
        }

        /** A page's text in the charset its header or its own `<meta>` names, UTF-8 when neither does. */
        private fun decode(bytes: ByteArray, charset: String?): String {
            if (charset != null) return TextDecoding.decode(bytes, charset)
            if (TextDecoding.isUtf(bytes)) return TextDecoding.utf(bytes)
            val head = bytes.copyOfRange(0, minOf(bytes.size, 4096)).joinToString("") { (it.toInt() and 0xFF).toChar().toString() }
            val named = Regex("""(?i)<meta[^>]+charset\s*=\s*["']?([\w-]+)""").find(head)?.groupValues?.get(1)
            return TextDecoding.decode(bytes, named ?: "windows-1252")
        }
    }
}
