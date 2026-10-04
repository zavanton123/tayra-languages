package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.settings.UserSettings
import kotlinx.coroutines.flow.StateFlow

/** Which service turns sentences and terms into the native language. */
/** In the order the engines are offered; stored by name, so the order can change freely. */
enum class TranslationEngine(val label: String) {
    ARGOS("On this device (offline, free)"),
    MYMEMORY("MyMemory (online, free)"),
    GOOGLE("Google Translate (online, API key)"),
}

/** The engine's name in menus; the on-device one is named after this platform's translator, [localTranslatorName]. */
fun TranslationEngine.label(localTranslatorName: String?): String =
    if (this == TranslationEngine.ARGOS && localTranslatorName != null) "$localTranslatorName (offline, free)" else label

/**
 * The engine that will actually answer: Argos needs a local translator, Google needs a key,
 * and either falls back to MyMemory without one.
 */
fun UserSettings.effectiveEngine(hasLocalTranslator: Boolean): TranslationEngine = when {
    translationEngine == TranslationEngine.ARGOS && !hasLocalTranslator -> TranslationEngine.MYMEMORY
    translationEngine == TranslationEngine.GOOGLE && googleTranslateApiKey.isBlank() -> TranslationEngine.MYMEMORY
    else -> translationEngine
}

/** The Google Cloud Translation client's self-check, for the Settings screen. */
interface GoogleTranslation {
    /** Translates a fixed phrase with the stored key and reports the outcome in one readable line. */
    suspend fun checkKey(): String
}


/** One downloadable model of the local translator, translating [fromCode] into [toCode]. */
data class LocalPackage(
    val fromCode: String,
    val toCode: String,
    val fromName: String,
    val toName: String,
    val installed: Boolean,
    val sizeBytes: Long = 0,
    /** A name to show instead of "from → to", for translators whose models are per language. */
    val label: String? = null,
) {
    val key: String get() = "$fromCode-$toCode"
    val title: String get() = label ?: "$fromName \u2192 $toName"
}

/** Why the local translator cannot translate right now, so the reader can offer the right way out. */
sealed interface LocalTranslationProblem {
    val message: String

    /** The models for the pair exist in the catalog but are not installed; [title] names them for the install prompt. */
    data class ModelMissing(val fromCode: String, val toCode: String, val title: String) : LocalTranslationProblem {
        override val message: String get() = "The $title ${if (title.endsWith("models")) "are" else "is"} available but not installed."
    }

    /** The catalog has no model for the pair, directly or through English. */
    data class NoModel(val fromName: String, val toName: String, val engineName: String) : LocalTranslationProblem {
        override val message: String get() = "$engineName has no $fromName \u2192 $toName model. Turn offline translation off to use MyMemory."
    }

    data class Failed(override val message: String) : LocalTranslationProblem
}

/** A translator running on this device: Argos Translate on the desktop, Google ML Kit on phones. */
interface LocalSentenceTranslator : SentenceTranslator {
    /** How the translator is named to the user. */
    val displayName: String

    /** One or two sentences for Settings on what it is and where its files live. */
    val description: String

    /** How its downloadable packages relate to languages, for the package list in Settings. */
    val packagesDescription: String

    /** Whether the translator has a runtime of its own to install and check (Argos does, ML Kit does not). */
    val hasRuntimeSetup: Boolean

    /** The package keys the pair needs from [catalog]: one direct model, or both halves of a detour through English. */
    fun requiredPackages(fromCode: String, toCode: String, catalog: List<LocalPackage>): Set<String> =
        if (catalog.any { it.fromCode == fromCode && it.toCode == toCode }) setOf("$fromCode-$toCode")
        else setOf("$fromCode-en", "en-$toCode")

    /** A readable line about the installation: version and installed language pairs, or what is wrong. */
    suspend fun status(): String

    /** Why the last sentence translation or setup failed, cleared by the next success. */
    val lastError: StateFlow<LocalTranslationProblem?>

    /** What [prepare] or [setUp] is doing right now, null when idle. */
    val progress: StateFlow<String?>

    /**
     * Makes translating [fromCode] into [toCode] possible: installs the runtime when missing and checks the
     * models. Missing models are not downloaded here; the problem in [lastError] says which ones [installModels]
     * would fetch. Throws when translation is not possible yet.
     */
    suspend fun prepare(fromCode: String, toCode: String, fromName: String = fromCode, toName: String = toCode)

    /** Whether the pair can be translated right now, without installing anything or touching [lastError]. */
    suspend fun canTranslate(fromCode: String, toCode: String): Boolean

    /** Downloads the models [prepare] found missing for the pair, reporting through [progress]. Throws on failure. */
    suspend fun installModels(fromCode: String, toCode: String)

    /** Installs the translator's own runtime into the app folder and points the settings at it. Returns a summary or throws. */
    suspend fun setUp(): String

    /** Every model the translator can download, with its installed state. Throws when the translator is unavailable. */
    suspend fun packages(): List<LocalPackage>

    /** Downloads and installs one model. Throws on failure. */
    suspend fun installPackage(fromCode: String, toCode: String)

    /** Deletes an installed model. Throws on failure. */
    suspend fun removePackage(fromCode: String, toCode: String)
}

/** Holder for the platform's local translator, null where none exists. */
class LocalTranslation(val translator: LocalSentenceTranslator?)
