package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.books.BookPackService
import com.tayra.languages.core.domain.courses.CoursePackService
import com.tayra.languages.core.domain.dictionary.DictionaryId
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.TermStatus
import com.tayra.languages.core.domain.repository.BookRepository
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.repository.TermListFilter
import com.tayra.languages.core.domain.repository.TermListSort
import com.tayra.languages.core.domain.repository.TermRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.CancellationException

/** What removing a language deletes: its texts, its saved words, and the bytes its downloads take. */
data class LanguageRemoval(val texts: Int, val words: Int, val downloadBytes: Long)

/**
 * Removes a language being learned with everything kept for it: its books, lessons, courses,
 * playlists, words, flashcards and progress, and the course and book packs, dictionaries, voices and
 * translation models downloaded for it. What other languages use too stays: a voice model of many
 * languages, the English translation models of the native language, the files of a language another one
 * being learned shares a code with. The cached audio and translations stay until they expire.
 */
class LanguageRemovalService(
    private val settings: SettingsRepository,
    private val languages: LanguageRepository,
    private val languageService: LanguageService,
    private val learning: LearningLanguageService,
    private val books: BookRepository,
    private val terms: TermRepository,
    private val coursePacks: CoursePackService,
    private val bookPacks: BookPackService,
    private val dictionaries: DictionaryService,
    private val speech: LocalSpeech,
    private val translation: LocalTranslation,
) {
    /** What removing the language with [languageId] would delete. */
    suspend fun preview(languageId: Long): LanguageRemoval {
        val texts = books.getBooks().count { it.languageId == languageId }
        val words = terms.list(TermListFilter(languageId = languageId, minStatus = TermStatus.NEW_1, maxStatus = TermStatus.WELL_KNOWN), TermListSort(), 0, 1).totalCount
        val code = sharedCode(languageId) ?: return LanguageRemoval(texts, words, 0)
        var bytes = 0L
        coursePacks.refresh()
        bytes += coursePacks.packs.value.filter { it.pack.languageCode == code }.sumOf { (it.state as? PackState.Installed)?.sizeBytes ?: 0 }
        bookPacks.refresh()
        bytes += bookPacks.packs.value.filter { it.pack.languageCode == code }.sumOf { (it.state as? PackState.Installed)?.sizeBytes ?: 0 }
        dictionaries.refresh()
        bytes += dictionaries.packs.value.filter { isFor(it.pack.id, code) }.sumOf { (it.state as? PackState.Installed)?.sizeBytes ?: 0 }
        bytes += quietly { voices(code).sumOf { (_, pkg) -> pkg.sizeBytes } } ?: 0
        bytes += quietly { translationModels(code).sumOf { it.sizeBytes } } ?: 0
        return LanguageRemoval(texts, words, bytes)
    }

    /** Removes the language with [languageId] and all that is kept for it; returns what could not be removed, with why. */
    suspend fun remove(languageId: Long): List<String> {
        val language = languages.getById(languageId) ?: return emptyList()
        val problems = mutableListOf<String>()
        suspend fun step(what: String, action: suspend () -> Unit) {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                problems += "$what: ${e.message ?: e}"
            }
        }
        learning.forget(languageId)
        sharedCode(languageId)?.let { code ->
            step("Course packs") {
                coursePacks.refresh()
                for (status in coursePacks.packs.value) if (status.pack.languageCode == code && status.state is PackState.Installed) coursePacks.remove(status.pack)
            }
            step("Book packs") {
                bookPacks.refresh()
                for (status in bookPacks.packs.value) if (status.pack.languageCode == code && status.state is PackState.Installed) bookPacks.remove(status.pack)
            }
            step("Dictionaries") {
                dictionaries.refresh()
                for (status in dictionaries.packs.value) if (isFor(status.pack.id, code) && status.state is PackState.Installed) dictionaries.remove(status.pack)
            }
            step("Voices") {
                for ((engine, pkg) in voices(code)) engine.removePackage(pkg.id)
                settings.update { s -> s.copy(speechVoices = s.speechVoices.filterKeys { !it.endsWith(":$code") }) }
            }
            step("Translation models") {
                val translator = translation.translator
                for (pkg in translationModels(code)) translator?.removePackage(pkg.fromCode, pkg.toCode)
            }
        }
        // The language's rows go with it, everything in the library made in it with them; the
        // catalog's language comes back empty, to be learned afresh.
        step("Library") {
            languages.delete(languageId)
            languageService.loadPredefined(language.name, withTutorial = false)
        }
        return problems
    }

    /**
     * The code of the language with [languageId], whose downloads are its own: null when it has
     * none, or another language being learned has the same code and needs them still.
     */
    private suspend fun sharedCode(languageId: Long): String? {
        val all = languages.getAll()
        val code = all.firstOrNull { it.id == languageId }?.let { LanguageCodes.codeFor(it.name) } ?: return null
        val others = learning.mine().filter { it != languageId }
        return code.takeIf { all.none { it.id in others && LanguageCodes.codeFor(it.name) == code } }
    }

    /** A dictionary of the language's words, or a word finder into it. */
    private fun isFor(id: DictionaryId, code: String): Boolean = id.sourceLanguage == code || (id.glossary && id.targetLanguage == code)

    /** The installed voice packages of the language alone; one of many languages has no code and stays. */
    private suspend fun voices(code: String): List<Pair<LocalSpeechEngine, SpeechPackage>> = speech.engines.flatMap { engine ->
        if (engine.hasRuntimeSetup && !engine.isReady()) emptyList()
        else engine.packages().filter { it.installed && it.languageCode == code }.map { engine to it }
    }

    /**
     * The installed translation models into or out of the language, but those the languages still
     * learned need, and, for English, those to and from the native language, which every language
     * without a model of its own goes through.
     */
    private suspend fun translationModels(code: String): List<LocalPackage> {
        val translator = translation.translator ?: return emptyList()
        // A translator whose runtime was never set up has nothing to remove.
        val catalog = try {
            translator.packages()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (translator.hasRuntimeSetup) return emptyList() else throw e
        }
        val native = settings.current.nativeLanguage
        val all = languages.getAll()
        val needed = learning.mine().mapNotNull { id -> all.firstOrNull { it.id == id }?.let { LanguageCodes.codeFor(it.name) } }
            .filter { it != code }.flatMap { translator.requiredPackages(it, native, catalog) }.toSet()
        return catalog.filter { pkg ->
            pkg.installed && (pkg.fromCode == code || pkg.toCode == code) && pkg.key !in needed &&
                !(code == PIVOT && (pkg.fromCode == native || pkg.toCode == native))
        }
    }

    private companion object {
        const val PIVOT = "en"
    }

    private suspend fun <T> quietly(action: suspend () -> T): T? = try {
        action()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}
