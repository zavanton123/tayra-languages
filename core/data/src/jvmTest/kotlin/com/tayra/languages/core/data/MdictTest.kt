package com.tayra.languages.core.data

import com.tayra.languages.core.data.db.SqliteQuery
import com.tayra.languages.core.data.db.readSqliteFile
import com.tayra.languages.core.data.mdict.DictionaryFiles
import com.tayra.languages.core.data.mdict.Lzo
import com.tayra.languages.core.data.mdict.MdictConverter
import com.tayra.languages.core.data.mdict.MdictFile
import com.tayra.languages.core.data.mdict.Ripemd128
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** MDict dictionaries: their headers, keys and records in each version, compression, text encoding and the hidden index of keys, and their conversion to packs. */
class MdictTest {

    private val entries = listOf(
        "casa" to "<div class=\"hw\">casa</div><div class=\"def\"><ol><li>house, home</li><li>household</li></ol><p><i>a minha casa</i> — my house</p></div>",
        "gato" to "<b>gato</b><br>cat<br>tomcat",
        "gatos" to "@@@LINK=gato",
        "água" to "water",
    )

    private suspend fun read(file: ByteArray): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        MdictFile(file).read { key, record -> out += key to record }
        return out
    }

    @Test
    fun ripemd128MatchesItsTestVectors() {
        assertEquals("cdf26213a150dc3ecb610f18f6b38b46", Ripemd128.digest(ByteArray(0)).hex())
        assertEquals("c14a12199c66e4ba84636b0f69144c77", Ripemd128.digest("abc".toByteArray()).hex())
        assertEquals("fd2aa607f71dc8f510714922b371834e", Ripemd128.digest("abcdefghijklmnopqrstuvwxyz".toByteArray()).hex())
        assertEquals("3f45ef194732c2dbb2c4a2c769795fa3", Ripemd128.digest("1234567890".repeat(8).toByteArray()).hex())
    }

    @Test
    fun aVersion2DictionaryInZlibBlocksIsReadWholeAcrossBlocks() = runTest {
        val file = Mdx(entries, perBlock = 3).build()
        assertEquals(entries, read(file))
        val mdict = MdictFile(file)
        assertEquals("Português–English", mdict.title)
        assertEquals("A <b>test</b> dictionary", mdict.description)
    }

    @Test
    fun theHiddenIndexOfKeysIsRevealed() = runTest {
        assertEquals(entries, read(Mdx(entries, encrypted = 2).build()))
    }

    @Test
    fun utf16TextStoredAndLzoBlocksAndVersion1AreRead() = runTest {
        assertEquals(entries, read(Mdx(entries, encoding = "UTF-16").build()))
        assertEquals(entries, read(Mdx(entries, compression = 0).build()))
        assertEquals(entries, read(Mdx(entries, compression = 1, perBlock = 1).build()))
        assertEquals(entries, read(Mdx(entries, version = "1.2").build()))
    }

    @Test
    fun lockedAndUnreadableDictionariesAreRefusedWithAReason() = runTest {
        assertTrue("registered user" in assertFailsWith<Exception> { MdictFile(Mdx(entries, encrypted = 1).build()) }.message!!)
        assertTrue("GBK" in assertFailsWith<Exception> { MdictFile(Mdx(entries, encoding = "GBK").build()) }.message!!)
        assertTrue(".mdd" in assertFailsWith<Exception> { MdictFile(Mdx(entries, root = "Library_Data").build()) }.message!!)
        assertTrue("Not a Yomitan (.zip) or MDict (.mdx)" in assertFailsWith<Exception> { DictionaryFiles().inspect("hello, world, this is not a dictionary".toByteArray()) }.message!!)
    }

    @Test
    fun lzoDecompressesLiteralsAndMatches() {
        assertEquals("hello", Lzo.decompress(byteArrayOf(22) + "hello".toByteArray() + byteArrayOf(0x11, 0, 0), 5).decodeToString())
        // "abcd", then a match of 8 bytes 4 back (M2: length 8, distance 4), then the end.
        val stream = byteArrayOf((17 + 4).toByte()) + "abcd".toByteArray() + byteArrayOf(((8 - 1) shl 5 or (3 shl 2)).toByte(), 0, 0x11, 0, 0)
        assertEquals("abcdabcdabcd", Lzo.decompress(stream, 12).decodeToString())
    }

    @Test
    fun aDictionaryBecomesAPackOfItsEntriesAndLinks() = runTest {
        val file = Mdx(entries, encrypted = 2).build()
        val info = DictionaryFiles().inspect(file)
        assertEquals("Português–English", info.title)
        assertEquals("A test dictionary", info.attribution)
        val pack = DictionaryFiles().convert(file, "pt", "en")
        val (rows, forms, meta) = readSqliteFile(pack, listOf(
            SqliteQuery("SELECT word, word_lc, senses FROM entries ORDER BY id", 3),
            SqliteQuery("SELECT form_lc, lemma FROM forms", 2),
            SqliteQuery("SELECT value FROM meta WHERE key = 'sources'", 1),
        ))
        assertEquals(listOf("casa", "gato", "água"), rows.map { it[0] })
        // The headword the entry opens with is left out; each line it shows is a gloss.
        assertEquals("""[{"glosses":["house, home","household","a minha casa — my house"]}]""", rows[0][2])
        assertEquals("""[{"glosses":["cat","tomcat"]}]""", rows[1][2])
        assertEquals("""[{"glosses":["water"]}]""", rows[2][2])
        assertEquals(listOf(listOf("gatos", "gato")), forms)
        assertEquals("Português–English (imported MDict dictionary)", meta.single()[0])
    }

    @Test
    fun aCompactDictionarysStylesBecomeTagsAgain() = runTest {
        // ECDICT's style sheet and records, "</br>" and all.
        val sheet = "1\n<b style=\"font-size:180%;\">\n</b>\n2\n</br>\n\n3\n<font color=dodgerblue>\n</font>"
        val compact = listOf("hello" to "`1`hello`2``2`\r\n`3`[hə'ləu] `2``2`\r\ninterj. 喂, 嘿 </br>\r\n")
        val file = Mdx(compact, styleSheet = sheet).build()
        assertEquals(listOf("hello" to "<b style=\"font-size:180%;\">hello</b></br></br>\r\n<font color=dodgerblue>[hə'ləu] </font></br></br>\r\ninterj. 喂, 嘿 </br>\r\n"), read(file))
        val pack = MdictConverter().convert(file, "en", "zh")
        assertEquals("""[{"glosses":["[hə'ləu]","interj. 喂, 嘿"]}]""", readSqliteFile(pack, listOf(SqliteQuery("SELECT senses FROM entries", 1))).single().single()[0])
    }

    @Test
    fun anEntrysGlossesAreTheLinesItShows() {
        assertEquals(listOf("one", "two"), MdictConverter.glosses("word", "Word\r\none\r\n\r\ntwo\none"))
        assertEquals(listOf("noun", "a thing"), MdictConverter.glosses("thing", "<h1>Thing</h1><script>x()</script><style>p{}</style><p>noun</p><p>a  thing</p>"))
    }

    /**
     * An MDict file written as readmdict reads them: a UTF-16 header, the keys' blocks behind their
     * index, the records' blocks behind theirs, each block compressed with zlib (2), stored (0) or
     * LZO (1, literal runs only).
     */
    internal class Mdx(
        val entries: List<Pair<String, String>>,
        val version: String = "2.0",
        val encoding: String = "UTF-8",
        val encrypted: Int = 0,
        val compression: Int = 2,
        val perBlock: Int = 2,
        val root: String = "Dictionary",
        val styleSheet: String? = null,
    ) {
        private val v2 = version.toDouble() >= 2.0
        private val width = if (v2) 8 else 4
        private val utf16 = encoding.startsWith("UTF-16")

        fun build(): ByteArray {
            val out = ByteArrayOutputStream()
            val header = """<$root GeneratedByEngineVersion="$version" RequiredEngineVersion="$version" Encrypted="$encrypted" Encoding="$encoding" Format="Html" Title="Português–English" Description="A &lt;b&gt;test&lt;/b&gt; dictionary"${styleSheet?.let { " StyleSheet=\"" + it.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;") + "\"" }.orEmpty()}/>""" + "\r\n\u0000"
            val headerBytes = header.toByteArray(Charsets.UTF_16LE)
            out.write(int32(headerBytes.size))
            out.write(headerBytes)
            out.write(ByteArray(4))

            // The records, one after another, and where each starts.
            val records = entries.map { text(it.second) }
            val offsets = records.runningFold(0L) { at, record -> at + record.size }
            val keyBlocks = entries.indices.chunked(perBlock).map { block ->
                val data = ByteArrayOutputStream()
                block.forEach { i -> data.write(number(offsets[i])); data.write(text(entries[i].first)) }
                data.toByteArray()
            }
            val compressedKeys = keyBlocks.map(::compress)
            val info = ByteArrayOutputStream()
            entries.indices.chunked(perBlock).forEachIndexed { b, block ->
                info.write(number(block.size.toLong()))
                for (key in listOf(entries[block.first()].first, entries[block.last()].first)) {
                    val units = if (utf16) key.length else key.toByteArray().size
                    if (v2) info.write(byteArrayOf((units shr 8).toByte(), units.toByte())) else info.write(units)
                    info.write(if (v2) text(key) else key.toByteArray(if (utf16) Charsets.UTF_16LE else Charsets.UTF_8))
                }
                info.write(number(compressedKeys[b].size.toLong()))
                info.write(number(keyBlocks[b].size.toLong()))
            }
            val rawInfo = info.toByteArray()
            val storedInfo = if (v2) {
                val checksum = byteArrayOf(1, 2, 3, 4)
                val body = byteArrayOf(2, 0, 0, 0) + checksum + zlib(rawInfo)
                if (encrypted and 2 != 0) encrypt(body) else body
            } else rawInfo

            out.write(number(keyBlocks.size.toLong()))
            out.write(number(entries.size.toLong()))
            if (v2) out.write(number(rawInfo.size.toLong()))
            out.write(number(storedInfo.size.toLong()))
            out.write(number(compressedKeys.sumOf { it.size }.toLong()))
            if (v2) out.write(ByteArray(4))
            out.write(storedInfo)
            compressedKeys.forEach(out::write)

            val recordBlocks = records.chunked(perBlock).map { block -> block.fold(ByteArray(0)) { a, b -> a + b } }
            val compressedRecords = recordBlocks.map(::compress)
            out.write(number(recordBlocks.size.toLong()))
            out.write(number(entries.size.toLong()))
            out.write(number(recordBlocks.size * 2L * width))
            out.write(number(compressedRecords.sumOf { it.size }.toLong()))
            recordBlocks.indices.forEach { out.write(number(compressedRecords[it].size.toLong())); out.write(number(recordBlocks[it].size.toLong())) }
            compressedRecords.forEach(out::write)
            return out.toByteArray()
        }

        private fun text(value: String): ByteArray = (value + "\u0000").toByteArray(if (utf16) Charsets.UTF_16LE else Charsets.UTF_8)

        private fun compress(data: ByteArray): ByteArray = when (compression) {
            0 -> byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0) + data
            1 -> {
                require(data.size <= 238) { "Literal runs only" }
                byteArrayOf(1, 0, 0, 0, 0, 0, 0, 0) + byteArrayOf((17 + data.size).toByte()) + data + byteArrayOf(0x11, 0, 0)
            }
            else -> byteArrayOf(2, 0, 0, 0, 0, 0, 0, 0) + zlib(data)
        }

        private fun zlib(data: ByteArray): ByteArray {
            val deflater = Deflater()
            deflater.setInput(data)
            deflater.finish()
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer))
            return out.toByteArray()
        }

        /** The inverse of the reader's revealing: mixed with the key, the byte before (as written) and the position, then its halves swapped. */
        private fun encrypt(block: ByteArray): ByteArray {
            val key = Ripemd128.digest(block.copyOfRange(4, 8) + byteArrayOf(0x95.toByte(), 0x36, 0, 0))
            val out = block.copyOf()
            var previous = 0x36
            for (i in 8 until block.size) {
                val mixed = (block[i].toInt() and 0xFF) xor previous xor ((i - 8) and 0xFF) xor (key[(i - 8) % key.size].toInt() and 0xFF)
                val cipher = ((mixed ushr 4) or (mixed shl 4)) and 0xFF
                out[i] = cipher.toByte()
                previous = cipher
            }
            return out
        }

        private fun number(n: Long): ByteArray = ByteArray(width) { i -> (n ushr (8 * (width - 1 - i))).toByte() }

        private fun int32(n: Int): ByteArray = ByteArray(4) { i -> (n ushr (8 * (3 - i))).toByte() }
    }

    private fun ByteArray.hex(): String = joinToString("") { "%02x".format(it) }
}
