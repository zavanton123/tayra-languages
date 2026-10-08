package com.tayra.languages.core.domain.service

import com.tayra.languages.core.domain.courses.CoursePack
import com.tayra.languages.core.domain.courses.CoursePackService
import com.tayra.languages.core.domain.courses.CoursePacks
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.DictionaryPacks
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What a language can have downloaded for it. */
enum class SetupKind { COURSES, DICTIONARY, VOICE, TRANSLATION }

/**
 * One download a language still lacks. The names are kept apart from any wording, which the
 * screen composes in the interface language.
 */
data class SetupItem(
    val id: String,
    val kind: SetupKind,
    /** The language being learned, by name. */
    val languageName: String,
    /** What is downloaded: the pack title, the voice, or the translator. */
    val name: String,
    /** The download size when it is known. */
    val sizeBytes: Long?,
    /** Whether it starts selected: everything but the large runtimes and engines not in use. */
    val recommended: Boolean,
    /** Whether the engine's own runtime comes with it (Python and its packages on the desktop), which is large. */
    val includesRuntime: Boolean = false,
    /** Whether installing it also makes its engine the one in use. */
    val switchesEngine: Boolean = false,
)

sealed interface SetupState {
    data object Waiting : SetupState
    data class Running(val progress: String?) : SetupState
    data object Done : SetupState
    data class Failed(val message: String) : SetupState
}

/**
 * The downloads that make a newly chosen language ready: its course pack, the dictionary into
 * the native language, a voice that reads it aloud, and the on-device translation models.
 * [missing] lists what is not on the device yet; [install] fetches the chosen ones one after
 * another in a scope of its own, so they go on after the screen that started them is closed.
 */
