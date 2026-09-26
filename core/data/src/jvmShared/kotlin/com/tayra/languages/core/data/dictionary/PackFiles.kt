package com.tayra.languages.core.data.dictionary

import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * Downloaded packs unpacked into a directory, each next to a stamp file recording its format
 * so files from an older layout count as not installed.
 */
internal class PackFiles(private val directory: File, private val downloader: DictionaryDownloader) {
    fun file(pack: DictionaryPack): File = File(directory, "${pack.id.name}.sqlite")

    private fun stamp(pack: DictionaryPack): File = File(directory, "${pack.id.name}.sqlite.format")

    fun installedSize(pack: DictionaryPack): Long? {
        val file = file(pack)
        val current = stamp(pack).takeIf { it.isFile }?.readText()?.trim() == DictionaryId.FORMAT.toString()
        return if (file.isFile && current) file.length() else null
    }

    suspend fun install(pack: DictionaryPack, onProgress: (Float?) -> Unit) {
        val bytes = downloader.download(pack.url, onProgress)
        directory.mkdirs()
        val target = file(pack)
        val temp = File(target.path + ".tmp")
        GZIPInputStream(bytes.inputStream()).use { input -> temp.outputStream().use { output -> input.copyTo(output) } }
        target.delete()
        check(temp.renameTo(target)) { "Could not move ${temp.path} to ${target.path}" }
        stamp(pack).writeText(DictionaryId.FORMAT.toString())
    }

    fun remove(pack: DictionaryPack) {
        stamp(pack).delete()
        file(pack).delete()
        File(file(pack).path + "-journal").delete()
    }
}
