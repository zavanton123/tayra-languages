package com.tayra.languages.cli

import com.github.ajalt.clikt.testing.CliktCommandTestResult
import com.github.ajalt.clikt.testing.test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The command against a library of its own: what it prints, the exit codes, and what it leaves in the library. */
class CliTest {
    private val dataDir: File = Files.createTempDirectory("tayra-cli").toFile()

    @AfterTest
    fun removeTheLibrary() {
        dataDir.deleteRecursively()
    }

    private fun run(vararg args: String): CliktCommandTestResult = tayra().test(listOf("--data-dir", dataDir.absolutePath) + args.toList())

    private fun json(vararg args: String) = Json.parseToJsonElement(run("--json", *args).also { assertEquals(0, it.statusCode, it.output) }.stdout)

    private fun ready() {
        assertEquals(0, run("init").statusCode)
        assertEquals(0, run("languages", "use", "pt").statusCode)
    }

    @Test
    fun aBookIsAddedAndReadWithTheStatusesOfItsWords() {
        ready()
        val id = json("books", "add", "--title", "Minha casa", "--tags", "casa", "--text", "Eu moro no Brasil. A minha casa é pequena.").jsonObject["id"]!!.jsonPrimitive.content
        val books = json("books", "list", "--tag", "casa").jsonArray
        assertEquals(listOf("Minha casa"), books.map { it.jsonObject["title"]!!.jsonPrimitive.content })

        assertEquals(0, run("terms", "set-status", "known", "eu", "no").statusCode)
        assertEquals(0, run("terms", "add", "casa", "--translation", "house", "--status", "2").statusCode)
        val page = json("read", "page", id).jsonObject
        assertEquals(9, page["distinctWords"]!!.jsonPrimitive.int)
        assertEquals(6, page["unknown"]!!.jsonPrimitive.int)

        // The page's new words were saved with status 0 when it was opened; the list leaves them out.
        val terms = json("terms", "list").jsonObject["terms"]!!.jsonArray.map { it.jsonObject["text"]!!.jsonPrimitive.content }
        assertEquals(listOf("casa", "eu", "no"), terms)
        assertTrue("house" in run("terms", "show", "casa").stdout)
    }

    /** A book's file is read as the app imports it: a web page's text, without its markup. */
    @Test
    fun aBookIsAddedFromAWebPageFile() {
        ready()
        val page = File(dataDir, "casa.html").apply { writeText("<html><head><title>x</title></head><body><h1>A casa</h1><p>Eu moro <b>no</b> Brasil.</p></body></html>") }
        val id = json("books", "add", "--title", "A casa", "--file", page.absolutePath).jsonObject["id"]!!.jsonPrimitive.content
        assertEquals("A casa\nEu moro no Brasil.", json("books", "text", id).jsonObject["text"]!!.jsonPrimitive.content)
    }

    @Test
    fun aCourseGetsLessonsInOrder() {
        ready()
        val course = json("courses", "create", "--title", "No mercado", "--level", "A2").jsonObject["id"]!!.jsonPrimitive.content
        json("lessons", "add", course, "--title", "As frutas", "--text", "Eu compro maçãs.")
        val second = json("lessons", "add", course, "--title", "O peixe", "--text", "O peixe está fresco.").jsonObject["id"]!!.jsonPrimitive.content
        assertEquals(0, run("lessons", "move", course, second, "--by", "-1").statusCode)
        val shown = json("courses", "show", course).jsonObject
        assertEquals("A2", shown["course"]!!.jsonObject["level"]!!.jsonPrimitive.content)
        assertEquals(listOf("O peixe", "As frutas"), shown["lessonList"]!!.jsonArray.map { it.jsonObject["title"]!!.jsonPrimitive.content })
    }

