package com.tayra.languages.core.data

import com.tayra.languages.core.data.anki.Zstd
import com.tayra.languages.core.data.anki.ZstdException
import java.io.File
import java.sql.DriverManager
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Zstandard data made by the zstd tool, at its levels and with its options, decompresses to what went in. */
class ZstdTest {

    private val tool = listOf("/opt/homebrew/bin/zstd", "/usr/local/bin/zstd", "/usr/bin/zstd").map(::File).firstOrNull { it.canExecute() }

    private val options = listOf(
        listOf("-1"),
        listOf("-3"),
        listOf("-19"),
        listOf("--ultra", "-22"),
        listOf("--fast=5"),
        listOf("-3", "--no-check"),
        listOf("-3", "--no-content-size"),
        listOf("-9", "--content-size", "--no-check"),
        listOf("-19", "--long=27"),
        listOf("-3", "--long=27", "--no-content-size"),
    )

    private fun compress(data: ByteArray, options: List<String>): ByteArray {
        val input = File.createTempFile("tayra-zstd", ".bin").apply { writeBytes(data) }
        val output = File(input.path + ".zst")
        try {
            val process = ProcessBuilder(listOf(tool!!.path, "-q", "-f") + options + listOf(input.path, "-o", output.path))
                .redirectErrorStream(true)
                .start()
            val messages = process.inputStream.readBytes().decodeToString()
            assertEquals(0, process.waitFor(), messages)
            return output.readBytes()
        } finally {
            input.delete()
            output.delete()
        }
    }

    private fun assertRoundTrips(data: ByteArray) {
        if (tool == null) return
        for (option in options) {
            assertContentEquals(data, Zstd.decompress(compress(data, option)), "zstd ${option.joinToString(" ")}")
        }
    }

    private fun text(size: Int): ByteArray {
        val words = "the a casa gato water house of and to in that it was he for on are as with his they at be this from".split(' ')
        val random = Random(1)
        return buildString {
            while (length < size) {
                append(words[random.nextInt(words.size)])
                append(if (random.nextInt(12) == 0) ".\n" else " ")
            }
        }.encodeToByteArray()
    }

    @Test
    fun emptyAndTinyInputs() {
        assertContentEquals(ByteArray(0), Zstd.decompress(ByteArray(0)))
        assertRoundTrips(ByteArray(0))
        assertRoundTrips(byteArrayOf(42))
        assertRoundTrips("hello, zstd".encodeToByteArray())
    }

    @Test
    fun repetitiveText() {
        assertRoundTrips(text(1_000_000))
        assertRoundTrips("abc".repeat(400_000).encodeToByteArray())
        assertRoundTrips(ByteArray(300_000))
    }

    @Test
    fun randomBytes() {
        assertRoundTrips(Random(2).nextBytes(1 shl 20))
        assertRoundTrips(text(200_000) + Random(3).nextBytes(200_000) + text(200_000))
    }

    @Test
    fun sqliteDatabase() {
        val file = File.createTempFile("tayra-zstd", ".db").apply { delete() }
        try {
            DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { c ->
                c.createStatement().use { it.execute("CREATE TABLE notes (id INTEGER PRIMARY KEY, front TEXT, back TEXT, due INTEGER)") }
                c.autoCommit = false
                c.prepareStatement("INSERT INTO notes (front, back, due) VALUES (?, ?, ?)").use { insert ->
                    val random = Random(4)
                    repeat(20_000) { i ->
                        insert.setString(1, "word $i ${random.nextInt(1000)}")
                        insert.setString(2, text(20 + random.nextInt(200)).decodeToString())
                        insert.setLong(3, random.nextLong(1_000_000_000L))
                        insert.addBatch()
                    }
                    insert.executeBatch()
                }
                c.commit()
            }
            assertRoundTrips(file.readBytes())
        } finally {
            file.delete()
        }
    }

    @Test
    fun concatenatedAndSkippableFrames() {
        if (tool == null) return
        val first = text(300_000)
        val second = Random(5).nextBytes(100_000)
        val skippable = byteArrayOf(0x5A, 0x2A, 0x4D, 0x18, 3, 0, 0, 0, 1, 2, 3)
        val data = compress(first, listOf("-3")) + skippable + compress(second, listOf("-19", "--no-content-size"))
        assertContentEquals(first + second, Zstd.decompress(data))
    }

    @Test
    fun damagedDataIsRefused() {
        assertFailsWith<ZstdException> { Zstd.decompress("not zstd data".encodeToByteArray()) }
        assertFailsWith<ZstdException> { Zstd.decompress(byteArrayOf(0x28, 0xB5.toByte(), 0x2F, 0xFD.toByte())) }
        if (tool == null) return
        val data = compress(text(100_000), listOf("-3"))
        assertFailsWith<ZstdException> { Zstd.decompress(data.copyOf(data.size / 2)) }
    }
}
