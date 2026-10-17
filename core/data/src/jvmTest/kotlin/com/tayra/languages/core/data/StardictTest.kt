package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.SqliteQuery
import com.tayra.languages.core.data.db.readSqliteFile
import com.tayra.languages.core.data.mdict.DictionaryFiles
import com.tayra.languages.core.data.stardict.ArchiveFile
import com.tayra.languages.core.data.stardict.Archives
import com.tayra.languages.core.data.stardict.Tar
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** StarDict dictionaries: their files however they come, their entries in each layout, synonyms as forms, phonetics as IPA. */
class StardictTest {

    /** A StarDict dictionary's files: entries as (headword, data) with data already laid out, and synonyms. */
    private fun dictionary(
        entries: List<Pair<String, ByteArray>>,
        sameTypeSequence: String? = "h",
        synonyms: List<Pair<String, Int>> = emptyList(),
        offsetBits: Int = 32,
        gzipIdx: Boolean = false,
    ): List<ArchiveFile> {
        val dict = ByteArrayOutputStream()
        val idx = ByteArrayOutputStream()
        for ((word, data) in entries) {
            idx.write(word.toByteArray()); idx.write(0)
            val offset = dict.size().toLong()
            for (i in (offsetBits / 8 - 1) downTo 0) idx.write((offset ushr (8 * i)).toInt())
            for (i in 3 downTo 0) idx.write(data.size ushr (8 * i))
            dict.write(data)
        }
        val ifo = buildString {
            appendLine("StarDict's dict ifo file")
            appendLine("version=${if (offsetBits == 64) "3.0.0" else "2.4.2"}")
            appendLine("bookname=Portuguese-English Test")
            appendLine("wordcount=${entries.size}")
            appendLine("idxfilesize=${idx.size()}")
            if (offsetBits == 64) appendLine("idxoffsetbits=64")
            sameTypeSequence?.let { appendLine("sametypesequence=$it") }
            appendLine("description=Publisher: Test<br>Available under the <a href=\"https://www.gnu.org/licenses/gpl-2.0.html\">GPL</a>.")
        }
        val syn = ByteArrayOutputStream()
        for ((word, index) in synonyms) {
            syn.write(word.toByteArray()); syn.write(0)
            for (i in 3 downTo 0) syn.write(index ushr (8 * i))
        }
        return listOfNotNull(
            ArchiveFile("pt-en/pt-en.ifo", ifo.toByteArray()),
            if (gzipIdx) ArchiveFile("pt-en/pt-en.idx.gz", gzip(idx.toByteArray())) else ArchiveFile("pt-en/pt-en.idx", idx.toByteArray()),
            ArchiveFile("pt-en/pt-en.dict.dz", gzip(dict.toByteArray())),
            synonyms.takeIf { it.isNotEmpty() }?.let { ArchiveFile("pt-en/pt-en.syn", syn.toByteArray()) },
        )
    }

    private val html = listOf(
        "casa" to "<div><ol><li><div>house</div></li><li><div>home</div></li></ol></div>".toByteArray(),
        "gato" to "<b>gato</b><br>cat".toByteArray(),
    )

    private suspend fun pack(file: ByteArray): Triple<List<List<String?>>, List<List<String?>>, List<List<String?>>> {
        val (rows, forms, meta) = readSqliteFile(DictionaryFiles().convert(file, "pt", "en"), listOf(
            SqliteQuery("SELECT word, ipa, senses FROM entries ORDER BY id", 3),
            SqliteQuery("SELECT form_lc, lemma FROM forms", 2),
            SqliteQuery("SELECT value FROM meta WHERE key = 'sources'", 1),
        ))
        return Triple(rows, forms, meta)
    }