class LanguageSetupService(
    private val languages: LanguageRepository,
    private val settings: SettingsRepository,
    private val coursePacks: CoursePackService,
    private val dictionaries: DictionaryService,
    private val speech: LocalSpeech,
    private val translation: LocalTranslation,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val actions = mutableMapOf<String, suspend () -> Unit>()

    private val _states = MutableStateFlow<Map<String, SetupState>>(emptyMap())

    /** Where each item of the last [install] is. */
    val states: StateFlow<Map<String, SetupState>> = _states.asStateFlow()

    /** What [languageId] could still download, in the order it matters to a learner; empty when everything is there. */
    suspend fun missing(languageId: Long): List<SetupItem> {
        val language = languages.getById(languageId) ?: return emptyList()
        val code = LanguageCodes.codeFor(language.name) ?: return emptyList()
        val native = settings.current.nativeLanguage
        val found = buildList {
            courses(code, language.name)?.let(::add)
            if (code != native) dictionary(code, native, language.name)?.let(::add)
            voice(code, language.name)?.let(::add)
            if (code != native) translation(code, native, language.name)?.let(::add)
        }
        return found
    }

    /** Starts the downloads of [items], in order; a failed one does not stop the next. */
    fun install(items: List<SetupItem>) {
        val chosen = items.mapNotNull { item -> actions[item.id]?.let { item to it } }
        _states.update { it + chosen.associate { (item, _) -> item.id to SetupState.Waiting } }
        scope.launch {
            for ((item, action) in chosen) {
                set(item.id, SetupState.Running(null))
                try {
                    action()
                    set(item.id, SetupState.Done)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    set(item.id, SetupState.Failed(e.message ?: e.toString()))
                }
            }
        }
    }

    private fun set(id: String, state: SetupState) = _states.update { it + (id to state) }

    private fun register(item: SetupItem, action: suspend () -> Unit): SetupItem {
        actions[item.id] = action
        return item
    }

    private suspend fun courses(code: String, name: String): SetupItem? {
        coursePacks.refresh()
        val pack: CoursePack = CoursePacks.forLanguage(code).firstOrNull() ?: return null
        val state = coursePacks.packs.value.firstOrNull { it.pack.id == pack.id }?.state
        if (state is PackState.Installed || state is PackState.Downloading) return null
        return register(SetupItem("courses:${pack.id}", SetupKind.COURSES, name, pack.title, pack.downloadSize, recommended = true)) {
            coursePacks.download(pack)
            failIfFailed(coursePacks.packs.value.firstOrNull { it.pack.id == pack.id }?.state)
        }
    }

    private suspend fun dictionary(code: String, native: String, name: String): SetupItem? {
        dictionaries.refresh()
        val pack: DictionaryPack = DictionaryPacks.find(code, native) ?: return null
        val state = dictionaries.packs.value.firstOrNull { it.pack.id == pack.id }?.state
        if (state is PackState.Installed || state is PackState.Downloading) return null
        return register(SetupItem("dictionary:${pack.id.name}", SetupKind.DICTIONARY, name, pack.title, null, recommended = true)) {
            dictionaries.download(pack)
            failIfFailed(dictionaries.packs.value.firstOrNull { it.pack.id == pack.id }?.state)
        }
    }

    private fun failIfFailed(state: PackState?) {
        if (state is PackState.Failed) error(state.message)
    }

    /**
     * A voice from the engine in use, or from Piper while the system voices are in use, since
     * those differ from device to device. Nothing when the engine already speaks the language.
     */
    private suspend fun voice(code: String, name: String): SetupItem? {
        val current = settings.current.speechEngine
        val engine = speech.find(current) ?: speech.find(SpeechEngine.PIPER) ?: return null
        val switches = engine.engine != current
        val ready = runCatching { engine.isReady() }.getOrDefault(false)
        if (ready && runCatching { engine.voices(code) }.getOrDefault(emptyList()).isNotEmpty()) return null
        val packages = runCatching { engine.packages() }.getOrNull() ?: return null
        val pkg = pickVoice(packages, code, name) ?: return null
        if (pkg.installed && ready) return null
        val runtime = engine.hasRuntimeSetup && !ready
        val item = SetupItem(
            "voice:${engine.engine.name}:${pkg.id}", SetupKind.VOICE, name, "${engine.displayName}: ${pkg.title}",
            pkg.sizeBytes.takeIf { it > 0 && !pkg.installed }, recommended = !runtime, includesRuntime = runtime, switchesEngine = switches,
        )
        return register(item) {
            if (engine.hasRuntimeSetup && !engine.isReady()) engine.setUp()
            if (!pkg.installed) engine.installPackage(pkg.id)
            if (switches) settings.update { it.copy(speechEngine = engine.engine) }
        }
    }

    /** The language's own voices, a medium-quality one first; a model serving several languages when the engine has only that. */
    private fun pickVoice(packages: List<SpeechPackage>, code: String, name: String): SpeechPackage? {
        val own = packages.filter { it.languageCode == code }
        if (own.isNotEmpty()) {
            return own.firstOrNull { it.installed } ?: own.firstOrNull { "medium" in it.title.lowercase() } ?: own.first()
        }
        return packages.firstOrNull { it.languageCode == null && name.substringBefore(" (") in it.group }
    }

    /** The models for translating the language into the native one, with the translator's runtime when it has none yet. */
    private suspend fun translation(code: String, native: String, name: String): SetupItem? {
        val translator = translation.translator ?: return null
        if (runCatching { translator.canTranslate(code, native) }.getOrDefault(false)) return null
        val catalog = runCatching { translator.packages() }.getOrNull()
        val runtime = translator.hasRuntimeSetup && catalog == null
        if (catalog == null && !runtime) return null
        val missing = catalog?.let { packagesNeeded(translator, it, code, native) ?: return null }
        if (missing != null && missing.isEmpty()) return null
        val inUse = settings.current.translationEngine == TranslationEngine.ARGOS
        val item = SetupItem(
            "translation:$code-$native", SetupKind.TRANSLATION, name, translator.displayName,
            missing?.sumOf { it.sizeBytes }?.takeIf { it > 0 }, recommended = inUse && !runtime, includesRuntime = runtime, switchesEngine = !inUse,
        )
        return register(item) {
            if (runtime) translator.setUp()
            val needed = packagesNeeded(translator, translator.packages(), code, native)
                ?: error("${translator.displayName} has no model for this language")
            for (pkg in needed) translator.installPackage(pkg.fromCode, pkg.toCode)
            if (!inUse) settings.update { it.copy(translationEngine = TranslationEngine.ARGOS) }
        }
    }

    /** The models the pair still needs; null when the catalog cannot translate it at all. */
    private fun packagesNeeded(translator: LocalSentenceTranslator, catalog: List<LocalPackage>, code: String, native: String): List<LocalPackage>? {
        val keys = translator.requiredPackages(code, native, catalog)
        val packages = catalog.filter { it.key in keys }
        if (packages.size < keys.size) return null
        return packages.filterNot { it.installed }
    }
}
