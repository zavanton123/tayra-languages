package com.tayra.languages.core.data.dictionary

import com.tayra.languages.core.domain.dictionary.DictionaryAssets
import com.tayra.languages.core.domain.dictionary.DictionaryId
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * Unpacks a bundled gzip-compressed dictionary next to a stamp file recording its format, so
 * the copy happens once per format rather than on every start. Returns false when the
 * dictionary is not bundled.
 */
internal suspend fun ensureDictionaryFile(target: File, dictionary: DictionaryId, assets: DictionaryAssets): Boolean {
    val stamp = File(target.path + ".format")
    if (target.isFile && stamp.isFile && stamp.readText().trim() == DictionaryId.FORMAT.toString()) return true
    val bytes = assets.readBytes(dictionary) ?: return false
    target.parentFile?.mkdirs()
    val temp = File(target.path + ".tmp")
    GZIPInputStream(bytes.inputStream()).use { input -> temp.outputStream().use { output -> input.copyTo(output) } }
    if (!temp.renameTo(target)) {
        target.delete()
        check(temp.renameTo(target)) { "Could not move ${temp.path} to ${target.path}" }
    }
    stamp.writeText(DictionaryId.FORMAT.toString())
    return true
}
