package com.tayra.languages.core.data.mdict

import com.tayra.languages.core.data.yomitan.YomitanConverter
import com.tayra.languages.core.domain.dictionary.DictionaryImporter
import com.tayra.languages.core.domain.dictionary.ImportedDictionaryInfo

/** Imports a dictionary file by what it is: a Yomitan dictionary (a zip) or an MDict one (.mdx). */
class DictionaryFiles(
    private val yomitan: DictionaryImporter = YomitanConverter(),
    private val mdict: DictionaryImporter = MdictConverter(),
) : DictionaryImporter {

    override suspend fun inspect(file: ByteArray): ImportedDictionaryInfo = importerFor(file).inspect(file)

    override suspend fun convert(file: ByteArray, sourceLanguage: String, targetLanguage: String, onProgress: (Float) -> Unit): ByteArray =
        importerFor(file).convert(file, sourceLanguage, targetLanguage, onProgress)

    private fun importerFor(file: ByteArray): DictionaryImporter = when {
        file.size >= 4 && file[0] == 'P'.code.toByte() && file[1] == 'K'.code.toByte() -> yomitan
        MdictFile.looksLike(file) -> mdict
        else -> throw MdictException("Not a Yomitan (.zip) or MDict (.mdx) dictionary")
    }
}
