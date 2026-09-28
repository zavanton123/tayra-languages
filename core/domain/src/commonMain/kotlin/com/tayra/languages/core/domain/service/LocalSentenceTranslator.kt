package com.tayra.languages.core.domain.service

/** Which service turns sentences into the native language. */
enum class TranslationEngine(val label: String) {
    MYMEMORY("MyMemory (online)"),
    ARGOS("Argos Translate (offline, desktop)"),
}

/** One downloadable model of the local translator, translating [fromCode] into [toCode]. */
data class LocalPackage(
    val fromCode: String,
    val toCode: String,
    val fromName: String,
    val toName: String,
    val installed: Boolean,
    val sizeBytes: Long = 0,
) {
    val key: String get() = "$fromCode-$toCode"
    val title: String get() = "$fromName \u2192 $toName"
}

/** A translator running on this device, such as Argos Translate on the desktop. */
interface LocalSentenceTranslator : SentenceTranslator {
    /** A readable line about the installation: version and installed language pairs, or what is wrong. */
    suspend fun status(): String

    /** Every model the translator can download, with its installed state. Throws when the translator is unavailable. */
    suspend fun packages(): List<LocalPackage>

    /** Downloads and installs one model. Throws on failure. */
    suspend fun installPackage(fromCode: String, toCode: String)

    /** Deletes an installed model. Throws on failure. */
    suspend fun removePackage(fromCode: String, toCode: String)
}

/** Holder for the platform's local translator, null where none exists. */
class LocalTranslation(val translator: LocalSentenceTranslator?)
