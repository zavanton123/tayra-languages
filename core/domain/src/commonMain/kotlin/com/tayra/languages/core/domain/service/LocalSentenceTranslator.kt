package com.tayra.languages.core.domain.service

/** Which service turns sentences into the native language. */
enum class TranslationEngine(val label: String) {
    MYMEMORY("MyMemory (online)"),
    ARGOS("Argos Translate (offline, desktop)"),
}

/** A translator running on this device, such as Argos Translate on the desktop. */
interface LocalSentenceTranslator : SentenceTranslator {
    /** A readable line about the installation: version and installed language pairs, or what is wrong. */
    suspend fun status(): String

    /** Downloads and installs the models for the pair (through English when needed). Returns a summary or throws. */
    suspend fun installPackage(fromCode: String, toCode: String): String
}

/** Holder for the platform's local translator, null where none exists. */
class LocalTranslation(val translator: LocalSentenceTranslator?)
