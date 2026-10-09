package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.courses.CoursePackService
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What clearing removes, in the order it goes. */
enum class ClearStep { LIBRARY, COURSE_PACKS, DICTIONARIES, VOICES, TRANSLATION_MODELS, CACHES }

/**
 * Removes everything the app keeps on the device: the library with its words and progress, the
 * downloaded course packs, dictionaries, voices and translation models, and the cached audio.
 * Settings, backups and the engines themselves stay. Each step runs even when an earlier one
 * failed; [clearEverything] says which ones did.
 */
class ClearDataService(
    private val demoData: DemoDataService,
    private val coursePacks: CoursePackService,
    private val dictionaries: DictionaryService,
    private val speech: LocalSpeech,
    private val translation: LocalTranslation,
    private val speechCache: SpeechAudioCache,
    private val settings: SettingsRepository,
) {
    private val _running = MutableStateFlow<ClearStep?>(null)

    /** The step under way, null between clearings. */
    val running: StateFlow<ClearStep?> = _running.asStateFlow()

    /** Removes everything; returns the steps that failed, with why. */
    suspend fun clearEverything(): Map<ClearStep, String> {
        val problems = linkedMapOf<ClearStep, String>()
        suspend fun step(step: ClearStep, action: suspend () -> Unit) {
            _running.value = step
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                problems[step] = e.message ?: e.toString()
            }
        }
        try {
            step(ClearStep.LIBRARY) { demoData.wipeDatabase() }
            step(ClearStep.COURSE_PACKS) {
                coursePacks.refresh()
                for (status in coursePacks.packs.value) if (status.state is PackState.Installed) coursePacks.remove(status.pack)
            }
            step(ClearStep.DICTIONARIES) {
                dictionaries.refresh()
                for (status in dictionaries.packs.value) if (status.state is PackState.Installed) dictionaries.remove(status.pack)
            }
            step(ClearStep.VOICES) { removeVoices() }
            step(ClearStep.TRANSLATION_MODELS) { removeTranslationModels() }
            step(ClearStep.CACHES) { speechCache.clear() }
        } finally {
            _running.value = null
        }
        return problems
    }

    /** Every engine's downloads; one that fails does not stop the next, and the chosen voices are forgotten either way. */
    private suspend fun removeVoices() {
        val failed = mutableListOf<String>()
        for (engine in speech.engines) {
            try {
                if (engine.hasRuntimeSetup && !engine.isReady()) continue
                for (pkg in engine.packages()) if (pkg.installed) engine.removePackage(pkg.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed += "${engine.displayName}: ${e.message ?: e}"
            }
        }
        settings.update { it.copy(speechVoices = emptyMap()) }
        if (failed.isNotEmpty()) error(failed.joinToString("; "))
    }

    private suspend fun removeTranslationModels() {
        val translator = translation.translator ?: return
        // A translator whose runtime was never set up has nothing to remove.
        val catalog = try {
            translator.packages()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (translator.hasRuntimeSetup) return else throw e
        }
        for (pkg in catalog) if (pkg.installed) translator.removePackage(pkg.fromCode, pkg.toCode)
    }
}