    @Test
    fun aTarGzArchiveOfHtmlEntriesWithSynonymsBecomesAPack() = runTest {
        val file = gzip(Tar.write(dictionary(html, synonyms = listOf("gatos" to 1, "Gato" to 1))))
        val info = DictionaryFiles().inspect(file)
        assertEquals("Portuguese-English Test", info.title)
        assertEquals("Publisher: Test Available under the GPL.", info.attribution)
        val (rows, forms, meta) = pack(file)
        assertEquals(listOf(
            listOf("casa", null, """[{"glosses":["house","home"]}]"""),
            listOf("gato", null, """[{"glosses":["cat"]}]"""),
        ), rows)
        assertEquals(listOf(listOf("gatos", "gato")), forms, "a synonym only differing in case is no form")
        assertEquals("Portuguese-English Test (imported StarDict dictionary)", meta.single()[0])
    }

    @Test
    fun aZipAndLooseFilesChosenTogetherAreReadAlike() = runTest {
        val files = dictionary(html, gzipIdx = true, offsetBits = 64)
        val zip = ByteArrayOutputStream().also { out -> ZipOutputStream(out).use { z -> files.forEach { z.putNextEntry(ZipEntry(it.name)); z.write(it.bytes); z.closeEntry() } } }.toByteArray()
        val loose = DictionaryFiles.bundle(files.map { it.name.substringAfterLast('/') to it.bytes })
        assertEquals(pack(zip).first, pack(loose).first)
        assertEquals("casa", pack(loose).first.first()[0])
    }

    @Test
    fun fieldsOfEveryLayoutAreReadAndPhoneticsBecomeIpa() = runTest {
        // A sequence of phonetics then plain text: the first field ends in a zero, the last runs to the end.
        val typed = listOf("casa" to "ˈka.zɐ\u0000house\nhome".toByteArray())
        val (rows) = pack(Tar.write(dictionary(typed, sameTypeSequence = "tm")))
        assertEquals(listOf("casa", "ˈka.zɐ", """[{"glosses":["house","home"]}]"""), rows.single())

        // No sequence: each field leads with its type, a text one ends in a zero, a binary one follows its size.
        val sound = byteArrayOf('W'.code.toByte(), 0, 0, 0, 3, 1, 2, 3)
        val led = listOf("gato" to sound + "m".toByteArray() + "cat\u0000".toByteArray() + "x".toByteArray() + "<k>gato</k>\n<dtrn>tomcat</dtrn>\u0000".toByteArray())
        val (others) = pack(Tar.write(dictionary(led, sameTypeSequence = null)))
        assertEquals("""[{"glosses":["cat","tomcat"]}]""", others.single()[2])
    }

    @Test
    fun whatCannotBeReadIsRefusedWithWhatToDo() = runTest {
        val xz = byteArrayOf(0xFD.toByte(), '7'.code.toByte(), 'z'.code.toByte(), 'X'.code.toByte(), 'Z'.code.toByte(), 0) + ByteArray(40)
        assertTrue("extract it and choose its files" in assertFailsWith<Exception> { DictionaryFiles().inspect(xz) }.message!!)
        assertTrue("no .ifo" in assertFailsWith<Exception> { DictionaryFiles().inspect(Tar.write(listOf(ArchiveFile("readme.txt", "hello".toByteArray())))) }.message!!)
        assertTrue("no .dict" in assertFailsWith<Exception> { DictionaryFiles().convert(Tar.write(dictionary(html).take(2)), "pt", "en") }.message!!)
    }

    @Test
    fun aTarArchiveIsWrittenAndReadBack() = runTest {
        val files = listOf(ArchiveFile("a.ifo", "x".toByteArray()), ArchiveFile("b.dict", ByteArray(1000) { it.toByte() }))
        val read = Archives.files(Tar.write(files))
        assertEquals(listOf("a.ifo", "b.dict"), read.map { it.name })
        assertTrue(read[1].bytes.contentEquals(files[1].bytes))
    }

    private fun gzip(data: ByteArray): ByteArray = ByteArrayOutputStream().also { out -> GZIPOutputStream(out).use { it.write(data) } }.toByteArray()
}
