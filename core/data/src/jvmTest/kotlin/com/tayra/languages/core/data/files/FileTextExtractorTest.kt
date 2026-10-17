package com.tayra.languages.core.data.files

import kotlinx.coroutines.test.runTest
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.font.Standard14Fonts
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Each format the book form imports comes out as its paragraphs, a line each, with the title the file gives. */
class FileTextExtractorTest {

    private fun zip(vararg files: Pair<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((name, bytes) in files) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun extract(name: String, bytes: ByteArray) = kotlinx.coroutines.runBlocking { FileTextExtractor.extract(name, bytes) }

    @Test
    fun epubChaptersInSpineOrderWithoutRubyNotesOrCode() = runTest {
        val opf = """<?xml version="1.0" encoding="utf-8"?>
            <package xmlns="http://www.idpf.org/2007/opf"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>Hoshi no &amp; Ōji</dc:title></metadata>
            <manifest><item id="c2" href="Text/Chapter%202.xhtml" media-type="application/xhtml+xml"/><item id="c1" href="Text/one.xhtml"/><item id="n" href="notes.xhtml"/></manifest>
            <spine><itemref idref="c1"/><itemref idref="c2"/><itemref idref="n" linear="no"/></spine></package>"""
        val one = """<html><head><title>Ignored</title><style>p { color: red }</style></head><body>
            <h1>Chapter&nbsp;1</h1><p>A <ruby>漢字<rp>(</rp><rt>かんじ</rt><rp>)</rp></ruby> word<a epub:type="noteref" href="#n1">1</a> here.</p>
            <p>Line one<br/>line   two &mdash; &#x263A; &#233;</p><script>var x = "<p>not text</p>";</script></body></html>"""
        val two = "<html><body><div><p>Second chapter.</p></div><aside epub:type=\"footnote\">A note.</aside></body></html>"
        val epub = zip(
            "mimetype" to "application/epub+zip".toByteArray(),
            "META-INF/container.xml" to """<container><rootfiles><rootfile full-path="OEBPS/content.opf"/></rootfiles></container>""".toByteArray(),
            "OEBPS/content.opf" to opf.toByteArray(),
            "OEBPS/Text/one.xhtml" to one.toByteArray(),
            "OEBPS/Text/Chapter 2.xhtml" to two.toByteArray(),
            "OEBPS/notes.xhtml" to "<p>Notes</p>".toByteArray(),
        )
        val book = FileTextExtractor.extract("book.epub", epub)
        assertEquals("Chapter 1\nA 漢字 word here.\nLine one\nline two — ☺ é\n\nSecond chapter.", book.text)
        assertEquals("Hoshi no & Ōji", book.title)
    }

    @Test
    fun fb2InWindows1251WithoutNotesOrPictures() = runTest {
        val fb2 = """<?xml version="1.0" encoding="windows-1251"?>
            <FictionBook xmlns:l="http://www.w3.org/1999/xlink"><description><title-info><book-title>Мастер и Маргарита</book-title></title-info></description>
            <body><title><p>Глава первая</p></title><section><p>Никогда не разговаривайте<a l:href="#n1" type="note">[1]</a> с неизвестными.</p>
            <empty-line/><poem><stanza><v>Строка один</v><v>Строка два</v></stanza></poem></section></body>
            <body name="notes"><section id="n1"><p>Примечание.</p></section></body><binary id="cover.jpg">AAAA</binary></FictionBook>"""
        val bytes = fb2.toByteArray(Charset.forName("windows-1251"))
        val book = FileTextExtractor.extract("master.fb2", bytes)
        assertEquals("Глава первая\n\nНикогда не разговаривайте с неизвестными.\n\nСтрока один\nСтрока два", book.text)
        assertEquals("Мастер и Маргарита", book.title)

        // Downloaded zipped, as "book.fb2.zip" or .fbz.
        assertEquals(book, FileTextExtractor.extract("master.fb2.zip", zip("master.fb2" to bytes)))
        assertEquals(book, FileTextExtractor.extract("master.fbz", zip("master.fb2" to bytes)))
    }

    @Test
    fun docxParagraphsWithoutDeletionsOrFieldCodes() = runTest {
        val document = """<w:document xmlns:w="w"><w:body>
            <w:p><w:r><w:t>Hola</w:t></w:r><w:r><w:t xml:space="preserve"> mundo.</w:t></w:r><w:del><w:r><w:delText>borrado</w:delText></w:r></w:del></w:p>
            <w:p><w:r><w:instrText>PAGE \* MERGEFORMAT</w:instrText></w:r><w:r><w:t>Uno</w:t><w:tab/><w:t>dos</w:t><w:br/><w:t>tres</w:t></w:r></w:p>
            <w:tbl><w:tr><w:tc><w:p><w:r><w:t>celda</w:t></w:r></w:p></w:tc></w:tr></w:tbl></w:body></w:document>"""
        val core = """<cp:coreProperties xmlns:dc="dc"><dc:title>Mi documento</dc:title></cp:coreProperties>"""
        val doc = FileTextExtractor.extract("doc.docx", zip("word/document.xml" to document.toByteArray(), "docProps/core.xml" to core.toByteArray()))
        assertEquals("Hola mundo.\nUno dos\ntres\ncelda", doc.text)
        assertEquals("Mi documento", doc.title)
    }

    @Test
    fun odtParagraphsAndHeadingsWithoutNotes() = runTest {
        val content = """<office:document-content><office:body><office:text>
            <text:h text:outline-level="1">Titel</text:h>
            <text:p>Ein<text:s text:c="3"/>Satz<text:note><text:note-citation>1</text:note-citation><text:note-body><text:p>Fußnote</text:p></text:note-body></text:note>.<text:line-break/>Zweite Zeile</text:p>
            </office:text></office:body></office:document-content>"""
        val meta = """<office:document-meta><office:meta><dc:title>Ein Dokument</dc:title></office:meta></office:document-meta>"""
        val doc = FileTextExtractor.extract("doc.odt", zip("content.xml" to content.toByteArray(), "meta.xml" to meta.toByteArray()))
        assertEquals("Titel\nEin Satz.\nZweite Zeile", doc.text)
        assertEquals("Ein Dokument", doc.title)
    }

    @Test
    fun rtfTextFromCodePageBytesAndUnicodeEscapes() = runTest {
        val rtf = ("{\\rtf1\\ansi\\ansicpg1251\\deff0{\\fonttbl{\\f0 Times New Roman;}}{\\colortbl;\\red0\\green0\\blue0;}\n" +
            "{\\*\\generator Riched20;}{\\info{\\title Заголовок}}\\f0\\fs24 \\'cf\\'f0\\'e8\\'e2\\'e5\\'f2 \\u1076?\\u1088?\\u1091?\\u1075? \\emdash  " +
            "{\\field{\\*\\fldinst HYPERLINK \"x\"}{\\fldrslt ссылка}}\\par\nSecond\\tab line\\par}").toByteArray(Charset.forName("windows-1251"))
        assertEquals("Привет друг — ссылка\nSecond line", FileTextExtractor.extract("doc.rtf", rtf).text)
    }

    @Test
    fun mhtmlHtmlPartDecodedFromQuotedPrintable() = runTest {
        val mhtml = "From: <Saved by Blink>\r\nSubject: Saved page\r\nMIME-Version: 1.0\r\nContent-Type: multipart/related;\r\n\ttype=\"text/html\";\r\n\tboundary=\"----MultipartBoundary--abc\"\r\n\r\n" +
            "------MultipartBoundary--abc\r\nContent-Type: text/html\r\nContent-Transfer-Encoding: quoted-printable\r\n\r\n" +
            "<html><head><meta charset=3D\"utf-8\"><title>=D0=A1=D1=82=D0=B0=D1=82=D1=8C=D1=8F</title></head><body><p>=D0=9F=D1=80=D0=B8=\r\n=D0=B2=D0=B5=D1=82, =D0=BC=D0=B8=D1=80.</p></body></html>\r\n" +
            "------MultipartBoundary--abc\r\nContent-Type: image/png\r\nContent-Transfer-Encoding: base64\r\n\r\niVBORw0KGgo=\r\n------MultipartBoundary--abc--\r\n"
        val page = FileTextExtractor.extract("page.mhtml", mhtml.toByteArray())
        assertEquals("Привет, мир.", page.text)
        assertEquals("Статья", page.title)
    }

    @Test
    fun htmlInTheCharsetItsMetaNames() = runTest {
        val html = """<html><head><meta charset="windows-1251"><title>Новости</title></head><body><table><tr><td>Один</td><td>два</td></tr></table><p>Текст.</p></body></html>"""
        val page = FileTextExtractor.extract("page.htm", html.toByteArray(Charset.forName("windows-1251")))
        assertEquals("Один два\nТекст.", page.text)
        assertEquals("Новости", page.title)
    }

    @Test
    fun aDoctypeWithItsOwnDeclarationsLeavesNoTrace() = runTest {
        val html = """<?xml version="1.0"?><!DOCTYPE html [ <!ENTITY nbsp "&#160;"> <!ENTITY copy "&#169;"> ]><html><body><p>Bora sabe escribir.</p></body></html>"""
        assertEquals("Bora sabe escribir.", FileTextExtractor.extract("page.xhtml", html.toByteArray()).text)
    }

    @Test
    fun markdownProseWithoutMarkup() = runTest {
        val md = """---
            |title: Notes
            |---
            |# El *viaje*
            |
            |Un párrafo que sigue
            |en otra línea, con [un enlace](https://example.com) y `código`.
            |Una variable_con_guiones queda **igual**.
            |
            |![foto](a.png)
            |
            |- primero
            |- segundo
            |
            |```kotlin
            |val x = 1
            |```
            |
            || Palabra | Sentido |
            ||---------|---------|
            || casa    | house   |
            |
            |> Una cita.
            |""".trimMargin()
        assertEquals(
            "El viaje\nUn párrafo que sigue en otra línea, con un enlace y código. Una variable_con_guiones queda igual.\nprimero\nsegundo\nPalabra Sentido\ncasa house\nUna cita.",
            FileTextExtractor.extract("notes.md", md.toByteArray()).text,
        )
    }

    @Test
    fun mobiRecordsWithoutTheirTrailingEntries() = runTest {
        val html = "<html><body><p>Ein Buch.</p><mbp:pagebreak/><p>Zweite Seite – schön.</p></body></html>".toByteArray()
        // One multibyte byte (0x00: none carried over) and one 2-byte trailing entry follow the text.
        val record1 = html + byteArrayOf(0x00, 0x41, 0x82.toByte())
        val mobi = palmDatabase(listOf(mobiHeader(html.size, recordCount = 1, encoding = 65001, title = "Ein Buch", trailingFlags = 0b11), record1))
        val book = FileTextExtractor.extract("buch.azw3", mobi)
        assertEquals("Ein Buch.\nZweite Seite – schön.", book.text)
        assertEquals("Ein Buch", book.title)

        val drm = palmDatabase(listOf(mobiHeader(html.size, recordCount = 1, encoding = 65001, title = "x", trailingFlags = 0, encryption = 2), html))
        assertEquals("This book is protected by DRM and cannot be imported", assertFailsWith<FileImportException> { extract("x.azw", drm) }.message)
    }

    @Test
    fun palmDocBackReferencesAndSpacePrefixedLetters() {
        val data = byteArrayOf(0x61, 0x62, 0x63, 0x80.toByte(), 0x1B, 0xC1.toByte())
        assertEquals("abcabcabc A", MobiText.palmDocInflate(data).decodeToString())
    }

    @Test
    fun pdfLinesJoinedIntoParagraphs() = runTest {
        val out = ByteArrayOutputStream()
        PDDocument().use { document ->
            val page = PDPage()
            document.addPage(page)
            document.documentInformation.title = "Ein PDF"
            PDPageContentStream(document, page).use { stream ->
                stream.beginText()
                stream.setFont(PDType1Font(Standard14Fonts.FontName.HELVETICA), 12f)
                stream.setLeading(14f)
                stream.newLineAtOffset(50f, 700f)
                for (line in listOf("Dies ist ein langer Absatz, der auf mehrere", "Zeilen verteilt ist und ein Wort mit Binde-", "strich am Ende hat, bis er endlich endet.", "", "Ein zweiter Absatz.")) {
                    stream.showText(line)
                    stream.newLine()
                }
                stream.endText()
            }
            document.save(out)
        }
        val pdf = FileTextExtractor.extract("doc.pdf", out.toByteArray())
        assertEquals("Dies ist ein langer Absatz, der auf mehrere Zeilen verteilt ist und ein Wort mit Bindestrich am Ende hat, bis er endlich endet.\nEin zweiter Absatz.", pdf.text)
        assertEquals("Ein PDF", pdf.title)
    }

    /** Chinese, Japanese and Thai have no spaces between words, so a wrapped line joins without one. */
    @Test
    fun linesWrappedInScriptsWithoutSpacesJoinWithoutOne() = runTest {
        assertEquals("我们是学生，他们也是学生。", FileTextExtractor.extract("a.html", "<p>我们是学生，\n他们也是学生。</p>".toByteArray()).text)
        assertEquals("日本語の文章です。 Next words", FileTextExtractor.extract("a.md", "日本語の文\n章です。\nNext\nwords".toByteArray()).text)
        assertEquals("ฉันชอบกินข้าว", LineReflow.paragraphs("ฉันชอบ\nกินข้าว"))
        assertEquals("Hello world", LineReflow.paragraphs("Hello\nworld"))
    }

    @Test
    fun wrongFilesAreRefusedWithAReason() {
        assertTrue(assertFailsWith<FileImportException> { extract("book.epub", "not a zip".toByteArray()) }.message!!.contains("not a valid EPUB file"))
        assertTrue(assertFailsWith<FileImportException> { extract("doc.pdf", "nope".toByteArray()) }.message!!.contains("not a valid PDF file"))
        assertEquals("The zip holds no file Tayra can read", assertFailsWith<FileImportException> { extract("a.zip", zip("a.exe" to byteArrayOf(1))) }.message)
        assertEquals("x.txt is not utf-8 encoding, please convert it to utf-8 first", assertFailsWith<FileImportException> { extract("x.txt", byteArrayOf(0xC3.toByte(), 0x28)) }.message)
    }

    /** A Palm database of [records], record 0 first. */
    private fun palmDatabase(records: List<ByteArray>): ByteArray {
        val header = ByteArray(78 + 8 * records.size + 2)
        "BOOKMOBI".encodeToByteArray().copyInto(header, 60)
        header[76] = (records.size shr 8).toByte(); header[77] = records.size.toByte()
        var offset = header.size
        records.forEachIndexed { i, record ->
            val at = 78 + 8 * i
            header[at] = (offset shr 24).toByte(); header[at + 1] = (offset shr 16).toByte(); header[at + 2] = (offset shr 8).toByte(); header[at + 3] = offset.toByte()
            offset += record.size
        }
        return records.fold(header) { acc, r -> acc + r }
    }

    private fun mobiHeader(textLength: Int, recordCount: Int, encoding: Int, title: String, trailingFlags: Int, encryption: Int = 0): ByteArray {
        val name = title.encodeToByteArray()
        val header = ByteArray(0x100 + name.size)
        fun u16(at: Int, v: Int) { header[at] = (v shr 8).toByte(); header[at + 1] = v.toByte() }
        fun u32(at: Int, v: Int) { u16(at, v shr 16); u16(at + 2, v and 0xFFFF) }
        u16(0, 1)
        u32(4, textLength)
        u16(8, recordCount)
        u16(10, 4096)
        u16(12, encryption)
        "MOBI".encodeToByteArray().copyInto(header, 16)
        u32(20, 0xE8)
        u32(28, encoding)
        u32(84, 0x100)
        u32(88, name.size)
        u16(0xF2, trailingFlags)
        name.copyInto(header, 0x100)
        return header
    }
}
