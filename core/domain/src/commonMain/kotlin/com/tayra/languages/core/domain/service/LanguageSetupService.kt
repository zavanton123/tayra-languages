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

/** Where a language stands on one kind of download. */
enum class SetupStatus {
    /** It can be downloaded, and is not on the device yet. */
    MISSING,
    /** It is on the device already. */
    INSTALLED,
    /** Nothing of this kind exists for the language, as no Piper voice for Croatian. */
    UNAVAILABLE,
}

/** What a file of a download is. */
enum class SetupFileKind { COURSES, DICTIONARY, ENGINE, VOICE, MODEL }

/** One file a download fetches, with its size in bytes when known. */
data class SetupFile(
    val kind: SetupFileKind,
    /** The pack title, the engine, the voice, or the model's language pair. */
    val name: String,
    val sizeBytes: Long?,
    /** Whether [sizeBytes] is an estimate, as for an engine's runtime. */
    val estimated: Boolean = false,
)

/**
 * One download a language still lacks. The names are kept apart from any wording, which the
 * screen composes in the interface language.
 */
data class SetupItem(
    val id: String,
    val kind: SetupKind,
    /** The language being learned, by name. */
    val languageName: String,
    /** What is downloaded, in a line: the pack title, "Piper · Joy · medium · Greece", "Argos Translate · Greek → Russian". */
    val name: String,
    val files: List<SetupFile>,
    /** Whether it starts selected: everything but the large runtimes and engines not in use. */
    val recommended: Boolean,
    /** Whether installing it also makes its engine the one in use. */
    val switchesEngine: Boolean = false,
    val status: SetupStatus = SetupStatus.MISSING,
) {
    /** The size of the files whose size is known; null when none is. */
    val sizeBytes: Long? get() = files.mapNotNull { it.sizeBytes }.takeIf { it.isNotEmpty() }?.sum()

    /** Whether [sizeBytes] is only roughly the download: some sizes are estimates or unknown. */
    val sizeEstimated: Boolean get() = files.any { it.estimated || it.sizeBytes == null }

    /** Whether the engine's own runtime comes with it (Python and its packages on the desktop), which is large. */
    val includesRuntime: Boolean get() = files.any { it.kind == SetupFileKind.ENGINE }
}

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
    suspend fun missing(languageId: Long): List<SetupItem> = overview(languageId).filter { it.status == SetupStatus.MISSING }

    /**
     * Every kind of download the language can have, in the order it matters to a learner, each
     * missing, installed or unavailable; empty for a language without a known code.
     */
    suspend fun overview(languageId: Long): List<SetupItem> {
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

    private fun settled(kind: SetupKind, language: String, name: String, status: SetupStatus) =
        SetupItem("${kind.name.lowercase()}:${status.name.lowercase()}", kind, language, name, emptyList(), recommended = false, status = status)

    private fun register(item: SetupItem, action: suspend () -> Unit): SetupItem {
        actions[item.id] = action
        return item
    }

    private suspend fun courses(code: String, name: String): SetupItem? {
        coursePacks.refresh()
        val pack: CoursePack = CoursePacks.forLanguage(code).firstOrNull() ?: return settled(SetupKind.COURSES, name, "", SetupStatus.UNAVAILABLE)
        val state = coursePacks.packs.value.firstOrNull { it.pack.id == pack.id }?.state
        if (state is PackState.Installed || state is PackState.Downloading) return settled(SetupKind.COURSES, name, pack.title, SetupStatus.INSTALLED)
        val files = listOf(SetupFile(SetupFileKind.COURSES, pack.title, pack.downloadSize))
        return register(SetupItem("courses:${pack.id}", SetupKind.COURSES, name, pack.title, files, recommended = true)) {
            coursePacks.download(pack)
            failIfFailed(coursePacks.packs.value.firstOrNull { it.pack.id == pack.id }?.state)
        }
    }

    private suspend fun dictionary(code: String, native: String, name: String): SetupItem? {
        dictionaries.refresh()
        val pack: DictionaryPack = DictionaryPacks.find(code, native)
            ?: return settled(SetupKind.DICTIONARY, name, "$name \u2192 ${LanguageCodes.option(native)?.name ?: native}", SetupStatus.UNAVAILABLE)
        val state = dictionaries.packs.value.firstOrNull { it.pack.id == pack.id }?.state
        if (state is PackState.Installed || state is PackState.Downloading) return settled(SetupKind.DICTIONARY, name, pack.title, SetupStatus.INSTALLED)
        val files = listOf(SetupFile(SetupFileKind.DICTIONARY, pack.title, pack.downloadSize))
        return register(SetupItem("dictionary:${pack.id.name}", SetupKind.DICTIONARY, name, pack.title, files, recommended = true)) {
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
        if (ready && runCatching { engine.voices(code) }.getOrDefault(emptyList()).isNotEmpty()) {
            return settled(SetupKind.VOICE, name, engine.displayName, SetupStatus.INSTALLED)
        }
        val packages = runCatching { engine.packages() }.getOrNull() ?: return null
        val pkg = pickVoice(packages, code, name) ?: return settled(SetupKind.VOICE, name, engine.displayName, SetupStatus.UNAVAILABLE)
        if (pkg.installed && ready) return settled(SetupKind.VOICE, name, "${engine.displayName} · ${pkg.title}", SetupStatus.INSTALLED)
        val runtime = engine.hasRuntimeSetup && !ready
        val files = listOfNotNull(
            SetupFile(SetupFileKind.ENGINE, engine.displayName, runCatching { engine.runtimeDownloadSize() }.getOrNull(), estimated = true).takeIf { runtime },
            SetupFile(SetupFileKind.VOICE, pkg.title, pkg.sizeBytes.takeIf { it > 0 }).takeIf { !pkg.installed },
        )
        val item = SetupItem(
            "voice:${engine.engine.name}:${pkg.id}", SetupKind.VOICE, name, "${engine.displayName} · ${pkg.title}",
            files, recommended = !runtime, switchesEngine = switches,
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
        val nativeName = LanguageCodes.option(native)?.name ?: native
        val pair = "${translator.displayName} · $name \u2192 $nativeName"
        if (runCatching { translator.canTranslate(code, native) }.getOrDefault(false)) return settled(SetupKind.TRANSLATION, name, pair, SetupStatus.INSTALLED)
        val catalog = runCatching { translator.packages() }.getOrNull()
        val runtime = translator.hasRuntimeSetup && catalog == null
        if (catalog == null && !runtime) return null
        // Before the runtime is there, the models the translator knows of tell whether the pair is possible.
        val models = (catalog ?: translator.knownPackages())?.let {
            packagesNeeded(translator, it, code, native) ?: return settled(SetupKind.TRANSLATION, name, pair, SetupStatus.UNAVAILABLE)
        }
        if (models != null && models.isEmpty() && !runtime) return settled(SetupKind.TRANSLATION, name, pair, SetupStatus.INSTALLED)
        val inUse = settings.current.translationEngine == TranslationEngine.ARGOS
        val files = listOfNotNull(
            SetupFile(SetupFileKind.ENGINE, translator.displayName, runCatching { translator.runtimeDownloadSize() }.getOrNull(), estimated = true).takeIf { runtime },
        ) + models.orEmpty().map { SetupFile(SetupFileKind.MODEL, it.title, it.sizeBytes.takeIf { size -> size > 0 }, it.estimated) }
        val item = SetupItem(
            "translation:$code-$native", SetupKind.TRANSLATION, name, pair,
            files, recommended = inUse && !runtime, switchesEngine = !inUse,
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
