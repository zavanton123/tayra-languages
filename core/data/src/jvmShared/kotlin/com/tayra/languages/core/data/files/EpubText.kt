package com.tayra.languages.core.data.files

import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

actual fun extractEpubText(bytes: ByteArray): String = EpubReader.text(bytes)

/** Reads an epub (zip of xhtml files) and returns its text in spine order. */
internal object EpubReader {

    fun text(bytes: ByteArray): String {
        val entries = readEntries(bytes)
        val opfPath = containerOpfPath(entries) ?: throw FileImportException("Could not parse epub: missing package file")
        val opf = entries[opfPath] ?: throw FileImportException("Could not parse epub: missing $opfPath")
        val base = opfPath.substringBeforeLast('/', "").let { if (it.isEmpty()) "" else "$it/" }

        val manifest = Regex("<item\\b[^>]*>", RegexOption.IGNORE_CASE).findAll(opf).mapNotNull { item ->
            val id = attr(item.value, "id") ?: return@mapNotNull null
            val href = attr(item.value, "href") ?: return@mapNotNull null
            id to base + href
        }.toMap()
        val spine = Regex("<itemref\\b[^>]*>", RegexOption.IGNORE_CASE).findAll(opf).mapNotNull { attr(it.value, "idref") }.toList()

        val documents = spine.mapNotNull { manifest[it] }.mapNotNull { entries[it] }
        if (documents.isEmpty()) throw FileImportException("Could not parse epub: no readable content")
        return documents.joinToString("\n\n") { htmlToText(it) }.trim()
    }

    private fun readEntries(bytes: ByteArray): Map<String, String> {
        val entries = HashMap<String, String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val name = entry.name.lowercase()
                    if (name.endsWith(".xml") || name.endsWith(".opf") || name.endsWith(".xhtml") || name.endsWith(".html") || name.endsWith(".htm")) {
                        entries[entry.name] = zip.readBytes().decodeToString()
                    }
                }
                entry = zip.nextEntry
            }
        }
        if (entries.isEmpty()) throw FileImportException("Could not parse epub: not a valid epub file")
        return entries
    }

    private fun containerOpfPath(entries: Map<String, String>): String? {
        val container = entries["META-INF/container.xml"] ?: return entries.keys.firstOrNull { it.endsWith(".opf") }
        return Regex("<rootfile\\b[^>]*>", RegexOption.IGNORE_CASE).find(container)?.let { attr(it.value, "full-path") }
    }

    private fun attr(tag: String, name: String): String? =
        Regex("\\b$name\\s*=\\s*[\"']([^\"']*)[\"']", RegexOption.IGNORE_CASE).find(tag)?.groupValues?.get(1)

    private val blockEnd = Regex("</(p|div|h[1-6]|li|tr|br|title)\\s*>|<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val head = Regex("<head\\b.*?</head>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val tags = Regex("<[^>]+>")

    private fun htmlToText(html: String): String {
        val body = head.replace(html, "")
        val withBreaks = blockEnd.replace(body, "\n")
        return tags.replace(withBreaks, "")
            .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'")
            .lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
    }
}
