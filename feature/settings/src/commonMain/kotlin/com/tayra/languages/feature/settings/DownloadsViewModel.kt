package com.tayra.languages.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tayra.languages.core.domain.books.BookPack
import com.tayra.languages.core.domain.books.BookPackService
import com.tayra.languages.core.domain.books.BookPackStatus
import com.tayra.languages.core.domain.courses.CoursePack
import com.tayra.languages.core.domain.courses.CoursePackService
import com.tayra.languages.core.domain.courses.CoursePackStatus
import com.tayra.languages.core.domain.dictionary.DictionaryPack
import com.tayra.languages.core.domain.dictionary.PackState
import com.tayra.languages.core.domain.dictionary.PackStatus
import com.tayra.languages.core.domain.language.LanguageCodes
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.ocr.NoTextRecognizer
import com.tayra.languages.core.domain.ocr.OcrPack
import com.tayra.languages.core.domain.ocr.OcrReadiness
import com.tayra.languages.core.domain.ocr.TextRecognizer
import com.tayra.languages.core.domain.repository.LanguageRepository
import com.tayra.languages.core.domain.service.DictionaryService
import com.tayra.languages.core.domain.service.LanguageSetupService
import com.tayra.languages.core.domain.service.SetupItem
import com.tayra.languages.core.domain.service.SetupKind
import com.tayra.languages.core.domain.service.SetupState
import com.tayra.languages.core.domain.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Text recognition for the language being learned: whether a model is needed, and the models on the device. */
data class OcrDownloads(
    val readiness: OcrReadiness = OcrReadiness.Unsupported,
    val installed: List<OcrPack> = emptyList(),
    /** The share of the model downloaded so far while it downloads; null when the share is not known. */
    val downloading: Boolean = false,
    val progress: Float? = null,
    val error: String? = null,
)

/** Everything the Downloads page shows for the language being learned. */
data class DownloadsUiState(
    val language: Language? = null,
    /** The language's code, or null when the app does not know it. */
    val code: String? = null,
    val native: String = "en",
    val courses: List<CoursePackStatus> = emptyList(),
    val books: List<BookPackStatus> = emptyList(),
    /** The dictionaries from the language being learned into the native one, those on the device first. */
    val dictionaries: List<PackStatus> = emptyList(),
    /** The voices, translation and speech recognition models, as the language's setup lists them; null while they are looked up. */
    val tools: List<SetupItem>? = null,
    val ocr: OcrDownloads = OcrDownloads(),
)

/**
 * Every download for the language being learned, in one place: the course and book packs, the
 * dictionaries, the voices, the translation and speech recognition models, and the text
 * recognition models, each to download or remove.
 */
class DownloadsViewModel(
    settings: SettingsRepository,
    private val languages: LanguageRepository,
    private val coursePacks: CoursePackService,
    private val bookPacks: BookPackService,
    private val dictionaries: DictionaryService,
    private val setup: LanguageSetupService,
    private val recognizer: TextRecognizer = NoTextRecognizer,
) : ViewModel() {
    private val language = MutableStateFlow<Language?>(null)
    private val native = settings.settings.map { it.nativeLanguage }.distinctUntilChanged()
    private val tools = MutableStateFlow<List<SetupItem>?>(null)
    private val ocr = MutableStateFlow(OcrDownloads())
    private var loadingTools: Job? = null

    /** Where each voice, translation or recognition model being downloaded or removed is. */
    val progress: StateFlow<Map<String, SetupState>> = setup.states

    val state: StateFlow<DownloadsUiState> = combine(
        combine(language, native, tools, ocr) { language, native, tools, ocr -> DownloadsUiState(language, language?.let { LanguageCodes.codeFor(it.name) }, native, tools = tools, ocr = ocr) },
        coursePacks.packs,
        bookPacks.packs,
        dictionaries.packs,
    ) { base, courses, books, dictionaries ->
        val code = base.code
        base.copy(
            courses = courses.filter { it.pack.languageCode == code },
            books = books.filter { it.pack.languageCode == code },
            dictionaries = dictionaries
                .filter { it.pack.id.sourceLanguage == code && it.pack.id.targetLanguage == base.native && !it.pack.id.glossary }
                .sortedBy { it.state !is PackState.Installed },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DownloadsUiState())

    init {
        viewModelScope.launch {
            settings.settings.map { it.currentLanguageId }.distinctUntilChanged().collect { id ->
                language.value = languages.getById(id)
                reload()
            }
        }
        // A voice or model that finished downloading, or went, changes what the language has.
        viewModelScope.launch {
            var before = emptyMap<String, SetupState>()
            setup.states.collect { now ->
                val settled = before.any { (id, was) -> was !is SetupState.Done && (now[id] is SetupState.Done || id !in now) }
                before = now
                if (settled) loadTools()
            }
        }
    }

    private fun reload() {
        viewModelScope.launch {
            runCatching { coursePacks.refresh() }
            runCatching { bookPacks.refresh() }
            runCatching { dictionaries.refresh() }
        }
        loadTools()
        loadOcr()
    }

    private fun loadTools() {
        val current = language.value
        loadingTools?.cancel()
        if (current == null) {
            tools.value = emptyList()
            return
        }
        loadingTools = viewModelScope.launch {
            tools.value = try {
                setup.overview(current.id).filter { it.kind in TOOL_KINDS }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    private fun loadOcr() {
        val current = language.value ?: return
        viewModelScope.launch {
            val readiness = runCatching { recognizer.readiness(current) }.getOrDefault(OcrReadiness.Unsupported)
            val installed = runCatching { recognizer.installedPacks() }.getOrDefault(emptyList())
            ocr.update { it.copy(readiness = readiness, installed = installed) }
        }
    }

    fun download(pack: CoursePack) = viewModelScope.launch { coursePacks.download(pack) }
    fun remove(pack: CoursePack) = viewModelScope.launch { coursePacks.remove(pack) }
    fun download(pack: BookPack) = viewModelScope.launch { bookPacks.download(pack) }
    fun remove(pack: BookPack) = viewModelScope.launch { bookPacks.remove(pack) }
    fun download(pack: DictionaryPack) = viewModelScope.launch { dictionaries.download(pack) }
    fun remove(pack: DictionaryPack) = viewModelScope.launch { dictionaries.remove(pack) }

    /** Downloads a voice, a translation model or a speech recognition model, which goes on when the page is left. */
    fun install(item: SetupItem) = setup.install(listOf(item))

    fun canRemove(item: SetupItem): Boolean = setup.canRemove(item)

    fun remove(item: SetupItem) = setup.remove(item)

    fun downloadOcr() {
        val current = language.value ?: return
        if (ocr.value.downloading) return
        ocr.update { it.copy(downloading = true, progress = null, error = null) }
        viewModelScope.launch {
            val error = try {
                recognizer.download(current) { share -> ocr.update { it.copy(progress = share) } }
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.message ?: e.toString()
            }
            ocr.update { it.copy(downloading = false, progress = null, error = error) }
            loadOcr()
        }
    }

    fun removeOcr(pack: OcrPack) = viewModelScope.launch {
        recognizer.remove(pack)
        loadOcr()
    }

    private companion object {
        val TOOL_KINDS = setOf(SetupKind.VOICE, SetupKind.TRANSLATION, SetupKind.RECOGNITION)
    }
}
