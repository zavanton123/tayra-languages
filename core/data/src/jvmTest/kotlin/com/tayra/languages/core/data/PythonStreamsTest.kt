package com.tayra.languages.core.data

import com.tayra.languages.core.data.runtime.inUtf8
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/** The app's Python workers talk to it in UTF-8 even where the system code page is another, as on a Russian Windows. */
class PythonStreamsTest {

    private val python = listOf("/opt/homebrew/bin/python3", "/usr/local/bin/python3", "/usr/bin/python3").map(::File).firstOrNull { it.canExecute() }

    /** Python reads a line, as a worker reads a request, and answers with its length and a sentence of its own, as a worker answers. */
    private fun ask(builder: ProcessBuilder, line: String): List<String> {
        val process = builder.start()
        process.outputStream.bufferedWriter().use { it.write(line); it.newLine() }
        return process.inputStream.bufferedReader().readLines().also { process.waitFor() }
    }

    private fun windowsLike() = ProcessBuilder(
        python!!.path, "-u", "-c",
        "import sys\nline = sys.stdin.readline().rstrip()\nsys.stdout.write(str(len(line)) + '\\n' + '\\u0423 \\u0411\\u0435\\u043d\\u0430' + '\\n')",
    ).apply { environment()["PYTHONIOENCODING"] = "cp1251" }

    @Test
    fun cyrillicGoesBothWaysWhole() {
        if (python == null) return
        val sentence = "Макс"
        assertNotEquals(listOf("4", "У Бена"), ask(windowsLike(), sentence), "how the translations came out broken")
        assertEquals(listOf("4", "У Бена"), ask(windowsLike().inUtf8(), sentence))
    }
}
