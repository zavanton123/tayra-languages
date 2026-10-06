package com.tayra.languages.core.data.dictionary

import java.io.File
import java.util.zip.GZIPInputStream

/**
 * Downloaded packs (dictionaries, course packs) unpacked into a directory as <name>.sqlite, each
 * next to a stamp file recording its format so files from an older layout count as not installed.
 */
internal class PackFiles(private val directory: File, private val downloader: DictionaryDownloader) {
    fun file(name: String): File = File(directory, "$name.sqlite")

    private fun stamp(name: String): File = File(directory, "$name.sqlite.format")

    fun installedSize(name: String, format: Int): Long? {
        val file = file(name)
        val current = stamp(name).takeIf { it.isFile }?.readText()?.trim() == format.toString()
        return if (file.isFile && current) file.length() else null
    }

    suspend fun install(name: String, url: String, format: Int, onProgress: (Float?) -> Unit) {
        val bytes = downloader.download(url, onProgress)
        directory.mkdirs()
        val target = file(name)
        val temp = File(target.path + ".tmp")
        GZIPInputStream(bytes.inputStream()).use { input -> temp.outputStream().use { output -> input.copyTo(output) } }
        target.delete()
        check(temp.renameTo(target)) { "Could not move ${temp.path} to ${target.path}" }
        stamp(name).writeText(format.toString())
    }

    fun remove(name: String) {
        stamp(name).delete()
        file(name).delete()
        File(file(name).path + "-journal").delete()
    }
}
