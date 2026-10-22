package com.tayra.languages.feature.playlists

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import com.russhwolf.settings.MapSettings
import com.tayra.languages.core.data.db.DatabaseDriverFactory
import com.tayra.languages.core.data.db.DatabaseProvider
import com.tayra.languages.core.data.repository.BookRepositoryImpl
import com.tayra.languages.core.data.repository.CourseRepositoryImpl
import com.tayra.languages.core.data.repository.LanguageRepositoryImpl
import com.tayra.languages.core.data.repository.PlaylistRepositoryImpl
import com.tayra.languages.core.data.repository.TermRepositoryImpl
import com.tayra.languages.core.data.repository.WordsReadRepositoryImpl
import com.tayra.languages.core.data.settings.SettingsRepositoryImpl
import com.tayra.languages.core.domain.courses.Course
import com.tayra.languages.core.domain.courses.CourseLevel
import com.tayra.languages.core.domain.courses.CourseService
import com.tayra.languages.core.domain.courses.Lesson
import com.tayra.languages.core.domain.courses.SampleCourseSource
import com.tayra.languages.core.domain.model.BookDraft
import com.tayra.languages.core.domain.model.Language
import com.tayra.languages.core.domain.playlists.ListeningTime
import com.tayra.languages.core.domain.playlists.PlaylistPlayer
import com.tayra.languages.core.domain.playlists.PlaylistService
import com.tayra.languages.core.domain.playlists.SpeechOutput
import com.tayra.languages.core.domain.service.BookService
import com.tayra.languages.core.domain.service.LocalSpeech
import com.tayra.languages.core.domain.service.MemorySpeechAudioCache
import com.tayra.languages.core.domain.service.ReadingService
import com.tayra.languages.core.domain.service.SentenceAudio
import com.tayra.languages.core.domain.service.TermService
import com.tayra.languages.core.ui.components.AppTopBar
import com.tayra.languages.core.ui.components.LearningLanguageState
import com.tayra.languages.core.ui.components.LocalLearningLanguage
import com.tayra.languages.core.ui.components.NavSection
import com.tayra.languages.core.ui.components.ProvideWindowWidth
import com.tayra.languages.core.ui.theme.TayraTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.Collections
import javax.imageio.ImageIO

/** A voice that says nothing and finishes a sentence only when told, so a test sees the player mid-text. */
internal class SilentOutput : SpeechOutput {
    val heard: MutableList<String> = Collections.synchronizedList(mutableListOf())
    @Volatile var done: (() -> Unit)? = null
    @Volatile var onWord: ((IntRange) -> Unit)? = null

    override fun play(wav: ByteArray, onDone: () -> Unit) {
        done = onDone
    }

    override fun speak(text: String, languageCode: String?, rate: Float, onWord: (IntRange) -> Unit, onDone: () -> Unit) {
        heard += text
        this.onWord = onWord
        done = onDone
    }

    override fun pause() = true
    override fun resume() {}
    override fun stop() {
        done?.also { done = null }?.invoke()
    }

    fun finish() {
        done?.also { done = null }?.invoke()
    }
}

/** A Portuguese library: three books, a news article and a course of three lessons, with the playlist services over it. */
internal class PlaylistTestData {
    val provider = DatabaseProvider(DatabaseDriverFactory(File.createTempFile("tayra-playlists-ui", ".db").also { it.delete() }))
    val languages = LanguageRepositoryImpl(provider)
    val books = BookRepositoryImpl(provider)
    val terms = TermRepositoryImpl(provider)
    val settings = SettingsRepositoryImpl(MapSettings())
    val bookService = BookService(books, languages)
    val course = Course(
        "pt-test-compras", "pt", "Compras e serviços", "", CourseLevel.A2, "",
        listOf(
            Lesson("pt-test-compras-1", "O ar-condicionado em promoção", "", "\"Chamar o aparelho de porcaria não resolve nada\", disse ela ao marido. O que resolve é registrar o problema. ".repeat(6)),
            Lesson("pt-test-compras-2", "Uma compra com problema", "", "A caixa chegou amassada e o produto não funcionava. ".repeat(10)),
            Lesson("pt-test-compras-3", "O vídeo da Júlia", "", "A Júlia gravou um vídeo do barulho e escreveu à loja. ".repeat(12)),
        ),
    )
    val courses = CourseService(books, languages, bookService, CourseRepositoryImpl(provider), SampleCourseSource { code -> if (code == "pt") listOf(course) else emptyList() })
    val time = ListeningTime(settings)
    val repository = PlaylistRepositoryImpl(provider)
    val service = PlaylistService(repository, courses, languages, time)
    val reading = ReadingService(books, languages, terms, WordsReadRepositoryImpl(provider), TermService(terms, languages))
    val output = SilentOutput()
    val player = PlaylistPlayer(
        service, repository, books, languages, reading, SentenceAudio(LocalSpeech(emptyList()), settings, MemorySpeechAudioCache()), LocalSpeech(emptyList()), settings, output, time,
        CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1)),
    )

    val languageId: Long = runBlocking { languages.save(Language(name = "Portuguese")).also { id -> settings.update { it.copy(currentLanguageId = id) }; courses.seedSamples() } }

    fun book(title: String, text: String) = runBlocking { bookService.create(BookDraft(languageId = languageId, title = title, text = text)) }

    fun newsArticle(title: String, text: String) = runBlocking { book(title, text).also { books.linkNews("https://example.org/${title.hashCode()}", it) } }
}

@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.host(data: PlaylistTestData, title: String = "Playlists", content: @Composable () -> Unit) = setContent {
    TayraTheme {
        ProvideWindowWidth {
            CompositionLocalProvider(LocalLearningLanguage provides LearningLanguageState(listOf(data.languageId to "Portuguese"), data.languageId) {}) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column {
                        AppTopBar(title = title, onNavigate = {}, section = NavSection.PLAYLISTS)
                        content()
                    }
                }
            }
        }
    }
}

/** Writes what is on screen to [name].png in the folder PLAYLIST_SCREENSHOTS names, when it is set. */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.screenshot(name: String) {
    System.getenv("PLAYLIST_SCREENSHOTS")?.let { folder ->
        waitForIdle()
        ImageIO.write(onAllNodes(isRoot())[0].captureToImage().toAwtImage(), "png", File(folder, "$name.png"))
    }
}
