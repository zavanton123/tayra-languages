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
enum class SetupFileKind {
    COURSES, DICTIONARY, ENGINE, VOICE, MODEL,
    /** A speech model holding several voices, downloaded once for whichever of them are chosen (Kokoro). */
    VOICE_MODEL,
}

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
    /** Whether it starts selected: every voice, and everything else but the large runtimes and engines not in use. */
    val recommended: Boolean,
    /** Whether installing it also makes its engine the one in use. */
    val switchesEngine: Boolean = false,
    val status: SetupStatus = SetupStatus.MISSING,
    /** The speech engine a voice belongs to, by name; a language's voices are listed per engine. */
    val engine: String? = null,
    /** The size of a voice's own data inside a model it shares with others, already part of that model's size. */
    val voiceSizeBytes: Long? = null,
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
            addAll(voices(code, language.name))
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
     * Every voice the local engines have for the language, each to choose on its own: a Piper voice is
     * a download of its own, Kokoro's voices share one model, fetched with the first of them. All
     * start ticked; the engine in use comes first, or Piper while the system voices are in use,
     * since those differ from device to device.
     */
    private suspend fun voices(code: String, name: String): List<SetupItem> {
        val current = settings.current.speechEngine
        val speaking = speaks(current, code)
        val preferred = (speech.find(current) ?: speech.find(SpeechEngine.PIPER))?.engine
        val found = speech.engines.sortedBy { it.engine != preferred }.flatMap { engine ->
            val packages = runCatching { engine.packages() }.getOrNull() ?: return@flatMap emptyList()
            val own = packages.filter { it.languageCode == code }
                .ifEmpty { packages.filter { it.languageCode == null && name.substringBefore(" (") in it.group } }
            if (own.isEmpty()) return@flatMap emptyList()
            val first = pickVoice(own)
            val ready = runCatching { engine.isReady() }.getOrDefault(false)
            val runtime = engine.hasRuntimeSetup && !ready
            val runtimeFile = if (runtime) SetupFile(SetupFileKind.ENGINE, engine.displayName, runCatching { engine.runtimeDownloadSize() }.getOrNull(), estimated = true) else null
            own.sortedBy { it != first }.flatMap { pkg -> voices(engine, pkg, code, name, ready, runtimeFile, switches = !speaking && engine.engine != current) }
        }
        return found.ifEmpty { listOf(settled(SetupKind.VOICE, name, speech.find(preferred ?: current)?.displayName.orEmpty(), SetupStatus.UNAVAILABLE)) }
    }

    /** The package's voices for the language: itself when it is one voice, else each voice it holds. */
    private fun voices(
        engine: LocalSpeechEngine, pkg: SpeechPackage, code: String, name: String, ready: Boolean,
        runtimeFile: SetupFile?, switches: Boolean,
    ): List<SetupItem> {
        val held = engine.packageVoices(pkg.id, code)
        val choices = held.ifEmpty { listOf(SpeechVoice(pkg.id, pkg.title, code)) }
        val download = SetupFile(if (held.isEmpty()) SetupFileKind.VOICE else SetupFileKind.VOICE_MODEL, pkg.title, pkg.sizeBytes.takeIf { it > 0 })
        return choices.map { voice ->
            val id = if (held.isEmpty()) "voice:${engine.engine.name}:${pkg.id}" else "voice:${engine.engine.name}:${pkg.id}:${voice.id}"
            val title = "${engine.displayName} \u00b7 ${voice.name}"
            if (pkg.installed && ready) {
                return@map SetupItem(id, SetupKind.VOICE, name, title, emptyList(), recommended = false, status = SetupStatus.INSTALLED, engine = engine.displayName, voiceSizeBytes = voice.sizeBytes)
            }
            val files = listOfNotNull(runtimeFile, download.takeIf { !pkg.installed })
            val item = SetupItem(
                id, SetupKind.VOICE, name, title, files,
                recommended = true, switchesEngine = switches, engine = engine.displayName, voiceSizeBytes = voice.sizeBytes.takeIf { held.isNotEmpty() },
            )
            register(item) {
                if (engine.hasRuntimeSetup && !engine.isReady()) engine.setUp()
                // Another voice of the same model may have fetched it already.
                if (engine.packages().none { it.id == pkg.id && it.installed }) engine.installPackage(pkg.id)
                val key = "${engine.engine.name}:$code"
                // The first voice chosen speaks the language, unless one was chosen before.
                settings.update { if (key in it.speechVoices) it else it.copy(speechVoices = it.speechVoices + (key to voice.id)) }
                if (!speaks(settings.current.speechEngine, code)) settings.update { it.copy(speechEngine = engine.engine) }
            }
        }
    }

    /** Whether [engine] can read [code] aloud with what is installed; the system voices are not counted on. */
    private suspend fun speaks(engine: SpeechEngine, code: String): Boolean {
        val local = speech.find(engine) ?: return false
        return runCatching { local.isReady() && local.voices(code).isNotEmpty() }.getOrDefault(false)
    }

    /** An installed voice, else a medium-quality one, else the first. */
    private fun pickVoice(own: List<SpeechPackage>): SpeechPackage =
        own.firstOrNull { it.installed } ?: own.firstOrNull { "medium" in it.title.lowercase() } ?: own.first()

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
