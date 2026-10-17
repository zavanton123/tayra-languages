package com.tayra.languages.core.data.mdict

import com.tayra.languages.core.data.files.pickerFiltersByExtension
import com.tayra.languages.core.data.stardict.ArchiveFile
import com.tayra.languages.core.data.stardict.Archives
import com.tayra.languages.core.data.stardict.StardictConverter
import com.tayra.languages.core.data.stardict.Tar
import com.tayra.languages.core.data.yomitan.YomitanConverter
import com.tayra.languages.core.data.yomitan.ZipArchive
import com.tayra.languages.core.domain.dictionary.DictionaryImporter
import com.tayra.languages.core.domain.dictionary.ImportedDictionaryInfo

/**
 * Imports a dictionary file by what it is: a Yomitan dictionary (a zip with an index.json), an
 * MDict one (.mdx), or a StarDict one (its files in a zip or a tar archive, plain or gzipped, or
 * chosen together and [bundle]d).
 */
class DictionaryFiles(
    private val yomitan: DictionaryImporter = YomitanConverter(),
    private val mdict: DictionaryImporter = MdictConverter(),
    private val stardict: DictionaryImporter = StardictConverter(),
) : DictionaryImporter {

    override suspend fun inspect(file: ByteArray): ImportedDictionaryInfo = importerFor(file).inspect(file)

    override suspend fun convert(file: ByteArray, sourceLanguage: String, targetLanguage: String, onProgress: (Float) -> Unit): ByteArray =
        importerFor(file).convert(file, sourceLanguage, targetLanguage, onProgress)

    private fun importerFor(file: ByteArray): DictionaryImporter = when {
        Archives.isZip(file) -> {
            val names = ZipArchive(file).entries.map { it.name }
            if ("index.json" !in names && names.any { it.lowercase().endsWith(".ifo") }) stardict else yomitan
        }
        MdictFile.looksLike(file) -> mdict
        Archives.isGzip(file) || Archives.isTar(file) || Archives.isXz(file) || Archives.isBzip2(file) -> stardict
        else -> throw MdictException("Not a Yomitan (.zip), MDict (.mdx) or StarDict dictionary")
    }

    companion object {
        /**
         * The extensions to give the file picker, or null for every file: Android's picker filters by
         * MIME type, and has none for .mdx or StarDict's files.
         */
        val pickerExtensions: Set<String>?
            get() = if (pickerFiltersByExtension) setOf("zip", "mdx", "tar", "gz", "tgz", "xz", "bz2", "ifo", "idx", "dict", "dz", "syn") else null

        /** Files chosen together (a StarDict dictionary's .ifo, .idx, .dict...) as one archive to import. */
        fun bundle(files: List<Pair<String, ByteArray>>): ByteArray =
            if (files.size == 1) files.single().second else Tar.write(files.map { (name, bytes) -> ArchiveFile(name, bytes) })
    }
}