    @Test
    fun aRestoreBringsBackWhatWasDeletedOnlyWhenConfirmed() {
        ready()
        val id = json("books", "add", "--title", "Kept", "--text", "Uma frase curta.").jsonObject["id"]!!.jsonPrimitive.content
        val backup = json("backups", "create").jsonObject["name"]!!.jsonPrimitive.content
        assertEquals(ExitCode.INVALID, run("books", "delete", id).statusCode, "deleting needs --yes")
        assertEquals(0, run("books", "delete", id, "--yes").statusCode)
        assertEquals(ExitCode.NOT_FOUND, run("books", "show", id).statusCode)

        assertEquals(ExitCode.INVALID, run("backups", "restore", backup).statusCode, "restoring needs --yes")
        assertEquals(0, run("backups", "restore", backup, "--yes").statusCode)
        assertEquals(0, run("books", "show", id).statusCode)
    }

    @Test
    fun errorsGoToStderrWithTheirExitCodes() {
        ready()
        val missing = run("books", "show", "999")
        assertEquals(ExitCode.NOT_FOUND, missing.statusCode)
        assertTrue("No book 999" in missing.stderr && missing.stdout.isEmpty())
        assertEquals(ExitCode.INVALID, run("languages", "use", "en").statusCode, "English is the native language")
        assertEquals(ExitCode.INVALID, run("books", "add", "--title", "No text").statusCode)
        assertEquals(ExitCode.INVALID, run("terms", "set-status", "7", "eu").statusCode)
        assertEquals(ExitCode.ERROR, run("no-such-command").statusCode)
    }

    @Test
    fun aDataFolderKeepsItsOwnSettings() {
        ready()
        assertEquals(0, run("settings", "set", "current_theme", "nord").statusCode)
        assertEquals("nord", run("settings", "get", "current_theme").stdout.trim())
        assertTrue(File(dataDir, "settings.properties").readText().contains("nord"))
        assertTrue(File(dataDir, "tayra.db").exists())
    }

    /** A StarDict dictionary's loose files become a pack file, and the library is left as it was. */
    @Test
    fun aDictionaryIsConvertedToAPackFile() {
        val dict = "house\u0000cat".toByteArray()
        val idx = java.io.ByteArrayOutputStream().apply {
            for ((word, offset, size) in listOf(Triple("casa", 0, 5), Triple("gato", 6, 3))) {
                write(word.toByteArray()); write(0)
                for (n in listOf(offset, size)) for (i in 3 downTo 0) write(n ushr (8 * i))
            }
        }.toByteArray()
        File(dataDir, "pt-en.ifo").writeText("StarDict's dict ifo file\nversion=2.4.2\nbookname=Test\nwordcount=2\nidxfilesize=${idx.size}\nsametypesequence=m\n")
        File(dataDir, "pt-en.idx").writeBytes(idx)
        File(dataDir, "pt-en.dict").writeBytes(dict)
        val out = File(dataDir, "pt-en.sqlite")
        val paths = listOf("ifo", "idx", "dict").map { File(dataDir, "pt-en.$it").absolutePath }.toTypedArray()

        val result = json("dictionaries", "convert", *paths, "--from", "pt", "--to", "en", "--out", out.absolutePath).jsonObject
        assertEquals("Test", result["title"]!!.jsonPrimitive.content)
        assertEquals("SQLite format 3", out.readBytes().copyOf(15).decodeToString())
        assertEquals(ExitCode.INVALID, run("dictionaries", "convert", paths[0], "--from", "pt", "--to", "en", "--out", out.absolutePath).statusCode, "an .ifo alone")
        assertEquals(ExitCode.INVALID, run("dictionaries", "convert", *paths, "--from", "xx", "--to", "en", "--out", out.absolutePath).statusCode)
    }

    @Test
    fun everyCommandIsDescribedForAgents() {
        val tree = json("commands").jsonObject
        fun paths(node: kotlinx.serialization.json.JsonObject): List<String> =
            listOf(node["command"]!!.jsonPrimitive.content) + node["subcommands"]!!.jsonArray.flatMap { paths(it.jsonObject) }
        val all = paths(tree)
        for (command in listOf("tayra books add", "tayra backups restore", "tayra read page", "tayra lessons open", "tayra dictionaries convert")) assertTrue(command in all, command)
    }
}
